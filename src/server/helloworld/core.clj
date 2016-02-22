(ns helloworld.core
  (:require [compojure.core :refer [defroutes GET POST]]
            [hiccup.core :as html :refer [html]]
            [hiccup.page :refer [include-js]]
            [reloaded.repl :refer [system]]
            [datomic.api :as d]
            [om.next.server :as om]
            [ring.util.response :refer [response]]
            [ring.middleware
             [keyword-params :refer [wrap-keyword-params]]
             [params :refer [wrap-params]]
             [session :refer [wrap-session]]]
            [taoensso.timbre :refer [debug spy log-env]]))

;; ==============================================================================
;; Datomic Initialize
(defn datomic-conn []
  (-> system :datomic :conn))

;; ==============================================================================
;; Parser
(defmulti readf (fn [env k params] k))

(defmethod readf :default
  [_ k _]
  {:value {:error (str "No handler for such key: " k)}})

(defmethod readf :app/bar
  [{:keys [conn query]} _ _]
  (debug query)
  {:value (d/q '[:find [(pull ?e selector) ...]
                 :in $ selector
                 :where [?e :counter/bar]]
            (d/db conn)
            query)})

(defmethod readf :app/baz
  [{:keys [conn query]} _ _]
  {:value [{:db/id 1 :counter/baz 100}]})

(defmulti mutatef (fn [env k params] k))

(defmethod mutatef :default
  [_ k _]
  {:value {:error (str "No handler for such key: " k)}})

;; (defmethod mutatef 'bar/increment
;;   [{:keys [conn query]} _ entity]
;;   (spy entity)
;;   (spy (update entity :counter/bar inc))
;;   (when
;;       {:value {:keys [:app/bar]}
;;        :action (fn []
;;                  @(d/transact conn
;;                     [(spy (update entity :counter/bar inc))]))}))

(defmethod mutatef 'bar/increment
  [{:keys [conn query]} _ entity]
  {:value {:keys [:app/bar :app/baz]}
   :action (fn []
             (do @ (d/transact conn
                     [[:db/add (:db/id entity) :counter/bar (inc (:counter/bar entity))]])
                   [(d/pull (d/db conn) '[*] (:db/id entity))]))})

(defn index-handler [req]
  (response
   (html
    [:html
     [:head]
     [:body
      [:div#app]
      (include-js "js/app.js")]])))

(defroutes route
  (GET "/" req index-handler)
  (GET "/chsk" req ((-> system :sente :ring-ajax-get-or-ws-handshake) req))
  (POST "/chsk" req ((-> system :sente :ring-ajax-post) req)))

(defonce app (-> #'route
               wrap-keyword-params
               wrap-params
               wrap-session))

(defn event-msg-handler*
  [{[event-type event-data :as event] :event ?reply-fn :?reply-fn :as msg}]
  (when (= :om/query)
    (debug event-type event-data)
    (let [data ((om/parser {:read readf
                            :mutate mutatef})
                {:conn (-> system :datomic :conn)}
                (:remote event-data))]
      (when ?reply-fn
        (debug "going to reply" data)
        (?reply-fn data)))))



