(ns clj-battlesnake.heuristics
  (:require [clj-battlesnake.common :as common]
            [clojure.set :as s]))

(defn avoid-walls
  "If we're using the `wrapped` ruleset we shouldn't care about
  walls."
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
        {:keys [up down left right]} (common/head-adjacent-positions req)
        hazard-positions (->> hazards
                              (map common/vectorize)
                              set)]
    (cond-> moves
      (hazard-positions up) (assoc :up 0)
      (hazard-positions down) (assoc :down 0)
      (hazard-positions left) (assoc :left 0)
      (hazard-positions right) (assoc :right 0))))

(defn avoid-snakes
  [moves
   {{:keys [snakes]} :board
    {:keys [head]} :you
    :as req}]
  (let [{:keys [x y]} head
        {:keys [up down left right]} (common/head-adjacent-positions req)
        snake-positions (->> snakes
                             (mapcat :body)
                             (map common/vectorize)
                             set)]
    (cond-> moves
      (snake-positions up) (assoc :up 0)
      (snake-positions down) (assoc :down 0)
      (snake-positions left) (assoc :left 0)
      (snake-positions right) (assoc :right 0))))

(defn allowed?
  "Check that a position is not a snake, nor a hazard, nor out of
  bounds."
  [[x y]
   {{:keys [height width hazards snakes]} :board}]
  (and (<= 0 x)
       (<= 0 y)
       (< x width)
       (< y height)
       (let [hazard-positions (->> hazards
                                   (map common/vectorize)
                                   set)
             snake-positions (->> snakes
                                  (mapcat :body)
                                  (map common/vectorize)
                                  set)
             disallowed-positions (s/union hazard-positions snake-positions)]
         (not (disallowed-positions [x y])))))

(defn flood-fill
  [req spaces pos i]
  (when (pos? i)
    (when-not (spaces pos)
      (when (allowed? pos req)
        (apply conj spaces pos (mapcat #(flood-fill req (conj spaces pos) % (dec i))
                                       (vals (common/cardinal-adjacent-positions pos))))))))

(defn count-space
  [[direction _ :as dir-kv]
   ;; This _should_ take into account `wrapped` rulesets
   {{:keys [head] :as you} :you
    :as req}]
  ;; do a flood-fill for a direction from the head, only consider
  ;; cardinal adjacency each time
  (let [adjacent-positions (common/head-adjacent-positions req)
        starting-pos (direction adjacent-positions)]
    [dir-kv (count (flood-fill req #{} starting-pos 10))]))

(defn prefer-space
  "Try not to get trapped in dead ends by preferring larger contiguous
  areas."
  [moves
   req]
  (let [valid-options (filter (comp pos? second) moves)
        head-pos (common/head-adjacent-positions req)]
    (if (= 2 (count valid-options))
      (let [[worst best] (->> valid-options
                              (map #(count-space % req))
                              (sort-by second))]
        (-> moves
            (update (ffirst worst) - 50)
            (update (ffirst best) + 50)))
      moves)))

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

(def active-heuristics
  [avoid-walls
   avoid-hazards
   avoid-snakes
   prefer-space
   find-food])

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
   {:keys [shout body health id name length head customizations latency squad] :as you} :you
   :keys [turn] :as req})
