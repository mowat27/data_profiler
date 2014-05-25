(defproject data_profiler "0.1.0-SNAPSHOT"
  :description "A data profiler written in Clojure"
  :url "https://github.com/mowat27/data_profiler"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :main data-profiler.web
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [camel-snake-kebab "0.1.5"]
                 [org.clojure/data.csv "0.1.2"]
                 
                 ;; Web
                 [ring/ring-core "1.3.0-RC1"]
                 [ring/ring-jetty-adapter "1.3.0-RC1"]
                 [de.ubercode.clostache/clostache "1.4.0"]
                 [bidi "1.10.3"]]
  :profiles {:dev {:dependencies [[ring/ring-devel "1.3.0-RC1"]]}})
