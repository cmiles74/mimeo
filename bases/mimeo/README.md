# Mimeo

This is the "base" that pulls several of our components together to provide a simple chat interface to a running agent. It's early days yet but our agent does, at least, have a simple memory implemented.

We expose a `main` function that is used to bootstrap the component, this in turns calls the `start-chat` function that starts a new interactive chat session. You may then type some text and press the "return" key to send it to the agent. When you want to quit simply press the "return" key without typing anything
