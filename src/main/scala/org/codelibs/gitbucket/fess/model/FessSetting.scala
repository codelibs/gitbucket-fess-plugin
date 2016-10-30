package org.codelibs.gitbucket.fess.model

trait FessSettingComponent { self: FessProfile =>
  import profile.simple._
  import self._

  lazy val FessSettings = TableQuery[FessSettings]

  class FessSettings(tag: Tag) extends Table[FessSetting](tag, "FESSSETTING") {
    val userName = column[String]("USER_NAME", O PrimaryKey)
    val fessUrl = column[String]("FESS_URL")
    val fessToken = column[String]("FESS_TOKEN")
    val updatedDate = column[java.util.Date]("UPDATED_DATE")
    def * = (userName, fessUrl, fessToken.?, updatedDate) <> (FessSetting.tupled, FessSetting.unapply)
  }

}

case class FessSetting(userName: String,
                       fessUrl: String,
                       fessToken: Option[String],
                       updatedDate: java.util.Date)