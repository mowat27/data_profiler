(ns data-profiler.profiler
  (:require [data-profiler.csv :as csv]
            [clojure.string :as str]))


(def elements (csv/parse (:elements csv/examples)))

(defn fields [coll] 
  (reduce into #{} (map keys coll)))

(defn pivot [coll] 
  (apply merge (for [field (fields coll)]
                 {field (mapv field coll)})))









