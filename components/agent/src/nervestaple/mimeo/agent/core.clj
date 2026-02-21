(ns nervestaple.mimeo.agent.core
  (:require
   [clojure.core.async :as async]
   [clojure.string :as string]
   [nervestaple.mimeo.log.interface :as log]
   [nervestaple.mimeo.ollama.interface :as ollama]
   [nervestaple.mimeo.uuid.interface :as uuid]))

(defonce SESSIONS (atom {}))

(defn clear-sessions []
  (reset! SESSIONS {}))

(defn create-session []
  (let [id (uuid/generate-sequential)]
    (swap! SESSIONS assoc id {})
    id))

(defn delete-session [id]
  (swap! SESSIONS dissoc id))

(defn store-session [id data]
  (swap! SESSIONS assoc id data))

(defn fetch-session [id]
  (@SESSIONS id))

(defn session-request [request]
  (let [session-id (or (request :session-id) (create-session))
        session-map (fetch-session session-id)]
    (merge request {:session-id session-id :session session-map})))

(defn session-response [response request]
  (let [session-id (request :session-id)
        session-map (request :session)]
    (when (nil? session-id) (log/warn "No session found in the request"))
    (when (and session-id session-map) (store-session session-id session-map))
    (merge response {:session-id session-id :session session-map})))

(defn wrap-session [handler]
  (fn [request]
    (let [request (session-request request)]
      (-> (handler request)
          (session-response request)))))

(defn wrap-transcript [handler]
  (fn [request]
    (let [transcript (or (get-in request [:session :transcript]) [])
          message (request :message)]
      (assoc-in request [:session :transcript] (conj transcript message)))))

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

