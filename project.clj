(defproject twimap-server "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [ring/ring-core "1.3.2"]
                 [ring/ring-jetty-adapter "1.3.2"]
                 [compojure "1.3.1"]
                 [cheshire "5.4.0"]
                 [org.twitter4j/twitter4j-core "4.0.2"]
                 [com.novemberain/monger "2.0.0"]
                 [clj-time "0.9.0"]]
  :plugins [[lein-ring "0.8.13"]]
  :main twimap-server.core
  :aot [twimap-server.core]
  :ring {:handler twimap-server.core/app}
  :profiles {:dev {:dependencies [[ring/ring-mock "0.2.0"]]}})
