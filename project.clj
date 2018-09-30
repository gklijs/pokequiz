(defproject pokequiz "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.10.339"]
                 [reagent "0.8.1"]
                 [re-frame "0.10.6"]]
  :plugins [[lein-cljsbuild "1.1.5"]]
  :min-lein-version "2.5.3"
  :source-paths ["src/clj"]
  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]
  :figwheel {:css-dirs ["resources/public/css"]}
  :profiles
  {:dev
   {:dependencies [[binaryage/devtools "0.9.10"]
                   [cljs-http "0.1.45"]
                   [day8.re-frame/trace "0.1.22"]
                   [re-frisk "0.5.4"]]
    :plugins      [[lein-figwheel "0.5.14"]]}}
  :cljsbuild
  {:builds
   [{:id           "dev"
     :source-paths ["src/cljs"]
     :figwheel     {:on-jsload "pokequiz.core/mount-root"}
     :compiler     {:main                 pokequiz.core
                    :output-to            "resources/public/js/compiled/app.js"
                    :output-dir           "resources/public/js/compiled/out"
                    :asset-path           "js/compiled/out"
                    :source-map-timestamp true
                    :preloads             [devtools.preload
                                           day8.re-frame.trace.preload
                                           re-frisk.preload]
                    :closure-defines      {"re_frame.trace.trace_enabled_QMARK_" true}
                    :external-config      {:devtools/config {:features-to-install :all}}
                    }}

    {:id           "min"
     :source-paths ["src/cljs"]
     :compiler     {:main            pokequiz.core
                    :output-to       "resources/public/js/compiled/app.js"
                    :optimizations   :advanced
                    :externs         ["externs/pokeapi-js-wrapper.js"]
                    :closure-defines {goog.DEBUG false}
                    :pretty-print    false}}
    ]})
