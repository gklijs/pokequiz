(ns pokequiz.question-generator
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]
            [clojure.string :as string]
            [re-frame.core :as re-frame]))

(defonce cache (atom {}))

(defn add-random
  [coll value]
  (let [split-coll (split-at (rand-int (count coll)) coll)]
    (flatten [(first split-coll) value (second split-coll)])))

(defn do-on-get
  [f href]
  (if-let [result-from-cache (get @cache href)]
    (f result-from-cache)
    (go (let [response (<! (http/get href
                                     {:with-credentials? false}))]
          (if
            (= 200 (:status response))
            (let [result (:body response)
                  _ (swap! cache #(assoc % href result))]
              (f result)))))))

(defn do-pokemon-by-id
  [f id]
  (do-on-get f (str "http://localhost:8000/api/v2/pokemon/" id)))

(defn random-pokemon-ids
  ([total] (random-pokemon-ids total #{}))
  ([total id-set]
   (let [new-set (conj id-set (+ 1 (rand-int 801)))]
     (if
       (= total (count new-set))
       new-set
       (recur total new-set)))))

(defn get-type-question
  [pokemon]
  (let [type (:type (rand-nth (:types pokemon)))
        val (:name type)
        has-type-f (fn [r] (let [types (:types r)]
                             (or (= val (:name (:type (first types)))) (= val (:name (:type (second types)))))))]
    {:property    "type"
     :value       val
     :has-value-f has-type-f
     :more-url    (:url type)}))

(defn get-ability-question
  [pokemon]
  (let [ability (:ability (rand-nth (:abilities pokemon)))
        val (:name ability)
        has-ability-f (fn [r] (let [abilities (:abilities r)]
                                (< 0 (count (filter #(= val (get-in % [:ability :name])) abilities)))))]
    {:property    "ability"
     :value       val
     :has-value-f has-ability-f
     :more-url    (:url ability)}))

(def question-functions [get-type-question get-ability-question])

(defn get-question
  [pokemon]
  (let [question-function (rand-nth question-functions)]
    (question-function pokemon)))

(defn id-from-url
  [url]
  (int (last (string/split url #"/"))))

(defn add-from-list [result-list to-add ids add-to-result loops]
  (if (= 10 loops)
    (let [add-random-ids (random-pokemon-ids (+ (count ids) to-add )ids)
          added (remove ids add-random-ids)]
      (doseq [id added] (do-pokemon-by-id add-to-result id)))
    (let [pokemon (:pokemon (rand-nth (:pokemon result-list)))
          id (id-from-url (:url pokemon))]
      (if (or (contains? ids id) (> id 802))
        (recur result-list to-add ids add-to-result (inc loops))
        (let [p-rep {:name (:name pokemon) :id id :valid-answer true}
              _ (add-to-result p-rep)]
          (if (not (= 1 to-add))
            (recur result-list (dec to-add) (conj ids id) add-to-result (inc loops))))))))

(defn set-question
  [pokemon question add-to-result ids]
  (let [new-question (reset! question (get-question pokemon))]
    (do-on-get #(add-from-list % 2 ids add-to-result 0) (:more-url new-question))))

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
              (assoc :id k)
              (assoc :name (:name v))
              (assoc :img-src (str "https://assets.pokemon.com/assets/cms2/img/pokedex/detail/" (id-part v) ".png"))
              (assoc :selected false))))

(defn question-reducer
  [f c k v]
  (if (f v) (conj c k) c))

(defn set-answer
  [question result]
  (let [include #(or (:valid-answer %) ((:has-value-f question) %))
        answer (reduce-kv (partial question-reducer include) [] result)]
    (-> question
        (dissoc :has-value-f)
        (dissoc :more-url)
        (assoc :answer (set answer)))))

(defn process-pokemon
  [question result]
  (let [v-result (vec result)
        new-question (swap! question #(set-answer % v-result))
        options (reduce-kv option-reducer [] v-result)]
    (re-frame/dispatch [:set-next [new-question options]])))

(defn generate
  []
  (let [ids (random-pokemon-ids 6)
        result (atom [])
        add-to-result (fn [item] (swap! result #(add-random % item)))
        question (atom nil)]
    (add-watch result :on-first (fn [_ _ _ n] (if (= 1 (count n)) (set-question (first n) question add-to-result ids))))
    (add-watch result :when-done (fn [_ _ _ n] (if (= 8 (count n)) (process-pokemon question n))))
    (doseq [id ids] (do-pokemon-by-id add-to-result id))))