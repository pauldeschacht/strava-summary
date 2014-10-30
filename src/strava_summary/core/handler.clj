(ns strava-summary.core.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [strava-summary.core.view :as view]
            [clj-http.lite.client :as http-client]
            [clojure.data.json :as json]
            [clj-time.core :as t :exclude [extend second]]
            [clj-time.coerce :as c]))

(defn connect-strava [request]
  "return the page with the STRAVA connect button. Once the user is authorized at STRAVA, a redirect to /token_request is executed."
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body (view/connect-html)
   }
  )


;;
;; retrieve list of activities for the current athlete
;;
(defn strava-get-activities [access-token]
  (let [epoch-now (c/to-long (t/now))
        response (http-client/get (str "https://www.strava.com/api/v3/athlete/activities?before=" epoch-now)
                                  {:headers {"Authorization" (str "Bearer " access-token)}}
                                  )
        _ (println response)
        ]
    (json/read-str (:body response) :key-fn keyword)))

(defn show-activities [access-token]
  (let [activities (strava-get-activities access-token)
        ]
    {:status 200
     :headers {"Content-Type" "text/html; charset=utf-8"}
     :body (view/activities-html activities)}
    
    )
  )

(defn get-access-token-from-cookies [request]
  (when-let [cookies (:cookies request)]
    (when-let [access-token (get cookies "access")]
      (:value access-token)
     )
    )
  )
(defn connect [request]
  ;;TODO: if request contains session information, bypass the strava connect
  ;;
  (let [access-token (get-access-token-from-cookies request)]
    (if (nil? access-token)
      (connect-strava request)
      (show-activities access-token)
      ))
  )


(defn strava-access-token [response]
  ;;TODO check if the reply is OK
  (do (println (str response))
      (let [body (:body response)
            body-as-map (json/read-str body :key-fn keyword)
            athlete-id (get-in body-as-map [:athlete :id])
            access-token (:access_token body-as-map)
            ]
        body-as-map)))

(defn strava-token-request [request]
  (let [code (:code (:params request))
        http-response (http-client/post "https://www.strava.com/oauth/token"
                                        {:form-params {:client_id 3424
                                                       :client_secret "c1344d27465a6166ec65f482d745ffbe234a0b49"
                                                       :code code}})
        response (strava-access-token http-response)
        access-token (:access_token response)
        athlete (:athlete response)
        ]

    {:status 200
     :headers {"Content-Type" "text/html; charset=utf-8"}
     :cookies {"access" access-token}
     :body (view/hello-world-html (str (:firstname athlete) "," (:lastname athlete)))}
    ))

(defn redirect-to-strava [request]
  {:status 307
   :headers {:location "https://www.strava.com/oauth/authorize?client_id=3424&response_type=code&redirect_uri=http://localhost:3000/strava_token_request" }})

(defn hello-world [request]
  (view/hello-world-html))

(defn connect-user [request]
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body (view/hello-world-html)})

(defn connect-test [request]
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body (view/summary-html)})

(defroutes app-routes
  (GET "/test" request (connect-test request))
  (GET "/strava_token_request" request (strava-token-request request))
  (GET "/user/" request (connect-user request) )
  (GET "/" request (connect request) )
  (route/resources "/")
  (route/not-found "Not Found"))


(def app
  (wrap-defaults app-routes site-defaults))
