(ns data-profiler.profiler
  (:require [clojure.string :as str]
            [clojure.core.matrix :as m]
            [data-profiler.csv :as csv]))

(def available-files csv/examples)

(def m-csv-parse (memoize csv/parse))

(defn parse [source]
  (let [uri (get available-files (keyword source))]
    (when uri (m-csv-parse uri))))

(defn fields [coll] 
  (reduce into #{} (map keys coll)))

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

(defn values [field rows & {:keys [distinct]}]
  (let [distinct (or distinct false)
        all (map #(get % field) rows)]
    (if distinct 
      (clojure.core/distinct all)
      all)))

(defn where [f field x rows]
  (filter #(= x (f (field %))) rows))

(defn pct [num-values total-values] 
  (* 100 (float (/ num-values total-values))))

(defn calculate-frequencies 
  ([m k]
     (calculate-frequencies m k identity))
  ([m k f]
     (assoc m k (into {}  
                      (for [field (:field-names m)]
                        [field (->> (values field (:rows m))
                                    (map f)
                                    frequencies
                                    (map (fn [[fmt c]] {:value fmt 
                                                        :count c}))
                                    (sort-by :count)
                                    reverse)])))))

(defn calculate-uniqueness [m k]
  (assoc m k (into {}
                   (for [field (:field-names m)]
                     [field (->> (:rows m)
                                 (values field)
                                 distinct
                                 count
                                 (#(pct % (:row-count m))))]))))

(defn profile [source]
  (let [rows (parse source)
        _ (assert (not (nil? rows)) (format "Could not parse source: '%s'" source))
        m {:rows rows
           :row-count (count rows)
           :field-names (fields rows)}]
    (-> m
        (calculate-frequencies :values)
        (calculate-frequencies :formats codify-format)
        (calculate-uniqueness :uniqueness))))






