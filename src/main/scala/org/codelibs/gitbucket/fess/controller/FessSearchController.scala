package org.codelibs.gitbucket.fess.controller

import gitbucket.core.controller.ControllerBase
import gitbucket.core.service.{AccountService, RepositoryService}
import org.codelibs.gitbucket.fess.service.{FessSearchService, FessSettingService}
import org.codelibs.gitbucket.fess.html
import gitbucket.core.util.ControlUtil._
import gitbucket.core.util._
import gitbucket.core.util.Implicits._

import io.github.gitbucket.scalatra.forms._
import org.slf4j.LoggerFactory

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
  with FessSettingService

trait FessSearchControllerBase extends ControllerBase {
  self: RepositoryService
    with AccountService
    with OwnerAuthenticator
    with UsersAuthenticator
    with GroupManagerAuthenticator
    with ReferrerAuthenticator
    with ReadableUsersAuthenticator
    with CollaboratorsAuthenticator
    with FessSearchService
    with FessSettingService =>

  val logger =  LoggerFactory.getLogger(getClass)

  case class SettingForm(url:   String, token: Option[String])

  val settingForm = mapping(
    "url"    -> trim(label("url", text(maxlength(200)))),
    "token"  -> trim(label("token", optional(text(length(60)))))
  )(SettingForm.apply)

  get("/fess")(usersOnly {
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
        case _ => html.code(searchFiles(query, offset, Display_num), page)
      }
    }
  })

  get("/fess/settings")(usersOnly {
    val (url, token): (String, String) =
      context.loginAccount.flatMap(user => {
        getFessSettingByUserName(user.userName)
      }).map(fessSetting => {
        (fessSetting.fessUrl, fessSetting.fessToken.getOrElse(""))
      }).getOrElse(("", ""))
    html.settings(url, token)
  })

  post("/fess/settings", settingForm)(usersOnly { form =>
    val userName = context.loginAccount.get.userName
    getFessSettingByUserName(userName).map { setting =>
      updateFessSetting(setting.copy(fessUrl=form.url, fessToken=form.token))
      flash += "info" -> "Fess setting has been updated."
    } getOrElse({
      createFessSetting(userName, form.url, form.token)
      flash += "info" -> "Fess setting has been created."
    })
    redirect("/fess?q=")
  })
}

