(ns ch2.update
  (:require [medley.core :refer [map-keys map-vals]]
            [ch1.recipe :refer :all]))

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



;; In addition to allowing the update of map values in a single call, medley
;; also provides another utility method for updating map values in a single
;; shot using the 'map-vals' function.
;; Like 'map-keys', it takes a function f as a parameter and applies it to
;; every values of the map, i.e (f val)

;; As an example, let's take the recipes we defined earlier in src/ch1/recipe.clj
;; If we want to add a :calories field into the recipes index which is the map of
;; all available recipes, we can do so by first defining a hypothetical function
;; for computing a recipe's calories
(defn- compute-calories
  [recipe]
  ;; it just randomly returns an integer between 0 and 1000
  (rand-int 1000))

;; Then we define another function for encapsulating the logic for computing
;; and associating a :calories field to each recipe
(defn- update-calories
  [recipe]
  (assoc recipe :calories (compute-calories recipe)))

;; We finally define a third function which will take as parameter the
;; 'recipe-index', which is a map of all recipes, and map each value using
;; 'update-calories'. In the end, each recipe will be associated with
;; a new field :calories whose value is (computed-calories r)
(defn include-calories
  "Computes and associates a :calories field to each recipe in the index"
  [recipe-index]
  (medley.core/map-vals update-calories recipe-index))

;; As an illustration, let's define the recipe-index as :
(def recipe-index {"r1" #ch1.recipe.Recipe{:name        "Toast",
                                           :author      "p1",
                                           :description "Crispy bread",
                                           :ingredients ["Slice of bread"],
                                           :steps       ["Toast bread in toaster"],
                                           :servings    1},
                   "r2" #ch1.recipe.Recipe{:name        "Boeuf bourguignon",
                                           :author      "p1",
                                           :description "Best french boeuf bourguignon",
                                           :ingredients ["Carrots"],
                                           :steps       ["Beef"],
                                           :servings    2}})

;; Calling 'include-calories' on the 'recipe-index' like so
(include-calories recipe-index)

;; would give ...
;{"r1" #ch1.recipe.Recipe{:name        "Toast",
;                         :author      "p1",
;                         :description "Crispy bread",
;                         :ingredients ["Slice of bread"],
;                         :steps       ["Toast bread in toaster"],
;                         :servings    1,
;                         :calories    90},
; "r2" #ch1.recipe.Recipe{:name        "Boeuf bourguignon",
;                         :author      "p1",
;                         :description "Best french boeuf bourguignon",
;                         :ingredients ["Carrots"],
;                         :steps       ["Beef"],
;                         :servings    2,
;                         :calories    837}}