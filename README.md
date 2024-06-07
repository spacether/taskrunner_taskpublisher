# Task Publisher
Every minute: reads tasks in that minute from the relational db,
 then publishes any found tasks to a queue

## Running

Execute this command to run this sample:

```bash
brew services start rabbitmq
./gradlew run
```
browse to http://localhost:15672/ with guest guest for rabbitmq visibility

## Other repos
- [taskui](https://github.com/spacether/taskrunner_taskui)
- [taskrunner](https://github.com/spacether/taskrunner_taskrunner)