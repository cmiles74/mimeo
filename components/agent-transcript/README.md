
# Agent Transcript Middleware

This component provides middlewares that will maintain a transcript of a conversation with a model and provide that transcript to the agent along with other input.

## Getting Started

You will want to include the interface for this component.

```clojure
(require '[nervestaple.mimeo.agent-transcript :as transcript])
```

You may use one or both of the middlewares, depending on your use-case. These middlewares _require_ some kind of agent session middleware, they need somewhere to store the transcript.

```clojure
(defn chat-agent (llm-agent/define-agent connection model
                   "You are a friendly and helpful assistant."))

(defn chat-session []
  (let [session (session/middleware)]
    (llm-agent/start-agent chat-agent
                       (-> your-fancy-handler
                           (transcript/fill-transcript-middleware)
                           (transcript/transcript-middleware)
                           session-middleware))))
```

When your handler is called, you'll find the transcript so far on the `:transcript` key of the session on both the request and response. If you're using the `fill-transcript-middleware`, the message provide to the model will be enriched with the transcript.
