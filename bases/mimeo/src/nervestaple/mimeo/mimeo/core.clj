(ns nervestaple.mimeo.mimeo.core
  (:gen-class)
  (:require
   [clojure.core.async :as async]
   [nervestaple.mimeo.ollama.interface :as ollama]
   [nervestaple.mimeo.agent.interface :as llm-agent]
   [nervestaple.mimeo.agent-session-memory.interface :as session]
   [nervestaple.mimeo.agent-transcript.interface :as transcript]))

(def CONNECTION (ollama/connect "http://localhost:11434" 300))
(def MODEL (ollama/name->model CONNECTION "gemma3:12b"))

(def demo-agent (llm-agent/define-agent
                  CONNECTION MODEL
                  "You are a friendly and helpful assistant named Gemma!\n\n"))

(defn chat-session [agent]
  (let [middleware (session/session-middleware)]
    (llm-agent/start-agent agent
                           (-> (llm-agent/null-handler)
                               (transcript/fill-transcript-middleware)
                               (transcript/transcript-middleware)
                               middleware))))

(defn start-chat []
  (let [chat-this (chat-session demo-agent)]
    (println "You are chatting with a helpful assistant. :-)")
    (loop [input (read-line)]
      (println (str "> " input))
      (when (< 0 (count (.trim input)))
        (let [sent? (async/>!! (chat-this :request-channel)
                               {:message (.trim input)})]
          (when sent?
            (let [result (async/<!! (chat-this :response-channel))]
              (println "\n" (.trim (get-in result [:response :response]))
                       "\n"))))
        (recur (read-line))))
    (llm-agent/shutdown-agent chat-this)
    (println "Bye!")))

(defn -main
  "Bootstrapping function for the application"
  [& _args]
  (start-chat))
