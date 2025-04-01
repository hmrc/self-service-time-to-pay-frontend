import sbt.{MavenRepository, Resolver}

resolvers += MavenRepository("HMRC-open-artefacts-maven2", "https://open.artefacts.tax.service.gov.uk/maven2")
resolvers += Resolver.url("HMRC-open-artefacts-ivy", url("https://open.artefacts.tax.service.gov.uk/ivy2"))(Resolver.ivyStylePatterns)


addSbtPlugin("uk.gov.hmrc"        % "sbt-auto-build"        % "3.24.0")
addSbtPlugin("uk.gov.hmrc"        % "sbt-distributables"    % "2.6.0")
addSbtPlugin("org.playframework"  % "sbt-plugin"            % "3.0.6" )
addSbtPlugin("io.github.irundaia" % "sbt-sassify"           % "1.5.2")
addSbtPlugin("org.scoverage"      % "sbt-scoverage"         % "2.0.11")
addSbtPlugin("org.scalastyle"    %% "scalastyle-sbt-plugin" % "1.0.0" exclude("org.scala-lang.modules", "scala-xml_2.12"))
addSbtPlugin("org.scalariform"    % "sbt-scalariform"       % "1.8.3" exclude("org.scala-lang.modules", "scala-xml_2.12"))
addSbtPlugin("org.wartremover"    % "sbt-wartremover"       % "3.2.7")
addSbtPlugin("com.timushev.sbt"   % "sbt-updates"           % "0.6.4")

addDependencyTreePlugin