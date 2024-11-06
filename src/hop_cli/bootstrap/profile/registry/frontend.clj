;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

(ns hop-cli.bootstrap.profile.registry.frontend
  (:require [hop-cli.bootstrap.profile.registry :as registry]
            [hop-cli.bootstrap.util :as bp.util]))

(defn- cljs-module
  []
  {:dev.gethop.duct.module/cljs-compiler
   {:environments
    {:development
     {:compiler :figwheel-main
      :compiler-config {:options {:closure-defines {"re_frame.trace.trace_enabled_QMARK_" true}
                                  :preloads ['day8.re-frame-10x.preload]}}}}}})

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
   :dependencies '[[org.clojure/clojurescript "1.11.132"]
                   [cljs-ajax/cljs-ajax "0.8.4"]
                   [cljsjs/react "17.0.2-0"]
                   [cljsjs/react-dom "17.0.2-0"]
                   [day8.re-frame/http-fx "0.2.4"]
                   [re-frame/re-frame "1.4.3"]
                   [reagent/reagent "1.2.0"]
                   [com.taoensso/tempura "1.5.4"]
                   [dev.gethop/duct.module.cljs-compiler "0.1.0"]
                   [duct/compiler.sass "0.3.0"]
                   [com.widdindustries/cljc.java-time "0.1.21"]]
   :dev-dependencies '[[day8.re-frame/re-frame-10x "1.9.10"]]
   :dev-requires '[[duct.repl.figwheel :refer [cljs-repl]]]
   :config-edn {:routes (routes settings)
                :modules (cljs-module)
                :base (merge
                       (root-static-route settings)
                       (sass-compiler))}})
