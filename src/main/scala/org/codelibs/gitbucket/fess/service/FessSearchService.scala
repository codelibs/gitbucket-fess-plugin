package org.codelibs.gitbucket.fess.service

import java.net.URL
import java.net.URLEncoder

import gitbucket.core.model.{Issue, Session}
import gitbucket.core.service.RepositorySearchService.IssueSearchResult
import gitbucket.core.service.IssuesService
import org.json4s._
import org.json4s.jackson.JsonMethods._

import scala.io.Source._
import gitbucket.core.util._
import gitbucket.core.util.JGitUtil._
import gitbucket.core.util.ControlUtil._
import gitbucket.core.util.Directory._
import org.codelibs.gitbucket.fess.service.FessSettingsService.FessSettings
import org.eclipse.jgit.api.Git
import org.slf4j.LoggerFactory

trait FessSearchService { self: IssuesService =>
  import gitbucket.core.service.RepositorySearchService._

  val logger = LoggerFactory.getLogger(getClass)

  val SourceLabel = "gitbucket_source"
  val IssueLabel  = "gitbucket_issue"
  val WikiLabel   = "gitbucket_wiki"

  def searchByFess(user: String,
                   query: String,
                   setting: FessSettings,
                   offset: Int,
                   num: Int,
                   label: String) = {
    val encodedQuery = URLEncoder.encode(query, "UTF-8")
    val encodedLabel = URLEncoder.encode("label:" + label, "UTF-8")
    val urlStr =
      s"${setting.fessUrl}/json/?q=$encodedQuery&start=$offset&num=$num&ex_q=$encodedLabel&permission=1$user"
    val conn = new URL(urlStr).openConnection
    setting.fessToken.foreach(token =>
      conn.addRequestProperty("Authorization", "Bearer " + token))

    fromInputStream(conn.getInputStream).mkString
  }

  def searchFilesByFess(user: String,
                        query: String,
                        setting: FessSettings,
                        offset: Int,
                        num: Int): Either[String, FessSearchResult] = {
    implicit val formats = DefaultFormats
    try {
      val response =
        searchByFess(user, query, setting, offset, num, SourceLabel)
      val fessJsonResponse =
        (parse(response) \ "response").extract[FessRawResponse]

      val fileList = fessJsonResponse.result.map(result => {
        val (owner, repo, branch, path) = getRepositoryDataFromURL(result.url)
        val content =
          getContent(owner, repo, branch, path).getOrElse("")
        val (highlightText, highlightLineNumber) =
          getHighlightText(content, query)
        FessFileInfo(owner,
                     repo,
                     result.url,
                     result.title,
                     highlightText,
                     highlightLineNumber)
      })
      Right(
        FessSearchResult(query,
                         offset,
                         fessJsonResponse.record_count,
                         fileList))
    } catch {
      case e: org.eclipse.jgit.errors.RepositoryNotFoundException => {
        logger.info(e.getMessage, e)
        Left(e.getMessage)
      }
      case e: java.net.UnknownHostException => {
        logger.info(e.getMessage, e)
        Left(s"Failed to connect to ${setting.fessUrl}")
      }
      case e: Throwable => {
        logger.info(e.getMessage, e)
        Left(e.getMessage)
      }
    }
  }

  def getIssueWithComments(
      owner: String,
      repo: String,
      issueId: String,
      query: String)(implicit session: Session): (Issue, Int, String) = {
    // TODO: Error Handling
    val issue = getIssue(owner, repo, issueId).get
    val comments = issue.content.getOrElse("") :: getComments(
        owner,
        repo,
        issueId.toInt).map(_.content)
    val matched = comments.find(_.contains(query)).getOrElse(comments.head)
    (issue, comments.length, matched)
  }

  def searchIssuesByFess(user: String,
                         query: String,
                         setting: FessSettings,
                         offset: Int,
                         num: Int)(
      implicit session: Session): Either[String, FessIssueSearchResult] = {
    implicit val formats = DefaultFormats
    try {
      val response =
        searchByFess(user, query, setting, offset, num, IssueLabel)
      val fessJsonResponse =
        (parse(response) \ "response").extract[FessRawResponse]

      val issueList = fessJsonResponse.result.map(result => {
        val (owner, repo, issueId) = getIssueDataFromURL(result.url)
        val (issue, commentCount, content) =
          getIssueWithComments(owner, repo, issueId, query)
        val issueInfo = IssueSearchResult(issue.issueId,
                                          issue.isPullRequest,
                                          issue.title,
                                          issue.openedUserName,
                                          issue.registeredDate,
                                          commentCount,
                                          getHighlightText(content, query)._1)
        (owner, repo, issueInfo)
      })
      Right(
        FessIssueSearchResult(query,
                              offset,
                              fessJsonResponse.record_count,
                              issueList))
    } catch {
      case e: org.eclipse.jgit.errors.RepositoryNotFoundException => {
        logger.info(e.getMessage, e)
        Left(e.getMessage)
      }
      case e: java.net.UnknownHostException => {
        logger.info(e.getMessage, e)
        Left(s"Failed to connect to ${setting.fessUrl}")
      }
      case e: Throwable => {
        logger.info(e.getMessage, e)
        Left(e.getMessage)
      }
    }
  }

  def getContent(owner: String,
                 repo: String,
                 revStr: String,
                 path: String): Option[String] =
    using(Git.open(getRepositoryDir(owner, repo))) { git =>
      val revCommit =
        JGitUtil.getRevCommitFromId(git, git.getRepository.resolve(revStr))
      getContentFromPath(git, revCommit.getTree, path, false).map(x =>
        new String(x))
    }

  def getRepositoryDataFromURL(url: String): (String, String, String, String) = {
    val Pattern =
      ".*/([a-zA-Z0-9-_.]+)/([a-zA-Z0-9-_.]+)/blob/([a-zA-Z0-9-_.]+)/(.*)".r
    val Pattern(owner, repo, revStr, path) = url
    (owner, repo, revStr, path)
  }
  def getIssueDataFromURL(url: String): (String, String, String) = {
    val Pattern =
      ".*/([a-zA-Z0-9-_.]+)/([a-zA-Z0-9-_.]+)/issues/([0-9]+)/?".r
    val Pattern(owner, repo, issueId) = url
    (owner, repo, issueId)
  }
}

case class FessSearchResult(query: String,
                            offset: Int,
                            hit_count: Int,
                            file_list: List[FessFileInfo])

case class FessIssueSearchResult(
    query: String,
    offset: Int,
    hit_count: Int,
    issue_list: List[(String, String, IssueSearchResult)])

case class FessFileInfo(owner: String,
                        repo: String,
                        url: String,
                        title: String,
                        digest: String,
                        highlight_line_number: Int)

case class FessRawResult(url: String, title: String)

case class FessRawResponse(status: Int,
                           page_size: Int,
                           page_number: Int,
                           page_count: Int,
                           record_count: Int,
                           result: List[FessRawResult])
