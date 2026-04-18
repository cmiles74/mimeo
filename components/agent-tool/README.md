# Tool Calling Middleware

This middleware provides the ability to define tools that may be called by the LLM agent. You provide a list of Clojure functions along with instructions for the agent that tells it how it might use those functions.

## Getting Started

You will want to include the interface for this component, of course.

```clojure
(require '[nervestaple.mimeo.agent-tool.interface :as tool])
```

When you are building up your middleware stack, you may provide your tool specification along with the handler. We also provide a `call-tool-middleware` that will read through the agent's response and handle actually calling the tool when necessary.

```clojure
(defn chat-agent (llm-agent/define-agent connection model
                   "You are a friendly and helpful assistant."))

(def tools-spec
  [[(tool/fn->str rand-int)
    "Accepts a single parameter, and returns a random integer between 0 (inclusive) and that parameter (exclusive)."]])

(defn chat-session []
  (let [session (session/middleware)]
    (llm-agent/start-agent chat-agent
                       (-> your-fancy-handler
                           (tool/call-tool-middleware)
                           (tool/tool-middleware tool-spec)))
```

Now the agent will know how to generate random numbers!

A few things to watch our for: you need to provide _a string_ that represents your function, not the actual function itself. Be sure to use the `fn->str` macro!

## How Does it Work?

The `tool-middleware` injects your instructions on how to use the tools, along with some instructions on how the agent should format tool calls, into the prompt. When the model responds, the `call-tool-middleware` will check that response and, if it's a tool call, it will go ahead and call the tool and return the tools output with the response.

It will be the job of your chat loop to check the response, if it's the result of a tool call then you'll want to provide it back to the model so that it can see the result and decide what to do next. The response will be of the type `:tool-response` and it will include a `:continue` key that is set to `true`, indicating that it should be fed back to the model.
