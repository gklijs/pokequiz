(ns pokequiz.events
  (:require [re-frame.core :as re-frame]
            [pokequiz.db :as db]))

(re-frame/reg-event-db
 ::initialize-db
 (fn  [_ _]
   db/default-db))

(re-frame/reg-event-db
  :set-result
  (fn [db [_ result]]
    (assoc db :result result)))
