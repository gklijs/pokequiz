(ns pokequiz.subs
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-sub
  ::options
  (fn [db]
    (:options db)))

(re-frame/reg-sub
  ::question
  (fn [db]
    (:question db)))

(re-frame/reg-sub
  ::score
  (fn [db]
    (:score db)))

(re-frame/reg-sub
  ::total-tries
  (fn [db]
    (:total-tries db)))

(re-frame/reg-sub
  ::tries-left
  (fn [db]
    (:tries-left db)))

(re-frame/reg-sub
  ::next-enabled
  (fn [db]
    (:next-enabled db)))

(re-frame/reg-sub
  ::re-start-enabled
  (fn [db]
    (:re-start-enabled db)))
