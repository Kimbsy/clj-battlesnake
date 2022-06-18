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
;; @TODO: we should be wary of positions that snake heads can move into
(defn avoid-hazards
  "Never move into positions with hazards or snakes."
  [moves board pos {:keys [width height] :as conf}]
  (let [{:keys [up down left right]} (common/cardinal-adjacent-positions pos)
        ;; in wrapped-mode we should check the other sides
        get-value-fn (if (wrapped? conf)
                       (fn [[x y]]
                          (board [(mod x width)
                                  (mod y height)]))
                       #(board %))]
    (cond-> moves
      (#{:S :H} (get-value-fn up)) (assoc :up ##-Inf)
      (#{:S :H} (get-value-fn down)) (assoc :down ##-Inf)
      (#{:S :H} (get-value-fn left)) (assoc :left ##-Inf)
      (#{:S :H} (get-value-fn right)) (assoc :right ##-Inf))))

(defn flood-fill
  [board pos spaces i]
  (when (pos? i)
    (when-not (spaces pos)
      ;; @TODO: could this also allow tails?
      ;; @TODO: should account for wrapped ruleset
      (when (#{:_ :f} (board pos))
        (apply conj spaces pos (mapcat
                                #(flood-fill board % (conj spaces pos) (dec i))
                                (vals (common/cardinal-adjacent-positions pos))))))))

(defn count-space
  [board cardinal-positions direction]
  [direction (count (flood-fill board (direction cardinal-positions) #{} 10))])

(defn prefer-space
  [moves board pos conf]
  (let [valid-options (map first (filter (comp pos? second) moves))
        cardinal-positions (common/cardinal-adjacent-positions pos)
        best (->> valid-options
                  (mapv (partial count-space board cardinal-positions))
                  (sort-by second)
                  last
                  first)]
    (update moves best * 2)))

(defn closest-food
  [board pos]
  (->> board
       (filter (fn [[p v]] (= :f v)))
       (map first)
       (sort-by (partial common/distance pos))
       first))

;; @TODO: this should really be based on the shortest available route to food, not just distance.
(defn find-food
  [moves board [x y :as pos] conf]
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
      (avoid-hazards board pos conf)
      (find-food board pos conf)
      (prefer-space board pos conf)))
