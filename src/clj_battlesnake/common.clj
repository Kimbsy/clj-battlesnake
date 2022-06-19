(ns clj-battlesnake.common
  (:require [clojure.spec.alpha :as s]
            [cheshire.core :as json]
            [clojure.java.io :as io]))

(comment
  ;; full destructuring map for request object
  {{:keys [id ruleset map source timeout] :as game} :game
   {:keys [height width food hazards snakes] :as game-board} :board
   {:keys [shout body health id name length head customizations latency squad] :as you} :you
   :keys [turn] :as req})

(s/def ::status int?)
(s/def ::body (s/map-of string? string?))
(s/def ::response
  (s/keys :req-un [::status
                   ::body]))

(defn wrapped?
  [{:keys [ruleset]}]
  (= "wrapped" (:name ruleset)))

(defn wrap-pos
  [[x y] {:keys [width height] :as foo}]
  [(mod x width)
   (mod y height)])

(defn cardinal-adjacent-positions
  [[x y]]
  {:up [x (inc y)]
   :down [x (dec y)]
   :left [(dec x) y]
   :right [(inc x) y]})

(defn distance
  [[x1 y1] [x2 y2]]
  (let [dx (- x2 x1)
        dy (- y2 y1)]
    (Math/sqrt (+ (Math/pow dx 2)
                  (Math/pow dy 2)))))

(defn parse-example-request
  [filename]
  (-> filename
      io/resource
      slurp
      (json/parse-string keyword)))

(defn parse-board
  "Creates a canonical data structure representing the board specified
  in the request."
  [{{:keys [id ruleset source timeout] game-map :map :as game} :game
    {:keys [height width food hazards snakes] :as game-board} :board
    {:keys [shout body health id length head customizations latency squad] you-name :name :as you} :you
    :keys [turn] :as req}]
  (let [;; initial board
        board (into {}
                    (for [x (range width)
                          y (range height)]
                      [[x y] :_]))

        ;; add food
        board (reduce (fn [acc {:keys [x y]}]
                        (assoc acc [x y] :f))
                      board
                      food)

        ;; add hazards
        board (reduce (fn [acc {:keys [x y]}]
                        (assoc acc [x y] :H))
                      board
                      hazards)

        ;; add snakes
        board (reduce (fn [acc {s-body :body}]
                        (reduce (fn [accc {:keys [x y]}]
                                  (assoc accc [x y] :S))
                                acc
                                s-body))
                      board
                      snakes)

        ;; mark enemy snake heads
        board (reduce (fn [acc {{:keys [x y] :as s-head} :head}]
                        (if (not= head s-head)
                          (reduce (fn [accc pos]
                                    (let [pos (if (wrapped? game)
                                                (wrap-pos pos game-board)
                                                pos)]
                                      (update accc pos (fn [existing]
                                                         (if (#{:_ :f} existing)
                                                           :h
                                                           existing)))))
                                  acc
                                  (vals (cardinal-adjacent-positions [x y])))
                          acc))
                      board
                      snakes)]

    {:board board
     :pos [(:x head) (:y head)]
     :conf {:width width
            :height height
            :ruleset ruleset}}))

(defn vec->map
  [v]
  (into {}
        (for [i (range (count v))
              j (range (count (first v)))]
          [[i j] (get-in (vec (reverse v)) [j i])])))

(defn map->vec
  [m]
  (->> m
       sort
       (map second)
       (partition 11)
       (apply mapv vector)
       reverse
       vec))

(defn print-vec
  [v]
  (newline)
  (newline)
  (doseq [row v]
    (println row)))

(defn print-map
  [m]
  (print-vec (map->vec m)))
