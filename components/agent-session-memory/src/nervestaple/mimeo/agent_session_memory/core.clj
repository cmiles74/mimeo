(ns nervestaple.mimeo.agent-session-memory.core
  (:require
   [nervestaple.mimeo.uuid.interface :as uuid]))

(defonce SESSIONS (atom {}))

(defn clear-sessions []
  (reset! SESSIONS {}))

(defn create-session [session-id]
  (swap! SESSIONS assoc session-id {})
  {})

(defn delete-session [id]
  (swap! SESSIONS dissoc id))

(defn store-session [id data]
  (swap! SESSIONS assoc id data))

(defn fetch-session [id]
  (@SESSIONS id))

(defn session-handler [session-id handler]
  (fn
    ([request]
     (let [session {:session-id session-id
                    :session (fetch-session session-id)}
           request (handler (merge request session))]
       (store-session session-id (request :session))
       request))
    ([request response]
     (let [session {:session-id session-id
                    :session (fetch-session session-id)}
           response-out (handler request (merge response session))]
       (store-session session-id (response-out :session))
       response-out))))

(defn session-middleware []
  (let [session-id (uuid/generate-sequential)]
    (create-session session-id)
    (fn [handler] (session-handler session-id handler))))
