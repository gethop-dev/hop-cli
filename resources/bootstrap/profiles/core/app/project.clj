(defproject {{project.name}} "0.1.0-SNAPSHOT"
  :min-lein-version "2.11.2"
  :dependencies [[org.clojure/clojure "1.12.0"]
                 [duct/core "0.8.1"]
                 [ring/ring "1.10.0"]
                 [duct/server.http.http-kit "0.1.4"
                  :exclusions [[http-kit]]]
                 [http-kit/http-kit "2.8.0"]
                 [duct/module.logging "0.5.0"]
                 [duct/logger.timbre "0.5.0"]
                 [com.taoensso/timbre "6.6.1"]
                 [metosin/jsonista "0.3.13"]
                 [metosin/reitit "0.9.1"]
                 [metosin/ring-swagger-ui "5.20.0"]
                 [metosin/malli "0.19.1"]
                 [com.widdindustries/cljc.java-time "0.1.21"]
                 {{#project.dependencies}}{{&.}}{{/project.dependencies}}]
  :plugins [[duct/lein-duct "0.12.3"]]
  :main ^:skip-aot {{project.files-name}}.main
  :resource-paths ["resources" "target/resources" "target/resources/{{project.files-name}}"]
  :middleware [lein-duct.plugin/middleware]
  :profiles {:dev [:project/dev :profiles/dev]
             :repl {:prep-tasks ^:replace ["javac" "compile"]
                    :dependencies [[cider/piggieback "0.6.0"]]
                    :jvm-opts ["-Djdk.attach.allowAttachSelf"]
                    :repl-options {:init-ns user
                                   :nrepl-middleware [cider.piggieback/wrap-cljs-repl]
                                   :host "0.0.0.0"
                                   :port 4001}}
             :uberjar {:aot :all :prep-tasks ["javac" "compile" ["run" ":duct/compiler"]]}
             :profiles/dev {}
             :project/dev {:eastwood {:linters [:all]
                                      :exclude-linters
                                      [:keyword-typos :boxed-math :non-clojure-file :performance :unused-namespaces]
                                      :debug [:progress :time]}
                           :resource-paths ["dev/resources"]
                           :source-paths ["dev/src"]
                           :plugins [[dev.weavejester/lein-cljfmt "0.13.1"]
                                     [jonase/eastwood "1.4.3"]]
                           :dependencies [[integrant/repl "0.4.0"]
                                          [hawk/hawk "0.2.11"]
                                          [ring/ring-mock "0.4.0"]
                                          {{#project.dev-dependencies}}{{&.}}{{/project.dev-dependencies}}]}}
  :uberjar-name "{{project.name}}-standalone.jar"
  :test-selectors {:default (fn [m] (not (or (:integration m) (:regression m))))
                   :all (constantly true)
                   :integration :integration
                   :regression :regression})
