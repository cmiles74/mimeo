(ns nervestaple.mimeo.agent.core
  (:require
   [clojure.core.async :as async]
   [nervestaple.mimeo.log.interface :as log]
   [nervestaple.mimeo.ollama.interface :as ollama]))

(defn null-handler []
  (fn
    ([request]
     request)
    ([request response]
     response)))

(defn define-agent [connection model system]
  (partial ollama/prompt connection model system))

;; REQUEST {:message "What's the average rainfall in Sudan?"}

(defn shutdown-agent
  ([agent-map]
   (async/close! (agent-map :request-channel))
   (async/close! (agent-map :response-channel))))

(defn start-agent
  ([agent-fn handler]
   (let [in-chan (async/chan)
         out-chan (async/chan)]
     (async/go-loop []
       (when-let [request (async/<! in-chan)]
         (let [request-out (handler request)
               response (handler request (agent-fn (request-out :message)))]
           (async/>! out-chan {:request request-out
                               :response response}))
         (recur)))
     {:request-channel in-chan
      :response-channel out-chan})))

