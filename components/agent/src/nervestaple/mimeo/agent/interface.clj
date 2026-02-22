(ns nervestaple.mimeo.agent.interface
  (:require
   [nervestaple.mimeo.agent.core :as core]))

(defn null-handler

  []
  (core/null-handler))

(defn define-agent
  ""
  [connection model system]
  (core/define-agent connection model system))

(defn start-agent
  ""
  [agent-fn handler]
  (core/start-agent agent-fn handler))

(defn shutdown-agent
  "Accepts a map of agent data and shuts down the agent, closing the agent's
  asynchronous channels."
  [agent-map]
  (core/shutdown-agent agent-map))
