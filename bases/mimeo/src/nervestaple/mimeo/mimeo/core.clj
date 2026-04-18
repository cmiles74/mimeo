(ns nervestaple.mimeo.mimeo.core
  (:gen-class)
  (:require
   [clojure.core.async :as async]
   [nervestaple.mimeo.ollama.interface :as ollama]
   [nervestaple.mimeo.agent.interface :as llm-agent]
   [nervestaple.mimeo.agent-session-memory.interface :as session]
   [nervestaple.mimeo.agent-transcript.interface :as transcript]
   [nervestaple.mimeo.agent-tool.interface :as tool]))

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

(defn start-chat []
  (let [connection (ollama/connect "http://localhost:11434" 300)
        model (ollama/name->model connection "gemma4:latest")
        agent (llm-agent/define-agent
                connection model
                "You are a friendly and helpful assistant named Gemma!\n\n")
        chat-this (chat-session agent)]
    (println "You are chatting with a helpful assistant. :-)")
    (loop [input (read-line) continue? false]
      (when (and (not continue?)
                 input
                 (< 0 (count (.trim input)))))
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

(defn -main
  "Bootstrapping function for the application"
  [& _args]
  (start-chat))
