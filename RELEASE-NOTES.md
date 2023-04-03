## 3.0.M2

* #113 Upgrade to Jetty 10.0.13 / 11.0.13
* #115 JettyTester - support for testing redirects
* #114 Upgrade of Jetty to 10.0.14 / 11.0.14
* #117 JettyTester's ResponseMatcher must allow access to any type of content

## 3.0.M1

* #108 Jetty 10 support
* #111 Jetty 11 (Jakarta) support

## 2.0.RC1

* #109 JettyTester should not cause eager initalization
* #110 Upgrade Jetty from 9.4.40.v20210413 to 9.4.43.v20210629

## 2.0.B1

* #103 Upgrade Jetty to 9.4.33.v20201020
* #104 Centralized MDC management service
* #105 WebSocket: API to use initiating request MDC
* #106 Upgrade Jetty to 9.4.36.v20210114
* #107 Upgrade Jetty to 9.4.40.v20210413

## 2.0.M1

* #95 Safer default value for ServerFactory.maxThreads
* #96 Support "classpath:" URLs for "resourceBase" of static servlets
* #97 "bootique-jetty-junit5" - a test module for Jetty 
* #98 "any" port - selecting port dynamically
* #99 SSL context factory class should be changed to comply with recent versions of Jetty
* #102 Upgrade to Jetty 9.4.31.v20200723

## 1.1

* #88 Turning off "Server" header
* #91 Upgrade Jetty to 9.4.19.v20190610

## 1.0

## 1.0.RC1

* #39 Add support for CORS configuration
* #74 Add a @BQConfigProperty for 'compactPath' in ContextHandler
* #75 Cleaning up APIs deprecated since <= 0.25
* #76 Upgrade Jetty to 9.4
* #77 Use percentage metric for thread pool utilization check
* #78 Metrics renaming to follow naming convention
* #79 Health check renaming to follow naming convention
* #80 JDK9 Compatibility
* #85 "bootique-jetty-websocket" - new module to support websocket apps
* #86 Support connector "idleTimeout" property in config 
* #87 Pool utilization threshold health check has out of range default "warn" and "critical" levels

## 0.25

* #64 jetty-instrumented: handle RequestTimer within the servlet spec
* #65 Log request execution time outside of jetty-instrumented
* #66 Support for listener ordering
* #67 Integrate "business transaction" ids in jetty-instrumented requests
* #68 Thread pool state health check
* #69 Slight change in health check format output 
* #70 Support YAML configuration of per-connector acceptor/selector threads
* #71 Change "utilization" metric definition, deprecate "utilization-max"
* #72 Non-blocking ServerCommand
* #73 Upgrade to bootique-modules-parent 0.8

## 0.24

* #61 Support for maxFormContentSize and maxFormKeys
* #62 Adds support for custom error pages handlers

## 0.21

* #59 Update JettyTestFactory to match new core test API

## 0.20

* #57 Module "extend" API - a better version of "contribute*"
* #58 Move bootique-metrics-web to bootique-jetty-metrics

## 0.19

* #55 HttpSessionListeners are not registered
* #56 Upgrade to Bootique 0.21: new app metadata package, annotated config help

## 0.18

* #40 Simplify MappedServlet/MappedFilter with parameterization
* #42 SSL connector
* #44 Removing deprecated API
* #45 Support for multiple connectors
* #47 Support binding connector to a specific interface
* #48 Upgrade to Bootique 0.20
* #49 Suppress Jetty INFO loggers by default
* #50 Use bootique-jetty-instrumented for tests run via JettyTestFactory
* #52 JettyTestFactory rework to align with new BQ test API

## 0.17

* #41 Upgrade to bootique 0.19 and metrics 0.7 to have access to healthchecks API
* #43 Move to io.bootique namespace

## 0.16

* #36 ServerCommand: Jetty daemon interruption should be reported as successful completion
* #37 Upgrade to BQ 0.18
* #38 Print full base URL on startup

## 0.15:

*  #9 Support for gzip compression
* #24 More flexible mapping of static folders.
* #25 ServerFactory - add getters
* #27 change 'staticResourceBase' type of FolderResourceFactory
* #28 InstrumentedRequestFilter : logging and metrics gathering filter 
* #29 dot-slash paths do not work with default Jetty servlet
* #30 Servlets/Filters: Support for 'urlPatterns' mapping in YAML
* #31 Support for servlet and filter parameters configured in annotations 
* #34 Configurable request and response header size

## 0.14:

* #16 Support for servlet spec annotations

## 0.13

* #11 Upgrade to Bootique 0.13
* #12 Support for init parameters for servlets and filters
* #14 Jetty test module
* #15 Support for sessions in Jetty
* #19 Support for context init parameters
* #20 Support for logging Jetty requests
* #21 Get rid of JettyBinder
  
## 0.12:

* #7 Support for contributing servlet listeners
* #8 Injectable object to provide access to ServletContext and ServletRequest

## 0.11:

* #2 Support for contributing filters
* #3 Support for multiple URL patterns for servlets
* #4 Support for request metrics
* #5 Upgrade Bootique to 0.12
* #6 Move contribution API from JettyBinder into static methods on JettyModule
 
## 0.10:

* #1 New API for contributing Servlets
