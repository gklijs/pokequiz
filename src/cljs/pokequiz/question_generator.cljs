(ns pokequiz.question-generator
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]
            [clojure.string :as string]
            [re-frame.core :as re-frame]))

(defonce cache (atom {}))

(def url-types {:nm "https://pokeapi.co/api/v2/pokemon/" :sp "https://pokeapi.co/api/v2/pokemon-species/"})

(defn do-on-get
  [f href]
  (if-let [result-from-cache (get @cache href)]
    (f result-from-cache)
    (go (let [response (<! (http/get href {:with-credentials? false}))]
          (if
            (= 200 (:status response))
            (let [result (:body response)
                  _ (swap! cache #(assoc % href result))]
              (f result)))))))

(defn do-pokemon-by-id
  [f u-key id]
  (do-on-get f (str (u-key url-types) id)))

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
    {:property    "vermogen"
     :value       val
     :has-value-f has-ability-f
     :more-url    (:url ability)}))

(defn get-habitat-question
  [pokemon]
  (let [habitat (:habitat pokemon)
        val (:name habitat)]
    {:property    "leefgebied"
     :value       val
     :has-value-f (fn [r] (= val (get-in r [:habitat :name])))
     :more-url    (:url habitat)}))

(defn get-color-question
  [pokemon]
  (let [color (:color pokemon)
        val (:name color)]
    {:property    "kleur"
     :value       val
     :has-value-f (fn [r] (= val (get-in r [:color :name])))
     :more-url    (:url color)}))

(defn get-shape-question
  [pokemon]
  (let [shape (:shape pokemon)
        val (:name shape)]
    {:property    "vorm"
     :value       val
     :has-value-f (fn [r] (= val (get-in r [:shape :name])))
     :more-url    (:url shape)}))

(defn get-generation-question
  [pokemon]
  (let [generation (:generation pokemon)
        val (:name generation)]
    {:property    "generatie"
     :value       val
     :has-value-f (fn [r] (= val (get-in r [:generation :name])))
     :more-url    (:url generation)}))

(def question-functions
  {:nm [get-type-question get-ability-question]
   :sp [get-habitat-question get-color-question get-shape-question get-generation-question]})

(defn get-question
  [pokemon u-key]
  (let [question-function (rand-nth (u-key question-functions))
        question (question-function pokemon)]
    (if (:value question)
      question
      (recur pokemon u-key))))

(defn id-from-url
  [url]
  (int (last (string/split url #"/"))))

(defn get-rnd-pokemon
  [list u-type]
  (cond
    (= u-type :nm) (:pokemon (rand-nth (:pokemon list)))
    (= u-type :sp) (rand-nth (:pokemon_species list))
    :else nil))

(defn add-from-list [result-list u-key to-add ids add-to-result loops]
  (if (= 10 loops)
    (let [add-random-ids (random-pokemon-ids (+ (count ids) to-add) ids)
          added (remove ids add-random-ids)]
      (doseq [id added] (do-pokemon-by-id add-to-result u-key id)))
    (let [pokemon (get-rnd-pokemon result-list u-key)
          id (id-from-url (:url pokemon))]
      (if (or (nil? id) (contains? ids id) (> id 802))
        (recur result-list u-key to-add ids add-to-result (inc loops))
        (let [p-rep {:name (:name pokemon) :id id :valid-answer true}
              _ (add-to-result p-rep)]
          (if (not (= 1 to-add))
            (recur result-list u-key (dec to-add) (conj ids id) add-to-result (inc loops))))))))

(defn set-question
  [pokemon u-key question add-to-result ids]
  (let [new-question (reset! question (get-question pokemon u-key))]
    (do-on-get #(add-from-list % u-key 2 ids add-to-result 0) (:more-url new-question))))

(defn get-id
  [v]
  (if-let [id (:id v)]
    id
    (if-let [varieties (:varieties v)]
      (if-let [defaults (filter #(true? (:is_default %)) varieties)]
        (id-from-url (:url (first defaults)))))))

(defn id-part
  [v]
  (let [id (get-id v)]
    (cond
      (> id 99) id
      (> id 9) (str "0" id)
      (> id 0) (str "00" id)
      :else "000")))

(defn option-reducer
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

(defn add-random
  [coll value]
  (let [split-coll (split-at (rand-int (+ 1 (count coll))) coll)]
    (flatten [(first split-coll) value (second split-coll)])))

(defn generate
  []
  (let [ids (random-pokemon-ids 6)
        u-key (rand-nth (keys url-types))
        result (atom [])
        add-to-result (fn [item] (swap! result #(add-random % item)))
        question (atom nil)]
    (add-watch result :on-first (fn [_ _ _ n] (if (= 1 (count n)) (set-question (first n) u-key question add-to-result ids))))
    (add-watch result :when-done (fn [_ _ _ n] (if (= 8 (count n)) (process-pokemon question n))))
    (doseq [id ids] (do-pokemon-by-id add-to-result u-key id))))