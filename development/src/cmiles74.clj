(ns cmiles74
  (:require
   [clojure.core.async :as async]
   [clojure.inspector :as inspector]
   [clojure.pprint :as pprint]
   [clojure.string :as string]
   [jsonista.core :as json]
   [nervestaple.mimeo.ollama.interface :as ollama]
   [nervestaple.mimeo.agent.interface :as agent]))

(def CONNECTION (ollama/connect "http://kipu-laptop.nervestaple.com:11434" 300))
(def MODEL (ollama/name->model CONNECTION "gemma3:12b"))

(def demo-agent (agent/start-agent
                 CONNECTION MODEL
                 "You are a friendly and helpful assistant named Gemma!\n\n"))

(defn hello [name]
  (->> (ollama/prompt CONNECTION MODEL
                      (str "Briefly greet the person named \"" name "\"."))
       :response))

(defn chat-agent [message]
  (let [sent? (async/>!! (demo-agent :request-channel) message)]
    (when sent?
      (time
       (let [result (async/<!! (demo-agent :response-channel))]
         (println (:response result) "\n"))))))

(defn agent-handler [response]
  (println (:response response) "\n"))
