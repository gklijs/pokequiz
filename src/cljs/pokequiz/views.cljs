(ns pokequiz.views
  (:require [re-frame.core :as re-frame]
            [pokequiz.events :as events]
            [pokequiz.subs :as subs]))

(defn option-notification
  [option]
  [:div.column.is-half-mobile.is-half-tablet.is-one-quarter-desktop.is-one-quarter-widescreen.is-one-quarter-fullhd
   [:div.card {:key (str "item-id-" (:id option)) :on-click #(re-frame/dispatch [:select (:id option)])}
    [:header.card-header
     [:p.card-header-title.is-centered (:name option)]]
    [:div.card-image
     [:figure.image.is-1by1.notification
      [:img {:src (:img-src option) :title (:name option) :alt (:name option) :style {:opacity (if (:selected option) 1 0.5)}}]]]]])

(defn main-panel []
  [:div
   [:div.tile.is-ancestor
    [:div.tile.is-vertical
     [:div.tile
      [:div.tile.is-4.is-parent
       [:article.tile.is-child.notification.is-info
        (let [score (re-frame/subscribe [::subs/score])]
          [:p.title "Score: " @score])
        (let [tt (re-frame/subscribe [::subs/total-tries])]
          [:p.subtitle "Totaal aantal pogingen is: " @tt])
        (let [tries (re-frame/subscribe [::subs/tries-left])]
          (if @tries [:p.subtitle "Je hebt voor deze vraag nog " @tries " pogingen."]))]]
      [:div.tile.is-8.is-parent
       [:article.tile.is-child.notification.is-warning
        (let [question (re-frame/subscribe [::subs/question])
              nr (count (:answer @question))]
          (cond
            (:answer @question)
            [:p.title "Welke " (count (:answer @question)) " pokemon " (if (= nr 1) "heeft" "hebben") " voor de eigenschap " (:property @question) " de waarde '" (:value @question) "'?"]
            (:finished @question)
            [:p.title "Goed gedaan Viento, je cadeau ligt in de onderste la op Gerard zijn kamer."]
            :else
            [:div
             [:p.title "Het duurt even voor je naar de eerste vraag kan. Zodra hij klaar is verschijnt vanzelf de knop."]
             [:p.subtitle "Je hebt voor iedere vraag drie kansen om het goede antwoord te geven. Je kun pokemon (de)selecteren door ze aan te klikken.
            na drie keer wordt het antwoord gegeven, maar krijg je geen punt. Probeer met zo min mogelijk pogingen 10 goede antwoorden te geven."]]))
        (let [show (re-frame/subscribe [::subs/next-enabled])]
          (if @show
            [:button.button.is-outlined.is-pulled-right {:on-click #(re-frame/dispatch [::events/next-question])} "Volgende vraag"]))
        (let [show (re-frame/subscribe [::subs/re-start-enabled])]
          (if @show
            [:button.button.is-outlined.is-pulled-right {:on-click #(re-frame/dispatch-sync [::events/re-start])} "Herstart spel"]))]]]]]
      (let [options (re-frame/subscribe [::subs/options])
            parts (partition 4 @options)]
        (if (= (count parts) 2)
          (for [row (range 2)] [:div.columns.is-mobile.is-multiline {:key (str "row-" row)}
                                (for [opt-n (nth parts row)] (option-notification opt-n))])))])