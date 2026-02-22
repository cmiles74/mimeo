# Mimeo

This is an experimental project where I play around with local large language models. Presently I'm interested in putting my own (admittedly simple) agent harness together. Most of this code is hack-as-I-go, but if you find anything useful be sure to let me know. `;-)`

The `agent` component is probably the place to get started right now. We functions that let you create an "agent function" (a function that accepts input and executes it against Ollama) and then to create an "agent loop" around that function. We use `core.async` channels to setup a request and response channel, you provide the middleware to make it do something interesting!

## About Polylith

The Polylith documentation can be found here:

- The [high-level documentation](https://polylith.gitbook.io/polylith)
- The [Polylith Tool documentation](https://github.com/polyfy/polylith)
- The [RealWorld example app documentation](https://github.com/furkan3ayraktar/clojure-polylith-realworld-example-app)

You can also get in touch with the Polylith Team via our
[forum](https://polylith.freeflarum.com) or on
[Slack](https://clojurians.slack.com/archives/C013B7MQHJQ).

## Development

To get started, create a new REPL using the `deps.edn` file at the root of the project as the target. Once the new REPL starts up, you may interact with the code. There's some work in progress in `development/src/cmiles74/clj`.

----

[clj]: https://en.wikipedia.org/wiki/Clojure
