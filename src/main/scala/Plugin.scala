import gitbucket.core.controller.Context
import gitbucket.core.plugin.Link
import io.github.gitbucket.solidbase.model.Version
import org.codelibs.gitbucket.fess.controller.{
  FessApiController,
  FessSearchController,
  FessSettingsController
}

class Plugin extends gitbucket.core.plugin.Plugin {
  override val pluginId: String    = "fess"
  override val pluginName: String  = "Fess Plugin"
  override val description: String = "Search GitBucket by Fess."
  override val versions: List[Version] =
    List(new Version("1.0.0"),
         new Version("1.1.0"),
         new Version("1.1.1"),
         new Version("1.2.0"),
         new Version("1.3.0"),
         new Version("1.4.0"),
         new Version("1.4.1"),
         new Version("1.5.0"),
         new Version("1.6.0"),
         new Version("1.7.0"),
    )
  override val controllers = Seq(
    // Note: "/api/v3/" is a special prefix in ControllerBase.scala and Implicits.scala
    "/*" -> new FessApiController(),
    "/*" -> new FessSettingsController(),
    "/*" -> new FessSearchController()
  )

  override val globalMenus = Seq(
    (_: Context) => Some(Link("fess", "Global Search", "fess?q="))
  )

  override val assetsMappings = Seq("/fess/assets" -> "/fess/assets")
}
