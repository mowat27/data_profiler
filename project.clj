(defproject data_profiler "0.1.0-SNAPSHOT"
  :description "A data profiler written in Clojure"
  :url "https://github.com/mowat27/data_profiler"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [ ;; System
                 [org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.logging "0.2.6"]
                 [com.stuartsierra/component "0.2.1"]
                 [juxt.modular/maker "0.1.0"]
                 [juxt.modular/wire-up "0.1.0"]

                 ;; Data
                 [camel-snake-kebab "0.1.5"]
                 [org.clojure/data.csv "0.1.2"]
                 [com.datomic/datomic-free "0.9.4815"]
                 [juxt.modular/datomic "0.2.0"]

                 ;; Web
                 [juxt.modular/http-kit "0.4.0"]
                 [juxt.modular/bidi "0.4.0"]
                 [juxt.modular/clostache "0.1.0"]
                 [bidi "1.10.3"]
                 [ring "1.3.0-RC1"]
                 [de.ubercode.clostache/clostache "1.4.0"]]
  :profiles {:dev {:dependencies [[ring/ring-devel "1.3.0-RC1"]
                                  [org.clojure/tools.namespace "0.2.4"]]
                   :source-paths ["dev"]}})
