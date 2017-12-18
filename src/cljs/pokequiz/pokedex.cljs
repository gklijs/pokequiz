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

(defn random-pokemon-ids
  ([total] (random-pokemon-ids total []))
  ([total list]
   (let [new-list (dedupe (conj list (+ 1 (rand-int 801))))]
     (if
       (= total (count new-list))
       new-list
       (recur total new-list)))))

(defn id-part
  [v]
  (let [id (:id v)]
    (cond
      (> id 99) id
      (> id 9) (str "0" id)
      (> id 0) (str "00" id)
      :else "000"
      )))

(defn option-reducer
  "docstring"
  [c k v]
  (conj c (-> {}
              (assoc :answer-id k)
              (assoc :name (:name v))
              (assoc :id (:id v))
              (assoc :img-src (str "https://assets.pokemon.com/assets/cms2/img/pokedex/detail/" (id-part v) ".png"))
              (assoc :selected false))))

(defn set-options
  [result]
  (let [options (reduce-kv option-reducer [] result)]
    (re-frame/dispatch-sync [:set-options options])))

(defn question-reducer
  [f c k v]
  (if (f v) (conj c k) c))

(defn get-type-question
  [result]
  (let [type (:name (:type (rand-nth (:types (rand-nth result)))))
        has-type-f (fn [r] (let [types (:types r)]
                             (or (= type (:name (:type (first types)))) (= type (:name (:type (second types)))))))
        answer (reduce-kv (partial question-reducer has-type-f) [] result)]
    {:property "type"
     :entity   "pokemon"
     :value    type
     :answer   answer}))

(def question-functions [get-type-question])

(defn set-question
  [result]
  (let [question-function (rand-nth question-functions)]
    (re-frame/dispatch [:set-question (question-function result)])))

(defn process-pokemon
  [result]
  (set-options result)
  (set-question result)
  (re-frame/dispatch [:set-result "done"])
  (re-frame/dispatch [:inc-score]))

(defn next-question
  []
  (let [ids (random-pokemon-ids 9)
        result (atom [])
        add-to-result (fn [item] (swap! result #(conj % item)))]
    (add-watch result :when-done (fn [_ _ _ n] (if (= 9 (count n)) (process-pokemon n))))
    (doseq [id ids] (do-some-by-id add-to-result "pokemon" id))))