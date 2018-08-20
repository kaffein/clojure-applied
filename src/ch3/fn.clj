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