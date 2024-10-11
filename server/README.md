# Server training project

This a Gradle project contains a training server meant to be used along the Gatling gRPC training projects.

## Usage

The Gradle application plugin is used to configure the project, which can then be run with the Gradle task `run`

Running the task with no project property will run the Calculator server by default:

```console
./gradlew run
```

It will run the service on the port `50052`.
