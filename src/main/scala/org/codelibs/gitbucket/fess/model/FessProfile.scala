package org.codelibs.gitbucket.fess.model

import gitbucket.core.util.DatabaseConfig

trait FessProfile {
  val profile: slick.driver.JdbcProfile
  import profile.simple._

  /**
    * java.util.Date Mapped Column Types
    */
  implicit val dateColumnType = MappedColumnType.base[java.util.Date, java.sql.Timestamp](
    d => new java.sql.Timestamp(d.getTime),
    t => new java.util.Date(t.getTime)
  )

  /**
    * Extends Column to add conditional condition
    */
  implicit class RichColumn(c1: Column[Boolean]){
    def &&(c2: => Column[Boolean], guard: => Boolean): Column[Boolean] = if(guard) c1 && c2 else c1
  }

  /**
    * Returns system date.
    */
  def currentDate = new java.util.Date()
}

trait FessProfileProvider { self: FessProfile =>

  lazy val profile = DatabaseConfig.slickDriver

}

trait CoreFessProfile extends FessProfileProvider with FessProfile
  with FessSettingComponent

object FessProfile extends CoreFessProfile
