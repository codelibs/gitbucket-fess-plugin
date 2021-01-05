package org.codelibs.gitbucket.fess.controller

import scala.util.Using

import gitbucket.core.api._
import gitbucket.core.service._
import gitbucket.core.util._
import gitbucket.core.util.Directory._
import gitbucket.core.util.SyntaxSugars._
import gitbucket.core.controller.ControllerBase
import gitbucket.core.service.IssuesService
import gitbucket.core.service.IssuesService.IssueSearchCondition
import gitbucket.core.util.Implicits._
import org.codelibs.gitbucket.fess.service.FessSearchService
import org.eclipse.jgit.api.Git

class FessApiController
    extends FessApiControllerBase
    with RepositoryService
    with AccountService
    with AdminAuthenticator
    with UsersAuthenticator
    with WikiService
    with LabelsService
    with PrioritiesService
    with MilestonesService
    with IssuesService
    with FessSearchService

trait FessApiControllerBase extends ControllerBase {
  self: RepositoryService
    with AccountService
    with AdminAuthenticator
    with UsersAuthenticator
    with WikiService
    with LabelsService
    with PrioritiesService
    with MilestonesService
    with IssuesService
    with FessSearchService =>

  val FESS_API_VERSION = "1.0.0"

  get("/api/v3/fess/info")(usersOnly {
    JsonFormat(
      FessInfoResponse(
        FESS_API_VERSION,
        SourceLabel,
        IssueLabel,
        WikiLabel
      ))
  })

  get("/api/v3/fess/repos")(usersOnly {
    val num: Int    = params.getOrElse("num", "20").toIntOpt.getOrElse(20)
    val offset: Int = params.getOrElse("offset", "0").toIntOpt.getOrElse(0)
    val allRepos    = getVisibleRepositories(context.loginAccount)
    val repos =
      allRepos.slice(offset, offset + num).map {
        r =>
          {
            // Note: 'r.issueCount' and 'r.pullCount' are always 0 (See the implementation of 'getVisibleRepositories')
            // Thus we should compute them here.
            val countFn = (state: String, isPull: IssueSearchOption) =>
              countIssue(IssueSearchCondition(state = state),
                         isPull,
                         (r.owner, r.name))
            val issueCount = countFn("open", IssueSearchOption.Issues) + countFn("closed", IssueSearchOption.Issues)
            val pullCount  = countFn("open", IssueSearchOption.PullRequests) + countFn("closed", IssueSearchOption.PullRequests)
            val branch     = getDefaultBranch(r).getOrElse("")

            FessRepositoryInfo(r.name,
                               r.owner,
                               branch,
                               r.repository.isPrivate,
                               issueCount,
                               pullCount,
                               getCollaboratorUserNames(r.owner, r.name))
          }
      }
    JsonFormat(FessResponse(allRepos.size, repos.size, offset, repos))
  })

  private def getDefaultBranch(
      r: RepositoryService.RepositoryInfo): Option[String] =
    Using.resource(Git.open(getRepositoryDir(r.owner, r.name))) { git =>
      JGitUtil.getDefaultBranch(git, r).map {
        case (_, branch) =>
          branch
      }
    }

  get("/api/v3/fess/:owner/:repo/wiki")(adminOnly({
    val owner = params.get("owner").get
    val repo  = params.get("repo").get
    FessWikiPageList(
      getWikiPageList(owner, repo).map(java.net.URLEncoder.encode(_, "UTF-8")))
  }))

  get("/api/v3/fess/:owner/:repo/wiki/contents/:path")(adminOnly({
    val owner = params.get("owner").get
    val repo  = params.get("repo").get
    contentType = "application/vnd.github.v3.raw"

    params
      .get("path")
      .flatMap({
        path =>
          Using.resource(Git.open(getWikiRepositoryDir(owner, repo))) {
            git =>
              getRepository(owner, repo).flatMap({ repository =>
                val revCommit =
                  JGitUtil.getRevCommitFromId(
                    git,
                    git.getRepository.resolve("master"))
                getPathObjectId(git, path, revCommit).map { objectId =>
                  responseRawFile(git,
                                  objectId,
                                  java.net.URLEncoder.encode(path, "UTF-8"),
                                  repository)
                }
              })
          }
      }) getOrElse NotFound()
  }))
}

case class FessInfoResponse(version: String,
                            source_label: String,
                            issue_label: String,
                            wiki_label: String)

case class FessLabelResponse(source_label: List[String])

case class FessRepositoryInfo(name: String,
                              owner: String,
                              branch: String,
                              is_private: Boolean,
                              issue_count: Int,
                              pull_count: Int,
                              collaborators: List[String])

case class FessResponse(total_count: Int,
                        response_count: Int,
                        offset: Int,
                        repositories: List[FessRepositoryInfo])

case class FessWikiPageList(pages: List[String])
