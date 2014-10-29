(ns strava-summary.core.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [strava-summary.core.view :as view]))


(defn redirect-to-strava [request]
  {:status 307
   :headers {:location "https://www.strava.com/oauth/authorize?client_id=3424&response_type=code&redirect_uri=localhost/token_request&scope=read&state=access&approval_prompt=auto" }})

(defn strava-token-request [request]
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body "<html><body>Token Request</body></html"})

(defn connect-init [request]
  ;; TODO: if request contains a valid id in the session, go to connect-user
  (redirect-to-strava request))

(defn hello-world [request]
  (view/hello-world))

(defn connect-user [request]
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body (view/hello-world)})

(defn connect-test [request]
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body (view/summary-html-test)})

(defroutes app-routes
  (GET "/test" request (connect-test request))
  (GET "/token_request" request (strava-token-request request))
  (GET "/user/" request (connect-user request) )
  (GET "/" request (connect-init request) )
  (route/not-found "Not Found"))


(def app
  (wrap-defaults app-routes site-defaults))
