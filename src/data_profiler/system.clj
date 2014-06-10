(ns data-profiler.system
  (:refer-clojure :exclude (read))
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.logging :refer :all]
            [clojure.java.io :as io]
            [clojure.tools.reader :refer (read)]
            [clojure.tools.reader.reader-types :refer (indexing-push-back-reader)]
            [data-profiler 
             [web :refer (new-website)]
             [profiles :refer (new-profiles)]]
            [modular
             [http-kit :refer (new-webserver)]
             [ring :refer (new-ring-binder RingBinding)]
             [bidi :refer (new-router WebService)]
             [maker :refer (make)]
             [wire-up :refer (autowire-dependencies-satisfying)]
             [clostache :refer (new-clostache-templater)]
             [datomic :refer (new-datomic-database
                              new-datomic-connection
                              new-datomic-schema)]]))

(defn ^:private read-file
  [f]
  (read
   ;; This indexing-push-back-reader gives better information if the
   ;; file is misconfigured.
   (indexing-push-back-reader
    (java.io.PushbackReader. (io/reader f)))))

(defn ^:private config-from
  [f]
  (if (.exists f)
    (read-file f)
    {}))

(defn ^:private user-config
  []
  (config-from (io/file (System/getProperty "user.home") ".data_profiler.edn")))

(defn ^:private config-from-classpath
  []
  (if-let [res (io/resource "data_profiler.edn")]
    (config-from (io/file res))
    {}))

(defn config
  "Return a map of the static configuration used in the component
  constructors."
  []
  (merge (config-from-classpath)
         (user-config)))

(defn new-system [config]
  (let [system-map (component/system-map 
                    :webserver   (new-webserver :port 3000)
                    :router      (new-router)
                    :ring-binder (new-ring-binder)
                    :site        (new-website)
                    :database    (new-datomic-database :uri "datomic:mem://data_profiler"
                                                       :ephemeral? true)
                    :connection (new-datomic-connection)
                    :schema     (new-datomic-schema "resources/database/schema.edn")
                    :profiles   (new-profiles))]
    (component/system-using system-map
                            (-> {:webserver   [:ring-binder]
                                 :ring-binder {:ring-handler :router}
                                 :schema      [:connection]
                                 :connection  [:database]
                                 :site        [:profiles]}
                                (autowire-dependencies-satisfying system-map :router WebService)
                                (autowire-dependencies-satisfying system-map :ring-binder RingBinding)))))
