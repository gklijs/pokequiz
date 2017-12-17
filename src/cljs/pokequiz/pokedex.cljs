(ns pokequiz.pokedex
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]
            [re-frame.core :as re-frame]))

(defonce cache (atom {}))

(defn do-some-by-id
  [f something id]
  (if-let [result-from-cache (get-in @cache [something id])]
    (f result-from-cache)
    (go (let [response (<! (http/get (str "http://localhost:8000/api/v2/" something "/" id)
                                     {:with-credentials? false}))]
          (if
            (= 200 (:status response))
            (let [result (:body response)
                  _ (swap! cache #(assoc-in % [something id] result))]
              (f result)))))))

(defn set-data-as-pokemon-name
  [id]
  (do-some-by-id #(re-frame/dispatch-sync [:set-result (:name %)]) "pokemon" id))