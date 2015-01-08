(ns twimap-server.core
  (:use [compojure.core]
        [ring.util.response]
        [ring.middleware.content-type]
        [ring.middleware.params])
  (:require [ring.adapter.jetty :as jetty]
            [cheshire.core :as json]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [twimap-server.twitter :as twitter])
  (:gen-class :main :true))

(defn json-response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/json; charset=utf-8"}
   :body (json/generate-string data)})

(defroutes api-routes
  (GET "/twitter/search/:word" [word]
       (json-response (twitter/search-keyword word)))
  (route/resources "/")
  (route/not-found "page not found"))

(def app (-> api-routes
             wrap-params
             wrap-content-type
             handler/site))

(defn -main
  [& args]
  (let [port (or (first *command-line-args*) 8080)]
    (jetty/run-jetty app {:port port})))
