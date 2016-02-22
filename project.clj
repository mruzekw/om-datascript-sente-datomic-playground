(defproject helloworld "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.170"]
                 [org.clojure/core.async "0.2.374"
                  :exclusions [org.clojure/tools.reader]]
                 [datascript "0.15.0"]
                 [cljsjs/react "0.14.3-0"]
                 [cljsjs/react-dom "0.14.3-1"]
                 [sablono "0.6.2"]
                 [org.omcljs/om "1.0.0-alpha30"]
                 [org.danielsz/system "0.3.0-SNAPSHOT"]
                 [hiccup "1.0.5"]
                 [http-kit "2.1.19"]
                 [com.taoensso/sente "1.7.0"]
                 [com.datomic/datomic-free "0.9.5206"
                  :exclusions [joda-time]]
                 [figwheel-sidecar "0.5.0"]
                 [compojure "1.4.0"]
                 [bidi "1.25.1"]]
  :plugins [[lein-cljsbuild "1.1.2"
             :exclusions [org.clojure/clojure]]]
  
  :clean-targets ^{:protect false} ["resources/public/js" "target"]
  
  :source-paths ["src/server" "env"]
  
  :figwheel
  {:http-server-port 3449
   :http-server-root "public"
   :ring-handler helloworld.core/app
   }

  :cljsbuild
  {:builds [{:id "dev"
             :source-paths ["src/client"]
             :figwheel true
             :compiler {:main helloworld.core
                        :asset-path "js/out"
                        :output-to "resources/public/js/app.js"
                        :output-dir "resources/public/js/out"}}]})
