(ns hop-cli.bootstrap.profile.registry.frontend
  (:require [hop-cli.bootstrap.profile.registry :as registry]
            [hop-cli.bootstrap.util :as bp.util]))

(defn- cljs-module
  [settings]
  (let [project-name (bp.util/get-settings-value settings :project/name)]
    {:duct.module/cljs
     {:main (symbol (str project-name ".client"))}
     :hydrogen.module/core
     {:externs
      {:production []}
      :figwheel-main {}}}))

(defn- sass-compiler
  []
  {:duct.compiler/sass
   {:source-paths ["resources"]
    :output-path "target/resources"}})

(defn- root-static-route
  [settings]
  (let [project-name (bp.util/get-settings-value settings :project/name)]
    {(keyword (str project-name ".static/root")) {}}))

(defn- routes
  [settings]
  (let [project-name (bp.util/get-settings-value settings :project/name)]
    [(tagged-literal 'ig/ref (keyword (str project-name ".static/root")))]))

(defmethod registry/pre-render-hook :frontend
  [_ settings]
  {:files [{:src "frontend"}]
   :dependencies '[[org.clojure/clojurescript "1.11.60"]
                   [cljs-ajax/cljs-ajax "0.8.4"]
                   [cljsjs/react "17.0.2-0"]
                   [cljsjs/react-dom "17.0.2-0"]
                   [day8.re-frame/http-fx "0.2.4"]
                   [re-frame/re-frame "1.1.2"]
                   [reagent/reagent "1.1.1"]
                   [com.taoensso/tempura "1.3.0"]
                   [hydrogen/module.cljs "0.5.2"]
                   [hydrogen/module.core "0.4.2"]
                   [duct/compiler.sass "0.2.1"]]
   :dev-dependencies '[[day8.re-frame/re-frame-10x "1.2.7"]]
   :config-edn {:routes (routes settings)
                :modules (cljs-module settings)
                :base (merge
                       (root-static-route settings)
                       (sass-compiler))}})
