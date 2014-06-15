(ns data-profiler.profiles
  (:require [com.stuartsierra.component :as component]
            [datomic.api :as d]
            [data-profiler.csv :as csv]))

(defn all [this]
  (d/q '[:find ?e ?name ?uri
         :where 
         [?e :profile/name ?name]
         [?e :profile/uri ?uri]] 
       (d/db (:connection this))))

(defrecord Profiles []
  component/Lifecycle
  (start [this] 
    (d/transact 
     (d/connect (get-in this [:database :uri])) 
     (mapcat vec 
             (for [[e-name e-uri] csv/examples]
               (let [entity (d/tempid :db.part/user)] 
                 [[:db/add entity :profile/name (name e-name)]
                  [:db/add entity :profile/uri e-uri]]))))
    this)
  (stop [this] this))

(defn new-profiles []
  (component/using 
   (->Profiles)
   [:connection :database :schema]))

(comment 
  (all (:profiles user/system))
  )
