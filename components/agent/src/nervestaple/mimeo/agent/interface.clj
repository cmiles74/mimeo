(ns nervestaple.mimeo.agent.interface
  (:require
   [nervestaple.mimeo.agent.core :as core]))

(defn start-agent
  "Accepts an active Ollama connection, a model name and a system prompt for the
  agent. Starts up a new agent and returns a map of agent data, including
  asynchronous channels for communicating with the agent."
  [connection model-name system-prompt]
  (core/start-agent connection model-name system-prompt))

(defn shutdown-agent
  "Accepts a map of agent data and shuts down the agent, closing the agent's
  asynchronous channels."
  [agent-map]
  (core/shutdown-agent agent-map))

(defn transcript
  "Accepts an agent map and returns a String with the transcript of the
  conversation."
  [agent-map]
  (core/transcript agent-map))
