package org.codelibs.gitbucket.fess.service

import java.net.{URL, URLEncoder}
import java.util.Date

import gitbucket.core.controller.ControllerBase
import gitbucket.core.model.{Issue, Session}
import gitbucket.core.service.{AccountService, IssuesService, WikiService}
import gitbucket.core.util.Directory._
import gitbucket.core.util.JGitUtil._
import gitbucket.core.util.SyntaxSugars._
import gitbucket.core.util._
import org.codelibs.gitbucket.fess.service.FessSettingsService.FessSettings
import org.eclipse.jgit.api.Git
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.slf4j.LoggerFactory

import scala.io.Source._

trait FessSearchService {
  self: IssuesService
    with WikiService
    with AccountService
    with ControllerBase =>
  import gitbucket.core.service.RepositorySearchService._

  val logger = LoggerFactory.getLogger(getClass)

  val SourceLabel = "gitbucket_source"
  val IssueLabel  = "gitbucket_issue"
  val WikiLabel   = "gitbucket_wiki"

  // Get Search Results from Fess
  def accessFess(user: Option[String],
                 query: String,
                 setting: FessSettings,
                 offset: Int,
                 num: Int,
                 label: String)(implicit session: Session): String = {
    val encodedQuery = URLEncoder.encode(query, "UTF-8")
    val encodedLabel = URLEncoder.encode("label:" + label, "UTF-8")
    val permissionParams = {
      user
        .map(s => {
          s :: getGroupsByUserName(s)
        })
        .getOrElse(List.empty)
    }.map(s => "permission=1" + s).mkString("&")

    val urlStr =
      s"${setting.fessUrl}/json/?q=$encodedQuery&start=$offset&num=$num&ex_q=$encodedLabel&$permissionParams"
    logger.debug("GET: " + urlStr)
    val conn = new URL(urlStr).openConnection
    setting.fessToken.foreach(token =>
      conn.addRequestProperty("Authorization", token))

    fromInputStream(conn.getInputStream).mkString
  }

  def execSearch(user: Option[String],
                 query: String,
                 setting: FessSettings,
                 offset: Int,
                 num: Int,
                 label: String)(implicit session: Session)
    : Either[String, (FessSearchInfo, List[FessRawResult])] = {
    implicit val formats = DefaultFormats
    try {
      val response =
        accessFess(user, query, setting, offset, num, label)
      val fessJsonResponse =
        (parse(response) \ "response").extract[FessRawResponse]
      Right(
        (FessSearchInfo(query, offset, fessJsonResponse.record_count),
         fessJsonResponse.result))
    } catch {
      case e: org.eclipse.jgit.errors.RepositoryNotFoundException =>
        logger.info(e.getMessage, e)
        Left(e.getMessage)
      case e: java.net.UnknownHostException =>
        logger.info(e.getMessage, e)
        Left(s"Failed to connect to ${setting.fessUrl}")
      case e: Throwable =>
        logger.info(e.getMessage, e)
        Left(e.getMessage)
    }
  }

  // Utils
  def getRepositoryDataFromURL(
      url: String): Option[(String, String, String, String)] =
    try {
      val Pattern =
        ".*/([a-zA-Z0-9-_.]+)/([a-zA-Z0-9-_.]+)/blob/([a-zA-Z0-9-_.]+)/(.*)".r
      val Pattern(owner, repo, revStr, path) = url
      Some(owner, repo, revStr, java.net.URLDecoder.decode(path, "UTF-8"))
    } catch {
      case e: Throwable =>
        logger.info(e.getMessage, e)
        None
    }

  def getIssueDataFromURL(url: String): Option[(String, String, String)] =
    try {
      val Pattern =
        ".*/([a-zA-Z0-9-_.]+)/([a-zA-Z0-9-_.]+)/issues/([0-9]+)/?".r
      val Pattern(owner, repo, issueId) = url
      Some((owner, repo, issueId))
    } catch {
      case e: Throwable =>
        logger.info(e.getMessage, e)
        None
    }

  def getWikiDataFromURL(url: String): Option[(String, String, String)] =
    try {
      val Pattern =
        ".*/([a-zA-Z0-9-_.]+)/([a-zA-Z0-9-_.]+)/wiki/(.*)".r
      val Pattern(owner, repo, filename) = url
      Some((owner, repo, java.net.URLDecoder.decode(filename, "UTF-8")))
    } catch {
      case e: Throwable =>
        logger.info(e.getMessage, e)
        None
    }

  // For Code Search
  def getFileContent(owner: String,
                     repo: String,
                     revStr: String,
                     path: String): Option[String] =
    using(Git.open(getRepositoryDir(owner, repo))) { git =>
      val revCommit =
        JGitUtil.getRevCommitFromId(git, git.getRepository.resolve(revStr))
      getPathObjectId(git, path, revCommit).flatMap({ objectId =>
        JGitUtil.getContentInfo(git, path, objectId).content
      })
    }

  def getCodeContents(query: String,
                      results: List[FessRawResult]): List[FessCodeInfo] =
    results.flatMap(result => {
      getRepositoryDataFromURL(result.url).map({
        case (owner, repo, branch, path) =>
          val content =
            getFileContent(owner, repo, branch, path).getOrElse("")
          val (highlightText, highlightLineNumber) =
            getHighlightText(content, query)
          FessCodeInfo(owner,
                       repo,
                       result.url,
                       result.title,
                       highlightText,
                       highlightLineNumber)
      })
    })

  // For Issue Search
  def getIssueWithComments(
      owner: String,
      repo: String,
      issueId: String,
      query: String)(implicit session: Session): Option[(Issue, Int, String)] =
    getIssue(owner, repo, issueId).map(issue => {
      val comments = issue.content.getOrElse("") :: getComments(
          owner,
          repo,
          issueId.toInt).map(_.content)
      val matched = comments.find(_.contains(query)).getOrElse(comments.head)
      (issue, comments.length, matched)
    })

  def getIssueContents(query: String, results: List[FessRawResult])(
      implicit session: Session): List[FessIssueInfo] =
    results.flatMap(result => {
      getIssueDataFromURL(result.url).flatMap({
        case (owner, repo, issueId) =>
          getIssueWithComments(owner, repo, issueId, query).map({
            case (issue, count, content) =>
              FessIssueInfo(owner,
                            repo,
                            issue.issueId,
                            issue.isPullRequest,
                            issue.title,
                            issue.openedUserName,
                            issue.registeredDate,
                            count,
                            getHighlightText(content, query)._1)
          })
      })
    })

  // For Wiki Search
  def getWikiContents(query: String,
                      results: List[FessRawResult]): List[FessWikiInfo] =
    results.flatMap(result => {
      getWikiDataFromURL(result.url).flatMap({
        case (owner, repo, filename) =>
          getWikiPage(owner, repo, filename).map(wikiInfo => {
            val (content, lineNum) = getHighlightText(wikiInfo.content, query)
            FessWikiInfo(owner, repo, result.url, filename, content, lineNum)
          })
      })
    })

  // Used from FessSearchController
  def searchCodeOnFess(user: Option[String],
                       query: String,
                       setting: FessSettings,
                       offset: Int,
                       num: Int)(implicit session: Session)
    : Either[String, (FessSearchInfo, List[FessCodeInfo])] =
    try {
      execSearch(user, query, setting, offset, num, SourceLabel).right.map({
        case (info, res) => (info, getCodeContents(query, res))
      })
    } catch {
      case e: Throwable =>
        logger.info(e.getMessage, e)
        Left(e.getMessage)
    }

  def searchIssueOnFess(user: Option[String],
                        query: String,
                        setting: FessSettings,
                        offset: Int,
                        num: Int)(implicit session: Session)
    : Either[String, (FessSearchInfo, List[FessIssueInfo])] =
    try {
      execSearch(user, query, setting, offset, num, IssueLabel).right.map({
        case (info, res) => (info, getIssueContents(query, res))
      })
    } catch {
      case e: Throwable =>
        logger.info(e.getMessage, e)
        Left(e.getMessage)
    }

  def searchWikiOnFess(user: Option[String],
                       query: String,
                       setting: FessSettings,
                       offset: Int,
                       num: Int)(implicit session: Session)
    : Either[String, (FessSearchInfo, List[FessWikiInfo])] =
    try {
      execSearch(user, query, setting, offset, num, WikiLabel).right.map({
        case (info, res) => (info, getWikiContents(query, res))
      })
    } catch {
      case e: Throwable =>
        logger.info(e.getMessage, e)
        Left(e.getMessage)
    }
}

sealed trait FessContentInfo
case class FessCodeInfo(owner: String,
                        repo: String,
                        url: String,
                        title: String,
                        digest: String,
                        highlight_line_number: Int)
    extends FessContentInfo
case class FessIssueInfo(owner: String,
                         repo: String,
                         issue_id: Int,
                         isPullRequest: Boolean,
                         title: String,
                         openedUserName: String,
                         registered_date: Date,
                         comment_count: Int,
                         highlighted_text: String)
    extends FessContentInfo
case class FessWikiInfo(owner: String,
                        repo: String,
                        url: String,
                        title: String,
                        digest: String,
                        highlight_line_number: Int)
    extends FessContentInfo

case class FessSearchInfo(query: String, offset: Int, hit_count: Int)

case class FessRawResult(url: String, title: String)

case class FessRawResponse(status: Int,
                           page_size: Int,
                           page_number: Int,
                           page_count: Int,
                           record_count: Int,
                           result: List[FessRawResult])
