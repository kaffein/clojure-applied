(ns ch1.image
  (:require [clojure.java.io :as io])
  (:import [javax.imageio ImageIO]
           [java.awt.image BufferedImage]))

;; Sometimes side-effects are unavoidable. For example, when constructing an entity
;; with an associated-image retrieved from I/O like in the following
(defrecord PlanetImage [src ^BufferedImage contents])

;; In this case, using a constructor help us isolate the side-effects from the rest
;; of the code
(defn make-planet-image
  "Make a PlanetImage which may throw an IOException"
  [src]
  (with-open [img (ImageIO/read (io/input-stream src))]
    (->PlanetImage src img)))