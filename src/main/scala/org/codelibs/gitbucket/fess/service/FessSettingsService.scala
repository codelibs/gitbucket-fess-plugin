package org.codelibs.gitbucket.fess.service

import scala.util.Using
import java.io.File

import gitbucket.core.util.Directory._
import gitbucket.core.util.SyntaxSugars._
import org.codelibs.gitbucket.fess.service.FessSettingsService._

trait FessSettingsService {

  val FessConf = new File(GitBucketHome, "fess.conf")

  def saveFessSettings(settings: FessSettings): Unit =
    defining(new java.util.Properties()) { props =>
      props.setProperty(FessURL, settings.fessUrl)
      settings.fessToken.foreach(x => props.setProperty(FessToken, x))
      Using.resource(new java.io.FileOutputStream(FessConf)) { out =>
        props.store(out, null)
      }
    }

  def loadFessSettings(): FessSettings =
    defining(new java.util.Properties()) { props =>
      if (FessConf.exists) {
        Using.resource(new java.io.FileInputStream(FessConf)) { in =>
          props.load(in)
        }
      }
      FessSettings(
        getValue[String](props, FessURL, ""),
        getOptionValue(props, FessToken, None)
      )
    }
}

object FessSettingsService {
  import scala.reflect.ClassTag

  case class FessSettings(fessUrl: String, fessToken: Option[String])

  private val FessURL   = "fess_url"
  private val FessToken = "fess_token"

  private def getValue[A: ClassTag](props: java.util.Properties,
                                    key: String,
                                    default: A): A =
    defining(props.getProperty(key)) { value =>
      if (value == null || value.isEmpty) default
      else convertType(value).asInstanceOf[A]
    }

  private def getOptionValue[A: ClassTag](props: java.util.Properties,
                                          key: String,
                                          default: Option[A]): Option[A] =
    defining(props.getProperty(key)) { value =>
      if (value == null || value.isEmpty) default
      else Some(convertType(value)).asInstanceOf[Option[A]]
    }

  private def convertType[A: ClassTag](value: String) =
    defining(implicitly[ClassTag[A]].runtimeClass) { c =>
      if (c == classOf[Boolean]) value.toBoolean
      else if (c == classOf[Int]) value.toInt
      else value
    }
}
