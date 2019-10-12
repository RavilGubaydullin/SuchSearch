package consumer.rabbitmq

data class RabbitmqConsumerConfig(
        val host: String,
        val queueName: String
)