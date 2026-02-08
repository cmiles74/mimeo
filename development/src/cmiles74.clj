(ns cmiles74
  (:require
   [clojure.core.async :as async]
   [clojure.inspector :as inspector]
   [clojure.pprint :as pprint]
   [clojure.string :as string]
   [jsonista.core :as json]
   [nervestaple.mimeo.ollama.interface :as ollama])
  (:import
   [io.github.ollama4j Ollama]
   [io.github.ollama4j.models.generate OllamaGenerateRequest]))

(def CONNECTION (ollama/connect "http://kipu-laptop.nervestaple.com:11434"))
(def MODEL (ollama/name->model CONNECTION "gemma3:12b"))

(defn setup-model-out []
  (let [model-out-chan (async/chan)]
    (async/go-loop []
      (if-let [result (async/<! model-out-chan)]
        (println "\n" (:response result)))
      (recur))
    model-out-chan))

(defn setup-model-in [connect model-name out-chan]
  (let [model-chan (async/chan)]
    (async/go-loop []
      (if-let [prompt (async/<! model-chan)]
        (async/>! out-chan (ollama/prompt-one-shot connect model-name prompt)))
      (recur))
    model-chan))

(def model-out-chan (setup-model-out))
(def model-in-chan (setup-model-in CONNECTION MODEL model-out-chan))

(defn shutdown []
  (async/close! model-in-chan)
  (async/close! model-out-chan))

(defn hello [name]
  (async/>!! model-in-chan (str "Briefly greet the person named \"" name "\".")))

