(ns helloworld.core
  (:require
   [om.next :as om :refer-macros [defui ui]]
   [sablono.core :as html :refer-macros [html]]
   [goog.dom :as gdom]
   [datascript.core :as d]
   [taoensso.sente :as sente :refer (cb-success?)]
   [taoensso.sente.packers.transit :as sente-transit])
  (:require-macros
   [cljs.core.async.macros :as asyncm :refer (go go-loop)]
   [taoensso.timbre :refer [log-env debugf debug spy]]))

(enable-console-print!)

;; ==============================================================================
;; Sente Init
(let [{:keys [chsk ch-recv send-fn state]}
      (sente/make-channel-socket! "/chsk"
                                  {:type :auto})]
  (def chsk chsk)
  (def ch-chsk ch-recv)
  (def chsk-send! send-fn)
  (def chsk-state state))

(defn event-msg-handler
  [{[event-type event-data :as event] :event}]
  )

(defonce router_ (atom nil))

(defn stop-router! [] (when-let [stop-f @router_] (stop-f)))

(defn start-router! []
  (stop-router!)
  (reset! router_
    (sente/start-chsk-router!
      ch-chsk event-msg-handler)))

(start-router!)

;; ==============================================================================
;; App State
(def conn (d/create-conn {}))

;; ==============================================================================
;; Initialize Data
(d/transact! conn
  [{:db/id -1
    :counter/foo 0}])

;; ==============================================================================
;; Parser
(defmulti read om/dispatch)

(defmethod read :app/foo
  [{:keys [state query]} _ _]
  {:value
   (d/q '[:find [(pull ?e ?selector) ...]
          :in $ ?selector
          :where [?e :counter/foo]]
     (d/db state) query)})

(defmethod read :app/baz
  [{:keys [state query]} _ _]
  {:remote true})

(defmethod read :app/bar
  [{:keys [state query] :as env} _ _]
  {:remote true
   :value
   (d/q '[:find [(pull ?e ?selector) ...]
          :in $ ?selector
          :where [?e :counter/bar]]
     (d/db state) query)})

(defmulti mutate om/dispatch)

(defmethod mutate 'foo/increment
  [_ _ entity]
  {:action (fn []
             (d/transact! conn
               [(update entity :counter/foo inc)]))})

(defmethod mutate 'bar/increment
  [_ _ entity]
  {:remote true})

;; ==============================================================================
;; Component
(defui HelloWorld
  static om/IQuery
  (query [this]
    [{:app/foo [:db/id :counter/foo]}
     {:app/bar [:db/id :counter/bar]}
     {:app/baz [:db/id :counter/baz]}])
  Object
  (render [this]
    (println (om/props this))
    (let [foo (get-in (om/props this) [:app/foo 0])
          bar (get-in (om/props this) [:app/bar 0])]
      (html [:div
             [:button {:on-click #(om/transact! this `[(foo/increment ~foo)])} "Foo +1"]
             [:button {:on-click #(om/transact! this `[(bar/increment ~bar)])} "Bar +1"]
             [:p (str "I'm Foo from datascript: " (:counter/foo foo))]
             [:p (str "I'm Bar from datomic: " (:counter/bar bar))]]))))

(def parser (om/parser {:read read
                        :mutate mutate}))

(defn sente-send
  [ast cb]
  (println @chsk-state)
  (let [f (fn []
            (println "send" [:om/query ast])
            (chsk-send! [:om/query ast]
              8000
              (fn [edn-reply]
                (spy edn-reply)
                (cb edn-reply))))]
    (if (true? (:open? @chsk-state))
      (f)
      (js/setTimeout
        f
        1000))))

(defn delta-key
  [d]
  (if (keyword? (first d))
    [d]
    (:keys (second d))))

(defn merge*
  [reconciler state delta query]
  (do 
    ;; (println "delta" delta (contains? delta :result))
    (doseq [[k v] delta]
      (if (keyword? k)
        (d/transact conn v)
        (d/transact conn (:result v))))
    {:keys (flatten (mapv delta-key delta))
     :next (d/db conn)
     :tempid nil}))

(def reconciler (om/reconciler {:state conn
                                :merge merge*
                                :parser parser
                                :send sente-send}))

(om/add-root! reconciler
  HelloWorld (gdom/getElement "app"))

