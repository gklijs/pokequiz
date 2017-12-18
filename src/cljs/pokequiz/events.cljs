(ns pokequiz.events
  (:require [re-frame.core :as re-frame]
            [pokequiz.db :as db]))

(re-frame/reg-event-db
  ::initialize-db
  (fn [_ _]
    db/default-db))

(re-frame/reg-event-db
  :set-result
  (fn [db [_ result]]
    (assoc db :result result)))

(re-frame/reg-event-db
  :set-options
  (fn [db [_ options]]
    (assoc db :options options)))

(re-frame/reg-event-db
  :set-question
  (fn [db [_ question]]
    (assoc db :question question)))

(re-frame/reg-event-db
  :inc-score
  (fn [db [_]]
    (update db :score inc)))
