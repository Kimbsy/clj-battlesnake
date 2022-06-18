(ns clj-battlesnake.heuristics
  (:require [clj-battlesnake.common :as common]
            [clojure.set :as s]))

(defn wrapped?
  [{:keys [ruleset]}]
  (= "wrapped" (:name ruleset)))

(defn avoid-walls
  "If we're using the `wrapped` ruleset we shouldn't care about
  walls."
  [moves board [x y] {:keys [width height] :as conf}]
  (if-not (wrapped? conf)
    (cond-> moves
      (= (dec height) y) (assoc :up ##-Inf)
      (= 0 y) (assoc :down ##-Inf)
      (= 0 x) (assoc :left ##-Inf)
      (= (dec width) x) (assoc :right ##-Inf))
    moves))

;; @TODO: we should be allowed to move into positions that are snake tails
(defn avoid-hazards
  "Never move into positions with hazards or snakes."
  [moves board pos]
  (let [{:keys [up down left right]} (common/cardinal-adjacent-positions pos)]
    (cond-> moves
      (= #{:S :H} (board up)) (assoc :up ##-Inf)
      (= #{:S :H} (board down)) (assoc :down ##-Inf)
      (= #{:S :H} (board left)) (assoc :left ##-Inf)
      (= #{:S :H} (board right)) (assoc :right ##-Inf))))

#_(defn allowed?
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

;; (defn flood-fill
;;   [req spaces pos i]
;;   (when (pos? i)
;;     (when-not (spaces pos)
;;       ;; we should really check if this pos is a tail
;;       (when (allowed? pos req)
;;         (apply conj spaces pos (mapcat #(flood-fill req (conj spaces pos) % (dec i))
;;                                        (vals (common/cardinal-adjacent-positions pos))))))))

;; (defn count-space
;;   [[direction _ :as dir-kv]
;;    ;; This _should_ take into account `wrapped` rulesets
;;    {{:keys [head] :as you} :you
;;     :as req}]
;;   ;; do a flood-fill for a direction from the head, only consider
;;   ;; cardinal adjacency each time
;;   (let [adjacent-positions (common/head-adjacent-positions req)
;;         starting-pos (direction adjacent-positions)]
;;     [dir-kv (count (flood-fill req #{} starting-pos 7))]))

;; (defn prefer-space
;;   "Try not to get trapped in dead ends by preferring larger contiguous
;;   areas."
;;   [moves
;;    req]
;;   (let [valid-options (filter (comp pos? second) moves)
;;         head-pos (common/head-adjacent-positions req)]
;;     (let [worst (->> valid-options
;;                     (map #(count-space % req))
;;                     (sort-by second)
;;                     first
;;                     ffirst)]
;;       (update moves worst - 50))))

(defn closest-food
  [board pos]
  (->> board
       (filter (fn [[p v]] (= :f v)))
       (map first)
       (sort-by (partial common/distance pos))
       first))

(defn find-food
  [moves board [x y :as pos]]
  (if-let [[fx fy] (closest-food board pos)]
    (cond-> moves
      (< y fy) (update :up * 1.5)
      (< fy y) (update :down * 1.5)
      (< fx x) (update :left * 1.5)
      (< x fx) (update :right * 1.5))
    moves))

;; we should be able to turn these on and off depending on e.g. we're low on energy, we're bigger than other snakes etc.
(defn apply-heuristics
  [moves board pos conf]
  (-> moves
      (avoid-walls board pos conf)
      (avoid-hazards board pos)
      (find-food board pos)))
