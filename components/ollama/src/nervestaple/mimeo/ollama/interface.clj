(ns nervestaple.mimeo.ollama.interface
  (:require
   [nervestaple.mimeo.ollama.core :as core]))

(defn connect
  "Accepts the URL to a running Ollama server and (optionally) a timeout value in
  seconds. Returns an active connection to that server."
  ([ollama-url]
   (core/connect ollama-url))
  ([ollama-url timeout-seconds]
   (core/connect ollama-url timeout-seconds)))

(defn models
  "Returns a list of maps with information about the all of the models available
  from the connected Ollama server."
  [connection]
  (core/models connection))

(defn name->model
  "Accepts a string with the name of a model and returns the map of data for that
  model if it is available on the provided Ollama server."
  [connection model-name]
  (core/name->model connection model-name))

(defn family->model
  "Accepts the 'family' name of a model (i.e. 'gemma3' or 'deepseek-r1') and
  returns all of the models that have that family name and are available on the
  provided Ollama server."
  [connection model-family-name]
  (core/family->model connection model-family-name))

(defn pull-model
  "Instructions the Ollama server to pull the named model, returns true if
  successful."
  [connection model]
  (core/pull-model connection model))

(defn prompt
  "Accepts an active Ollama connection, the name of a model, an optional system
  prompt, an optional context and a prompt for the model. Provides that prompt
  to the specified model and returns a map with the response."
  ([connection model prompt-text]
   (core/prompt connection model prompt-text))
  ([connection model system prompt-text]
   (core/prompt connection model system prompt-text))
  ([connection model system context prompt-text]
   (core/prompt connection model system context prompt-text)))
