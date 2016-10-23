package org.codelibs.gitbucket.fess.service

import java.net.URL

import gitbucket.core.util._
import org.json4s._
import org.json4s.jackson.JsonMethods._
import scala.io.Source._

trait FessSearchService {
  import gitbucket.core.service.RepositorySearchService._

  def searchFiles(query: String, offset: Int, num: Int): FessSearchResult = {
    implicit val formats = DefaultFormats
    val conn = new URL(s"http://localhost:8080/json/?q=$query&start=$offset&num=$num").openConnection // TODO prefix_query
    val response = fromInputStream(conn.getInputStream).mkString
    val fessJsonResponse = (parse(response) \ "response").extract[FessRawResponse]
    val fileList = fessJsonResponse.result.map(result => {
      val (highlightText, _)  = getHighlightText(result.digest, query)
      val (owner, repo) = getRepositoryDataFromURL(result.url)
      FessFileInfo(owner, repo, result.url, result.title, highlightText)
    })
    FessSearchResult(query, offset, fessJsonResponse.record_count, fileList)
  }

  def getRepositoryDataFromURL(url: String): (String, String) = {
    val pattern = ".*/([a-zA-Z0-9]+)/([a-zA-Z0-9]+)/blob/.*".r
    val pattern(owner, repo) = url
    (owner, repo)
  }

}



case class FessSearchResult(query: String,
                            offset: Int,
                            hit_count: Int,
                            file_list: List[FessFileInfo])

case class FessFileInfo(owner: String,
                        repo: String,
                        url: String,
                        title: String,
                        digest: String)

case class FessRawResult(filetype: String,
                         url: String,
                         title: String,
                         digest: String)

case class FessRawResponse(status: Int,
                           page_size: Int,
                           page_number: Int,
                           page_count: Int,
                           record_count: Int,
                           result: List[FessRawResult])

