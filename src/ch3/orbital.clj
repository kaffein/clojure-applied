(ns ch3.orbital
  (:require [ch1.apollo])
  (:import [ch1.apollo Planet]))

(def G ,,,)

(defn mu [mass] (* G mass))

(defn semi-major-axis
  "The planet's average distance from the star" [p]
  (/ (+ (:aphelion p) (:perihelion p)) 2))

(defn orbital-period
  "The time it takes for a planet to make a complete
  orbit around a mass, in seconds"
  [p mass]
  (* Math/PI 2
     (Math/sqrt (/ (Math/pow (semi-major-axis p) 3)
                   (mu mass)))))

;; Transducers were introduced in Clojure 1.7.
;; They solve an issue related to how sequence processing-functions in general, were conceived and
;; implemented.

;; In fact, sequence processing-functions combine :
;;  - the input sequence
;;  - the transformation function (the actual processing)
;;  - the result or output sequence

;; this combination resulted in :
;;  - tight coupling with the concrete type of the input but also the output sequence
;;  - tight coupling with the iteration mechanism which prevents reuse and composition since it also varies
;; with the input and output sequence types

;; Worse, we had a combinatorial explosion since we were forced to (re)write sequence type-specific
;; variation of those sequence processing-functions in the standard library but also any other functions that
;; we may add because reuse/composition were not possible.

;; The idea of transducers was to decouple the input/output sequence and its iteration mechanism from
;; the transformation (processing) itself.
;; so instead of having the input sequence, the transformation function and the result all baked-in, we would only
;; have the transformation included within the transducer.

;; It turns out, there was already a function in the standard library that :
;;  - encapsulates an 'iterative' recursion on its input sequence
;;  - can decouple the iteration mechanism from the transformation function
;; that function is 'reduce'

;; 'Transducers' are an elaboration on that idea of reduction by building transformation stacks which consist in
;; transforming one reducing function to another.
;; They abstract away the input/output sequences and define only the processing/transformation that
;; has to be performed on each element of the sequence.
;; The fact that input/output sequences are decoupled from the transformation has deep consequences : it means
;; it is possible to build, reuse and compose entire data processing pipelines in totally different contexts
;; (with different sequence types)


;; As an example, we can create a map transducer the same way we would with a normal call to map but omitting
;; the input collection like so

(defn orbital-period-transformation
  "Create a map transformation for planet->orbital-period."
  [star]
  (map #(orbital-period % (:mass star))))                   ;; the input sequence is omitted here

;; we can then use this transducer with a variety of input and build totally different outputs like in the following
;; example where we build a sequence out of the transformation stack
(defn orbital-period-sequence
  "This function returns a sequence"
  [planets star]
  (sequence (orbital-period-transformation star) planets))

;; we can also retrieve a vector with the following
(defn orbital-period-vector
  "This function returns a vector"
  [planets star]
  (into [] (orbital-period-transformation star) planets))

;; or event return a list ...
(defn orbital-period-list
  "This function returns a list"
  [planets star]
  (into () (orbital-period-transformation star) planets))



;; Reducing a collection consists in repeatedly applying a function to an accumulated value and the next item
;; in the collection, hence consumming the collection and giving a value as a final result.
;; A optional value, also known as a 'seed' can be provided as the first initial value of the accumulator.
;; Sometimes, like in the case of the 'into' function, we get back a collection as a final result instead of a
;; single value : 'into' is a specialization of reduce

;; as an example, let's say that we would like to compute the number of moon of our solar system.
;; It involves :
;;   - getting the planets collection as input
;;   - extracting for each planet its moon count (hence a tranformation operation via map)
;;   - and combining each planets moon count (hence a reduction operation via +)
(defn total-moons
  "Computes the total number of moons of the solar system"
  [planets]
  (reduce + 0 (map #(:moons %) planets)))
   ;; or with a more idiomatic version of the map portion
   ;; (reduce + 0 (map :moons planets))

;; We can also achieve the same result using a transducer. As seen earlier, transducers are an elaboration on the
;; idea of reducers with the added-value of having transformations abstracted away from the implementation details of the
;; input and output. It means that in the previous example, we could have abstracted away the transformation part (via map)
;; which could then be reused in other contexts.
(def xf-moon-count
  (map :moons))

(defn total-moons-transduced
  "Computes the total number of moons of the solar system using a transducer"
  [planets]
  (transduce xf-moon-count + 0 planets))