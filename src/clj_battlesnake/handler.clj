(ns clj-battlesnake.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [ring.util.response :refer [response]]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.set :as s]))

(defn get-handler
  [_]
  (response {"apiversion" "1"
             "author" "Kimbsy"
             "color" "#000000"
             "head" "beluga"
             "tail" "round-bum"
             "version" "1.0.1"}))

(defn start-handler
  [req]
  (response nil))

(defn end-handler
  [req]
  (response nil))

(defn at-top?
  [req]
  (= (get-in req [:you :head :y])
     (dec (get-in req [:board :height]))))

(defn at-bottom?
  [req]
  (= (get-in req [:you :head :y])
     0))

(defn at-left?
  [req]
  (= (get-in req [:you :head :x])
     0))

(defn at-right?
  [req]
  (= (get-in req [:you :head :x])
     (dec (get-in req [:board :width]))))

(defn apply-heuristics
  [moves req heuristics]
  (reduce (fn [acc heuristic]
            (heuristic acc req))
          moves
          heuristics))

(defn avoid-walls
  [moves req]
  (cond-> moves
    (at-top? req) (assoc :up 0)
      (at-bottom? req) (assoc :down 0)
      (at-left? req) (assoc :left 0)
      (at-right? req) (assoc :right 0)))

(defn avoid-snakes
  [moves
   {{:keys [snakes]} :board
    {:keys [head]} :you
    :as req}]
  (let [{:keys [x y]} head
        u [x (inc y)]
        d [x (dec y)]
        l [(dec x) y]
        r [(inc x) y]
        snake-positions (->> snakes
                             (mapcat #(get % :body))
                             (map (fn [p] [(get p :x) (get p :y)]))
                             set)]
    (cond-> moves
      (snake-positions u) (assoc :up 0)
      (snake-positions d) (assoc :down 0)
      (snake-positions l) (assoc :left 0)
      (snake-positions r) (assoc :right 0))))

(defn find-food
  [moves
   {{:keys [id ruleset map source timeout] :as game} :game
    {:keys [height width food hazards snakes] :as board} :board
    {:keys [shout body health id name length head customizations latency squad] :as you} :you
    :keys [turn] :as req}]
  (reduce (fn [acc fd]
            (cond-> acc
              (< (get head :y) (get fd :y)) (update :up + 10)
              (> (get head :y) (get fd :y)) (update :down + 10)
              (> (get head :x) (get fd :x)) (update :left + 10)
              (< (get head :x) (get fd :x)) (update :right + 10)))
          moves
          food))

(def base-moves
  {:up 100
   :down 100
   :left 100
   :right 100})

(defn move-handler
  [{{:keys [id ruleset map source timeout] :as game} :game
    {:keys [height width food hazards snakes] :as board} :board
    {:keys [shout body health id length head customizations latency squad] :as you} "you"
    :keys [turn] :as req}]
  (let [results (apply-heuristics base-moves req [avoid-walls
                                                  avoid-snakes
                                                  find-food])
        valid-options (remove (comp zero? second) results)]
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
  (GET "/" req (get-handler (:body req)))
  (POST "/start"req (start-handler (:body req)))
  (POST "/end" req (end-handler (:body req)))
  (POST "/move" req (move-handler (:body req)))
  (route/not-found "Error, not found!\n"))

(def app
  (-> app-routes
      (wrap-json-body {:keywords? true})
      wrap-json-response))

;; curl -XPOST http://clj-battlesnake-dev.us-west-2.elasticbeanstalk.com/move --header "Content-type:application/json" -d "$(cat resources/example-request.json)"
