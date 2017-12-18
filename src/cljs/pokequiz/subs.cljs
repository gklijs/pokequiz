(ns pokequiz.subs
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-sub
  ::options
  (fn [db]
    (:options db)))

(re-frame/reg-sub
  ::result
  (fn [db]
    (:result db)))

(re-frame/reg-sub
  ::question
  (fn [db]
    (:question db)))

(re-frame/reg-sub
  ::score
  (fn [db]
    (:score db)))
