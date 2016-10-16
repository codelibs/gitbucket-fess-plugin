package org.codelibs.gitbucket.fess.controller

import gitbucket.core.api._
import gitbucket.core.controller._
import gitbucket.core.service._
import gitbucket.core.util.JGitUtil.{getFileList}
import gitbucket.core.util._
import gitbucket.core.util.Implicits._
import gitbucket.core.controller.ControllerBase
import org.codelibs.gitbucket.fess.html
import play.twirl.api.Html

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
        r.name, r.owner, r.repository.isPrivate,
        r.issueCount, getCollaborators(r.owner, r.name))
    }
    JsonFormat(FessResponse(allRepos.size, repos.size, offset, repos))
  })

  get("/fess")(usersOnly{
    html.fess("Fess")
  })
}

case class FessRepositoryInfo(name: String,
                              owner: String,
                              is_private: Boolean,
                              issue_count: Int,
                              collaborators: List[String])

case class FessResponse(total_count: Int,
                        response_count: Int,
                        offset: Int,
                        repositories: List[FessRepositoryInfo])
