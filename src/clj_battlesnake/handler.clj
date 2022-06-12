(ns clj-battlesnake.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [ring.util.response :refer [response]]
            [clj-battlesnake.common :as common]
            [clj-battlesnake.heuristics :as h]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.set :as s]))

(def base-moves
  {:up    100
   :down  100
   :left  100
   :right 100})

(defn info-handler
  [_]
  (response {"apiversion" "1"
             "author"     "Kimbsy"
             "color"      "#000000"
             "head"       "beluga"
             "tail"       "round-bum"
             "version"    "1.0.1"}))

(defn start-handler
  [req]
  (response nil))

(defn end-handler
  [req]
  (response nil))

(defn move-handler
  [req]
  (let [results (h/apply-heuristics req base-moves)
        valid-options (filter (comp pos? second) results)]
    (when (seq valid-options)
      (response {"move" (->> valid-options
                             (group-by second)
                             (sort-by first)
                             last
                             second
                             rand-nth
                             first
                             name)}))))

(defroutes app-routes
  (GET "/" req (info-handler (:body req)))
  (POST "/start"req (start-handler (:body req)))
  (POST "/end" req (end-handler (:body req)))
  (POST "/move" req (move-handler (:body req)))
  (route/not-found "Error, not found!\n"))

(def app
  (-> app-routes
      (wrap-json-body {:keywords? true})
      wrap-json-response))

;; curl -XPOST http://clj-battlesnake-dev.us-west-2.elasticbeanstalk.com/move --header "Content-type:application/json" -d "$(cat resources/example-request.json)"
