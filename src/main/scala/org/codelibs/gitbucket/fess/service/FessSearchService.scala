package org.codelibs.gitbucket.fess.service

import java.net.URL

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

  def searchFiles(query: String, setting: FessSetting, offset: Int, num: Int): Either[String, FessSearchResult] = {
    implicit val formats = DefaultFormats
    try {
      val conn = new URL(s"${setting.fessUrl}/json/?q=$query&start=$offset&num=$num").openConnection
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
    val Pattern = ".*/([a-zA-Z0-9]+)/([a-zA-Z0-9]+)/blob/([a-zA-Z0-9]+)/(.*)".r
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

