
package testsupport

import play.api.Application
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}

import java.time.LocalDate

class ConfigSpec extends ItSpec {
  def appWithConfig(configOverride: Map[String, Any]): Application =
    new GuiceApplicationBuilder()
      .overrides(GuiceableModule.fromGuiceModules(Seq(module)))
      .configure(configMap ++ configOverride)
      .build()

  def date(date: String): LocalDate = LocalDate.parse(date)
}
