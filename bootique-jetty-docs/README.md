This module contains source of the Bootique documentation published on the website. The docs are in Docbook XML format.

## Building the Docs

```shell
cd <bootique_jetty_checkout>/bootique-jetty-docs
mvn clean package
```

You can now inspect the local docs under ```target/site/index/```. If you are not a Bootique maintainer, you may stop here. 

## Publishing Prerequisites

Follow instructions for the [Bootique Core](https://github.com/nhl/bootique/blob/master/bootique-docs/README.md).

## Publishing the Docs

Build the docs locally as described above, and then do this:

```shell

cd <bootique_jetty_checkout>/bootique-jetty-docs
cp -r target/site/ ../../bootique-pages/docs/0/
cd ../../bootique-pages/docs/ 
git add -A
git commit -m "jetty docs update"
git push
```

In a few seconds you will be able to check the result at http://bootique.io/docs/ .