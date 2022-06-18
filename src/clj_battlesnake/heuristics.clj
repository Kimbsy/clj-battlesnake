(ns clj-battlesnake.heuristics
  (:require [clj-battlesnake.common :as common]
            [clojure.set :as s]))

(defn wrapped?
  [{:keys [ruleset]}]
  (= "wrapped" (:name ruleset)))

(defn tile-fn
  [board {:keys [width height] :as conf}]
  (if (wrapped? conf)
    (fn [[x y]]
      (board [(mod x width)
              (mod y height)]))
    #(board %)))

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
        get-tile (tile-fn board conf)]
    (cond-> moves
      (#{:S :H} (get-tile up)) (assoc :up ##-Inf)
      (#{:S :H} (get-tile down)) (assoc :down ##-Inf)
      (#{:S :H} (get-tile left)) (assoc :left ##-Inf)
      (#{:S :H} (get-tile right)) (assoc :right ##-Inf))))

(defn wrap-pos
  [[x y] {:keys [width height]}]
  [(mod x width)
   (mod y height)])

(defn flood-fill
  [board pos {:keys [width height] :as conf} spaces i]
  (when (pos? i)
    (when-not (spaces pos)
      ;; @TODO: could this also allow tails?
      (let [pos (if (wrapped? conf)
                  (wrap-pos pos conf)
                  pos)]
        (when (#{:_ :f} (board pos))
          (apply conj
                 spaces
                 pos
                 (mapcat
                  #(flood-fill board % conf (conj spaces pos) (dec i))
                  (vals (common/cardinal-adjacent-positions pos)))))))))

;; @TODO: this is naiive, we should record number of tails, heads etc. score for tail should be realative to number of spaces.
(def tile-score
  {:_ 1
   :f 2})

(defn score-space
  [board pos conf]
  (->> (flood-fill board pos conf #{} 10)
       (map board)
       (map tile-score)
       (reduce +)))

(defn prefer-valuable-area
  [moves board pos conf]
  (let [valid-options (map first (filter (comp pos? second) moves))
        cardinal-positions (common/cardinal-adjacent-positions pos)
        scores (map (fn [direction]
                      [direction (score-space board
                                              (direction cardinal-positions)
                                              conf)])
                    valid-options)]
    (reduce (fn [acc [direction score]]
              (update acc direction * (+ 1 (/ score 10))))
            moves
            scores)))

;; @TODO: we should be able to turn these on and off depending on e.g. we're low on energy, we're bigger than other snakes etc.
(defn apply-heuristics
  [moves board pos conf]
  (-> moves
      (avoid-walls board pos conf)
      (avoid-hazards board pos conf)
      (prefer-valuable-area board pos conf)))
