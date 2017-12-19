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
  db
  )

(defn failed-try
  [db]
  db
  )

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
    (let [new-db (update-in db [:options id :selected] not)
          selection (get-selected (:options new-db))
          answer (get-in db [:question :answer])]
      (if (= (count selection) (count answer))
        (if (= selection answer)
          (successful-try new-db)
          (failed-try new-db))
        new-db))))

(re-frame/reg-event-db
  ::next-question
  (fn [db [_ _]]
    (generate)
    (-> db
        (assoc :next-enabled false)
        (assoc :tries-left 3))))
