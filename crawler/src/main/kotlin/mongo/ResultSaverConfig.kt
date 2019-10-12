package mongo

data class ResultSaverConfig(
        val host: String,
        val databaseName: String,
        val collectionName: String
)