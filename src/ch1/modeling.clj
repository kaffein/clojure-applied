(ns ch1.modeling)

;; In Clojure, domain entities are either represented with maps or records.
;; Maps are generic key/value pairs collections whereas records are predefined types with fields like
;; what we are used to, in OO programming.

;; MAPS are very flexible in that they do not have a predefined structure. Hence, we can not
;; "capture" nor communicate about the aim and characteristics of the domain entity.
;; Furthermore, they are ephemeral in that the absence of a "captured" structure does not allow
;; "reuse" for further instantiation
(def earth {:name       "Earth"
            :moons      1
            :volume     1.08321e12                          ;; km^3
            :mass       5.97219e24                          ;; kg
            :aphelion   152098232                           ;; km, farthest from sun
            :perihelion 147098290                           ;; km, closest to sun
            :type       :Planet
            })

;; RECORDS on the other hand are more like classes in OO-programming. They "capture" the structure
;; of the domain entity to allow other developers to understand the aim and characteristics of this
;; entity and also to allow the "reuse" of this structure to create other instances sharing the same
;; structure
(defrecord Planet [name
                   moons
                   volume                                   ;; km^3
                   mass                                     ;; kg
                   aphelion                                 ;; km, farthest from sun
                   perihelion]                              ;; km, closest to sun
  )

;; There are two (2) ways for creating records :
;; 1 - Positional factory function
;; 2 - Map factory function

;; Positional factory function has the advantage of being very concise but all attributes have
;; to be provided in the order specified by the record definition.
;; It is then very convenient for simple domain entities consisting in only a few attributes
;; It is much more constraining if there are many fields since modification in the entity structure
;; (by adding/removing a field) will likely break the callers implementation
(def earth (->Planet "Earth" 1 1.08321e12 5.97219e24 5.97219e24 147098290))

;; Map factory function on the other hand is much more resilient to changes because they allow
;; the caller to specify the attributes/fields to be set in an arbitrary order. Hence, optional
;; attributes are possible and callers are unlikely to break on domain entity changes (field added
;; or removed)
(def earth (map->Planet {:name       "Earth"
                         :moons      1
                         :volume     1.08321e12
                         :aphelion   152098232
                         :mass       5.97219e24
                         :perihelion 147098290}))

;; Records should be the first time choice when modeling domain entities since they leverage the JVM
;; machinery by creating classes for the type. Records can then take full advantage of optimizations
;; operated by the JVM at the class level

;; Maps are better choices when dealing with public facing APIs where the API caller should not have
;; too much constraints and does not need to know the details of the internal representation of the
;; entity