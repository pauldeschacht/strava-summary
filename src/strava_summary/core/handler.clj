(ns strava-summary.core.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [strava-summary.core.view :as view]
            [strava-summary.core.strava :as strava]
            [strava-summary.core.db :as db]
            [clj-time.core :as t :exclude [extend second]]
            [clojure.tools.logging :as log]))

(def not-nil? (complement nil?))

(defn connect-strava [request]
  "return the page with the STRAVA connect button. Once the user is authorized at STRAVA, a redirect to /token_request is executed."
  (log/info "connect-strava")
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body (view/connect-html)
   }
  )

(defn exists-activities-in-db? [athlete-id]
  (not-nil? (db/athlete-first-last-activity athlete-id)))

(defn find-activity-type [activities type]
  (->> activities
       (filter #(= type (:activity_type %)))
       (first)))

(defn get-summary-activities [athlete-id]
  (let [periods (strava/summary-periods)
        now (:now periods)
        last-week (:last-week periods)
        last-month (:last-month periods)
        begin-year (:begin-year periods)

        _ (log/info (str "periods: " now "," last-week "," last-month "," begin-year "."))
        
        weekly-summaries (db/get-summary-activities athlete-id last-week now)
        monthly-summaries (db/get-summary-activities athlete-id last-month now)
        yearly-summaries (db/get-summary-activities athlete-id begin-year now)
        ]
    {:sport_1 "Swim"
     :sport_2 "Ride"
     :sport_3 "Run"

     :weekly {:sport_1 (:distance (find-activity-type weekly-summaries "Swim"))
              :sport_2 (:distance (find-activity-type weekly-summaries "Ride"))
              :sport_3 (:distance (find-activity-type weekly-summaries "Run"))}
     
     :monthly {:sport_1 (:distance (find-activity-type monthly-summaries "Swim"))
               :sport_2 (:distance (find-activity-type monthly-summaries "Ride"))
               :sport_3 (:distance (find-activity-type monthly-summaries "Run"))}
     
     :yearly {:sport_1 (:distance (find-activity-type yearly-summaries "Swim"))
              :sport_2 (:distance (find-activity-type yearly-summaries "Ride"))
              :sport_3 (:distance (find-activity-type yearly-summaries "Run"))}
     }))


(defn show-summaries [athlete-id]
  (log/info "show summaries")
  (let [summaries (get-summary-activities athlete-id)
        ]
    {:status 200
     :headers {"Content-Type" "text/html; charset=utf-8"}
     :body (view/summary-html summaries)}))

(defn refresh-and-show-summaries [access-token athlete-id]
  (log/info "refresh-and-show-summaries")
  (do
    (db/remove-activities-for-athlete athlete-id)
    (db/insert-activities
     (strava/get-all-activities-current-athlete access-token))
    (show-summaries athlete-id)
    )
  )

(defn get-field-from-cookie [request field]
  (when-let [cookies (:cookies request)]
    (when-let [access-token (get cookies field)]
      (:value access-token))))

(defn get-access-token-from-cookie [request]
  (get-field-from-cookie request "access"))

(defn get-athlete-id-from-cookie [request]
  (get-field-from-cookie request "athlete-id"))

(defn connect [request]
  (log/info "connect")
  (let [access-token (get-access-token-from-cookie request)
        athlete-id (get-athlete-id-from-cookie request)
        ]
    (if (nil? access-token)
      (connect-strava request) ;; once logged in to Strava, user is redirected to strava-token-request
      (if (false? (exists-activities-in-db? athlete-id))
        (refresh-and-show-summaries access-token athlete-id)
        (show-summaries athlete-id)
        ))))


(defn strava-token-request [request]
  (log/info "strava-token-request")
  (let [client_id 3424
        client_secret "c1344d27465a6166ec65f482d745ffbe234a0b49"
        code (:code (:params request))
        response (strava/token-request client_id client_secret code)
        ]
    (if (nil? response)
      {:status 400
       :headers {"Content-Type" "text/html; charset=utf-8"}
       :body "Unable to get access token from Strava"} ;;Use connect.html with error message
      
      {:status 200
       :headers {"Content-Type" "text/html; charset=utf-8"}
       :cookies {"access" (:access_token response) "athlete-id" (:id (:athlete response))}
       :body (view/hello-world-html (str (:firstname (:athlete response)) "," (:lastname (:athlete response))))})))


(defn hello-world [request]
  (log/info "hello-world")
  (view/hello-world-html))

(defn connect-user [request]
  (log/info "connect-user")
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body (view/hello-world-html)})

(defn connect-test [request]
  (log/info "connect-test")
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body (view/summary-html)})

(defn logout [request]
  (log/info "logout")
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :cookies {"ring-session" {:value "" :max-age 0}
             "access" {:value "" :max-age 0}
             "athlete-id" {:value "" :max-age 0}}
   :body "Logged out and thanks for all the fish"
   }
  )

(defroutes app-routes
  (GET "/test" request (connect-test request))
  (GET "/strava_token_request" request (strava-token-request request))
  (GET "/user/" request (connect-user request) )
  (GET "/" request (connect request))
  (GET "/logout" request (logout request))
  (route/resources "/resources")
  (route/not-found "Not Found"))

(def app
  (wrap-defaults app-routes site-defaults))
