#!/bin/bash

VERSION="2.4.2"
NAME=org.apache.karaf.management.server
cat >${NAME}.bnd  <<STARTBND
Bundle-Blueprint                         OSGI-INF/blueprint/karaf-management.xml
Bundle-Name                              Apache Karaf :: Management :: Server
Bundle-SymbolicName                      org.apache.karaf.management.server
Bundle-Vendor                            The Apache Software Foundation
Bundle-Version                           2.4.2
Export-Package                           org.apache.karaf.management;version="2.4.2";uses:="javax.management,javax.management.openmbean,javax.management.remote,javax.security.auth,org.apache.karaf.jaas.config,org.osgi.service.cm"
Export-Service                           javax.management.MBeanServer
Import-Package                           javax.management,javax.management.openmbean,javax.management.remote,javax.net,javax.net.ssl,javax.rmi.ssl,javax.security.auth,javax.security.auth.callback,javax.security.auth.login,org.apache.karaf.jaas.boot.principal;version="[2.4,3)",org.apache.karaf.jaas.boot;version="[2.4,3)",org.apache.karaf.jaas.config;version="[2.4,3)",org.apache.karaf.management.boot;version="[2.4,3)",org.osgi.framework;version="[1.7,2)",org.osgi.service.blueprint;version="[1.0.0,2.0.0)",org.osgi.service.cm;version="[1.5,2)",org.slf4j;version="[1.7,2)"
Import-Service                           org.osgi.service.cm.ConfigurationAdmin;multiple:=false,org.apache.aries.blueprint.NamespaceHandler;filter=(osgi.service.blueprint.namespace=http://aries.apache.org/blueprint/xmlns/blueprint-ext/v1.0.0),org.apache.karaf.jaas.config.KeystoreManager;multiple:=false,javax.management.MBeanServer;multiple:=false;availability:=optional,org.apache.aries.blueprint.NamespaceHandler;filter=(osgi.service.blueprint.namespace=http://aries.apache.org/blueprint/xmlns/blueprint-cm/v1.1.0),org.apache.karaf.jaas.config.KeystoreInstance;multiple:=true;availability:=optional
Require-Capability                       osgi.ee;filter:="(&(osgi.ee=JavaSE)(version=1.5))"
STARTBND

bnd wrap -o ${NAME}-${VERSION}.bar -p ${NAME}.bnd ${NAME}-${VERSION}.jar


rm -f ${NAME}.bnd
