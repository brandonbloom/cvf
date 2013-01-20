(defproject cvf "0.1.0-SNAPSHOT"
  :description "Quick and dirty app to plot contributors vs forks"
  :url "https://github.com/brandonbloom/cvf"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [tentacles "0.2.4"]
                 [compojure "1.1.5"]
                 [hiccup "1.0.2"]
                 [ring/ring-jetty-adapter "1.0.3"]]
  :ring {:handler cvf.web/handler}
  :main cvf.web
  :plugins [[lein-ring "0.8.1"]])
