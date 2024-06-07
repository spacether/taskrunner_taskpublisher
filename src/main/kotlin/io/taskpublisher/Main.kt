package io.taskpublisher

import java.time.LocalDateTime
import org.jetbrains.exposed.sql.Database
import kotlinx.coroutines.*
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import com.rabbitmq.client.ConnectionFactory
import io.taskdata.DbTaskRepository
import io.taskdata.TaskRepository
import io.taskmodels.TaskMessage
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Settings {
    companion object {
        const val QUEUE_NAME = "tasks"
        val DELAY_TIME = 1.toDuration(DurationUnit.MINUTES)
    }
}

suspend fun publishTask(repository: TaskRepository) {
    val now = LocalDateTime.now()
    val hour = now.hour
    val minute = now.minute
    val tasks = repository.queryTasks(hour, minute)
    println(tasks)
    println("I ran. hour = ${hour} minute=${minute} \n")

    val factory = ConnectionFactory()
    val uri = System.getenv("CLOUDAMQP_URL") ?: "amqp://guest:guest@localhost"
    factory.setUri(uri);
    factory.setRequestedHeartbeat(30);
    factory.setConnectionTimeout(30);
    val connection = factory.newConnection()
    val channel = connection.createChannel()

    channel.queueDeclare(Settings.QUEUE_NAME, true, false, false, null)
    for (task in tasks) {
        val triggerTime = "${String.format("%02d", hour)}:${String.format("%02d", minute)}"
        val taskMessage = TaskMessage(
            task=task,
            triggerTime=triggerTime,
            timesRun=0
        )
        val rawMessage = Json.encodeToString(taskMessage).toByteArray()
        channel.basicPublish("", Settings.QUEUE_NAME, null, rawMessage)
        println(" [x] Sent '$taskMessage'")
    }
    channel.close()
    connection.close()
}

fun configureDatabases() {
    val url = System.getenv("JDBC_DATABASE_URL") ?: "jdbc:postgresql://localhost:5432/taskrunner_db"
    Database.connect(url)
}

suspend fun main() {
    configureDatabases()
    val repository = DbTaskRepository()
    while (true) {
        publishTask(repository)
        delay(Settings.DELAY_TIME)
    }
}