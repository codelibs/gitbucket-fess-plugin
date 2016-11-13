package org.codelibs.gitbucket.fess.service

import java.net.URL
import java.net.URLEncoder

import org.json4s._
import org.json4s.jackson.JsonMethods._

import scala.io.Source._
import gitbucket.core.util._
import gitbucket.core.util.JGitUtil._
import gitbucket.core.util.ControlUtil._
import gitbucket.core.util.Directory._
import org.codelibs.gitbucket.fess.model.FessSetting
import org.eclipse.jgit.api.Git
import org.slf4j.LoggerFactory

trait FessSearchService {
  import gitbucket.core.service.RepositorySearchService._
  val logger =  LoggerFactory.getLogger(getClass)

  val SourceLabel = "gitbucket_source"

  def searchFiles(query: String, setting: FessSetting, offset: Int, num: Int): Either[String, FessSearchResult] = {
    implicit val formats = DefaultFormats
    try {
      val encodedQuery = URLEncoder.encode(query, "UTF-8")
      val encodedLabel = URLEncoder.encode("label:" + SourceLabel, "UTF-8")
      val urlStr = s"${setting.fessUrl}/json/?q=$encodedQuery&start=$offset&num=$num&ex_q=$encodedLabel"
      val conn = new URL(urlStr).openConnection
      setting.fessToken.foreach(token => conn.addRequestProperty("Authorization", token))
      val response = fromInputStream(conn.getInputStream).mkString
      val fessJsonResponse = (parse(response) \ "response").extract[FessRawResponse]

      val fileList = fessJsonResponse.result.map(result => {
        val (owner, repo, branch, path) = getRepositoryDataFromURL(result.url)
        val content = getContent(owner, repo, branch, path).getOrElse(result.digest)
        val (highlightText, highlightLineNumber)  = getHighlightText(content, query)
        FessFileInfo(owner, repo, result.url, result.title, highlightText, highlightLineNumber)
      })
      Right(FessSearchResult(query, offset, fessJsonResponse.record_count, fileList))
    } catch {
      case e:org.eclipse.jgit.errors.RepositoryNotFoundException => {
        logger.info(e.getMessage, e)
        Left(e.getMessage)
      }
      case e: java.net.UnknownHostException => {
        logger.info(e.getMessage, e)
        Left(s"Failed to connect to ${setting.fessUrl}")
      }
      case e:Throwable => {
        logger.info(e.getMessage, e)
        Left(e.getMessage)
      }
    }
  }

  def getContent(owner: String, repo: String, revStr: String, path: String): Option[String] = {
    using(Git.open(getRepositoryDir(owner, repo))){ git =>
      val revCommit = JGitUtil.getRevCommitFromId(git, git.getRepository.resolve(revStr))
      getContentFromPath(git, revCommit.getTree, path, false).map(x => new String(x))
    }
  }

  def getRepositoryDataFromURL(url: String): (String, String, String, String) = {
    val Pattern = ".*/([a-zA-Z0-9-_.]+)/([a-zA-Z0-9-_.]+)/blob/([a-zA-Z0-9-_.]+)/(.*)".r
    val Pattern(owner, repo, revStr, path) = url
    (owner, repo, revStr, path)
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
                        digest: String,
                        highlight_line_number: Int)

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

