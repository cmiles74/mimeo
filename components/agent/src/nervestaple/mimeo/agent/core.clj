(ns nervestaple.mimeo.agent.core
  (:require
   [clojure.core.async :as async]
   [nervestaple.mimeo.ollama.interface :as ollama]))

(defn agent-channel-out []
  (let [model-out-chan (async/chan)]
    (async/go-loop []
      (when-let [result (async/<! model-out-chan)]
        (println "\n" (:response result)))
      (recur))
    model-out-chan))

(defn agent-channel-in [connect model-name out-chan]
  (let [model-chan (async/chan)]
    (async/go-loop []
      (when-let [prompt (async/<! model-chan)]
        (async/>! out-chan (ollama/prompt connect model-name prompt)))
      (recur))
    model-chan))

(defn start-agent [connect model-name]
  (let [out-chan (agent-channel-out)
        in-chan (agent-channel-in connect model-name out-chan)]
    {:request-channel in-chan
     :response-channel out-chan}))

(defn shutdown-agent [agent-map]
  (async/close! (agent-map :request-channel))
  (async/close! (agent-map :response-channel)))

