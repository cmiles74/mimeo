# Mimeo

![Continuous Integration](https://github.com/cmiles74/mimeo/workflows/Build%20and%20Test/badge.svg)

This is an experimental project where I play around with local large language models. Presently I'm interested in putting my own (admittedly simple) agent harness together and having a couple of agents that talk to each other. Most of this code is hack-as-I-go, but if you find anything useful be sure to let me know. `;-)`

The `mimeo` project and the `agent` component are probably the places to get started right now. We have functions that let you create an "agent function" (a function that accepts input and executes it against Ollama) and then to create an "agent loop" around that function. We use `core.async` channels to setup a request and response channel, you provide the middleware to make it do something interesting!

We also provide some middleware for you that will...
- Create a transcript and inject it into the prompt
- Let you define tool calls and then handle the calling of those tools
- Maintain session information between calls to the model

## About Polylith

The Polylith documentation can be found here:

- The [high-level documentation](https://polylith.gitbook.io/polylith)
- The [Polylith Tool documentation](https://github.com/polyfy/polylith)
- The [RealWorld example app documentation](https://github.com/furkan3ayraktar/clojure-polylith-realworld-example-app)

You can also get in touch with the Polylith Team via our
[forum](https://polylith.freeflarum.com) or on
[Slack](https://clojurians.slack.com/archives/C013B7MQHJQ).

## Building and Running

All of the documentation for the various projects are in their respective
directories in the "project" folder. The build script for all of these projects,
however, lives in the root directory of this project (`./build.clj`).

To build the "uberjar" for any project, invoke the script for the root directory like so:

    clj -T:build uberjar :project mimeo

The invocation above calls the "uberjar" function in the "build.clj" file for
the project "mimeo", the output will be in the "target" directory of that
project. Documentation about these projects can be found in their respective
folders.

Once the build is complete you can invoke the uberjar like so:

     java -jar projects/mimeo/target/mimeo.jar

The `mimeo` project is our main one for now, it provides an executable Uberjar that provides a chat interface to a running agent. Our plan is to build this out a bit but the real goal of the project is to make it easy to play with agents that talk to one another.

## Development

To get started, create a new REPL using the `deps.edn` file at the root of the project as the target. Once the new REPL starts up, you may interact with the code. There's some work in progress in `development/src/cmiles74/clj`.

----

[clj]: https://en.wikipedia.org/wiki/Clojure
