(ns data-profiler.web
  (:require [com.stuartsierra.component :refer (Lifecycle)]
            [modular
             [bidi :refer (WebService)]
             [ring :refer (RingBinding)]]
            [bidi.bidi :as bidi]
            [ring.middleware.params :refer (wrap-params)]
            [clostache.parser :refer (render-resource)]
            [data-profiler
             [profiler :as profiler]
             [profiles :as profiles]]
            [clojure.pprint :refer (pprint)]
            [clojure.edn :as edn]
            [clojure.pprint :refer :all]))

(declare make-routes)

(defn assert-required-keys! [m & required-keys]
  (doseq [key required-keys]
    (assert (key m) (str "Missing required value " key))))

(defn displayable-string [x]
  (cond (nil? x) "nil"
        (empty? x) "\"\""
        :else x))

(defn show-rows-path [file-name field attr value]
  (format "%s?where=%s"
          (bidi/path-for (make-routes) :show-rows :file-name file-name :limit "all" )
          (with-out-str (pr [field attr value]))))

(defn render-formats [{file-name :name :as m} field]
  (->> (get-in m [:formats field])
       (map (fn [{fmt :value c :count}] 
              {:format (displayable-string fmt)
               :count c
               :show-path (show-rows-path file-name field :format fmt)}))))

(defn render-common-values [{file-name :name :as m} field limit] 
  (->> (get-in m [:values field])
       (take limit)
       (map (fn [{val :value c :count}]
              {:value (displayable-string val)
               :count c
               :show-path (show-rows-path file-name field :value val)}))))

(defn add-fields [m]
  (assert-required-keys! m :field-names :name :row-count)
  (assoc m :fields (for [field (:field-names m)]
                     (let [file-name (:name m)]
                       {:name       (name field)
                        :uniqueness    (get-in m [:uniqueness field])
                        :common-values (render-common-values m field 10)
                        :formats       (render-formats m field)}))))

(defn add-name [m s]
  (assoc m 
    :name s
    :profile-path  (bidi/path-for (make-routes) :show-profile :file-name s)
    :examples-path (bidi/path-for (make-routes) :show-rows :file-name s :limit 10)))

(defn add-source [m s]
  (assoc m :source s))

(defn apply-condition [[entity attr value] rows]
    (cond (= attr :format) (profiler/where profiler/codify-format (keyword entity) value rows)
          (= attr :value)  (filter #(= value (get % (keyword entity))) rows)))

(defn add-values [m & {:keys [limit where]}]
  (assert-required-keys! m :field-names :rows)
  (assoc m :rows (let [fields (:field-names m)
                       rows (:rows m)
                       values (if where
                                (for [row (apply-condition where rows)] 
                                  {:values (map row fields)})
                                (for [row rows] 
                                  {:values (map row fields)}))] 
                   (if (= "all" limit)
                     values
                     (take (Integer/parseInt limit) values)))))

(defn render-template [m template]
  (render-resource template m))

(defn show-profile [req]
  (let [source (-> req :route-params :file-name)] 
    {:status 200 
     :body (-> (profiler/profile source) 
               (add-name source)
               (add-source "TODO")
               add-fields
               (render-template "views/layouts/dashboard.mustache"))}))

(defn show-rows [{:keys [query-params] :as req}]
  (let [source (-> req :route-params :file-name)
        limit  (-> req :route-params :limit)]
    {:status 200 
     :body 
     (let [conditions (when (get query-params "where")
                        ((fn [[e a v]] [(keyword e) (keyword a) v]) (edn/read-string (get query-params "where"))))] 
       (-> (profiler/profile source)
           (add-name source)
           (add-source "TODO")
           add-fields
           (add-values :limit limit :where conditions)
           (render-template "views/layouts/rows.mustache")))}))

(defn add-available-profiles [m model]
  (assoc m :profiles 
         (for [[_ name source] (profiles/all model)] 
           (-> {}
               (add-name name)
               (add-source source)))))

(defn index [req]
  {:status 200 
   :body   (-> {}
               (add-available-profiles (:profiles req))
               (render-template "views/layouts/index.mustache"))})

(defn make-handlers []
  {:index        index
   :show-profile show-profile
   :show-rows    (wrap-params show-rows)})

(defn make-routes [] 
  ["/" 
   {"" :index
    "profile/" {[:file-name]                 :show-profile
                [:file-name "/rows/" :limit] :show-rows}}])


(defrecord Website [uri-context]
  Lifecycle
  (start [this] this)
  (stop  [this] this)

  RingBinding
  (ring-binding [this req] 
    (assoc req :profiles (:profiles this)))

  WebService
  (ring-handler-map [this] (make-handlers))
  (routes [this] (make-routes))
  (uri-context [_] uri-context))


(defn new-website
  ([] 
     (new-website ""))
  ([uri-context] 
     (->Website uri-context)))

