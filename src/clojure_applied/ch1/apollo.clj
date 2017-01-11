(ns ch1.apollo)

;; In the following function, the optional arguments are 'captured' by #opts and
;; are retrieved by destructuring the arguments map by keys.
;; Using :keys as a destructuring function and specifying the key names of the values
;; to be retrieved in the let binding form fetches the values defined within the
;; #opts map
(defn make-mission
  [name system launched manned? opts]
  (let [{:keys [cm-name lm-name orbits evas]} opts]
    {:cm-name cm-name
     :lm-name lm-name
     :orbits  orbits
     :evas    evas}))

;; It is also possible to define sensible defaults for the optional arguments.
;; Here we define a map containing these defaults. When calling the make-mission
;; function, we ensure that those default values are first set as the values
;; of the optionals then we override those by the values defined within the #opts argument.
;; This way, we can always be sure that in the end, only the values actually provided/defined
;; in the #opts argument are the ones that will be used.
(def mission-defaults {:orbits 3 :evas 0})

(defn make-mission
  [name system launched manned? opts]
  ;; The merge method is a convenient method for this : it takes two maps arguments and values
  ;; defined in the first map are overriden by the values defined in the second map
  ;; Here we first merge the maps before extracting the keyed-values with the let-binding
  (let [{:keys [cm-name lm-name orbits evas]} (merge mission-defaults opts)]
    {:cm-name cm-name
     :lm-name lm-name
     :orbits  orbits
     :evas    evas}))

(def apollo-4
  (make-mission "Apollo 4"
                "Saturn V"
                #inst "1967-11-09T12:00:01-00:00"
                false
                {:orbits 3}))
;; We didn't provide the :evas value for that last call so the default value 0 from the
;; mission-defaults map will be kept giving the following result when evaluated :
;; {:cm-name nil, :lm-name nil, :orbits 3, :evas 0}

;; It is also possible to destructure #opts as a varargs sequence. We can then
;; provide the destructuring logic with the default values directly (via :or) instead of having
;; to merge an externally-defined map of options with the arguments provided by the caller
(defn make-mission
  [name system launched manned? & opts]
  (let [{:keys [cm-name lm-name orbits evas]} :or {:orbits 2 :evas 1} opts]
    {:cm-name cm-name
     :lm-name lm-name
     :orbits  orbits
     :evas    evas}))

(def apollo-4
  (make-mission "Apollo 4"
                "Saturn V"
                #inst "1967-11-09T12:00:01-00:00"
                false
                :orbits 3))

(def apollo-11
  (make-mission "Apollo 11"
                "Saturn V"
                #inst "1969-07-16T13:32:00-00:00"
                true
                :cm-name "Columbia"
                :lm-name "Eagle"
                :orbits 30
                :evas 2))