package org.codelibs.gitbucket.fess.controller

import gitbucket.core.controller.ControllerBase
import gitbucket.core.service.{AccountService, RepositoryService}
import org.codelibs.gitbucket.fess.service.{
  FessSearchService,
  FessSettingsService
}
import org.codelibs.gitbucket.fess.html
import gitbucket.core.util._

class FessSearchController
    extends FessSearchControllerBase
    with RepositoryService
    with AccountService
    with OwnerAuthenticator
    with UsersAuthenticator
    with GroupManagerAuthenticator
    with ReferrerAuthenticator
    with ReadableUsersAuthenticator
    with WritableUsersAuthenticator
    with FessSearchService
    with FessSettingsService

trait FessSearchControllerBase extends ControllerBase {
  self: RepositoryService
    with AccountService
    with OwnerAuthenticator
    with UsersAuthenticator
    with GroupManagerAuthenticator
    with ReferrerAuthenticator
    with ReadableUsersAuthenticator
    with WritableUsersAuthenticator
    with FessSearchService
    with FessSettingsService =>

  val Display_num = 10 // number of documents per a page

  get("/fess")(usersOnly {
    val userName = context.loginAccount.get.userName
    val isAdmin = context.loginAccount.get.isAdmin
    val settings = loadFessSettings()
    if (!settings.fessUrl.isEmpty) {
      // Settings is done
      val query = params.getOrElse("q", "")
      val target = params.getOrElse("type", "code")
      val page = try {
        val i = params.getOrElse("page", "1").toInt
        if (i <= 0) 1 else i
      } catch {
        case e: NumberFormatException => 1
      }
      val offset = (page - 1) * Display_num
      target.toLowerCase match {
        // case "issue" | "wiki" => // TODO
        case _ => {
          searchFiles(userName, query, settings, offset, Display_num) match {
            case Right(result) => html.code(result, page, isAdmin)
            case Left(message) => html.error(query, message, isAdmin)
          }
        }
      }
    } else {
      // Settings are not finished yet
      if (isAdmin) {
        redirect("/fess/settings")
      } else {
        html.error(
          "",
          "Settings for Fess are not finished yet. Please contact the administrator.",
          isAdmin)
      }
    }
  })
}
