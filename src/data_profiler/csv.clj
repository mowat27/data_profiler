(ns data-profiler.csv
  (:require [clojure.java.io :as io]
            [clojure.data.csv :as csv]
            [camel-snake-kebab :refer (->kebab-case-keyword)]))

(defn fill-empties [prefix coll] 
  (map 
   #(if (empty? %1) (str prefix (inc %2)) %1) 
   coll (range)))

(defn parse
  "Read data from a CSV source (File, InputStream, byte array, URI, etc.)"
  [stream & {:keys [limit] :or {limit nil}}]
  (with-open [in (io/reader stream)]
    (doall
     (let [[header & tail] (csv/read-csv in)
           header          (fill-empties "field" header)
           rows            (if limit (take limit tail) tail)]
       (map (partial zipmap (map ->kebab-case-keyword header)) rows)))))

(def examples
  {:elements "http://introcs.cs.princeton.edu/java/data/elements.csv"
   :ip-by-country "/Users/adrian/Desktop/ip-by-country.csv"
   :crime  "/Users/adrian/Desktop/crime_incidents_2013_CSV.csv"})

(comment 
  (take 3 (parse (:ip-by-country examples)))
)



