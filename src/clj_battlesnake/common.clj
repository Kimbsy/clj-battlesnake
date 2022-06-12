(ns clj-battlesnake.common
  (:require [clojure.spec.alpha :as s]))

(s/def ::status int?)
(s/def ::body (s/map-of string? string?))
(s/def ::response
  (s/keys :req-un [::status
                   ::body]))
