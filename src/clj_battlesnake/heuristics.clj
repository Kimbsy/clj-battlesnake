(ns clj-battlesnake.heuristics
  (:require [clj-battlesnake.common :as common]))

(defn avoid-walls
  [moves
   {{:keys [ruleset]} :game :as req}]
  (if-not (= "wrapped" (:name ruleset))
    (cond-> moves
      (common/at-top? req) (assoc :up 0)
      (common/at-bottom? req) (assoc :down 0)
      (common/at-left? req) (assoc :left 0)
      (common/at-right? req) (assoc :right 0))
    moves))

(defn avoid-hazards
  [moves
   {{:keys [hazards]} :board
    {:keys [head]} :you
    :as req}]
  (let [{:keys [x y]} head
        {:keys [u d l r]} (common/cardinal-adjacent-positions req)
        hazard-positions (->> hazards
                              (map common/vectorize)
                              set)]
    (cond-> moves
      (hazard-positions u) (assoc :up 0)
      (hazard-positions d) (assoc :down 0)
      (hazard-positions l) (assoc :left 0)
      (hazard-positions r) (assoc :right 0))))

(defn avoid-snakes
  [moves
   {{:keys [snakes]} :board
    {:keys [head]} :you
    :as req}]
  (let [{:keys [x y]} head
        {:keys [u d l r]} (common/cardinal-adjacent-positions req)
        snake-positions (->> snakes
                             (mapcat :body)
                             (map common/vectorize)
                             set)]
    (cond-> moves
      (snake-positions u) (assoc :up 0)
      (snake-positions d) (assoc :down 0)
      (snake-positions l) (assoc :left 0)
      (snake-positions r) (assoc :right 0))))

(defn find-food
  [moves
   {{:keys [food]} :board
    {:keys [head]} :you
    :as req}]
  (reduce (fn [acc fd]
            (cond-> acc
              (< (:y head) (:y fd)) (update :up + 10)
              (> (:y head) (:y fd)) (update :down + 10)
              (> (:x head) (:x fd)) (update :left + 10)
              (< (:x head) (:x fd)) (update :right + 10)))
          moves
          food))

(defn prefer-space
  [moves
   req]
  moves)

(def active-heuristics
  [avoid-walls
   avoid-hazards
   avoid-snakes
   find-food
   prefer-space])

(defn apply-heuristics
  [req moves]
  (reduce (fn [acc heuristic]
            (heuristic acc req))
          moves
          active-heuristics))

(comment
  ;; full destructuring map for request object
  {{:keys [id ruleset map source timeout] :as game} :game
   {:keys [height width food hazards snakes] :as board} :board
   {:keys [shout body health id name length head customizations latency squad] :as you} "you"
   :keys [turn] :as req})
