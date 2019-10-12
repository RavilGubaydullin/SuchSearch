package controllers

import javax.inject._
import akka.actor.ActorSystem
import com.mongodb.casbah.Imports._
import model.Page
import play.api._
import play.api.libs.concurrent.Akka
import play.api.mvc._
import protocols.LoggerActorProtocol.LogMessage
import protocols.RankActorProtocol.PredictMessage
import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import akka.pattern.ask

import akka.util.Timeout
import scala.collection.mutable

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(system: ActorSystem) extends Controller {

  val QUERY_PARAM = "query"

  private val CONNECTION_STRING = "mongodb://139.59.128.242:27017"
  private val DATABASE_NAME = "SearchEngine"
  private val INDEX_COLLECTION = "IndexTable"
  private val WEB_TABLE_COLLECTION = "WebTable"

  private val USER_ID = "user_id"

  private val rankActor = system.actorSelection("user/rank*")




  def index = Action { request =>
    if (request.queryString.contains(QUERY_PARAM)) {
      val querySeq = request.queryString.get(QUERY_PARAM).get
      val queryString = querySeq.head
      val words = queryString.split(" ")
      Logger.info(queryString)

      val uri = MongoClientURI(CONNECTION_STRING)
      val client = MongoClient(uri)
      val database = client(DATABASE_NAME)
      val indexTable = database(INDEX_COLLECTION)
      val webTable = database(WEB_TABLE_COLLECTION)

      // get pages url
      val result = new mutable.MutableList[String]
      words.foreach((word) => {

        val recordOptinal = indexTable.findOneByID(word)
        if (recordOptinal.isDefined) {
          val record = recordOptinal.get.asInstanceOf[BasicDBObject]
          val pagesString = record.get("value").asInstanceOf[String]

          val pagesWithCount = pagesString.split(",")
          pagesWithCount.foreach((pageWithCount: String) => {
//            val url = pageWithCount.split(" ")(0)

            result += pageWithCount
            Logger.info(pageWithCount)
          })
        }
      })

      // urls to pages
      var relativePageResult = new mutable.MutableList[Page]
      val nonRelativePageResult = new mutable.MutableList[Page]
      result.foreach((pageWithCount: String) => {
        val splited = pageWithCount.split(" ")
        val urlStr = splited(0)
        val wordsCount = splited(1).toInt

        val pageDocument = webTable.findOneByID(urlStr).get.asInstanceOf[BasicDBObject]

        val contentRawObj = Option(pageDocument.get("content"))

        if (contentRawObj.isDefined) {
          val contentRaw = contentRawObj.get.asInstanceOf[String]
          val content =
            if (contentRaw.length > 300) {
              contentRaw.substring(0, 300)
            } else {
              contentRaw
            }

          val titleStr = pageDocument.get("title").asInstanceOf[String]

          val page = new Page(url = urlStr, title = titleStr, pageContent = content, wordsCount = wordsCount)

          implicit val timeout: Timeout = 5.seconds
          (rankActor ? new PredictMessage(queryString, page.pageContent, page.wordsCount)).mapTo[Int].map { message =>
            if (message == 1) {
              relativePageResult += page

              Logger.info(s"Page with url ${page.url} is relevant")
            } else {
              nonRelativePageResult += page
              Logger.info(s"Page with url ${page.url} is not relevant")
            }
          }
        }

      })

      relativePageResult = relativePageResult.++=(nonRelativePageResult)

      if (request.cookies.get(USER_ID).isDefined) {
        val id = request.cookies.get(USER_ID).get.value
        Ok(views.html.result.render(queryString, relativePageResult.toList, id))
      } else {
        val id = java.util.UUID.randomUUID.toString
        Ok(views.html.result.render(queryString, relativePageResult.toList, id)).withCookies(Cookie(USER_ID, id))
      }
    } else {
      if (request.cookies.get(USER_ID).isDefined) {
        val id = request.cookies.get(USER_ID).get.value
        Ok(views.html.result.render("", List(), id))
      } else {
        val id = java.util.UUID.randomUUID.toString
        Ok(views.html.result.render("", List(), id)).withCookies(Cookie(USER_ID, id))
      }
    }
  }

  def log = Action { request =>
    Logger.info("LOGGER")

    val map = request.body.asFormUrlEncoded.get

    val userId = map.get("userUnID").get.head
    val eventTime = System.currentTimeMillis()
    val query = map.get("query").get.head
    val wordsCount = map.get("count").get.head.toInt
    val url = map.get("url").get.head

    Logger.info(userId)

    val logger = system.actorSelection("user/logger*")
    logger ! new LogMessage(userId = userId, eventTime = eventTime, query = query, wordsCount = wordsCount, url = url)

    Ok("ok")
  }
}
