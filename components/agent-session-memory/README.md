# In-Memory Session Middleware

This middleware provides an in-memory session manager. A session identifier is associated with each agent session, this middleware stores that data and attaches it to both inbound requests and outbound responses.

## Getting Started

You will want to include the interface for this component. `;-)`

```clojure
(require '[nervestaple.mimeo.agent-session-memory :as session])
```

When you build up the middleware stack, be sure to include this one towards the bottom.

```clojure
(defn chat-agent (llm-agent/define-agent connection model
                   "You are a friendly and helpful assistant."))

(defn chat-session []
  (let [session (session/middleware)]
    (llm-agent/start-agent chat-agent
                       (-> your-fancy-handler
                           session-middleware))))
```

When you're handler is called, you will find a `:session-id` key with the unique identifier of the session and a `:session` key with a map of session data. You may add data to that map when handling a request or a response, the middleware will handle storing that data and ensuring it's attached to the next request and response.
