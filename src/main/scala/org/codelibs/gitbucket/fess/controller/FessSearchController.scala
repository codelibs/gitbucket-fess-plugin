package org.codelibs.gitbucket.fess.controller

import gitbucket.core.controller.ControllerBase
import gitbucket.core.service.{AccountService, RepositoryService}
import gitbucket.core.util._
import gitbucket.core.util.ControlUtil._
import org.codelibs.gitbucket.fess.service.FessSearchService


class FessSearchController extends FessSearchControllerBase
  with RepositoryService
  with AccountService
  with OwnerAuthenticator
  with UsersAuthenticator
  with GroupManagerAuthenticator
  with ReferrerAuthenticator
  with ReadableUsersAuthenticator
  with CollaboratorsAuthenticator
  with FessSearchService

class FessSearchControllerBase extends ControllerBase {
  self: RepositoryService
    with AccountService
    with OwnerAuthenticator
    with UsersAuthenticator
    with GroupManagerAuthenticator
    with ReferrerAuthenticator
    with ReadableUsersAuthenticator
    with CollaboratorsAuthenticator
    with FessSearchService =>

  get("/fess")(usersOnly{
    defining(params("q").trim, params.getOrElse("type", "code")){ case (query, target) =>
      val Display_num = 10 // number of documents per a page
      val page   = try {
        val i = params.getOrElse("page", "1").toInt
        if(i <= 0) 1 else i
      } catch {
        case e: NumberFormatException => 1
      }
      val offset = (page - 1) * Display_num

      target.toLowerCase match {
        // case "issue" | "wiki" => // TODO
        case _ => org.codelibs.gitbucket.fess.html.code(searchFiles(query, offset, Display_num), page)
      }
    }
  })
}
