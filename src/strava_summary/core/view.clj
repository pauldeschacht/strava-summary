(ns strava-summary.core.view
  (:require [net.cgrand.enlive-html :as html])
  (:import [java.io StringReader]))

;; connect-html: html page with STRAVA connect button
;;
(html/deftemplate connect-template "connect.html" [])

(defn connect-html []
  (apply str (connect-template)))
;;
;; summary-html: html page to display the summary statistics
;;
(defn meter-to-km [m]
  (/ (float m) 1000))

(defn stat-to-html [stats keys]
  (->> (get-in stats keys)
       (meter-to-km)
       (format "%02.2f")
       (html/content)))

(html/deftemplate summary-template "summary.html" [stats]
  [:div#sport_1] (html/content (:sport_1 stats))
  [:div#sport_2] (html/content (:sport_2 stats))
  [:div#sport_3] (html/content (:sport_3 stats))
  
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

(defn summary-html
  ([]
     (summary-html {:weekly  {:sport_1 1 :sport_2 2 :sport_3 3}
                    :monthly {:sport_1 10 :sport_2 20 :sport_3 30}
                    :yearly  {:sport_1 100 :sport_2 200 :sport_3 300}
                    }))
  ([stats]
     (apply str (summary-template stats))))

(html/deftemplate activities-template (java.io.StringReader. "<html><body><h1>Activities</h1><div></div></body></html>") [content]
  [:html :body :div] (html/content (str content)))

(defn activities-html [activities]
  (apply str (activities-template (str activities))))

;;
;; test pages
;;
(html/deftemplate hello-world-template (java.io.StringReader. "<html><body><h1></h1></body></html>") [name]
  [:html :body :h1] (html/content (str name)))

(defn hello-world-html
  ([]
     (hello-world-html "Stranger"))
  ([name]
     (apply str (hello-world-template name)))
  )
