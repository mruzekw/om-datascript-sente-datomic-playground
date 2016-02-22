(ns user
  (:require [reloaded.repl :refer [system start stop go reset init]]
            [figwheel-sidecar.system :as fw]
            [helloworld.systems :refer [dev-system]]
            [helloworld.core]))

(reloaded.repl/set-init! dev-system)

(defn cljs-repl
  "Launch fighweel CLJS repl"
  []
  (fw/cljs-repl (:figwheel-system system)))

