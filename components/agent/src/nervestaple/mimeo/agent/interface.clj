(ns nervestaple.mimeo.agent.interface
  (:require
   [nervestaple.mimeo.agent.core :as core]))

(defn start-agent
  "Accepts an active Ollama connection and a model name. Starts up a new agent and
  returns a map with asynchronous channels for communicating with the agent."
  [connection model-name]
  (core/start-agent connection model-name))

(defn shutdown-agent
  "Accepts a map of agent data and shuts down the agent, closing the agent's
  asynchronous channels."
  [agent-map]
  (core/shutdown-agent agent-map))
