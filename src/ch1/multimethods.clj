(ns ch1.multimethods
  (:require [ch1.validate]
            [ch1.money :refer (+$ zero-dollars)])
  (:import [ch1.validate Recipe Ingredient]))

;; We extend the recipe-manager domain to accommodate the multimethod use-case
;; illustration by adding a Store
(defrecord Store [,,,])

;; and a convenient (hypothetical) function that can look up the cost of an
;; ingredient in a particular store
(def cost-of [store ingredient],,,)

;; Then we define the 'multidispatch' by specifying the 'dispatch function'. This function
;; is at the core of the multimethod machinery since it will be called to produce a value
;; called the 'dispatch value'.
;; The 'dispatch value' will then be used to select the particular method implementation
;; to which the call should be dispatched
(defmulti cost
          "This macro defines a 'dispatch function' which returns the 'type' of a domain object
          (either Recipe or Ingredient) and will allow the multimethod machinery to select
          the right implementation for the cost calculation based on this domain object type."
          (fn [entity store]
            (class entity)))                                ;; returns the domain object class

;; We finally define cost-calculation method implementations for each 'dispatch value' for
;; which we would like to be able to calculate costs. The method implementation must
;; have the same signature as the 'dispatch function' since their inputs will be mapped to
;; those of the 'dispatch function' to return the 'dispatch value' first, and then to invoke
;; the correct implementation.

;; We define the cost-calculation method implementation for the Recipe domain object, hence the Recipe
;; dispatch value of 'Recipe' in the defmethod macro header in the following form.
(defmethod cost Recipe
  [recipe store]                                            ;; Same signature as the dispatch function here
  ;; Note that this implementation is also dispatching to the cost-calculation method implementation
  ;; of the Ingredient domain object when mapping on '#(cost store %)' each recipe ingredients.
  (reduce +$ zero-dollars (map #(cost store %) (:ingredients recipe))))

;; We define the cost-calculation method implementation for the Ingredient domain object, hence the Ingredient
;; dispatch value of 'Ingredient' in the defmethod macro header in the following form.
(defmethod cost Ingredient
  [ingredient store]                                        ;; Same signature as the dispatch function here
  (cost-of store ingredient))