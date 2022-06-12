(ns clj-battlesnake.common
  (:require [clojure.spec.alpha :as s]))

(s/def ::status int?)
(s/def ::body (s/map-of string? string?))
(s/def ::response
  (s/keys :req-un [::status
                   ::body]))

(defn vectorize
  [p]
  [(:x p) (:y p)])

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

(defn cardinal-adjacent-positions
  [[x y]]
  {:up [x (inc y)]
   :down [x (dec y)]
   :left [(dec x) y]
   :right [(inc x) y]})

(defn head-adjacent-positions
  [{{{:keys [x y]} :head} :you}]
  (cardinal-adjacent-positions [x y]))
