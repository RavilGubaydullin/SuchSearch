package main

import consumer.rabbitmq.RabbitmqConsumerConfig
import consumer.rabbitmq.RabbitmqTaskConsumer
import mongo.ResultSaver
import mongo.ResultSaverConfig
import worker.CrawlerController

object Application {

    @JvmStatic
    fun main(args: Array<String>) {



        val crawlerController = CrawlerController()

        val resultSaverConfig = ResultSaverConfig(
                host = "139.59.128.242:27017",
                databaseName = "SearchEngine",
                collectionName = "WebTable"
        )
        val resultSaver = ResultSaver(resultSaverConfig)

        val config = RabbitmqConsumerConfig(
                host = "139.59.128.242",
                queueName = "crawl-tasks"
        )
        val rabbitmqTaskConsumer = RabbitmqTaskConsumer(
                config = config,
                crawlerController = crawlerController,
                resultSaver = resultSaver
        )
        rabbitmqTaskConsumer.start()
    }

}