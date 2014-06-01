(ns data-profiler.web
  (:require [com.stuartsierra.component :refer (Lifecycle)]
            [modular.bidi :refer (WebService)]
            [bidi.bidi :as bidi]
            [ring.middleware.params :refer (wrap-params)]
            [clostache.parser :refer (render-resource)]
            [data-profiler.csv :as csv]
            [data-profiler.profiler :as profiler]
            [clojure.pprint :refer (pprint)]
            [clojure.edn :as edn]))

(declare make-routes)

(def available-files csv/examples)

(defn displayable-string [x]
  (cond (nil? x) "nil"
        (empty? x) "\"\""
        :else x))

(defn render-formats [field values]
  (->> (map profiler/codify-format values)
       frequencies
       (map (fn [[value c]] 
              {:format (displayable-string value) 
               :count c
               :show-path (format "%s?where=%s"
                                  (bidi/path-for (make-routes) :show-rows :file-name "elements" :limit "all" )
                                  (with-out-str (pr [field "format" value])))}))
       (sort-by :count)
       reverse))

(defn render-common-values [field values] 
  (->> (frequencies values)
       (map (fn [[value c]] 
              {:value (displayable-string value) 
               :count c
               :show-path (format "%s?where=%s"
                                  (bidi/path-for (make-routes) :show-rows :file-name "elements" :limit "all" )
                                  (with-out-str (pr [field "value" value])))}))
       (sort-by :count)
       reverse
       (take 5)))

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
                        :common-values (render-common-values field values)
                        :formats    (render-formats field values)}))))

(defn apply-condition [[entity attr value] rows]
    (cond (= attr :format) (profiler/where profiler/codify-format (keyword entity) value rows)
          (= attr :value)  (filter #(= value (get % (keyword entity))) rows)))

(defn add-values [m rows & {:keys [limit where]}]
  (assoc m :rows (let [fields (profiler/fields rows)
                       values (if where
                                (for [row (apply-condition where rows)] {:values (map row fields)})
                                (for [row rows] {:values (map row fields)}))] 
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

(defn show-rows [{:keys [query-params] :as req}]
  (let [source (-> req :route-params :file-name)
        limit  (-> req :route-params :limit)
        uri (get available-files (keyword source))
        rows (when uri (csv/parse uri))]
    (if uri
      {:status 200 
       :body 
       (let [conditions (when (get query-params "where")
                          ((fn [[e a v]] [(keyword e) (keyword a) v]) (edn/read-string (get query-params "where"))))] 
         (-> empty-view
             (add-name source)
             (add-source uri)
             (add-row-count rows)
             (add-fields rows)
             (add-values rows :limit limit :where conditions)
             (render-template "views/layouts/rows.mustache")))}
      {:status 404 :body (str "No data set named " source " available.")})))

(defn make-handlers []
  {:show-profile show-profile
   :show-rows    (wrap-params show-rows)})

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

