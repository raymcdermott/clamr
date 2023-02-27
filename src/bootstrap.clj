;; AWS Lambda runtime using Clojure
;;
;;  The bootstrap shell script will run this
(ns bootstrap
  (:require [clj-http.client :as client]
            [clojure.string :as str]
            [cheshire.core :as cheshire]))

(defn- getenv-or-bail [env-var]
  (let [ev (System/getenv env-var)]
    (or ev (ex-info "Cannot find required environment variable"
                    {:env-var env-var}))))

(def handler-name (getenv-or-bail "_HANDLER"))

(println "Loading lambda handler:" handler-name)

(def runtime-api (getenv-or-bail "AWS_LAMBDA_RUNTIME_API"))

(def runtime-api-url (str "http://" runtime-api "/2018-06-01/runtime/"))

(def error-api-url (str runtime-api-url "init/error"))

(def next-invocation-url (str runtime-api-url "invocation/next"))

(defn throwable->error-body [t]
  {:errorMessage (.getMessage t)
   :errorType    (-> t .getClass .getName)
   :stackTrace   (mapv str (.getStackTrace t))})

;; load handler
(def handler
  (let [[handler-ns handler-fn] (str/split handler-name #"/")]
    (try
      (require (symbol handler-ns))
      (resolve (symbol handler-ns handler-fn))
      (catch Throwable t
        (println "Unable to run initialize handler fn " handler-fn "in namespace" handler-ns
                 "\nthrow: " t)
        (client/post error-api-url {:body (cheshire/encode (throwable->error-body t))})
        nil))))

(when-not handler
  (client/post error-api-url {:headers {"Lambda-Runtime-Function-Error-Type" "Runtime.NoSuchHandler"}
                              :body    (cheshire/encode {"error" (str handler-name " didn't resolve.")})}))

;; API says not to timeout when getting next invocation, so make it a long one
(def timeout-ms (* 1000 60 60 24))

(defn next-invocation
  "Blocking get for the next invocation, returns payload and fn to respond."
  []
  (let [{:keys [headers body]} (client/get next-invocation-url {:socket-timeout     timeout-ms
                                                                :connection-timeout timeout-ms})
        id (:lambda-runtime-aws-request-id headers)]
    {:event          (cheshire/decode body keyword)
     :context        headers
     :send-response! (fn [response]
                       (client/post (str runtime-api-url "invocation/" id "/response")
                                    {:body (cheshire/encode response)}))
     :send-error!    (fn [thrown]
                       (client/post (str runtime-api-url "invocation/" id "/error")
                                    {:body (cheshire/encode (throwable->error-body thrown))}))}))

(when handler
  (println "Starting Clojure lambda event loop")
  (loop [{:keys [event context send-response! send-error!]} (next-invocation)]
    (try
      (let [response (handler event context)]
        (send-response! response))
      (catch Throwable t
        (println "Error in executing handler" t)
        (send-error! t)))
    (recur (next-invocation))))

