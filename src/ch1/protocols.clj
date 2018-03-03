(ns ch1.protocols
  (:require [ch1.validate]
            [ch1.money :refer (+$ zero-dollars)])
  (:import [ch1.validate Recipe Ingredient]))

;; We extend the recipe-manager domain to accommodate the protocol use-case
;; illustration by adding a Store
(defrecord Store [,,,])

;; and a convenient (hypothetical) function that can look up the cost of an
;; ingredient in a particular store
(def cost-of [store ingredient],,,)


;; we then define the Protocol which is a 'contract' grouping the functions
;; that has to be implemented by any component interested in complying with
;; the terms of the contract
(defprotocol Cost
  (cost [entity store]))

;; we finally extend the types by providing, for each, implementations to the methods
;; defined in the protocol they are extending
(extend-protocol Cost
  Recipe
  (cost [recipe store]
    (reduce +$ zero-dollars (map #(cost % store) (:ingredients recipe))))
  Ingredient
  (cost [ingredient store]
    (cost-of store ingredient)))