(ns nervestaple.mimeo.agent.core
  (:require
   [clojure.core.async :as async]
   [clojure.string :as string]
   [nervestaple.mimeo.log.interface :as log]
   [nervestaple.mimeo.ollama.interface :as ollama]
   [nervestaple.mimeo.uuid.interface :as uuid]))

(defonce ENVIRONMENTS (ref {}))

(defn register-agent [session agent-map]
  (dosync (alter ENVIRONMENTS assoc (:id session)
                 (merge (@ENVIRONMENTS (:id session))
                        {:agent (dissoc agent-map :session)}))))

(defn fetch-session [session-id]
  (@ENVIRONMENTS session-id))

(defn update-transcript [session type message]
  (let [environment (@ENVIRONMENTS (:id session))
        environment-out (assoc environment :messages
                               (conj (or (environment :messages) [])
                                     {:type type :message message}))]
    (dosync (alter ENVIRONMENTS assoc (:id session) environment-out))
    environment-out))

(defn current-transcript [session]
  (let [messages (session :messages)]
    (when messages
      (apply str
             (for [message messages]
               (str (if (= :request (message :type))
                      "Request: " "Response: ")
                    (.trim (message :message))
                    "\n\n"))))))

(defn agent-channel-in [connect session out-chan]
  (let [model-chan (async/chan)]
    (async/go-loop []
      (when-let [prompt (async/<! model-chan)]
        (let [session-this (fetch-session (:id session))
              model-name (get-in session-this [:agent :model-name])
              system-prompt (get-in session-this [:agent :system-prompt])
              transcript (when session
                           (str "Below is a transcript of the conversation so "
                                "far.\n\n"
                                (current-transcript session-this)
                                prompt))
              environment (update-transcript session :request prompt)
              result (ollama/prompt connect model-name system-prompt
                                    (or transcript prompt))]
          (update-transcript session :response (:response result))
          (async/>! out-chan result)))
      (recur))
    model-chan))

(defn new-session []
  (let [session {:id (uuid/generate-sequential)}]
    (dosync (alter ENVIRONMENTS assoc (:id session) {}))
    session))

(defn transcript [agent-map]
  (let [session (fetch-session (get-in agent-map [:session :id]))]
    (current-transcript session)))

(defn shutdown-agent [agent-map]
  (let [agent (fetch-session (get-in agent-map [:session :id]))]
    (async/close! (get-in agent [:agent :request-channel]))
    (async/close! (get-in agent [:agent :response-channel]))))

(defn start-agent [connect model-name system-prompt]
  (let [session (new-session)
        out-chan (async/chan)
        agent-map {:session session
                   :model-name model-name
                   :system-prompt system-prompt
                   :response-channel out-chan
                   :request-channel (agent-channel-in connect session
                                                      out-chan)}]
    (register-agent session agent-map)
    (dissoc agent-map :model-out-channel :system-prompt)))

