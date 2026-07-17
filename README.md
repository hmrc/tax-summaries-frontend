
Annual Tax Summary - Frontend Microservice
====================================================================

Annual Tax Summary is an online service that allows individuals and agents to view the annual summary of an individual's personal tax and National Insurance contributions (NICs) and how they've been spent.

Running the service using service manager
------------
sm2 --start TAXS

Running the app locally
------------
sbt "run -Dapplication.router=testOnlyDoNotUseInAppConf.Routes"


Testing
------------
Please run Unit tests by running `sbt test` and `sbt it:test`



### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").


[Scala]: http://www.scala-lang.org/
[Play]: http://playframework.com/
[JRE]: http://www.oracle.com/technetwork/java/javase/overview/index.html
[Government Gateway]: http://www.gateway.gov.uk/
    
