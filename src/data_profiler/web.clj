(ns data-profiler.web
  (:require [com.stuartsierra.component :refer (Lifecycle)]
            [modular.bidi :refer (WebService)]
            [bidi.bidi :as bidi]
            [clostache.parser :refer (render-resource)]
            [data-profiler.csv :as csv]
            [data-profiler.profiler :as profiler]
            [clojure.pprint :refer (pprint)]))

(declare make-routes)

(def available-files csv/examples)

(defn displayable-string [x]
  (cond (nil? x) "nil"
        (empty? x) "\"\""
        :else x))

(defn render-formats [values]
  (->> (map profiler/codify-format values)
       frequencies
       (map #(hash-map :format (displayable-string (first %)) 
                       :count (second %)))
       (sort-by :count)
       reverse))

(defn pct [num-values total-values] (* 100 (float (/ num-values total-values))))

(def empty-view {})

(defn add-name [m s]
  (assoc m 
    :name s
    :profile-path  (bidi/path-for (make-routes) :show-profile :file-name s)
    :examples-path (bidi/path-for (make-routes) :show-rows :file-name s :limit 10)))

(defn add-source [m s]
  (assoc m :source s))

(defn add-row-count [m x]
  (assoc m :row-count (if (integer? x) x (count x))))

(defn add-profile [m rows]
  (assoc m :base-profile (profiler/base-profile rows)))

(defn add-fields [m rows]
  (assert (:row-count m) "Missing required value")
  (assoc m :fields (for [field (profiler/fields rows)]
                     (let [values (profiler/values field rows)]
                       {:name       (name field)
                        :uniqueness (pct (count (distinct values)) (:row-count m))
                        :common-values (->> (frequencies values)
                                            (map (fn [[v c]] {:value (displayable-string v) 
                                                              :count c}))
                                            (sort-by :count)
                                            reverse
                                            (take 5))
                        :formats    (render-formats values)}))))

(defn add-values [m rows limit]
  (assoc m :rows (let [fields (profiler/fields rows)
                       values (for [row rows] {:values (map row fields)})] 
                   (if (= "all" limit)
                     values
                     (take (Integer/parseInt limit) values)))))

(defn render-template [m template]
  (render-resource template m))

(defn show-profile [req]
  (let [source (-> req :route-params :file-name)
        uri (get available-files (keyword source))
        rows (when uri (csv/parse uri))] 
    (if rows
      (let [num-rows (count rows)] 
        {:status 200 
         :body (-> empty-view 
                   (add-name source)
                   (add-source uri)
                   (add-row-count num-rows)
                   (add-profile rows)
                   (add-fields rows)
                   (render-template "views/layouts/dashboard.mustache"))})
      {:status 404 
       :body (format "<h1>Cannot find and data for %s</h1>" source)})))

(defn show-rows [req]
  (let [source (-> req :route-params :file-name)
        limit  (-> req :route-params :limit)
        uri (get available-files (keyword source))
        rows (when uri (csv/parse uri))]
    (if uri
      {:status 200 
       :body (-> empty-view
                 (add-name source)
                 (add-source uri)
                 (add-row-count rows)
                 (add-fields rows)
                 (add-values rows limit)
                 (render-template "views/layouts/rows.mustache"))}
      {:status 404 :body (str "No data set named " source " available.")})))

(defn make-handlers []
  {:show-profile show-profile
   :show-rows    show-rows})

(defn make-routes [] 
  ["/" 
   {"profile/" {[:file-name]                 :show-profile
                [:file-name "/rows/" :limit] :show-rows}}])

(defn new-website
  ([] 
     (new-website ""))
  ([uri-context] 
     (reify
       Lifecycle
       (start [this] this)
       (stop  [this] this)

       WebService
       (ring-handler-map [this] (make-handlers))
       (routes [this] (make-routes))
       (uri-context [_] uri-context))))

