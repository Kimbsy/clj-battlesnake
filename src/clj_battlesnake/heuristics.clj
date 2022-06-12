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
      (common/at-top? req) (assoc :up ##-Inf)
      (common/at-bottom? req) (assoc :down ##-Inf)
      (common/at-left? req) (assoc :left ##-Inf)
      (common/at-right? req) (assoc :right ##-Inf))
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
      (hazard-positions up) (assoc :up ##-Inf)
      (hazard-positions down) (assoc :down ##-Inf)
      (hazard-positions left) (assoc :left ##-Inf)
      (hazard-positions right) (assoc :right ##-Inf))))

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
      (snake-positions up) (assoc :up ##-Inf)
      (snake-positions down) (assoc :down ##-Inf)
      (snake-positions left) (assoc :left ##-Inf)
      (snake-positions right) (assoc :right ##-Inf))))

(defn allowed?
  "Check that a position is not a snake, nor a hazard, nor out of
  bounds."
  [[init-x init-y :as pos]
   {{:keys [ruleset]} :game
    {:keys [height width hazards snakes]} :board}]
  (let [[x y] (cond-> pos
                (or (< init-x 0) (<= width init-x)) (update 0 mod width)
                (or (< init-y 0) (<= height init-y)) (update 1 mod height))]
    (and (and (<= 0 x)
              (<= 0 y)
              (< x width)
              (< y height))
         (let [hazard-positions (->> hazards
                                     (map common/vectorize)
                                     set)
               snake-positions (->> snakes
                                    (mapcat :body)
                                    (map common/vectorize)
                                    set)
               disallowed-positions (s/union hazard-positions snake-positions)]
           (not (disallowed-positions [x y]))))))

(defn flood-fill
  [req spaces pos i]
  (when (pos? i)
    (when-not (spaces pos)
      ;; we should really check if this pos is a tail
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
    [dir-kv (count (flood-fill req #{} starting-pos 7))]))

(defn prefer-space
  "Try not to get trapped in dead ends by preferring larger contiguous
  areas."
  [moves
   req]
  (let [valid-options (filter (comp pos? second) moves)
        head-pos (common/head-adjacent-positions req)]
    (let [worst (->> valid-options
                    (map #(count-space % req))
                    (sort-by second)
                    first
                    ffirst)]
      (update moves worst - 50))))

(defn find-food
  [moves
   {{:keys [food]} :board
    {:keys [head]} :you
    :as req}]
  (reduce (fn [acc fd]
            (cond-> acc
              (< (:y head) (:y fd)) (update :up + (max 0 (- 20 (* 2 (common/distance (common/vectorize head) (common/vectorize fd))))))
              (> (:y head) (:y fd)) (update :down + (max 0 (- 20 (* 2 (common/distance (common/vectorize head) (common/vectorize fd))))))
              (> (:x head) (:x fd)) (update :left + (max 0 (- 20 (* 2 (common/distance (common/vectorize head) (common/vectorize fd))))))
              (< (:x head) (:x fd)) (update :right + (max 0 (- 20 (* 2 (common/distance (common/vectorize head) (common/vectorize fd))))))))
          moves
          food))

(def active-heuristics
  [avoid-walls
   avoid-hazards
   avoid-snakes
#_   prefer-space
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
