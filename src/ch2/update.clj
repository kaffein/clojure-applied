(ns ch2.update
  (:require [medley.core :refer (map-keys)]))

;; Though the map abstraction provides a method for updating values,
;; there is not yet a method for updating map keys.
;; Instead, it is possible to use external small utility libs like
;; 'medley', which offer functions in areas not covered (yet) by
;; the language core library, in our case 'map-keys'.
(defn keywordize-entity
  "Transform the entity map string keys to keywords"
  [entity]
  ;; Given a function f as its first parameter, the 'map-keys'
  ;; function maps each key value to the result of applying the
  ;; function parameter f to its old value i.e the result of (f k-value)
  (map-keys keyword entity))

;; For illustration, 'sanitizing' the following entity in order to have
;; keywords as keys to the map instead of strings would require us
;; to apply the 'keyword' function to each string keys which, in our case,
;; is encapsulated in the 'keywordize-entity' function...
(keywordize-entity
  {"name"   "Earth"
   "moons" 1
   "volume" 1.08321e12
   "mass" 5.97219e24
   "aphelion" 152098232
   "perihelion" 147098290})

;; which gives ...
;; {:name "Earth",
;; :moons 1,
;; :volume 1.08321E12,
;; :mass 5.97219E24,
;; :aphelion 152098232,
;; :perihelion 147098290}
