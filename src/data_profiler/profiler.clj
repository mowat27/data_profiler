(ns data-profiler.profiler
  (:require [data-profiler.csv :as csv]
            [clojure.string :as str]))


(def elements (csv/parse (:elements csv/examples)))

(defn fields [coll] 
  (reduce into #{} (map keys coll)))

(defn pivot [fields coll] 
  (apply merge (for [field (fields coll)]
                 {field (mapv field coll)})))

(defn codify-format [x] 
  (if (nil? x)
    nil
    (str/join 
     (map (fn [c] (let [s (str c)] 
                    (cond (re-matches #"[a-z]" s) "a"
                          (re-matches #"[0-9]" s) "n"
                          (re-matches #"[A-Z]" s) "A"
                          :else s))) 
          (str x)))))

(defn where [f field x rows]
  (filter #(= x (f (field %))) rows))

(defn base-profile [rows]
  {:rows rows})

(defn profile-fields [profile]
  (assoc profile :fields (let [by-field (pivot (:rows profile))]
                           {:formats (mapv codify-format)})))
