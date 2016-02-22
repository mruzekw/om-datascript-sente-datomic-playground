(ns helloworld.systems
  (:require [helloworld.core :as helloworld]
            [system.core :refer [defsystem]]
            [system.components
             [http-kit :refer [new-web-server]]
             [datomic :refer [new-datomic-db]]
             [sente :refer [new-channel-sockets]]]
            [dev.datomic
             :refer [new-mem-datomic-db]]
            [taoensso.sente.server-adapters.http-kit
             :refer [sente-web-server-adapter]]
            [figwheel-sidecar.system :as fw]))

(defsystem dev-system
  [:figwheel-system (fw/figwheel-system (fw/fetch-config))
   :datomic (new-mem-datomic-db "datomic:mem://helloworld")
   :sente (new-channel-sockets #'helloworld/event-msg-handler* sente-web-server-adapter)])

