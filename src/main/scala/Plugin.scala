import gitbucket.core.controller.Context
import gitbucket.core.plugin.Link
import io.github.gitbucket.solidbase.migration.LiquibaseMigration
import org.codelibs.gitbucket.fess.controller.{FessApiApiController, FessSearchController}
import io.github.gitbucket.solidbase.model.Version

class Plugin extends gitbucket.core.plugin.Plugin {
  override val pluginId: String = "fess"
  override val pluginName: String = "Fess Plugin"
  override val description: String = "Search GitBucket by Fess."
  override val versions: List[Version] = List(
    new Version("1.0.0",
      new LiquibaseMigration("update/gitbucket-fess_1.0.xml")
    )
  )

  override val controllers = Seq(
    // FIXME: path should not start with "/api/v3/"
    // "/api/v3/*" is treated as a special path in ControllerBase
    "/api/v3/fess/*" -> new FessApiApiController(),
    "/*" -> new FessSearchController()
  )

  override val globalMenus = Seq(
    (context: Context) => Some(Link("search", "Search", "fess?q="))
  )

  override val assetsMappings = Seq("/fess/assets" -> "/fess/assets")
}
