package actors

import akka.actor.Actor
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.map_reduce.MapReduceStandardOutput
import com.rabbitmq.client.{Channel, Connection, ConnectionFactory}
import play.Logger
import play.api.libs.json._
import protocols.CrawlQueueCreatorActorProtocol.StartMessage




/**
  * @author ravil
  */
class CrawlTasksCreatorActor extends Actor {

  private val CONNECTION_STRING = "mongodb://139.59.128.242:27017"
  private  val DATABASE_NAME = "SearchEngine"
  private val COLLECTION_NAME = "WebTable"
  private val CRAWL_QUEUE_COLLECTION = "CrawlCollection"

  val uri = MongoClientURI(CONNECTION_STRING)
  val client = MongoClient(uri)
  val database = client(DATABASE_NAME)
  val webTable = database(COLLECTION_NAME)
  val crawlQueueTable = database(CRAWL_QUEUE_COLLECTION)

  private val RABBIT_MQ_HOST = "139.59.128.242"
  private val QUEUE_NAME = "crawl-tasks"

  val factory = new ConnectionFactory()
  factory.setHost(RABBIT_MQ_HOST)
  val connection: Connection = factory.newConnection()
  val channel: Channel = connection.createChannel()
  channel.queueDeclare(QUEUE_NAME, false, false, false, null)

  override def receive: Receive = {
    case StartMessage() =>
      Logger.info("Started creat queue")
      crawlQueueTable.drop()

      val command = MapReduceCommand(COLLECTION_NAME, mapJs, reduceJs, new MapReduceStandardOutput(CRAWL_QUEUE_COLLECTION))
      val result = webTable.mapReduce(command)

      crawlQueueTable.find().foreach((obj: DBObject) => {
        sendToMessageBroker(obj)
      })
  }

  private def sendToMessageBroker(obj: DBObject): Unit = {
    val priority = obj.get("_id").asInstanceOf[Double]
    val pagesStr = obj.get("value").asInstanceOf[String]

    val pages = pagesStr.split(",").toList


    if (pages.size > 200) {
      pages.grouped(200).foreach((list: List[String]) => {
        val resultStr = buildJsonStr(priority, list)
        sendJsonToBroker(resultStr)
      })
    } else {
      val resultStr = buildJsonStr(priority, pages)
      sendJsonToBroker(resultStr)
    }

    Logger.info("Queue sended to broker")
  }

  private def sendJsonToBroker(jsonStr: String): Unit = {
    channel.basicPublish("", QUEUE_NAME, null, jsonStr.getBytes)
  }

  private def buildJsonStr(priority: Double, pages: List[String]): String = {
    val resultJs = JsObject(Seq(
      "priority" -> Json.toJson(priority),
      "pages" -> Json.toJson(pages)
    ))

    resultJs.toString()
  }

  val mapJs =
      """
        |function() {
        | var isCrawl = this.isCrawled;
        | var otherDay, today;
        |
        | if (!isCrawl) {
        |	  today = new Date().valueOf();
        |	  otherDay = this.added;
        |	  if (otherDay < today - 86400000) {
        |		  emit(1, this._id)
        |	  } else {
        |	  	emit(2, this._id)
        |	  }
        | }
        |}
      """.stripMargin

  val reduceJs =
    """
      |function(keyCustId, values) {
      | var a = {
      |   "priority" : keyCustId,
      |   "pages" : values
      | };
      | return values.join();
      |};
    """.stripMargin
}
