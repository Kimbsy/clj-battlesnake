(ns clj-battlesnake.handler-test
  (:require [clojure.test :refer :all]
            [clojure.spec.alpha :as s]
            [clj-battlesnake.handler :refer :all]
            [clj-battlesnake.common :as common]
            [clojure.java.io :as io]
            [cheshire.core :as json]))

(def example-map
  {[4 3] :H,
   [2 2] :S,
   [0 0] :S,
   [1 0] :S,
   [2 3] :_,
   [3 3] :H,
   [1 1] :_,
   [3 4] :_,
   [4 2] :S,
   [3 0] :_,
   [4 1] :_,
   [1 4] :_,
   [1 3] :_,
   [0 3] :f,
   [2 4] :_,
   [0 2] :_,
   [2 0] :S,
   [0 4] :f,
   [3 1] :_,
   [2 1] :_,
   [4 4] :H,
   [1 2] :_,
   [3 2] :S,
   [0 1] :_,
   [4 0] :f})

(def example-vector
  [[:f :_ :_ :_ :H]
   [:f :_ :_ :H :H]
   [:_ :_ :S :S :S]
   [:_ :_ :_ :_ :_]
   [:S :S :S :_ :f]])





(= example-map (vec->map example-vector))
(= example-vector (map->vec example-map))

(deftest snake-moves-somewhere
  (let [req (json/parse-string (slurp (io/resource "simple-request.json")) keyword)
        resp (move-handler req)]
    (is (s/valid? ::common/response resp))))

;; ..+.
;; .+++
;; xx+.
;; .x..
