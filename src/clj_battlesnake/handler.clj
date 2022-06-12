(ns clj-battlesnake.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [ring.util.response :refer [response]]
            [cheshire.core :as json]
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
  [head height]
  (= (dec height) (get head "y")))

(defn at-bottom?
  [head height]
  (= 0 (get head "y")))

(defn at-left?
  [head width]
  (= 0 (get head "x")))

(defn at-right?
  [head width]
  (= (dec width) (get head "x")))

(defn apply-modifications
  [moves mods]
  (reduce (fn [acc [k f]]
            (update acc k f))
          moves
          mods))

(defn apply-heuristics
  [moves req heuristics]
  (reduce (fn [acc {:keys [pred mods]}]
            (if (pred req)
              (apply-modifications acc mods)
              acc))
          moves
          heuristics))

(def dont-hit-top
  {:pred (fn [req]
           (at-top? (get-in req ["you" "head"])
                    (get-in req ["board" "height"])))
   :mods {"top" #(* 0 %)}})

(def dont-hit-bottom
  {:pred (fn [req]
           (at-bottom? (get-in req ["you" "head"])
                       (get-in req ["board" "height"])))
   :mods {"down" #(* 0 %)}})

(def dont-hit-left
  {:pred (fn [req]
           (at-left? (get-in req ["you" "head"])
                     (get-in req ["board" "width"])))
   :mods {"left" #(* 0 %)}})

(def dont-hit-right
  {:pred (fn [req]
           (at-right? (get-in req ["you" "head"])
                      (get-in req ["board" "width"])))
   :mods {"right" #(* 0 %)}})

(def base-moves
  {"up" 100
   "down" 100
   "left" 100
   "right" 100})

(defn move-handler
  [{{:strs [id ruleset map source timeout] :as game} "game"
    {:strs [height width food hazards snakes] :as board} "board"
    {:strs [shout body health id name length head customizations latency squad] :as you} "you"
    :strs [turn] :as req}]

  (let [results (apply-heuristics base-moves req [dont-hit-top
                                                  dont-hit-bottom
                                                  dont-hit-left
                                                  dont-hit-right])]
    (response {"move" (->> results
                           (remove (comp zero? second))
                           keys
                           rand-nth)})))

(defroutes app-routes
  (GET "/" req (get-handler (:body req)))
  (POST "/start"req (start-handler (:body req)))
  (POST "/end" req (end-handler (:body req)))
  (POST "/move" req (move-handler (:body req)))
  (route/not-found "Error, not found!\n"))

(def app
  (-> app-routes
      ;; (wrap-defaults site-defaults)
      wrap-json-body
      wrap-json-response))

;; curl -XPOST localhost:3000/move --header "Content-type:application/json" -d "$(cat resources/example-request.json)"
