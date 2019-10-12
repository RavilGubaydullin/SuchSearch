package actors

import akka.actor.Actor
import akka.actor.Actor.Receive
import com.mongodb.casbah.Imports._
import info.debatty.java.stringsimilarity.Cosine
import protocols.RankActorProtocol.{PredictMessage, BuildRankModel}
import play.Logger
import smile.classification.{KNN, SVM}
import smile.math.kernel.GaussianKernel

import scala.collection.mutable


/**
  * @author ravil
  */
class RankActor extends Actor {

  private val CONNECTION_STRING = "mongodb://139.59.128.242:27017"
  private val DATABASE_NAME = "SearchEngine"
  private val TRANINIG_COLLECTION_NAME = "TrainingSet"

  val uri = MongoClientURI(CONNECTION_STRING)
  val client = MongoClient(uri)
  val database = client(DATABASE_NAME)
  val trainingTable = database(TRANINIG_COLLECTION_NAME)

  private val USER_ID_FIELD = "userId"
  private val URL_FIELD = "url"
  private val WORDS_COUNT_FIELD = "wordsCount"
  private val QUERY_FIELD = "query"
  private val TIME_FIELD = "eventTime"
  private val COSINE_SIMIL_FIELD = "cosineSimilarity"
  private val LABEL_FIELD = "label"

  var knn: KNN[Array[Double]] = null

  override def receive: Receive = {
    case BuildRankModel() =>
      Logger.info("started")
      val set = trainingTable.find()

      val list = mutable.MutableList[Int]()
      val allX = set.map((obj: DBObject) => {
        val wordsCount = obj.get(WORDS_COUNT_FIELD).asInstanceOf[Int].toDouble
        val similarity = obj.get(COSINE_SIMIL_FIELD).asInstanceOf[Double]

        val label = obj.get(LABEL_FIELD).asInstanceOf[Int]

        list += label

        Array(wordsCount, similarity)
      }).toArray
      val allY = list.toArray

      Logger.info(allX.toString)

      val sizeOfTraning = (70 * allX.length) / 100
      val groupX = allX.grouped(sizeOfTraning.toInt).toList
      val traningX = groupX.head
      val testX = groupX(1)

      val groupY = allY.grouped(sizeOfTraning).toList
      val traningY = groupY.head
      val testY = groupY(1)

      knn = KNN.learn(traningX, traningY)


      var erorsCount = 0
      var index = 0
      testX.foreach((row: Array[Double]) => {
        val result = knn.predict(row)

        if (result != testY(index)) {
          erorsCount = erorsCount + 1
        }

        index = index + 1

      })

      Logger.info(s"Ranked done with $erorsCount errors")

    case PredictMessage(query, content, wordsCount) =>
      val arrayArgs = Array[Double](wordsCount.toDouble, findCosineSimilarity(query, content))

      sender() ! knn.predict(arrayArgs)
  }



  private def findCosineSimilarity(str1: String, str2: String): Double = {
    val cosine = new Cosine()
    if (str1 != null && str2 != null) {
      val cosineResult = cosine.similarity(str1, str2)
      cosineResult
    } else {
      0
    }

  }
}
