## 0.16

* #36 ServerCommand: Jetty daemon interruption should be reported as successful completion
* #37 Upgrade to BQ 0.18

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
