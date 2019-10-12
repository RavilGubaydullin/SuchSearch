package model

/**
  * @author ravil
  */
class Page(val url: String, val pageContent: String = null, var isMain: Boolean = false, var isCrawled: Boolean = false,
           val crawledTime: Long = 0, var title: String = "Title", val wordsCount: Int = 0) {


}
