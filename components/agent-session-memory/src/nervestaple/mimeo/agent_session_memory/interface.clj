(ns nervestaple.mimeo.agent-session-memory.interface
  (:require
   [nervestaple.mimeo.agent-session-memory.core :as core]))

(defn fetch-session
  "Accepts a unique session identifier and returns the map of session data
  associated with that identifier."
  [session-id]
  (core/fetch-session session-id))

(defn delete-session
  "Accepts a unique session identifier and removes that session's data from the in
  memory data store."
  [session-id]
  (core/delete-session session-id))

(defn session-middleware
  "Returns a middleware that adds a unique session identifier and a map of session
  data to the inbound requests and outbound responses. Session data is stored in
  memory."
  []
  (core/session-middleware))
