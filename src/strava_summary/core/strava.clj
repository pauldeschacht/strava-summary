(ns strava-summary.core.strava
  (:require [clj-http.lite.client :as http-client]
            [clojure.data.json :as json]
            [clj-time.core :as t :exclude [extend second]]
            [clj-time.coerce :as c]
            [clj-time.format :as f]
            [clojure.tools.logging :as log]
            ))

(defn epoch-to-f [epoch]
  (f/unparse (f/formatters :date-time-no-ms) (c/from-long (* 1000 epoch))))

(defn epoch-ms-to-second [ms]
  (int (/ ms 1000)))

(def limit-jan-2013 ;;no activities before 2013
  (epoch-ms-to-second
   (c/to-long (t/date-time 2013 1 1))))

(defn summary-periods []
  (let [now (t/now)]
    {:now (epoch-ms-to-second (c/to-long now))
     :last-week (epoch-ms-to-second (c/to-long (t/minus now (t/days 7))))
     :last-month (epoch-ms-to-second (c/to-long (t/minus now (t/days 30))))
     :begin-year (epoch-ms-to-second (c/to-long (t/date-time (t/year (t/now)) 1 1)))}))

(defn strava-date-to-epoch [start_date_local]
  (epoch-ms-to-second
   (c/to-long
    (f/parse (f/formatters :date-time-no-ms) start_date_local))))


(defn token-request [client_id client_secret code]
  (let [http-response (http-client/post "https://www.strava.com/oauth/token"
                                        {:form-params {:client_id client_id
                                                       :client_secret client_secret
                                                       :code code}})
        ]
    (if (= 200 (:status http-response))
      (json/read-str (:body http-response) :key-fn keyword)
      (do
        (log/error (str "Strava token request received status " (:status http-response)))
        nil)
      )))


;;
;; retrieve list of activities for the current athlete
;;

(defn add-epoch-to-activity [activity]
  (merge
   {:start_date_epoch (strava-date-to-epoch (:start_date_local activity))}
   activity)
  )
(defn get-activities-current-athlete
  ([access-token]
     (get-activities-current-athlete access-token (epoch-ms-to-second (c/to-long (t/now)))))
  ([access-token before]
     (let [url (str "https://www.strava.com/api/v3/athlete/activities")
           _ (log/info "testing")
           response (http-client/get url
                                     {:headers {"Authorization" (str "Bearer " access-token)}
                                      :query-params {"before"  before
                                                     "per_page" 200}
                                      }
                                     )
           ]
       (->> (json/read-str (:body response) :key-fn keyword)
            (map #(add-epoch-to-activity %))
           ))))


(defn limit-start-date [start-date-local]
  (if (nil? start-date-local)
    nil
    (when-let [epoch (epoch-ms-to-second
                      (c/to-long
                       (f/parse
                        (f/formatters :date-time-no-ms)
                        start-date-local)))
               ]
      (log/info (str "epoch " epoch "," limit-jan-2013))
      (if (> epoch limit-jan-2013)
        (dec epoch)
        nil))))


(defn log-start-dates [activities]
  (log/info "get-all-activities-current-athlete ")
  (map #(log/info (:start_date_local %)) activities))

(defn get-all-activities-current-athlete
  ([access-token]
     (get-all-activities-current-athlete access-token (epoch-ms-to-second (c/to-long (t/now))) [] 0 ))
  ([access-token before result counter]

     (if (or (nil? before)
             (= counter 10) ;;safety counter
             )
       result
       (let [_ (log/info (str "get-activities before " (epoch-to-f before)))
             batch (get-activities-current-athlete access-token before)
             _ (log/info batch)
             _ (log-start-dates batch)
             _ (log/info (str "first: " (:start_date (first batch))))
             _ (log/info (str "last:  " (:start_date (last batch))))
             oldest-start-date (:start_date (last batch)) ;;
             before* (limit-start-date oldest-start-date)
             ]
         (get-all-activities-current-athlete access-token before* (concat result batch) (inc counter))))))
