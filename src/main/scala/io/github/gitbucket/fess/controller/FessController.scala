package io.github.gitbucket.fess.controller

import gitbucket.core.api._
import gitbucket.core.service._
import gitbucket.core.util.JGitUtil.{getFileList}
import gitbucket.core.util._
import gitbucket.core.util.Implicits._
import gitbucket.core.controller.ControllerBase

/**
  * Created by Keiichi Watanabe
  */

class FessController extends FessControllerBase
  with RepositoryService
  with AccountService
  with ProtectedBranchService
  with IssuesService
  with LabelsService
  with PullRequestService
  with CommitStatusService
  with RepositoryCreationService
  with HandleCommentService
  with WebHookService
  with WebHookPullRequestService
  with WebHookIssueCommentService
  with WikiService
  with ActivityService
  with OwnerAuthenticator
  with UsersAuthenticator
  with GroupManagerAuthenticator
  with ReferrerAuthenticator
  with ReadableUsersAuthenticator
  with CollaboratorsAuthenticator

trait FessControllerBase extends ControllerBase {
  self: RepositoryService
    with AccountService
    with ProtectedBranchService
    with IssuesService
    with LabelsService
    with PullRequestService
    with CommitStatusService
    with RepositoryCreationService
    with HandleCommentService
    with OwnerAuthenticator
    with UsersAuthenticator
    with GroupManagerAuthenticator
    with ReferrerAuthenticator
    with ReadableUsersAuthenticator
    with CollaboratorsAuthenticator =>

  get("/api/v3/fess/repos")(usersOnly{
    contentType = "application/json"
    val num:Int = params.getOrElse("num", "20").toIntOpt.getOrElse(20)
    val offset:Int = params.getOrElse("offset", "0").toIntOpt.getOrElse(0)
    val allRepos = getVisibleRepositories(context.loginAccount)
    val repos = allRepos.slice(offset, offset + num).map{
      r => FessRepositoryInfo(
        ApiRepository(r, getAccountByUserName(r.owner).get),
        r.issueCount,
        getCollaborators(r.owner, r.name))
    }
    JsonFormat(FessResponse(allRepos.size, repos.size, offset, repos))
  })

  get("/api/v3/fess/repos/:owner/:repository/issues/:id")(referrersOnly { repository =>
    (for{
      issueId <- params("id").toIntOpt
      issue <- getIssue(repository.owner, repository.name, issueId.toString)
    } yield {
      JsonFormat(ApiIssue(issue, RepositoryName(repository), ApiUser(getAccountByUserName(repository.owner).get)))
    }) getOrElse NotFound
  })
}

case class FessRepositoryInfo(
  repository: ApiRepository,
  issue_count: Int,
  collaborators: List[String])

case class FessResponse(
  total_count: Int,
  response_count: Int,
  offset: Int,
  repositories: List[FessRepositoryInfo])
