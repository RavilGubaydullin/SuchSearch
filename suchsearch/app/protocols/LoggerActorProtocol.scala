package protocols

/**
  * @author ravil
  */
object LoggerActorProtocol {

  case class LogMessage(userId: String, url: String, wordsCount: Int, query: String, eventTime: Long)
}
