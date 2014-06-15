(ns data-profiler.profiler
  (:require [clojure.string :as str]
            [data-profiler.csv :as csv]
            [data-profiler.matrix :as matrix-csv]
            [clojure.core.matrix :as m]
            [clojure.core.reducers :as r]))

(m/set-current-implementation :vectorz)

(def available-files csv/examples)

(def m-csv-parse (memoize matrix-csv/parse))

(defn parse [source]
  (let [uri (get available-files (keyword source))]
    (when uri (m-csv-parse uri))))

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

(defn apply-condition [[entity attr value] rows]
    (cond (= attr :format) (where codify-format (keyword entity) value rows)
          (= attr :value)  (filter #(= value (get % (keyword entity))) rows)))

(defn pct [total-values num-values] 
  (* 100 (float (/ num-values total-values))))

(defn calculate-frequencies 
  ([m]
     (calculate-frequencies m identity))
  ([m f]
     (into {} (pmap (fn [[field values]] 
                      [field (->> (map f values)
                                  frequencies
                                  (map (fn [[fmt c]] {:value fmt 
                                                      :count c}))
                                  (sort-by :count)
                                  reverse)]) 
                    (:columns m)))))

(defn calculate-uniqueness [m]
  (into {} (pmap (fn [[field values]] 
                   [field (pct (:row-count m) (count (distinct values)))])
                (:columns m))))

(defn base-profile [profile-name]
  (let [{:keys [headers matrix]} (parse profile-name)
        _ (assert (not (nil? matrix)) (format "Could not parse profile-name: '%s'" profile-name))]
    {:profile-name profile-name
     :uri         (get available-files (keyword profile-name))
     :columns     (zipmap headers (m/columns matrix))
     :rows        (m/rows matrix)
     :row-count   (m/row-count matrix)
     :field-names headers}))

(def profile 
  (memoize (fn [profile-name]
             (let [m (base-profile profile-name)]
               (-> m
                   (assoc :values (calculate-frequencies m))
                   (assoc :formats (calculate-frequencies m codify-format))
                   (assoc :uniqueness (calculate-uniqueness m)))))))

(comment
  
  (time (:matrix (matrix-csv/parse (get available-files :ip-by-country))))
  (time (m/row-count (:matrix (m-csv-parse (get available-files :ip-by-country)))))
  (time (profile :crime))
 
  (def all (m-csv-parse (get available-files :ip-by-country)))
  (def headers (:headers (m-csv-parse (get available-files :ip-by-country))))
  (def matrix  (:matrix (m-csv-parse (get available-files :ip-by-country))))

  
  (def base (base-profile :ip-by-country))

  (time (calculate-frequencies base codify-format))

  (time (map (fn [row] (map codify-format row)) (m/rows matrix)))

  (time (map (fn [col] (map codify-format col)) (m/rows (m/transpose matrix))))
    
  (time (->> (map vector headers (m/columns matrix))
             (r/reduce (fn [result [col vals]] 
                       (concat result (interleave (repeat col) vals))) [])
             (partition 2)
             (r/reduce (fn [result [col val]] 
                       (let [fmt (codify-format val)] 
                         (update-in result [col fmt] (fnil inc 0)))) 
                     {})
             ))

  

  )






