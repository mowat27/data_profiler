(ns data-profiler.matrix
  (:require [clojure.core.matrix :refer :all]
            [clojure.java.io :as io]
            [clojure.data.csv :as csv]
            [camel-snake-kebab :refer (->kebab-case-keyword)]))

(defn name-empty-columns [prefix coll] 
  (map 
   #(if (empty? %1) (str prefix (inc %2)) %1) 
   coll (range)))

(defn force-size [c coll]
  (take c (concat coll (repeat ""))))

(defn parse
  "Read data from a CSV source (File, InputStream, byte array, URI, etc.)"
  [stream & {:keys [limit] :or {limit nil}}]
  (with-open [in (io/reader stream)]
    (doall
     (let [[header & tail] (csv/read-csv in)
           header          (mapv ->kebab-case-keyword (name-empty-columns "field" header))
           num-headers     (count header) 
           tail            (map #(force-size num-headers %) tail)
           rows            (if limit (take limit tail) tail)]
       {:headers header :matrix (matrix rows)}))))


