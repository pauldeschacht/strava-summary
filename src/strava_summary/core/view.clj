(ns strava-summary.core.view
  (:require [net.cgrand.enlive-html :as html])
  (:import [java.io StringReader]))

(defn stat-to-html [stats keys]
  (html/content (str (get-in stats keys))))

(html/deftemplate summary-template "summary.html" [stats]
  [:div#w_sport_1] (stat-to-html stats [:weekly :sport_1])
  [:div#w_sport_2] (stat-to-html stats [:weekly :sport_2])
  [:div#w_sport_3] (stat-to-html stats [:weekly :sport_3])
  
  [:div#m_sport_1] (stat-to-html stats [:monthly :sport_1])
  [:div#m_sport_2] (stat-to-html stats [:monthly :sport_2])
  [:div#m_sport_3] (stat-to-html stats [:monthly :sport_3])

  [:div#y_sport_1] (stat-to-html stats [:yearly :sport_1])
  [:div#y_sport_2] (stat-to-html stats [:yearly :sport_2])
  [:div#y_sport_3] (stat-to-html stats [:yearly :sport_3])
  )

(defn summary-html [stats]
  (apply str (summary-template stats)))

(defn summary-html-test []
  (summary-html {:weekly  {:sport_1 1 :sport_2 2 :sport_3 3}
                 :monthly {:sport_1 10 :sport_2 20 :sport_3 30}
                 :yearly  {:sport_1 100 :sport_2 200 :sport_3 300}
                 }))

(html/deftemplate hello-world-template (java.io.StringReader. "<html><body><h1></h1></body></html>") []
  [:html :body :h1] (html/content "Hello"))

(defn hello-world []
  (apply str (hello-world-template)))
