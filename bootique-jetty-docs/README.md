<!--
     Licensed to the ObjectStyle LLC under one
   or more contributor license agreements.  See the NOTICE file
   distributed with this work for additional information
   regarding copyright ownership.  The ObjectStyle LLC licenses
   this file to you under the Apache License, Version 2.0 (the
   “License”); you may not use this file except in compliance
   with the License.  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing,
   software distributed under the License is distributed on an
   “AS IS” BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
   KIND, either express or implied.  See the License for the
   specific language governing permissions and limitations
   under the License.
  -->

This module contains source of the Bootique documentation published on the website. The docs are in Docbook XML format.

## Building the Docs

```shell
cd <bootique_jetty_checkout>/bootique-jetty-docs
mvn clean package
```

You can now inspect the local docs under ```target/site/index/```. If you are not a Bootique maintainer, you may stop here. 

## Publishing Prerequisites

Follow instructions for the [Bootique Core](https://github.com/bootique/bootique/blob/master/bootique-docs/README.md).

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