;; Clojure's dynamic types give great flexibility in terms of implementation
;; but also enforce fewer constraints. Functions expose their interface (signature)
;; without being explicit about the types of their arguments.
;; Of course, type hinting is an option but then, the dynamic aspect of programming
;; in Clojure would rapidly vanish into the process.
;; Sometimes though, we may need to verify/validate data especially at system boundaries
;; namely when they are coming in or out of the sytem (e.g : APIs). Most of the time,
;; data coming in are subject to data validation more than data coming out of the system.

;; To a broader range, data validation is about making data comply with a specification
;; which helps in ensuring that the system is fed with, but also produces data that are
;; "correctly-shaped" and "valid".
;; Last but not least, it also acts as a documentation for the codebase.

;; Prismatic Schema will be used in the following examples but there are many other options
;; out there for example the long-awaited Clojure Specs.
;; Let's first pull the Schema namespace...
(ns ch1.validate
  (:require [schema.core :as s]))

;; Spec'ing the entities we had from the ch1.recipe namespace is about using
;; Schema's own definition of defrecord to redefine those entities with the added
;; benefit that it is possible to specify schemas for each fields of the record in
;; addition to the plain old defrecord effect of creating a record
(s/defrecord Ingredient [name :- s/Str
                         quantity :- s/Int
                         unit :- s/Keyword])

(s/defrecord Recipe [name :- s/Str
                     description :- s/Str
                     ingredients :- [Ingredient]
                     steps :- [s/Str]
                     servings :- s/Int])

;; In Schema, it is even possible to use a variation of defn to spec' function input
;; parameters and return types in the same way a statically-typed language specifies
;; its parameters and return types. They are used to generate helpful docstrings
(s/defn add-ingredients :- Recipe
  [recipe :- Recipe & ingredients :- [Ingredient]]
  (update-in recipe [:ingredients] into ingredients))

;; when calling the docstring (doc) function on the add-ingredients function, the
;; following output is returned

;; (doc ch1.validate/add-ingredients)
;; -------------------------
;; ch1.validate/add-ingredients
;; ([recipe & ingredients])
;; Inputs: [recipe :- Recipe & ingredients :- [Ingredient]]
;; Returns: Recipe