package org.codelibs.gitbucket.fess.controller

import gitbucket.core.controller.ControllerBase
import gitbucket.core.service._
import org.codelibs.gitbucket.fess.service._
import org.codelibs.gitbucket.fess.html
import gitbucket.core.util._
import gitbucket.core.util.Implicits._

import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

class FessSearchController
    extends FessSearchControllerBase
    with FessSearchService
    with ActivityService
    with IssuesService
    with WikiService
    with RepositoryService
    with AccountService
    with OwnerAuthenticator
    with UsersAuthenticator
    with GroupManagerAuthenticator
    with ReferrerAuthenticator
    with ReadableUsersAuthenticator
    with WritableUsersAuthenticator
    with FessSettingsService

trait FessSearchControllerBase extends ControllerBase {
  self: FessSearchService
    with ActivityService
    with IssuesService
    with WikiService
    with RepositoryService
    with AccountService
    with OwnerAuthenticator
    with UsersAuthenticator
    with GroupManagerAuthenticator
    with ReferrerAuthenticator
    with ReadableUsersAuthenticator
    with WritableUsersAuthenticator
    with FessSettingsService =>

  val Display_num = 10 // number of documents per a page

  get("/fess") {
    val userName = context.loginAccount.map(_.userName)
    val isAdmin  = context.loginAccount.exists(_.isAdmin)
    val settings = loadFessSettings()
    val query  = params.getOrElse("q", "")
    val target = params.getOrElse("type", "code").toLowerCase
    if (!settings.fessUrl.isEmpty) { // Setup completed
      val page = try {
        val i = params.getOrElse("page", "1").toInt
        if (i <= 0) 1 else i
      } catch {
        case _: NumberFormatException => 1
      }
      val offset = (page - 1) * Display_num

      target match {
        case "issues" =>
          searchIssueOnFess(userName, query, settings, offset, Display_num) match {
            case Right((r, issues)) =>
              html.issues(r.query,
                          r.offset,
                          r.hit_count,
                          issues,
                          page,
                          isAdmin)
            case Left(message) => html.error(target, query, message, isAdmin, true)
          }
        case "wiki" =>
          searchWikiOnFess(userName, query, settings, offset, Display_num) match {
            case Right((r, contents)) =>
              html.wiki(r.query,
                        r.offset,
                        r.hit_count,
                        contents,
                        page,
                        isAdmin)
            case Left(message) => html.error(target, query, message, isAdmin, true)
          }
        case _ => // "code"
          searchCodeOnFess(userName, query, settings, offset, Display_num) match {
            case Right((r, codes)) =>
              html.code(r.query, r.offset, r.hit_count, codes, page, isAdmin)
            case Left(message) => html.error(target, query, message, isAdmin, true)
          }
      }

    } else { // Setup is incomplete
      if (isAdmin) {
        redirect("/fess/settings")
      } else {
        html.error(target, query, "", isAdmin, false)
      }
    }
  }
}
