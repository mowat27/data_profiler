(ns data-profiler.matrix
  (:refer-clojure :exclude [* - + == /])
  (:require [clojure.core.matrix :refer :all]
            [clojure.core.matrix.operators :refer :all]
            [clojure.java.io :as io]
            [clojure.data.csv :as csv]
            [camel-snake-kebab :refer (->kebab-case-keyword)]
            [data-profiler.profiler]))

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
           header          (map ->kebab-case-keyword (fill-empties "field" header))
           rows            (if limit (take limit tail) tail)]
       {:headers header :matrix (matrix rows)}))))

(def m-parse (memoize parse))

(def examples
  {:elements "http://introcs.cs.princeton.edu/java/data/elements.csv"
   :ip-by-country "/Users/adrian/Desktop/ip-by-country.csv"})
