(ns data-profiler.web
  (:require [ring.adapter.jetty :refer (run-jetty)]
            [ring.middleware.reload :refer (wrap-reload)]
            [bidi.bidi :as bidi]
            [clostache.parser :refer (render-resource)]
            [data-profiler.csv :as csv]
            [data-profiler.profiler :as profiler]))

(defn using-layout [layout handler & {:keys [status]}]
  (let [response {:status (or status 200)}
        layouts {:application "views/layouts/application.mustache"}]
    (fn [req] 
      (assoc response :body (render-resource (layout layouts) 
                                             {:content (handler req)})))))

(defn index [] 
  (using-layout :application (fn [_] "<p>Hello World</p>")))

(def router (bidi/make-handler ["/" {"index.html" (index)}]))

(defn -main [& args] 
  (let [port (or (first args) "8080")] 
    (run-jetty #'router {:port (Integer. port) :join? false})))

