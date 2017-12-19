(ns pokequiz.views
  (:require [re-frame.core :as re-frame]
            [pokequiz.events :as events]
            [pokequiz.subs :as subs]))

(defn option-notification
  [option]
  [:a.tile.is-child {:key (str "item-id-" (:id option)) :on-click #(re-frame/dispatch [:select (:answer-id option)])}
   [:figure.image.is-1by1.notification
    [:img {:src (:img-src option) :title (:name option) :alt (:name option) :style {:opacity (if (:selected option) 1 0.5)}}]]])

(defn main-panel []
  [:div.tile.is-ancestor
   [:div.tile.is-vertical
    [:div.tile
     [:div.tile.is-4.is-parent
      [:article.tile.is-child.notification.is-info
       (let [score (re-frame/subscribe [::subs/score])]
         [:p.title "Current score: " @score])
       (let [tt (re-frame/subscribe [::subs/total-tries])]
         [:p.subtitle "Total tries is " @tt])
       (let [tries (re-frame/subscribe [::subs/tries-left])]
         (if @tries [:p.subtitle "You can try " @tries " more times."]))]]
     [:div.tile.is-8.is-parent
      [:article.tile.is-child.notification.is-warning
       (let [question (re-frame/subscribe [::subs/question])
             nr (count (:answer @question))]
         (cond
           (:answer @question)
           [:p.title "Which " (if (= nr 1) "is" "are") " the " (count (:answer @question)) " pokemon where " (:property @question) " is " (:value @question) "?"]
           (:waiting @question)
           [:p.title "Waiting for the new question to load."]
           (:finished @question)
           [:p.title "Well done, now get your present from the bottom drawer in Gerard's room."]
           :else
           [:div
            [:p.title "As soon as you press the next question button, we start getting data to generate a question, this can take a while."]
            [:p.subtitle "You have 3 chances to get a question right. You (de)select pokemon by clinking on them.
            When you have selected the asked amount the answer is evaluated."]]))
       (let [show (re-frame/subscribe [::subs/next-enabled])]
         (if @show
           [:button.button.is-outlined.is-pulled-right {:on-click #(re-frame/dispatch [::events/next-question])} "Next question"]))
       (let [show (re-frame/subscribe [::subs/re-start-enabled])]
         (if @show
           [:button.button.is-outlined.is-pulled-right {:on-click #(re-frame/dispatch-sync [::events/initialize-db])} "Restart game"]))]]]
    [:div.tile
     (let [options (re-frame/subscribe [::subs/options])
           parts (partition 2 @options)]
       (if (= (count parts) 4)
         (for [row (range 4)] [:div.tile.is-3.is-parent.is-vertical {:key (str "row-" row)}
                               (for [opt-n (nth parts row)] (option-notification opt-n))])))]]])