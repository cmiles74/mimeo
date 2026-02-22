(ns nervestaple.mimeo.agent.interface
  (:require
   [nervestaple.mimeo.agent.core :as core]))

(defn null-handler
  "Returns a handler function that does nothing, it simply returns the request and
  response. If you don't need a handler function (you are solely looking for the
  side-effect of LLM output) then this is the function for you!"
  []
  (core/null-handler))

(defn define-agent
  "Accepts an Ollama connection, the name of a model and a system prompt. Returns
  a function that accepts a prompt and then invokes Ollama with the connection
  and model to generate a response to the prompt."
  [connection model system]
  (core/define-agent connection model system))

(defn start-agent
  "Accepts an agent function and a handler function. Creates two channels, one for
  requests `:request-channel` and another for responses `:response-channel` and
  returns a map with those channels. Starts an agent loop that reads data from
  the request channel, passes it to the handler function, and writes the
  response from the handler function to the output channel."
  [agent-fn handler]
  (core/start-agent agent-fn handler))

(defn shutdown-agent
  "Accepts a map of agent data and shuts down the agent, closing the agent's
  asynchronous channels."
  [agent-map]
  (core/shutdown-agent agent-map))
