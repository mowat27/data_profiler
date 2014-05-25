(ns data-profiler.web
  (:require [ring.adapter.jetty :refer (run-jetty)]
            [ring.middleware.reload :refer (wrap-reload)]
            [bidi.bidi :as bidi]
            [clostache.parser :refer (render-resource)]
            [data-profiler.csv :as csv]
            [data-profiler.profiler :as profiler]
            [clojure.pprint :refer (pprint)]))

(def available-files csv/examples)

(defn using-layout [layout handler & {:keys [status]}]
  (let [response {:status (or status 200)}
        layouts {:application "views/layouts/application.mustache"
                 :dashboard "views/layouts/dashboard.mustache" }]
    (fn [req] 
      (assoc response :body (render-resource (layout layouts) 
                                             {:content (handler req)})))))

(defn index [] 
  (using-layout :application (fn [_] "<p>Hello World</p>")))

(defn render-formats [field rows]
  (->> rows
       (map (comp profiler/codify-format field))
       frequencies
       (map #(hash-map :format (let [x (first %)]
                                 (cond (nil? x) "nil"
                                       (empty? x) "\"\""
                                       :else x)) 
                       :count (second %)))
       (sort-by :count)
       reverse))

(defn show-profile [req]
  (let [source (-> req :route-params :file-name)
        uri (get available-files (keyword source))
        rows (when uri (csv/parse uri))] 
    (if rows
      {:status 200 
       :body (render-resource "views/layouts/dashboard.mustache"
                              {:name source
                               :source uri
                               :row-count (count rows)
                               :base-profile (profiler/base-profile rows)
                               :fields (for [field (profiler/fields rows)]
                                         {:name    (name field)
                                          :formats (render-formats field rows)})})}
      {:status 404 
       :bosy (format "<h1>Cannot find and data for %s</h1>" source)})))

(->> (:elements available-files) 
     csv/parse 
     (map (comp profiler/codify-format :element))
     (map #(hash-map :format %)))

(def router (wrap-reload (bidi/make-handler ["/" {"index.html" (index)
                                      ["profile/" :file-name] show-profile}])))

(defn -main [& args] 
  (let [port (or (first args) "8080")] 
    (run-jetty #'router {:port (Integer. port) :join? false})))

