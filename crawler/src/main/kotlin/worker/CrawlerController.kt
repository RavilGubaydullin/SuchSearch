package worker

import data.WebPageTask
import java.util.concurrent.Executors
import com.github.kittinunf.fuel.httpGet
import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture

import java.util.function.Supplier

/**
 * Created by ravil on 31/05/2019.
 */
class CrawlerController {

    // get from config
    private val threadPool = Executors.newFixedThreadPool(10)

    fun executeNewTasks(list: List<WebPageTask>): CompletableFuture<List<Page>> {
        val futeres = list
                .map { task ->
                    CompletableFuture.supplyAsync( Supplier {
                        val response = task.url.httpGet().response()
                        val body = response.second.body().asString(null)

                        val parsed = body
                                .replace(Regex.fromLiteral("""[\p{Punct}]"""), "")
                                .trim()
                        return@Supplier Page(task.url, parsed)
                    }, threadPool)
                }

        return CompletableFuture.allOf(*futeres.toTypedArray())
                .thenApply {
                    futeres.map { it.join() }
                }
    }


    class Page(
            val url: String,
            val content: String
    )
}