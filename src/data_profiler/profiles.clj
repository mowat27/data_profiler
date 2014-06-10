(ns data-profiler.profiles
  (:require [com.stuartsierra.component :as component]
            [datomic.api :as d]))

(defn all [this]
  (d/q '[:find ?e ?name ?source 
         :where 
         [?e :profile/name ?name]
         [?e :profile/source ?source]] 
       (d/db (:connection this))))

(defrecord Profiles []
  component/Lifecycle
  (start [this] 
    (d/transact 
     (d/connect (get-in this [:database :uri])) 
     (mapcat vec 
             (for [[e-name e-source] 
                   {:elements "http://introcs.cs.princeton.edu/java/data/elements.csv"
                    :ip-by-country "/Users/adrian/Desktop/ip-by-country.csv"}]
               (let [entity (d/tempid :db.part/user)] 
                 [[:db/add entity :profile/name (name e-name)]
                  [:db/add entity :profile/source e-source]]))))
    this)
  (stop [this] this))

(defn new-profiles []
  (component/using 
   (->Profiles)
   [:connection :database :schema]))

(comment 
  (all (:profiles user/system))
  )
