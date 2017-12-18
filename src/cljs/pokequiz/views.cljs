(ns pokequiz.views
  (:require [re-frame.core :as re-frame]
            [pokequiz.pokedex :as pokedex]
            [pokequiz.subs :as subs]))

(defn option-notification
  [option]
  [:div.tile.is-child {:key (str "item-id-" (:id option))}
   [:figure.image.is-1by1.notification
    [:img {:src (:img-src option) :title (:name option) :alt (:name option)}]]])

(defn main-panel []
  [:div.tile.is-ancestor
   [:div.tile.is-vertical
    [:div.tile
     [:div.tile.is-4.is-parent
      (let [score (re-frame/subscribe [::subs/score])]
        [:article.tile.is-child.notification.is-info
         [:p.title "Current score: " @score]])]
     [:div.tile.is-8.is-parent
      (let [question (re-frame/subscribe [::subs/question])]
        [:article.tile.is-child.notification.is-warning
         (if (:entity @question)
           [:p.title "Which are the " (count (:answer @question)) " " (:entity @question) " for which " (:property @question) " is " (:value @question) " ?"]
           [:p.title "As soon as we fetched some data from the server, a question will pop up"]
           )])]]
    [:div.tile
     (let [options (re-frame/subscribe [::subs/options])
           parts (partition 3 3 @options)]
       (if (= (count parts) 3)
       (for [row (range 3)] [:div.tile.is-4.is-parent.is-vertical {:key (str "row-" row)}
                          (for [opt-n (nth parts row)] (option-notification opt-n))])))]
    [:div.tile
     [:div.tile.is-parent
      (let [result (re-frame/subscribe [::subs/result])]
        [:div.tile.is-child.notification
         [:div "Result is " @result]
         [:button.button.is-success.is-outlined
          {:on-click #(pokedex/next-question)} "test"]
         ])]
     ]]])