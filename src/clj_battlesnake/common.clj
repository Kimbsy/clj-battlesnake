(ns clj-battlesnake.common
  (:require [clojure.spec.alpha :as s]))

(s/def ::status int?)
(s/def ::body (s/map-of string? string?))
(s/def ::response
  (s/keys :req-un [::status
                   ::body]))

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
