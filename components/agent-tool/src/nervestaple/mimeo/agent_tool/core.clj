(ns nervestaple.mimeo.agent-tool.core
  (:require
   [clj-yaml.core :as yaml]
   [clojure.string :as string]
   [nervestaple.mimeo.log.interface :as log]))

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

(defn parse-fenced-code-block
  "Accepts a string of text containing a Markdown fenced code block (minus the
  beginning fence) and returns the contents of that block (minus the ending
  fence)."
  [text-seq]
  (loop [current (first text-seq) remainder (rest text-seq) block []]
    (cond
      (and (= \` current) (= [\` \`] (take 2 remainder)))
      {:block (apply str block) :remainder (drop 2 remainder)}

      (< 0 (count remainder))
      (recur (first remainder) (rest remainder) (conj block current))

      :else
      (throw (Exception. "End of fenced code block missing")))))

(defn parse-fenced-code-blocks
  "Accepts a string of text and returns a sequence of the Markdown fenced code
  blocks present in that text."
  [text]
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


(defmacro fn->str
  "Accepts a function and returns a string with that functions symbol."
  [arg]
  `(str (symbol (resolve (quote ~arg)))))

(defn str->fn
  "Accepts a string that represents a function and returns that function, if
  found."
  [text]
  (resolve (symbol text)))

(defn parse-tool-call
  "Accepts the content of a Markdown fenced code block and returns a map with the
  name of the function to call and the arguments to apply to that function."
  [code-block]
  (let [lines (string/split code-block #"\n")]
    (when (string/starts-with? (first lines) "tool:")
      (let [fn-name (apply str (drop 5 (first lines)))
            params (yaml/parse-string (apply str (rest lines)))]
        {:fn (str->fn fn-name)
         :args params}))))

(defn execute-tool-call
  "Accepts a map with a tool call (a string with the name of the function to call
  and a sequence of arguments to apply to that function) and invokes that
  function with the supplied arguments."
  [tool-call]
  (log/debug "Calling tool" (:fn tool-call) "with parameters" (:args tool-call))
  (let [tool-output (apply (:fn tool-call) (:args tool-call))]
    (log/debug "Tool returned" tool-output)
    tool-output))

(defn tool-spec->str
  [tool-spec]
  (apply str
         (map (fn [[fn-this description]]
                (str fn-this "  " description "\n\n"))
              tool-spec)))

(defn tool-middleware
  [handler tool-spec]
  (fn
    ([request]
     (let [tool-message (str "\n\n"
                             "You have the following tools available to you:\n\n"
                             (tool-spec->str tool-spec)
                             "\n\n"
                             "When you want to call a tool, you need to return a "
                             "response that is only a Markdown fenced code block, "
                             "this code block should include the text 'tool:' on "
                             "the first line follows by the name of the tool and a "
                             "carriage return. Next, provide a YAML formatted list "
                             "with the parameters for that tool. For instance\n\n"
                             "```tool:NAME-OF-THE-TOOL-TO-CALL\n"
                             "- tool call argument 1\n"
                             "- tool call argument 2\n"
                             "```\n\n")]
       (-> (merge request {:message-original (request :message)
                           :message (if (= :continue (request :message-original))
                                      tool-message
                                      (str tool-message
                                         (request :message)))})
           handler)))
    ([request response] (handler request response))))

(defn call-tool-middleware [handler]
  (fn
    ([request]
     (handler request))
    ([request response]
     (let [out-text (response :response)
           blocks (parse-fenced-code-blocks out-text)]
       (if (< 0 (count blocks))
         (let [tool-calls (map parse-tool-call blocks)
               tool-responses (apply str
                                     (map execute-tool-call
                                          tool-calls))]
           (handler request (merge response
                                   {:type :tool-response
                                    :tool-call (apply str (interpose "\n\n" tool-calls))
                                    :continue true
                                    :response-original (str tool-responses)
                                    :response nil})))
         (handler request response))))))
