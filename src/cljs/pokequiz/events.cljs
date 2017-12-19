(ns pokequiz.events
  (:require [re-frame.core :as re-frame]
            [pokequiz.question-generator :refer [generate]]
            [pokequiz.db :as db]))

(defn get-selected
  [options]
  (let [sel-options (filter #(:selected %) options)]
    (set (map #(:answer-id %) sel-options))))

(defn successful-try
  [db]
  (let [new-db (-> db
                   (update :score inc)
                   (update :total-tries inc)
                   (assoc :tries-left nil))]
    (cond (= 10 (:score new-db)) (-> new-db
                                     (assoc :re-start-enabled true)
                                     (assoc :question {:finished true}))
          (:next db) (assoc new-db :next-enabled true)
          :else (assoc new-db :show-next-when-ready true))))

(defn clean-selection
  [options]
  (mapv #(assoc % :selected false) options))

(defn set-awnser
  [answer options]
  (mapv #(assoc % :selected (contains? answer (:answer-id %))) options))

(defn failed-try
  [db]
  (if (= 1 (:tries-left db))
    (if (:next db)
      (-> db
          (update :total-tries inc)
          (assoc :tries-left nil)
          (assoc :next-enabled true)
          (update :options (partial set-awnser (get-in db [:question :answer]))))
      (-> db
          (update :total-tries inc)
          (assoc :tries-left nil)
          (assoc :show-next-when-ready true)
          (update :options (partial set-awnser (get-in db [:question :answer])))))
    (-> db
        (update :tries-left dec)
        (update :total-tries inc)
        (update :options clean-selection))))

(re-frame/reg-event-db
  ::initialize-db
  (fn [_ _]
    db/default-db))

(re-frame/reg-event-db
  :set-next
  (fn [db [_ next]]
    (if (:show-next-when-ready db)
      (-> db
          (assoc :show-next-when-ready false)
          (assoc :next next)
          (assoc :next-enabled true))
      (assoc db :next next))))

(re-frame/reg-event-db
  :select
  (fn [db [_ id]]
    (if (:tries-left db)
      (let [new-db (update-in db [:options id :selected] not)
            selection (get-selected (:options new-db))
            answer (get-in db [:question :answer])]
        (if (= (count selection) (count answer))
          (if (= selection answer)
            (successful-try new-db)
            (failed-try new-db))
          new-db))
      db)))

(re-frame/reg-event-db
  ::next-question
  (fn [db [_ _]]
    (if-let [next (:next db)]
      (do
        (generate)
        (-> db
            (assoc :next-question nil)
            (assoc :next-options nil)
            (assoc :question (first next))
            (assoc :options (second next))
            (assoc :next-enabled false)
            (assoc :tries-left 3)))
      db)))
