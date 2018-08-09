(ns ch2.orbital)

;; In Clojure, 'map' is a function for applying a transformation to every
;; element within a sequence to produce a new sequence

;; for the sake of example, we will consider the need to extract the orbital
;; periods for every planet entity in our space simulation for display on the screen.
;; The source input will be a vector of Planet (defined in ch1/modeling.clj) and for
;; each Planet we will compute its orbital period knowing that it depends on the
;; planet but also on the mass of the star it revolves around.

;; to calculate the orbital period, we use the following formula :
;; T = 2π * sqrt (a^3 / μ)
;;
;; where μ = G * M is the standard gravitational parameter, G being the gravitational
;; constant, M the planet's mass and a (the semi-major-axis) the planet's average
;; distance from its star
(defn semi-major-axis
  "The planet's average distance from its star"
  [p]
  (/ (+ (:aphelion p) (:perihelion p)) 2))

(def G 6.674e11)

(defn mu
  "The planet's standard gravitational parameter"
  [mass]
  (* G mass))

(defn orbital-period
  "The time taken by the planet for a complete orbit around its star"
  [p mass]
  (* Math/PI 2
     (Math/sqrt (/ (Math/pow (semi-major-axis p) 3)
                   (mu mass)))))

;; Now that the function to be applied to each planet is defined, we can now provide it
;; to map, with the planet sequence, to have it produce a new sequence containing each
;; planet's orbital period as the consequence of the 'map'-ping function
(defn orbital-periods
  "Given a collection of planets, and a star, return the orbital periods of every planet."
  [planets star]
  (map #(orbital-period % (:mass star)) planets))


;; As an illustration, given the following inputs (with some fake and inaccurate data):
;(def earth {:name "Earth" :aphelion 152098232 :perihelion 147098290})
;=> #'user/earth
;(def neptune {:name "Neptune" :aphelion 100932 :perihelion 84590})
;=> #'user/neptune
;(def sun {:name "sun" :mass 2})
;=> #'user/sun

; calling orbital-periods on the planets (neptune and earth), given the star (sun)
; would give :
;(orbital-periods [neptune earth] sun)
;=> (153.6455201923353 9950882.24612072)