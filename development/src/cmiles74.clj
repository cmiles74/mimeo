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
   [nervestaple.mimeo.agent-transcript.interface :as transcript]))

(def CONNECTION (ollama/connect "http://friday.nervestaple.com:11434" 300))
(def MODEL (ollama/name->model CONNECTION "gemma3:12b"))

(def demo-agent (llm-agent/define-agent
                 CONNECTION MODEL
                 "You are a friendly and helpful assistant named Gemma!\n\n"))

(defn hello [name]
  (->> (ollama/prompt CONNECTION MODEL
                      (str "Briefly greet the person named \"" name "\"."))
       :response))

(def sample-tool-call
"This is some text that contains a tool call.

```tool:clojure.core/rand-int
- 101
```

It also has some trailing text but that stuff is just junk.

```tool:cmiles74/weather
- \"01027\"
```

And then some more crap!
")

(defn parse-fenced-code-block [text-seq]
  (loop [current (first text-seq) remainder (rest text-seq) block []]
    (cond
      (and (= \` current) (= [\` \`] (take 2 remainder)))
      {:block (apply str block) :remainder (drop 2 remainder)}

      (< 0 (count remainder))
      (recur (first remainder) (rest remainder) (conj block current))

      :else
      (throw (Exception. "End of fenced code block missing")))))

(defn parse-fenced-code-blocks [text]
  (loop [current (first text) remainder (rest text) blocks []]
    (cond
      (and (= \` current) (= [\` \`] (take 2 remainder)))
      (let [{:keys [block remainder]}
            (parse-fenced-code-block (drop 2 remainder))]
        (recur (first remainder) (rest remainder) (conj blocks block)))

      (< 0 (count remainder))
      (recur (first remainder) (rest remainder) blocks)

      :else
      blocks)))

(defn parse-tool-call [code-block]
  (let [lines (string/split code-block #"\n")]
    (when (string/starts-with? (first lines) "tool:")
      (let [fn-name (apply str (drop 5 (first lines)))
            params (yaml/parse-string (apply str (rest lines)))]
        {:fn (resolve (symbol fn-name))
         :args params}))))

(defn execute-tool-call [tool-call]
  (println "[ Calling tool" (:fn tool-call) "with parameters" (:args tool-call) "]")
  (let [tool-result (apply (:fn tool-call) (:args tool-call))]
    (println "[ Tool returned:" tool-result "]")
    tool-result))

(defn tool-middleware [handler]
  (fn
    ([request]
     (-> (merge request {:message-original (request :message)
                         :message (str "\n\n"
                                   "You have the following tools available to you:\n\n"
                                   "clojure.core/rand-int Accepts a single parameter, "
                                   "and returns a random integer between 0 (inclusive) "
                                   "and that parameter (exclusive).\n\n"
                                   "When you want to call a tool, you need to return a "
                                   "response that is only a Markdown fenced code block, "
                                   "this code block should include the text 'tool:' on "
                                   "the first line follows by the name of the tool and a "
                                   "carriage return. Next, provide a YAML formatted list "
                                   "with the parameters for that tool. For instance\n\n"
                                   "```tool:NAME-OF-THE-TOOL-TO-CALL\n"
                                   "- tool call argument 1\n"
                                   "- tool call argument 2\n"
                                   "```\n\n"
                                   (request :message))})
         handler))
    ([request response] (handler request response))))

(defn call-tool-middleware [handler]
  (fn
    ([request] (handler request))
    ([request response]
     (let [out-text (response :response)
           blocks (parse-fenced-code-blocks out-text)]
       (if (< 0 (count blocks))
         (let [tool-calls (map parse-tool-call blocks)
               tool-responses (apply str
                                     (map execute-tool-call
                                          tool-calls))]
           (handler request (merge response
                                   {:tool-call (apply str (interpose "\n\n" tool-calls))
                                    :continue true
                                    :response-original (str tool-responses)
                                    :response nil})))
         (handler request response))))))


(defn chat-session [agent]
  (let [middleware (session/session-middleware)]
    (llm-agent/start-agent agent
                           (-> (llm-agent/null-handler)
                               (transcript/fill-transcript-middleware)
                               (transcript/transcript-middleware)
                               (call-tool-middleware)
                               (tool-middleware)
                               middleware))))

(defn demo-chat []
  (let [chat-this (chat-session demo-agent)]
    (println "You are chatting with a helpful assistant. :-)")
    (loop [input (read-line)]
      (when (< 0 (count (.trim input)))
        (println ">" (.trim input))
        (let [sent? (async/>!! (chat-this :request-channel)
                               {:message (.trim input)})]
          (when sent?
            (let [result (async/<!! (chat-this :response-channel))
                  response (get-in result [:response :response])]
              (when response
                (println "\n" (.trim (get-in result [:response :response]))
                         "\n")))))
        (recur (read-line))))
    (llm-agent/shutdown-agent chat-this)
    (println "Bye!")))

