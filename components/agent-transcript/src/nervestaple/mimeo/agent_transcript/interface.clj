(ns nervestaple.mimeo.agent-transcript.interface
  (:require
   [nervestaple.mimeo.agent-transcript.core :as core]))

(defn transcript-middleware
  "Returns a middleware function that maintains a transcript of the conversation
  under the `:transcript` key of the active session. (requires some kind of
  session middleware)"
  [handler]
  (core/transcript-middleware handler))

(defn fill-transcript-middleware
  "Returns a middleware that maintains a transcript in the current
  session (requires some kind of session middleware). Supplies the transcript to
  the agent along with the provided prompt."
  [handler]
  (core/fill-transcript-middleware handler))
