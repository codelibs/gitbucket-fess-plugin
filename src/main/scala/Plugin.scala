import gitbucket.core.controller.Context
import gitbucket.core.plugin.Link
import org.codelibs.gitbucket.fess.controller.{FessApiApiController, FessSearchController}
import io.github.gitbucket.solidbase.model.Version

class Plugin extends gitbucket.core.plugin.Plugin {
  override val pluginId: String = "fess"
  override val pluginName: String = "Fess Plugin"
  override val description: String = "Search GitBucket by Fess."
  override val versions: List[Version] = List(new Version("1.0.0"))

  override val globalMenus = Seq(
    (context: Context) => Some(Link("search", "Search", "fess?q="))
  )

  override val controllers = Seq(
    // FIXME: path should not start with "/api/v3/"
    // "/api/v3/*" is treated as a special path in ControllerBase
    "/api/v3/fess/*" -> new FessApiApiController(),
    "/fess/*" -> new FessSearchController()
  )
}
