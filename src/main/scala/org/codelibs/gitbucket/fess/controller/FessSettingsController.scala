package org.codelibs.gitbucket.fess.controller

import gitbucket.core.controller.ControllerBase
import gitbucket.core.util.AdminAuthenticator
import org.codelibs.gitbucket.fess.html
import io.github.gitbucket.scalatra.forms._
import org.codelibs.gitbucket.fess.service.FessSettingsService
import org.codelibs.gitbucket.fess.service.FessSettingsService._

class FessSettingsController
    extends FessSettingsControllerBase
    with FessSettingsService
    with AdminAuthenticator

trait FessSettingsControllerBase extends ControllerBase {
  self: FessSettingsService with AdminAuthenticator =>

  val settingsForm: MappingValueType[FessSettings] = mapping(
    "fessUrl"   -> text(required, maxlength(200)),
    "fessToken" -> optional(text(length(60)))
  )(FessSettings.apply)

  get("/fess/settings")(adminOnly {
    val settings = loadFessSettings()
    html.settings(settings.fessUrl, settings.fessToken.getOrElse(""), isAdmin = true)
  })

  post("/fess/settings", settingsForm)(adminOnly { form =>
    assert(form.fessUrl != null)
    assert(!form.fessUrl.isEmpty)
    saveFessSettings(form)
    redirect("/fess?q=")
  })

}
