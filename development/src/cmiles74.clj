(ns cmiles74
  (:require
   [clojure.inspector :as inspector]
   [clojure.pprint :as pprint]
   [clojure.string :as string]
   [jsonista.core :as json])
  (:import
   [io.github.ollama4j Ollama]
   [io.github.ollama4j.models.generate OllamaGenerateRequest]))

(def SERVER "http://friday.nervestaple.com:11434")
(def MODEL "gemma3:12b")

(defonce ollama (Ollama. SERVER))

(defn list-models
  "Accepts an Ollama server and returns a list of available models."
  [server]
  (let [models (.listModels server)]
    (doseq [model models]
      (println (.getName model)))))

(defn new-request
  "Accepts a LLM model name and a textual prompt, returns a new request."
  [model prompt]
  (let [builder (OllamaGenerateRequest/builder)]
    (doto builder
      (.withModel model)
      (.withPrompt prompt)
      (.build))))

(defn wrap-result
  "Accepts a result from an Ollama request and returns a map with that result's
  data."
  [result]
  {:context (.getContext result)
   :created-at (.getCreatedAt result)
   :done (= (.getDoneReason result) "stop")
   :done-reason (.getDoneReason result)
   :eval-count (.getEvalCount result)
   :eval-duration (.getEvalDuration result)
   :load-duration (.getLoadDuration result)
   :model (.getModel result)
   :prompt-eval-count (.getPromptEvalCount result)
   :prompt-eval-duration (.getPromptEvalDuration result)
   :response (.getResponse result)
   :response-time (.getResponseTime result)
   :thinking (.getThinking result)
   :total-duration (.getTotalDuration result)
   :result result})

(defn prompt-model
  "Accepts an Ollama server, LLM model name and a textual prompt. Returns the
  result from Ollama as a map."
  [server model prompt]
  (let [request (new-request model prompt)
        result (.generate server request nil)]
    (wrap-result result)))

(defn hello [name]
  (let [result (prompt-model ollama MODEL (str "Hello! My name is " name))]
    (println (result :response))))
