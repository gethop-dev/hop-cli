(ns hop-cli.bootstrap.profile.frontend)

(defn- cljs-module
  [settings]
  {:duct.module/cljs
   {:main (symbol (str (:project/name settings) ".client"))}
   :hydrogen.module/core
   {:externs
    {:production []}
    :figwheel-main {}}})

(defn profile
  [settings]
  {:files [{:src "frontend"}]
   :dependencies '[[cljs-ajax/cljs-ajax "0.8.4"]
                   [cljsjs/react "17.0.2-0"]
                   [cljsjs/react-dom "17.0.2-0"]
                   [day8.re-frame/http-fx "0.2.4"]
                   [re-frame/re-frame "1.1.2"]
                   [reagent/reagent "1.1.1"]
                   [com.taoensso/tempura "1.3.0"]
                   [hydrogen/module.cljs "0.5.2"]
                   [hydrogen/module.core "0.4.2"]]
   :config-edn {:modules (cljs-module settings)}})
