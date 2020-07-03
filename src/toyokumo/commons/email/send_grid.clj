(ns toyokumo.commons.email.send-grid
  (:require
   [clj-http.client :as http]
   [clojure.core.async :as a]
   [clojure.tools.logging :as log]
   [com.stuartsierra.component :as component]
   [diehard.core :as dh]
   [diehard.rate-limiter :as dh.rl]
   [jsonista.core :as json]
   [toyokumo.commons.email :as tc.email]
   [toyokumo.commons.json :as tc.json]))

(def send-url "https://api.sendgrid.com/v3/mail/send")

(def ^:dynamic *json-mapper*
  (json/object-mapper {:escape-non-ascii true}))

(def expected-status
  #{202 400 401 403 404 405 413})

(defn get-x-rate-limit-reset
  [resp]
  (some-> (get-in resp [:headers "X-RateLimit-Reset"])
          (Integer/parseInt)))

(defn- email*
  "Send a email asynchronously"
  [{:keys [:api-token
           :default-email-body
           :max-retry
           :retry-wait
           :rate-limiter]}
   {:keys [:body]}]
  (let [body (tc.json/json-encode *json-mapper* (merge default-email-body body))
        req {:headers {"Authorization" (str "Bearer " api-token)}
             :content-type :json
             :async? true
             :throw-exceptions false
             :body body}]
    (a/go-loop [remaining (inc max-retry)
                result nil]
      (if-not (pos? remaining)
        result
        (let [c (a/chan 1)
              handle (fn [resp]
                       (a/put! c resp)
                       (a/close! c))
              _ (if rate-limiter
                  (dh/with-rate-limiter {:ratelimiter rate-limiter}
                    (http/post send-url req handle handle))
                  (http/post send-url req handle handle))
              resp (a/<! c)]
          (cond
            (and (not (instance? Throwable resp))
                 (expected-status (:status resp)))
            resp

            ;; too many request
            ;; https://sendgrid.com/docs/API_Reference/Web_API_v3/How_To_Use_The_Web_API_v3/rate_limits.html
            (and (not (instance? Throwable resp))
                 (= (:status resp) 429))
            (let [unix-timestamp (int (/ (System/currentTimeMillis) 1000))
                  reset (get-x-rate-limit-reset resp)
                  _ (log/infof "429 TOO MANY REQUESTS X-RateLimit-Reset:%s" reset)
                  sleep-sec (- reset unix-timestamp)]
              (do (a/<! (a/timeout (* sleep-sec 1000)))
                  (recur remaining
                         resp)))

            ;; unexpected error
            :else
            (do (a/<! (a/timeout retry-wait))
                (recur (dec remaining)
                       resp))))))))

(defrecord SendGrid [api-token default-email-body max-retry retry-wait rate-limit rate-limiter]
  component/Lifecycle
  (start [this]
    (assoc this :rate-limiter (when rate-limit
                                (dh.rl/rate-limiter {:rate rate-limit}))))
  (stop [this]
    (assoc this :rate-limiter nil))

  tc.email/Email
  (email [this params]
    (a/<!! (email* this params)))
  (email-async [this params]
    (email* this params))

  tc.email/EmailResponse
  (success? [_ response]
    (cond
      (instance? Throwable response) false
      (= (:status response) 202) true
      :else false)))

(defn new-send-grid
  "Create SendGrid instance

  api-token          -  token that you get from SendGrid
                        required
  default-email-body -  a map such as {:from {:email \"...\" :name \"...\"}} which is merged
                        with mail send parameters
                        See https://sendgrid.api-docs.io/v3.0/mail-send/v3-mail-send
                        optional, default nil
  max-retry          -  the number of retries that run when a send request fails unexpectedly
                        optional, default 0
  retry-wait         -  milliseconds between retry
                        optional, default 1000
  rate-limit         -  send limit per second
                        optional, default nil"
  [{:keys [:api-token
           :default-email-body
           :max-retry
           :retry-wait
           :rate-limit]}]
  (map->SendGrid {:api-token api-token
                  :default-email-body default-email-body
                  :max-retry (or max-retry 0)
                  :retry-wait (or retry-wait 1000)
                  :rate-limit rate-limit}))
