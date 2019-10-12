package protocols

/**
  * @author ravil
  */
object RankActorProtocol {

  case class BuildRankModel()

  case class PredictMessage(query: String, content: String, wordsCount: Int)

}
