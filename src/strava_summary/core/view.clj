(ns strava-summary.core.view
  (:require [net.cgrand.enlive-html :as html])
  (:import [java.io StringReader]))


(html/deftemplate summary-template "summary.html" [stats]
  [:div.w_sport_1] (html/content (get-in stats [:weekly :sport_1]))
  [:div.w_sport_2] (html/content (get-in stats [:weekly :sport_2]))
  [:div.w_sport_3] (html/content (get-in stats [:weekly :sport_3]))
  
  [:div.m_sport_1] (html/content (get-in stats [:monthly :sport_1]))
  [:div.m_sport_2] (html/content (get-in stats [:monthly :sport_2]))
  [:div.m_sport_3] (html/content (get-in stats [:monthly :sport_3]))

  [:div.y_sport_1] (html/content (get-in stats [:yearly :sport_1]))
  [:div.y_sport_2] (html/content (get-in stats [:yearly :sport_2]))
  [:div.y_sport_3] (html/content (get-in stats [:yearly :sport_3]))
  )

(defn summary-html [stats]
  (apply str (summary-template stats)))


(html/deftemplate hello-world-template (java.io.StringReader. "<html><body><h1></h1></body></html>") []
  [:html :body]) (html/content "Hello")

(defn hello-world []
  (apply str (hello-world-template)))