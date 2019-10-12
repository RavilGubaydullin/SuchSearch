package actors

import akka.actor.Actor
import akka.actor.Actor.Receive
import com.rabbitmq.client.{Channel, Connection, ConnectionFactory}
import play.api.libs.json.{Json, JsObject}
import protocols.LoggerActorProtocol.LogMessage

/**
  * @author ravil
  */
class LoggerActor extends Actor {

  private val RABBIT_MQ_HOST = "139.59.128.242"
  private val QUEUE_NAME = "logs"

  val factory = new ConnectionFactory()
  factory.setHost(RABBIT_MQ_HOST)
  val connection: Connection = factory.newConnection()
  val channel: Channel = connection.createChannel()
  channel.queueDeclare(QUEUE_NAME, false, false, false, null)

  private val USER_ID_FIELD = "userId"
  private val URL_FIELD = "url"
  private val WORDS_COUNT_FIELD = "wordsCount"
  private val QUERY_FIELD = "query"
  private val TIME_FIELD = "eventTime"

  override def receive: Receive = {
    case LogMessage(userId, url, wordsCount, query, eventTime) =>
      val resultJs = JsObject(Seq(
        USER_ID_FIELD -> Json.toJson(userId),
        URL_FIELD -> Json.toJson(url),
        WORDS_COUNT_FIELD -> Json.toJson(wordsCount),
        QUERY_FIELD -> Json.toJson(query),
        TIME_FIELD -> Json.toJson(eventTime)
      ))

      channel.basicPublish("", QUEUE_NAME, null, resultJs.toString().getBytes)
  }
}
