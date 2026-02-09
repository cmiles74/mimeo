(ns cmiles74
  (:require
   [clojure.core.async :as async]
   [clojure.inspector :as inspector]
   [clojure.pprint :as pprint]
   [clojure.string :as string]
   [jsonista.core :as json]
   [nervestaple.mimeo.ollama.interface :as ollama]
   [nervestaple.mimeo.agent.interface :as agent]))

(def CONNECTION (ollama/connect "http://kipu-laptop.nervestaple.com:11434"))
(def MODEL (ollama/name->model CONNECTION "gemma3:12b"))

(defonce demo-agent (agent/start-agent CONNECTION MODEL))

(defn hello [name]
  (async/>!! (demo-agent :request-channel)
             (str "Briefly greet the person named \"" name "\".")))

