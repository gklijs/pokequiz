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
    (if
      (= 10 (:score new-db))
      (-> new-db
          (assoc :re-start-enabled true)
          (assoc :question {:finished true}))
      (assoc new-db :next-enabled true))))

(defn clean-selection
  [options]
  (mapv #(assoc % :selected false) options))

(defn set-awnser
  [answer options]
  (mapv #(assoc % :selected (contains? answer (:answer-id %))) options))

(defn failed-try
  [db]
  (if (= 1 (:tries-left db))
    (-> db
        (update :total-tries inc)
        (assoc :tries-left nil)
        (assoc :next-enabled true)
        (update :options (partial set-awnser (get-in db [:question :answer]))))
    (-> db
        (update :tries-left dec)
        (update :total-tries inc)
        (update :options clean-selection))))

(re-frame/reg-event-db
  ::initialize-db
  (fn [_ _]
    db/default-db))

(re-frame/reg-event-db
  :set-options
  (fn [db [_ options]]
    (assoc db :options options)))

(re-frame/reg-event-db
  :set-question
  (fn [db [_ question]]
    (assoc db :question question)))

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
    (generate)
    (-> db
        (assoc :next-enabled false)
        (assoc :tries-left 3)
        (assoc :options nil)
        (assoc :question {:waiting true}))))
