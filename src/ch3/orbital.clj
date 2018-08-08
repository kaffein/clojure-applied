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