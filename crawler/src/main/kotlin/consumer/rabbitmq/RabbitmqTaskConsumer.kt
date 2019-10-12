package consumer.rabbitmq

import com.beust.klaxon.Klaxon
import com.rabbitmq.client.*
import consumer.TaskConsumer
import data.WebPageTask
import data.WebPagesModel
import mongo.ResultSaver
import worker.CrawlerController
import java.lang.Exception

class RabbitmqTaskConsumer(
         private val config: RabbitmqConsumerConfig,
         private val crawlerController: CrawlerController,
         private val resultSaver: ResultSaver
) : TaskConsumer {

    private val connectionFactory: ConnectionFactory = ConnectionFactory()
    private val connection: Connection

    private var channel: Channel? = null

    init {
        connectionFactory.host = config.host
        connection = connectionFactory.newConnection()
    }

    override fun start() {
        channel = connection.createChannel()

        channel!!.basicQos(1)
        channel!!.basicConsume(
                config.queueName,
                false,
                "consumerTag",
                object : DefaultConsumer(channel) {
                    override fun handleDelivery(consumerTag: String, envelope: Envelope, properties: AMQP.BasicProperties, body: ByteArray?) {
                        super.handleDelivery(consumerTag, envelope, properties, body)

                        println("message handled")

                        val stringContent = String(body!!)


                        val pagesModel: WebPagesModel
                        try {
                            pagesModel = Klaxon().parse<WebPagesModel>(stringContent)!!
                        } catch (e: Exception) {
                            channel.basicAck(envelope.deliveryTag, true)
                            return
                        }

                        val futures = crawlerController.executeNewTasks(
                                pagesModel!!.pages.map { WebPageTask(it) }
                        )


                        futures
                                .thenApply { pages ->
                                    resultSaver.sendResult(pages)

                                }
                                .whenComplete { _, throwable ->
                                    channel.basicAck(envelope.deliveryTag, true)
                                    println("message proccesed")
                                }
                                .get()




                        // wait all futeres

                        // send to mongo


                        // ack



//                        envelope.deliver
//                        channel.basicAck()



                    }
                }
        )

    }

    override fun stop() {

    }

}