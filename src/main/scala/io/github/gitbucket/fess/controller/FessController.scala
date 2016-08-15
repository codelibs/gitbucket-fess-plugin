package io.github.gitbucket.fess.controller

import gitbucket.core.api.{ApiRepository, JsonFormat}
import gitbucket.core.service._
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

  get("/fess/repos")(usersOnly{
    val num:Int = params.getOrElse("num", "20").toIntOpt.getOrElse(20)
    val offset:Int = params.getOrElse("offset", "0").toIntOpt.getOrElse(0)
    val allRepos = getVisibleRepositories(context.loginAccount)
    val repos = allRepos.drop(offset).take(num).map{
      r => ApiRepository(r, getAccountByUserName(r.owner).get)
    }
    val response:Map[String, Any] = Map(
      "total_count" -> allRepos.size,
      "response_count" -> repos.size,
      "offset" -> offset,
      "repositories" -> repos
    )
    JsonFormat(response)
  })
}
