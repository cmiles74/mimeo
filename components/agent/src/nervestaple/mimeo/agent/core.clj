(ns nervestaple.mimeo.agent.core
  (:require
   [clojure.core.async :as async]
   [nervestaple.mimeo.log.interface :as log]
   [nervestaple.mimeo.ollama.interface :as ollama]
   [nervestaple.mimeo.uuid.interface :as uuid]))

(defonce ENVIRONMENTS (ref {}))

(defn store-message [session request]
  (let [environment (ENVIRONMENTS (:id session))
        environment-out (assoc environment :messages
                               (conj (or (environment :messages) []) request))]
    (dosync (alter ENVIRONMENTS assoc (:id session) environment-out))
    environment-out))

(defn agent-channel-out []
  (let [model-out-chan (async/chan)]
    (async/go-loop []
      (when-let [result (async/<! model-out-chan)]
        (log/info "\n" (:response result)))
      (recur))
    model-out-chan))

(defn agent-channel-in [connect model-name out-chan]
  (let [model-chan (async/chan)]
    (async/go-loop []
      (when-let [prompt (async/<! model-chan)]
        (async/>! out-chan (ollama/prompt connect model-name prompt)))
      (recur))
    model-chan))

(defn start-agent [connect model-name]
  (let [out-chan (agent-channel-out)
        in-chan (agent-channel-in connect model-name out-chan)]
    {:request-channel in-chan
     :response-channel out-chan}))

(defn shutdown-agent [agent-map]
  (async/close! (agent-map :request-channel))
  (async/close! (agent-map :response-channel)))

(defn new-session []
  (let [session {:id (uuid/generate-sequential)}]
    (dosync (alter ENVIRONMENTS assoc (:id session) {}))
    session))

(defn session-channel-in [session agent-map]
  (let [session-chan (async/chan)]
    (async/go-loop []
      (when-let [prompt (async/<! session-chan)]
        (let [environment (store-message session prompt)]
          (log/debug "Session" (session :id))
          (async/>! (agent-map :request-channel) prompt)))
      (recur))
    session-chan))

(defn start-session [agent-map]
  (let [session (new-session)
        in-chan (session-channel-in session agent-map)]
    {:session session
     :request-channel in-chan
     :response-channel (agent-map :response-channel)}))

(defn stop-session [session-map]
  (async/close! (session-map :request-channel))
  (dosync (alter ENVIRONMENTS dissoc (:id session-map))))

