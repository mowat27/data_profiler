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

(defn assert-required-keys! [m code-ref & required-keys]
  (doseq [key required-keys]
    (assert (key m) (format "Missing required attribute in %s '%s'" code-ref key))))

(defn displayable-string [x]
  (cond (nil? x) "nil"
        (empty? x) "\"\""
        :else x))

(defn show-rows-path [profile-name field attr value]
  (format "%s?where=%s"
          (bidi/path-for (make-routes) :show-rows :profile-name profile-name :limit "all" )
          (with-out-str (pr [field attr value]))))

(defn render-formats [{profile-name :profile-name :as m} field]
  (->> (get-in m [:formats field])
       (map (fn [{fmt :value c :count}] 
              {:format (displayable-string fmt)
               :count c
               :show-path (show-rows-path profile-name field :format fmt)}))))

(defn render-common-values [{:keys [profile-name] :as m} field limit] 
  (->> (get-in m [:values field])
       (take limit)
       (map (fn [{val :value c :count}]
              {:value (displayable-string val)
               :count c
               :show-path (show-rows-path profile-name field :value val)}))))

(defn add-fields [m]
  (assert-required-keys! m "add-fields" :field-names :profile-name :row-count)
  (assoc m :fields (for [field (:field-names m)]
                     (let [profile-name (:profile-name m)]
                       {:name       (name field)
                        :uniqueness    (get-in m [:uniqueness field])
                        :common-values (render-common-values m field 10)
                        :formats       (render-formats m field)}))))

(defn add-profile-paths [{:keys [profile-name] :as m}]
  (assert-required-keys! m "add-profile-paths" :profile-name)
  (assoc m 
    :profile-path  (bidi/path-for (make-routes) :show-profile :profile-name profile-name)
    :examples-path (bidi/path-for (make-routes) :show-rows :profile-name profile-name :limit 10)))

(defn add-values [m & {:keys [limit where]}]
  (assert-required-keys! m "add-values" :field-names :rows)
  (assoc m :rows (let [{:keys [:field-names :rows]} m
                       rows (map (partial zipmap field-names) rows)
                       values (if where
                                (for [row (profiler/apply-condition where rows)] 
                                  {:values (map row field-names)})
                                (for [row rows] 
                                  {:values (map row field-names)}))] 
                   (if (= "all" limit)
                     values
                     (take (Integer/parseInt limit) values)))))

(defn render-template [m template]
  (render-resource template m))

(defn show-profile [req]
  (let [profile-name (-> req :route-params :profile-name)] 
    {:status 200 
     :body (-> (profiler/profile profile-name) 
               add-profile-paths
               add-fields
               (render-template "views/dashboard.mustache"))}))

(defn show-rows [{:keys [query-params] :as req}]
  (let [{:keys [profile-name limit]} (:route-params req)]
    {:status 200 
     :body 
     (let [conditions (when (get query-params "where")
                        ((fn [[e a v]] [(keyword e) (keyword a) v]) (edn/read-string (get query-params "where"))))] 
       (-> (profiler/profile profile-name)
           add-profile-paths 
           add-fields
           (add-values :limit limit :where conditions)
           (render-template "views/rows.mustache")))}))

(defn add-available-profiles [m model]
  (assoc m :profiles 
         (for [[_ profile-name uri] (profiles/all model)] 
           (-> {:profile-name profile-name
                :uri uri}
               add-profile-paths))))

(defn index [req]
  {:status 200 
   :body   (-> {}
               (add-available-profiles (:profiles req))
               (render-template "views/index.mustache"))})

(defn make-handlers []
  {:index        index
   :show-profile show-profile
   :show-rows    (wrap-params show-rows)})

(defn make-routes [] 
  ["/" 
   {"" :index
    "profile/" {[:profile-name]                 :show-profile
                [:profile-name "/rows/" :limit] :show-rows}}])


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

