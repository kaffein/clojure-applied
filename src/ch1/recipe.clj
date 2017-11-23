(ns ch1.recipe)

;; There are three (3) techniques for modeling relationships in Clojure :
;; 1 - Nesting
;; 2 - Identifiers
;; 3 - Stateful references

;; As an illustration, let us consider a system allowing recipes and their
;; authors to be managed. The domain entities for this system would be for example:
(defrecord Recipe [name
                   author                                   ;; recipe creator
                   description
                   ingredients
                   steps
                   servings])
;; and ...
(defrecord Person [lastname firstname])

;; Considering these domain entities, we now have 3 ways of connecting
;; the recipes (Recipe) with their authors (Person)

;; 1 - NESTING
;; Nesting consists in embedding the information related to the referred
;; domain entity directly in the referring domain entity.
;; This solution should be considered when the referred domain entity does not
;; (need to) have an existence of its own outside the referring domain entity.
;; It then introduces a tight coupling between the domain entities.

;; For example, if we do not consider the Person as an entity having an
;; existence of its own outside the Recipe then we can just nest it inside the
;; recipe.
;; Any changes that we may want to apply to the author of the recipe will have
;; to be applied through a modification of the instantiated (nesting) Recipe
;; itself since the author does not have an existence outside the recipe and
;; thus does not allow any changes to be applied directly to it.
(->Recipe
  "Toast"
  (->Person "Rich" "Hickey")
  "Homemade tasty toast by the master himself"
  ["Slice of bread"]
  ["Toast bread in toaster"]
  1)

;; 2 - IDENTIFIERS
;; Identifiers are "artificial identities" assigned to domain entities allowing
;; them to be referred to, anywhere the domain entities instances they are referring
;; are needed. They allow domain entities to have their very own existence and
;; to live outside the scope of the referring domain entity. The result is a more
;; loose coupling between the entities since they can live and evolve on their own.
;; In fact, the "artificial identity" will be used as a "stable" identity and an
;; "indirection" allowing any changes to the entity to be applied in only one place.
(def people {"p1" (->Person "Alex" "Miller")})

(def recipes {"r1" (->Recipe
                     "Toast"
                     "p1"                                   ;; Person id
                     "Crispy bread"
                     ["Slice of bread"]
                     ["Toast bread in toaster"] 1)})

;; "p1" here can be considered as a stable identity allowing the Person entity it is
;; pointing to, to be referred anywhere it is needed. Most of the time, these
;; identifiers are generated Ids (e.g : from an auto_increment field of a DB primary key)

;; 3 - Stateful references are more like OO-based relationship