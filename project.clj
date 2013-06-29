(defproject ped-spikes "0.1.0-SNAPSHOT"
  :description "Sample TODO app written with pure Pedestal."
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.0"]
                 [org.clojure/tools.namespace "0.2.1"]
                 
                 [domina "1.0.1"]
                 [ch.qos.logback/logback-classic "1.0.6"]
                 [org.clojure/clojurescript "0.0-1450"]
                 [io.pedestal/pedestal.app "0.1.2"]
                 [io.pedestal/pedestal.app-tools "0.1.2"]]
  :profiles {:dev {:source-paths ["dev"]}}
  :source-paths ["app/src" "app/templates"]
  :resource-paths ["config"]
  :aliases {"dumbrepl" ["trampoline" "run" "-m" "clojure.main/main"]})
