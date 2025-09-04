
Frontend of the Annual Tax Summary application
======================================================================

This service provides the frontend endpoint for the [Annual Tax Summary - Individual](https://github.com/hmrc/tax-summaries) and [Annual Tax Summary - Agent](https://github.com/hmrc/tax-summaries-agent) projects.

Summary
----------------

This service is designed for users and agents to view their personal tax and how they're spent.
 

Requirements
---------------

This service is written in [Scala] and [Play], so needs the latest [JRE] to run.


Authentication
------------

This user logs into this service using [Government Gateway]

Testing
------------
Please run Unit tests by running `sbt test` and `sbt it:test`

Uprating
------------
There are separate tax years for SA and PAYE. This is so that the yearly uprating can be done separately for SA and PAYE. The tax year can be increased for either without breaking anything in the other's tests. The tax years are stored in application.conf: taxYearSA and taxYearPAYE. The currentTaxYearSA and currentTaxYearPAYE items in the TaxYearForTesting class should be updated too at the same time: they should always be the same as the items in application.conf.   



### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").


[Scala]: http://www.scala-lang.org/
[Play]: http://playframework.com/
[JRE]: http://www.oracle.com/technetwork/java/javase/overview/index.html
[Government Gateway]: http://www.gateway.gov.uk/
    
