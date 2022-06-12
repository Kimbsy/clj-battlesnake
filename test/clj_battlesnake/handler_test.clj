(ns clj-battlesnake.handler-test
  (:require [clojure.test :refer :all]
            [clojure.spec.alpha :as s]
            [clj-battlesnake.handler :refer :all]
            [clj-battlesnake.common :as common]
            [clojure.java.io :as io]
            [cheshire.core :as json]))

(deftest snake-moves-somewhere
  (let [req (json/parse-string (slurp (io/resource "simple-request.json")) keyword)
        resp (move-handler req)]
    (is (s/valid? ::common/response resp))))

;; ..+.
;; .+++
;; xx+.
;; .x..
