package mongo

import com.mongodb.MongoClient
import org.bson.BsonDocument
import org.bson.Document
import worker.CrawlerController
import java.util.concurrent.CompletableFuture

class ResultSaver(resultSaverConfig: ResultSaverConfig) {

    val mongoClient = MongoClient(resultSaverConfig.host)
    val database = mongoClient.getDatabase(resultSaverConfig.databaseName)
    val collection = database.getCollection(resultSaverConfig.collectionName)

    fun sendResult(pages: List<CrawlerController.Page>): CompletableFuture<Void> {
        return CompletableFuture.runAsync {
            val documents =  pages.forEach() {
                val doc = Document("_id", it.url.replace(".", ""))
                        .append("content", it.content)
                        .append("isCrawled", false)
                        .append("crawled", System.currentTimeMillis())

                collection.updateOne(Document("_id", it.url), doc)
            }
        }
    }
}