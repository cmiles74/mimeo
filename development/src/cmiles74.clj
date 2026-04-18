(ns cmiles74
  (:require
   [clojure.core.async :as async]
   [clojure.inspector :as inspector]
   [clojure.pprint :as pprint]
   [clojure.string :as string]
   [jsonista.core :as json]
   [clj-yaml.core :as yaml]
   [nervestaple.mimeo.log.interface :as log]
   [nervestaple.mimeo.uuid.interface :as uuid]
   [nervestaple.mimeo.ollama.interface :as ollama]
   [nervestaple.mimeo.agent.interface :as llm-agent]
   [nervestaple.mimeo.agent-session-memory.interface :as session]
   [nervestaple.mimeo.agent-transcript.interface :as transcript]
   [nervestaple.mimeo.agent-tool.interface :as tool]))

(def CONNECTION (ollama/connect "http://friday.nervestaple.com:11434" 300))
(def MODEL (ollama/name->model CONNECTION "gemma4:latest"))

(def demo-agent (llm-agent/define-agent
                 CONNECTION MODEL
                 "You are a friendly and helpful assistant named Gemma!\n\n"))

(defn hello [name]
  (->> (ollama/prompt CONNECTION MODEL
                      (str "Briefly greet the person named \"" name "\"."))
       :response))

(def tools-spec
  [[(tool/fn->str rand-int)
    "Accepts a single parameter, and returns a random integer between 0 (inclusive) and that parameter (exclusive)."]])

(defn chat-session [agent]
  (let [middleware (session/session-middleware)]
    (llm-agent/start-agent agent
                           (-> (llm-agent/null-handler)
                               (transcript/fill-transcript-middleware)
                               (transcript/transcript-middleware)
                               (tool/call-tool-middleware)
                               (tool/tool-middleware tools-spec)
                               middleware))))

(defn demo-chat []
  (let [chat-this (chat-session demo-agent)]
    (println "You are chatting with a helpful assistant. :-)")
    (loop [input (read-line) continue? false]
      (when (and (not continue?)
                 input
                 (< 0 (count (.trim input))))
        (println ">" (.trim input)))
      (let [sent? (when (or continue? (and input (< 0 (count (.trim input)))))
                    (async/>!! (chat-this :request-channel)
                               {:message (if continue?
                                           input
                                           (.trim input))}))]
        (when sent?
          (let [result (async/<!! (chat-this :response-channel))
                response (get-in result [:response :response])
                continue-this (get-in result [:response :continue])]
            (when response
              (println "\n" response "\n"))
            (recur (if continue-this :continue (read-line))
                   continue-this)))))
    (llm-agent/shutdown-agent chat-this)
    (println "Bye!")))
