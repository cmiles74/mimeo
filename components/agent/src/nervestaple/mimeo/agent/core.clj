(ns nervestaple.mimeo.agent.core
  (:require
   [clojure.core.async :as async]
   [nervestaple.mimeo.log.interface :as log]
   [nervestaple.mimeo.ollama.interface :as ollama]
   [nervestaple.mimeo.uuid.interface :as uuid]))

(defonce ENVIRONMENTS (ref {}))

(defn update-transcript [session type request]
  (let [environment (@ENVIRONMENTS (:id session))
        environment-out (assoc environment :messages
                               (conj (or (environment :messages) [])
                                     {:type type :message request}))]
    (dosync (alter ENVIRONMENTS assoc (:id session) environment-out))
    environment-out))

(defn transcript [session]
  (let [messages (session :messages)]
    (when messages
      (apply str
             (for [message messages]
               (str (if (= :request (message :type))
                      "Request: " "Response: ")g
                    (.trim (message :message))
                    "\n\n"))))))

(defn agent-channel-out [session]
  (let [model-out-chan (async/chan)]
    (async/go-loop []
      (when-let [result (async/<! model-out-chan)]
        (update-transcript session :response (:response result))
        (println "\n" (:response result)))
      (recur))
    model-out-chan))

(defn agent-channel-in [connect model-name session out-chan]
  (let [model-chan (async/chan)]
    (async/go-loop []
      (when-let [prompt (async/<! model-chan)]
        (let [transcript (when session (transcript session))
              environment (update-transcript session :request prompt)]
          (async/>! out-chan
                    (ollama/prompt connect model-name nil transcript prompt))))
      (recur))
    model-chan))

(defn shutdown-agent [agent-map]
  (async/close! (agent-map :request-channel))
  (async/close! (agent-map :response-channel)))

(defn new-session []
  (let [session {:id (uuid/generate-sequential)}]
    (dosync (alter ENVIRONMENTS assoc (:id session) {}))
    session))

(defn start-agent [connect model-name]
  (let [session (new-session)
        out-chan (agent-channel-out session)
        in-chan (agent-channel-in connect model-name session out-chan)]
    {:session session
     :request-channel in-chan
     :response-channel out-chan}))

