package org.codelibs.gitbucket.fess.service

import gitbucket.core.model.{Session => _, _}
import org.codelibs.gitbucket.fess.model.FessSetting
import org.codelibs.gitbucket.fess.model.Profile._
import profile.simple._

trait FessSettingService {

  def getFessSettingByUserName(userName: String)(implicit s: Session): Option[FessSetting] =
    FessSettings filter(t => (t.userName === userName.bind)) firstOption

  def createFessSetting(userName: String, fessUrl: String, fessToken: Option[String])(implicit s: Session): Unit =
    FessSettings insert FessSetting(
      userName    = userName,
      fessUrl     = fessUrl,
      fessToken   = fessToken,
      updatedDate = currentDate
    )

  def updateFessSetting(fessSetting: FessSetting)(implicit s: Session): Unit =
    FessSettings
      .filter { s => s.userName === fessSetting.userName.bind }
      .map    { s => (s.fessUrl, s.fessToken.?, s.updatedDate) }
      .update (
        fessSetting.fessUrl,
        fessSetting.fessToken,
        currentDate
      )
}

object FessSettingService extends FessSettingService