(ns ch3.fn)

;; Instead of filtering elements, we sometimes need to retrieve or remove some elements at the beginning of a collection.
;; In Clojure, these actions can be achieved with 'take' and 'drop'.
;; As their name imply :
;;   - 'take' returns the n first elements of a collection, hence retrieving a subset of the original collection (at the
;; beginning).
;;   - 'drop' removes the n first elements of a collection and returns the remaining elements, hence retrieving a subset
;; of the original collection (at the end).

;; let's take for the sake of example the case where we would like to paginate the elements we have retrieved from
;; a data source (in our simple case, a collection). If we want to get the n-th page of the data, we have to :
;;   - remove the data contained within the first (n - 1)th pages
;;   - retrieve the data contained within a single page (a page containing 'page-size' elements) from the result of the
;; prior removal of the (n - 1)th pages content above.
(defn nth-page
  "Return up to page-size results for the nth (0-based) page of source."
  [source page-size page]
  (->> source
       (drop (* page-size page))
       (take page-size)))

;; this implementation uses sequences so elements are evaluated lazily which also means that any elements beyond the retrieved
;; page are not realized.

;; Sometimes, we may also need the data contained within the page and all the rest (i.e data beyond the page we retrieved) for
;; further processing for example (but also taking advantage of the lazy processing).
;; In this case, we can use the 'split-at' function to retrieve both the content of the page and the remaining of the collection
;; in a tuple. The elements of the tuple are 'complements' to each other regarding the initial collection. In fact, their
;; union gives the initial collection.
(defn page-and-rest
  "Return the content of the current page and the remaining in the source in a tuple"
  [source page-size]
  (split-at page-size source))


;; There are other variations on the use of take, drop and split-at where instead of providing the element counts to be
;; removed/retained, we specify a predicate which takes as argument each element of the collection and returns a truthy value
;; (true/false) in order to decide whether the element is to be retained/dropped. These variations are 'take-while', 'drop-while',
;; and 'split-with'.
;; It is often recommended to combine a sorting step prior to using the predicated functions since the processing stops as soon as
;; it encounters the first element for which the predicates return false.

;; let's for example retain only any positive number from within a range, here coll
(def coll (vec (range 10 -3 -1)))
;; => [10 9 8 7 6 5 4 3 2 1 0 -1 -2]

(take-while pos? coll)
;; => (10 9 8 7 6 5 4 3 2 1)

;; let's now drop any positive number from within that same range, which would then retain only the last few numbers at the end of the
;; range which are negative
(drop-while pos? coll)
;; => (0 -1 -2)

;; at last, using split-at with the same pos? predicate would provide us with a tuple whose first element would be the result of applying
;; take-while and the second element would be the result of applying drop-while to the initial collection.
(split-with pos? coll)
;; => [(10 9 8 7 6 5 4 3 2 1) (0 -1 -2)]


;; Talking about sorting, the most basic function for sorting a collection is 'sort' which, given a comparator function, allows defining a
;; custom sorting logic. Otherwise, it will just use the 'compare' function.

;; Let's say for example that we would like to sort the first 5 planets name : the sorting logic i.e the comparator function would then
;; be the planet's name
;; (take 5 (sort (map :name planets)))

;; sometimes though, we would like to have the original entities sorted instead of just one attribute (here name). In this case, we use the
;; 'sort-by' function and provide it with the key (the object/entity attribute) with which to sort the entities and an optional comparator if
;; there is a need to define a custom logic for sorting with the chosen key.

;; let's say then that instead of retrieving the name of the first 5 planets, we would like to retrieve the 5 planets themselves :
;; (take 5 (sort-by :name planets))
;=>
;({:name "Earth", :moons ["moon"], :orbit-days 365.2564}
; {:name "Jupiter", :moons ["io" "europa" "ganymede" "callisto"]}
; {:name "Mars", :orbit-days 686.93}
; {:name "Mercury", :orbit-days 87.969}
; {:name "Neptune", :moons ["triton"], :orbit-days 60148.35})

;; as a last illustration of the sorting mechanism, let's say we would like to define a function allowing us to retrieve the n smallest
;; planets, n being in this case a parameter.
;; The :volume is then the attribute or key function on which the comparison will be held.
(defn smallest-n
  "Retrieve the n smallest planets"
  [planets n]
  (->> planets
       (sort-by :volume)                                    ;; we first sort the planets by volume in ascending order
       (take n)))                                           ;; then we take the n first planets of the previous sorting step

;; with the following data (with fake volume data obviously) :
;(def planets
;  [{:name "Earth", :moons ["moon"], :orbit-days 365.2564 :volume 2531545}
;   {:name "Jupiter", :moons ["io" "europa" "ganymede" "callisto"] :volume 1231545}
;   {:name "Mars", :orbit-days 686.93 :volume 9231545}
;   {:name "Mercury", :orbit-days 87.969 :volume 231545}
;   {:name "Neptune", :moons ["triton"], :orbit-days 60148.35 :volume 431231545}
;   {:name "Pluto", :moons ["charon" "styx" "nix" "kerberos" "hydra"] :volume 150231545}
;   {:name "Saturn", :moons ["titan"], :orbit-days 10755.7 :volume 431231545}
;   {:name "Uranus", :moons ["titania" "oberon"], :orbit-days 30688.5 :volume 131545}
;   {:name "Venus", :orbit-days 224.7 :volume 505431}])

;; the 6 smallest planets would be :
; (smallest-n planets 6)

;; which would return :
;=>
;({:name "Uranus", :moons ["titania" "oberon"], :orbit-days 30688.5, :volume 131545}
;  {:name "Mercury", :orbit-days 87.969, :volume 231545}
;  {:name "Venus", :orbit-days 224.7, :volume 505431}
;  {:name "Jupiter", :moons ["io" "europa" "ganymede" "callisto"], :volume 1231545}
;  {:name "Earth", :moons ["moon"], :orbit-days 365.2564, :volume 2531545}
;  {:name "Mars", :orbit-days 686.93, :volume 9231545})
