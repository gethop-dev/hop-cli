(defproject {{project.name}} "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.9.8"
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [duct/core "0.8.0"]
                 [duct/module.web "0.7.3"]
                 [duct/module.logging "0.5.0"]
                 [metosin/reitit "0.5.18"]
                 {{#project.dependencies}}{{&.}}{{/project.dependencies}}]
  :plugins [[duct/lein-duct "0.12.3"]]
  :main ^:skip-aot {{#lambdas.to-snake-case}}{{project.name}}{{/lambdas.to-snake-case}}.main
  :resource-paths ["resources" "target/resources" "target/resources/{{#lambdas.to-snake-case}}{{project.name}}{{/lambdas.to-snake-case}}"]
  :middleware [lein-duct.plugin/middleware]
  :profiles {:dev [:project/dev :profiles/dev]
             :repl {:prep-tasks ^:replace ["javac" "compile"]
                    :dependencies [[cider/piggieback "0.5.2"]]
                    :repl-options {:init-ns user
                                   :nrepl-middleware [cider.piggieback/wrap-cljs-repl]
                                   :host "0.0.0.0"
                                   :port 4001}}
             :uberjar {:aot :all :prep-tasks ["javac" "compile" ["run" ":duct/compiler"]]}
             :profiles/dev {}
             :project/dev
             {:eastwood {:linters [:all]
                         :exclude-linters
                         [:keyword-typos :boxed-math :non-clojure-file :performance :unused-namespaces]
                         :debug [:progress :time]
                         :config-files ["eastwood_config.clj"]}
              :resource-paths ["dev/resources"]
              :source-paths ["dev/src"]
              :plugins [[lein-cljfmt "0.8.0"]
                        [jonase/eastwood "1.2.3"]]
              :dependencies [[integrant/repl "0.3.2"]
                             [hawk "0.2.11"]
                             [eftest "0.5.9"]
                             {{#project.dev-dependencies}}{{&.}}{{/project.dev-dependencies}}]}}
  :uberjar-name "{{project.name}}-standalone.jar"
  :test-selectors {:default (fn [m] (not (or (:integration m) (:regression m))))
                   :all (constantly true)
                   :integration :integration
                   :regression :regression})
