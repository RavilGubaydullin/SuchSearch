package actors

import akka.actor.Actor
import akka.actor.Actor.Receive
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.map_reduce.MapReduceStandardOutput

import play.Logger
import protocols.IndexatorActorProtocol.StartIndex

/**
  * @author ravil
  */
class IndexatorActor extends Actor{

  private val CONNECTION_STRING = "mongodb://139.59.128.242:27017"
  private val DATABASE_NAME = "SearchEngine"
  private val WEB_TABLE_COLLECTION = "WebTable"
  private val INDEX_COLLECTION = "IndexTable"

  val uri = MongoClientURI(CONNECTION_STRING)
  val client = MongoClient(uri)
  val database = client(DATABASE_NAME)
  val webTable = database(WEB_TABLE_COLLECTION)
  val indexTable = database(INDEX_COLLECTION)

  override def receive: Receive = {
    case StartIndex() =>
      Logger.info("Start index")

      indexTable.drop()

      val command = MapReduceCommand(WEB_TABLE_COLLECTION, mapJs, reduceJs, new MapReduceStandardOutput(INDEX_COLLECTION))
      val result = webTable.mapReduce(command)

      if (!result.isError) {
        Logger.info("Indexed")
      } else {
        Logger.info("Not indexed")
      }
  }

  val mapJs =
    """
      |function() {
      |	if (this.content) {
      |		var splitedItems = this.content.split(" ");
      |		splitedItems.sort();
      |
      |		var curent = null;
      |		var count = 0;
      |
      |		for (var i = 0; i < splitedItems.length; i++) {
      |       if (splitedItems[i].length < 30) {
      |	   		  if (splitedItems[i] != curent) {
      |	   			  if (count > 0) {
      |	   				  var resultStr = this._id + " " + count;
      |	   				  emit(curent, resultStr);
      |	   		  	}
      |
      |	   			  curent = splitedItems[i];
      |	   			  count = 1;
      |	   		  } else {
      |	   		  	count++;
      |	   		  }
      |      }
      |		}
      |	}
      |}
    """.stripMargin

  val reduceJs =
    """
      |function(keyCustId, values) {
      | return values.join();
      |};
    """.stripMargin

}
