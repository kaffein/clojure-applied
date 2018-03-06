(ns ch1.convert
  (:require [ch1.validate]
            [ch1.money :refer (+$ zero-dollars)]))

;; We want to be able to combine and calculate the cost of an ingredient from 2 different recipes
;; but which may be expressed with different units for example. We then have to convert them to the same
;; unit before being able to make the calculation.

;; Using value-based (the values here are the units) dispatch with multi-methods, we will use a
;; dispatch function returning the couple [unit1 unit2], unit1 being the ingredient unit (source) to be
;; converted to unit2 (target). Based on this couple "value" returned by the dispatch function, we then
;; choose the right method implementation to dispatch the call to.
(defmulti convert
          "Converts quantity from unit1 to unit2, matching on the value of [unit1, unit2]"
          (fn [unit1 unit2 quantity] [unit1 unit2]))

;; This is the implementation for converting from :lb to :oz, matched with the couple value [:lb :oz]
(defmethod convert [:lb :oz]
  [_ _ lb]
  (* lb 16))

;; This is the implementation for converting from :oz to :lb, matched with the couple value [:oz :lb]
(defmethod convert [:oz :lb]
  [_ _ oz]
  (/ oz 16))

;; This is a fallback mechanism covering the case where the units are the same, hence returning the quantity
;; without any conversion, or throwing an exception when the particular couple "value" [unit1 unit2] does not
;; have a matching method implementation
(defmethod convert :default
  [u1 u2 quantity]
  (if (= u1 u2)
    quantity
    (assert false (str "Unknown unit conversion from " u1 " to " u2))))

;; This is a utility method allowing the combination of two (2) ingredients expressed with different units
;; using the multimethod convert implemented above
(defn ingredient+
  "Add two ingredients into a single ingredient, converting their quantities with unit conversion if needed"
  [{unit1 :unit quantity1 :quantity :as ingredient1} {unit2 :unit quantity2 :quantity}]
  (assoc ingredient1 :quantity (+ quantity1 (convert unit2 unit1 quantity2))))