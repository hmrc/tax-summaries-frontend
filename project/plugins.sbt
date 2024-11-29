resolvers += "HMRC-open-artefacts-maven" at "https://open.artefacts.tax.service.gov.uk/maven2"
resolvers += Resolver.url("HMRC-open-artefacts-ivy", url("https://open.artefacts.tax.service.gov.uk/ivy2"))(Resolver.ivyStylePatterns)

addSbtPlugin("uk.gov.hmrc"         % "sbt-auto-build"           % "3.24.0")
addSbtPlugin("uk.gov.hmrc"         % "sbt-distributables"       % "2.5.0")
addSbtPlugin("org.playframework"   % "sbt-plugin"               % "3.0.5")
addSbtPlugin("org.scoverage"       % "sbt-scoverage"            % "2.0.9")
addSbtPlugin("org.scalameta"       % "sbt-scalafmt"             % "2.5.2")
addSbtPlugin("com.typesafe.sbt"    % "sbt-digest"               % "1.1.4")
addSbtPlugin("uk.gov.hmrc"         % "sbt-accessibility-linter" % "0.39.0")
