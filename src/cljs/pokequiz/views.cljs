(ns pokequiz.views
  (:require [re-frame.core :as re-frame]
            [pokequiz.pokedex :as pokedex]
            [pokequiz.subs :as subs]))

(defn main-panel []
  [:div (let [name (re-frame/subscribe [::subs/name])]
    [:div "Hello from " @name])
   (let [result (re-frame/subscribe [::subs/result])]
     [:div
      [:div "Result is " @result]
      [:button.button.is-success.is-outlined
       {:on-click #(pokedex/set-data-as-pokemon-name (+ 1 (rand-int 30)))}"test"]
      ])])
