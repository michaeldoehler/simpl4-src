#!/bin/sh

SRCTOPDIR=
DESTTOPDIR=
#########################################################
# usage
#########################################################
usage() {
    echo "usage: $0 -s srcdir -d destdir"
}

#########################################################
# parameter
#########################################################
shortoptions='s:d:'
longoptions='srcdir:destdir:'
getopt=$(getopt -o $shortoptions --longoptions  $longoptions -- "$@")
if [ $? != 0 ]; then
   usage
   exit 1;
fi


eval set -- "$getopt"
while true; do
   case "$1" in
      -d|--destdir)
         shift
         DESTTOPDIR=$1
         shift
      ;;
      -s|--srcdir)
         shift
         SRCTOPDIR=$1
         shift
      ;;
      *)
				break
      ;;
   esac
done

if [ -z "$SRCTOPDIR" ] ; then
	SRCTOPDIR=$TOPDIR/simpl4-src
fi
if [ -z "$DESTTOPDIR" ] ; then
	DESTTOPDIR=$TOPDIR/simpl4-deployed
fi
SERVERDIR="$DESTTOPDIR/server"
export REPOSITORY="$SRCTOPDIR/repository"


#########################################################
# branch
#########################################################
cd $SRCTOPDIR
CURRENTBRANCH=`git rev-parse --abbrev-ref HEAD`

cd $DESTTOPDIR
git checkout $CURRENTBRANCH >/dev/null 2>&1

export BUNDLESBUILD="$SRCTOPDIR/build/${CURRENTBRANCH}/bundlesBuild"

#########################################################
# info
#########################################################
echo "================="
echo "Create osgi-env ->"
echo "================="
echo -e "\t:SRCTOPDIR=$SRCTOPDIR"
echo -e "\t:DESTTOPDIR=$DESTTOPDIR"
echo -e "\t:REPOSITORY=$REPOSITORY"
echo -e "\t:CURRENTBRANCH=$CURRENTBRANCH"
echo -e "\t:BUNDLESBUILD=$BUNDLESBUILD"
#########################################################
# main
#########################################################
sed  "s!__REPOSITORY__!$REPOSITORY!" $SRCTOPDIR/etc/felix.tpl.xml >$SRCTOPDIR/etc/felix.xml
rm -fr $SERVERDIR
vmOptions="\
 -Xmx1500m \
 -XX:MaxPermSize=256m \
 -Djava.security.egd=file:/dev/./urandomx -Dworkspace=\$SIMPL4DIR/workspace \
 -Dorg.osgi.service.http.port=\$CONTROL_PORT \
 -DdisableCheckForReferencesInContentException=true \
 -Dgit.repos=\$SIMPL4DIR/gitrepos \
 -Dgroovy.target.indy=false \
 -Dsimpl4.dir=\$SIMPL4DIR \
 -Djetty.port=\$JETTY_PORT \
 -Dkaraf.startLocalConsole=true \
 -Dkaraf.systemBundlesStartLevel=0 \
 -Dkaraf.startRemoteShell=false \
 -Dfelix.cm.dir=\$SIMPL4DIR/etc/config \
 -Dfelix.fileinstall.dir=\$SIMPL4DIR/gitrepos/.bundles \
 -Dorg.ops4j.pax.logging.DefaultServiceLog.level=ERROR \
 -Ddrools.dialect.java.compiler=JANINO \
 -Dkaraf.local.roles=admin,manager \
 -Djava.naming.factory.initial=org.ms123.common.system.jndi.InitialContextFactory \
 -Dkaraf.shell.init.script=\$SIMPL4DIR/etc/shell.init.script \
"

activitibundles=""
for i in $REPOSITORY/activiti-5.16/*ar
do
	activitibundles=${activitibundles}"scan-bundle:file:$i "
done

nucleusbundles=""
for i in $REPOSITORY/datanucleus-3.3.6/*ar
do
	nucleusbundles=${nucleusbundles}"scan-bundle:file:$i "
done

camelbundles=""
for i in $REPOSITORY/camel-2.14-ms/*jar
do
	camelbundles=${camelbundles}"scan-bundle:file:$i "
done

$SRCTOPDIR/bin/pax-run.sh \
	scan-bundle:file:$REPOSITORY/org.apache.felix.configadmin-1.8.0.jar \
	scan-bundle:file:$REPOSITORY/org.osgi.compendium-4.3.1.jar \
	scan-bundle:file:$REPOSITORY/org.apache.felix.prefs-1.0.4.jar \
	scan-bundle:file:$REPOSITORY/org.apache.aries.blueprint.core-1.4.2.jar \
	scan-bundle:file:$REPOSITORY/org.apache.aries.jmx.api-1.0.0.jar \
	scan-bundle:file:$REPOSITORY/org.apache.aries.jmx-1.0.0.jar \
	scan-bundle:file:$REPOSITORY/org.apache.aries.util-1.0.0.jar \
	scan-bundle:file:$REPOSITORY/org.apache.aries.proxy-1.0.0.jar \
	scan-bundle:file:$REPOSITORY/org.apache.commons.collections_3.2.1.jar  \
	scan-bundle:file:$REPOSITORY/org.apache.commons.fileupload_1.2.1.jar \
	scan-bundle:file:$REPOSITORY/commons-io-2.4.jar \
	scan-bundle:file:$REPOSITORY/org.apache.commons.lang_2.5.jar \
	scan-bundle:file:$REPOSITORY/derby-10.5.3.0_1.jar \
	scan-bundle:file:$REPOSITORY/groovy-all-2.2.2-indy.jar \
	scan-bundle:file:$REPOSITORY/mail-1.4.7.bar \
	scan-bundle:file:$REPOSITORY/org.apache.felix.bundlerepository-1.6.4.jar \
	scan-bundle:file:$REPOSITORY/org.apache.felix.eventadmin-1.2.2.jar \
	scan-bundle:file:$REPOSITORY/org.apache.felix.http.bridge-2.0.4.jar \
	scan-bundle:file:$REPOSITORY/org.apache.felix.http.whiteboard-2.0.4.jar \
	scan-bundle:file:$REPOSITORY/org.apache.felix.metatype-1.0.4.jar \
	scan-bundle:file:$REPOSITORY/org.apache.felix.scr-1.6.0.jar \
	scan-bundle:file:$REPOSITORY/org.apache.felix.fileinstall-3.4.2.jar \
	scan-bundle:file:$REPOSITORY/org.apache.felix.webconsole-3.1.2.jar \
	scan-bundle:file:$REPOSITORY/org.apache.felix.webconsole.plugins.event-1.0.2.jar \
	scan-bundle:file:$REPOSITORY/org.apache.felix.webconsole.plugins.memoryusage-1.0.2.jar \
${activitibundles} \
${nucleusbundles} \
${camelbundles} \
${localbundles} \
	scan-bundle:file:$REPOSITORY/pax-web-jetty-bundle-3.0.7.jar@3 \
	scan-bundle:file:$REPOSITORY/pax-web-spi-1.0.4.jar@3 \
	scan-bundle:file:$REPOSITORY/pax-web-extender-war-1.0.4.jar@3 \
  scan-bundle:file:$REPOSITORY/ant-1.8.2.bar \
	scan-bundle:file:$REPOSITORY/antlr.bar \
  scan-bundle:file:$REPOSITORY/antlr-runtime-3.3.bar \
	scan-bundle:file:$REPOSITORY/asm-all-3.3.jar \
	scan-bundle:file:$REPOSITORY/asm-all-4.1.jar \
  scan-bundle:file:$REPOSITORY/aspectjrt-1.7.1.bar \
	scan-bundle:file:$REPOSITORY/biz.aQute.bndlib.jar \
  scan-bundle:file:$REPOSITORY/codemodel-2.4.bar \
	scan-bundle:file:$REPOSITORY/commons-beanutils-1.8.4-SNAPSHOT.jar \
  scan-bundle:file:$REPOSITORY/commons-codec-1.9.jar \
	scan-bundle:file:$REPOSITORY/commons-digester-1.8.bar \
	scan-bundle:file:$REPOSITORY/commons-email-1.3.1.jar \
  scan-bundle:file:$REPOSITORY/commons-exec-1.1.jar \
  scan-bundle:file:$REPOSITORY/com.springsource.org.jaxen-1.1.1.jar \
  scan-bundle:file:$REPOSITORY/connector-api-1.5.bar \
	scan-bundle:file:$REPOSITORY/DynamicJasper-3.1.8.bar \
	scan-bundle:file:$REPOSITORY/ezmorph-1.0.6.jar \
  scan-bundle:file:$REPOSITORY/flexjson-2.1.bar \
  scan-bundle:file:$REPOSITORY/guava-10.0-rc3.bar \
	scan-bundle:file:$REPOSITORY/h2-1.4.182.jar \
  scan-bundle:file:$REPOSITORY/hibernate-5.0.1.jar \
	scan-bundle:file:$REPOSITORY/hsqldb.jar \
	scan-bundle:file:$REPOSITORY/itext-2.1.7.bar \
  scan-bundle:file:$REPOSITORY/janino.bar \
	scan-bundle:file:$REPOSITORY/jasperreports-4.0.1.jar \
  scan-bundle:file:$REPOSITORY/jaxb-xjc-2.1.9-osgi.jar \
  scan-bundle:file:$REPOSITORY/jdom-1.1.bar \
	scan-bundle:file:$REPOSITORY/jettison-1.2.jar \
  scan-bundle:file:$REPOSITORY/jms-api-1.1-rev-1.bar \
  scan-bundle:file:$REPOSITORY/joda-time-2.3.jar \
  scan-bundle:file:$REPOSITORY/jotm-core-2.2.3.jar \
	scan-bundle:file:$REPOSITORY/jsch-0.1.44-1.bar \
	scan-bundle:file:$REPOSITORY/json-lib-2.3-jdk15.jar \
  scan-bundle:file:$REPOSITORY/lucene-core-3.6.2.bar \
	scan-bundle:file:$REPOSITORY/milton-trunk_14-02-12.bar \
  scan-bundle:file:$REPOSITORY/milyn-smooks-all-1.5.jar \
	scan-bundle:file:$REPOSITORY/mina-core-2.0.2.jar \
  scan-bundle:file:$REPOSITORY/mvel2-2.1.RC1.jar \
	scan-bundle:file:$REPOSITORY/mysql-connector-java-5.1.6.bar \
	scan-bundle:file:$REPOSITORY/object-traverser.bar \
  scan-bundle:file:$REPOSITORY/ops4j-base-lang-1.2.2.jar \
	scan-bundle:file:$REPOSITORY/org.apache.felix.shell-1.4.3.jar \
	scan-bundle:file:$REPOSITORY/org.apache.karaf.jaas.boot-2.4.0.jar \
	scan-bundle:file:$REPOSITORY/org.apache.karaf.jaas.config-2.4.0.jar \
	scan-bundle:file:$REPOSITORY/org.apache.karaf.jaas.modules-2.4.0.jar \
	scan-bundle:file:$REPOSITORY/org.apache.karaf.shell.commands-2.4.0.jar \
	scan-bundle:file:$REPOSITORY/org.apache.karaf.shell.config-2.4.0.jar \
	scan-bundle:file:$REPOSITORY/org.apache.karaf.shell.table-3.0.2.jar \
	scan-bundle:file:$REPOSITORY/org.apache.karaf.shell.console-2.4.0.jar \
	scan-bundle:file:$REPOSITORY/jline-2.12.jar \
	scan-bundle:file:$REPOSITORY/org.jledit.core_0.2.1.jar \
	scan-bundle:file:$REPOSITORY/org.apache.karaf.shell.osgi-2.4.0.jar \
	scan-bundle:file:$REPOSITORY/org.apache.karaf.shell.packages-2.4.0.jar \
	scan-bundle:file:$REPOSITORY/org.apache.karaf.shell.ssh-2.4.0.jar \
	scan-bundle:file:$REPOSITORY/org.eclipse.jgit-2.1.0.201209190230-r.bar \
	scan-bundle:file:$REPOSITORY/org.json_2.0.jar \
  scan-bundle:file:$BUNDLESBUILD/org.ms123.common.auth-1.0.0.jar \
  scan-bundle:file:$BUNDLESBUILD/org.ms123.common.auth.api-1.0.0.jar \
  scan-bundle:file:$BUNDLESBUILD/org.ms123.common.data-1.0.0.jar \
  scan-bundle:file:$BUNDLESBUILD/org.ms123.common.data.api-1.0.0.jar \
  scan-bundle:file:$BUNDLESBUILD/org.ms123.common.form-1.0.0.jar \
  scan-bundle:file:$BUNDLESBUILD/org.ms123.common.libhelper-1.0.0.jar \
  scan-bundle:file:$BUNDLESBUILD/org.ms123.common.store-1.0.0.jar \
  scan-bundle:file:$BUNDLESBUILD/org.ms123.common.domainobjects-1.0.0.jar \
  scan-bundle:file:$BUNDLESBUILD/org.ms123.common.message-1.0.0.jar \
  scan-bundle:file:$BUNDLESBUILD/org.ms123.common.ea-1.0.0.jar \
  scan-bundle:file:$BUNDLESBUILD/org.ms123.common.bhs-1.0.0.jar \
  scan-bundle:file:$BUNDLESBUILD/org.ms123.common.entity-1.0.0.jar \
  scan-bundle:file:$BUNDLESBUILD/org.ms123.common.entity.api-1.0.0.jar \
  scan-bundle:file:$BUNDLESBUILD/org.ms123.common.enumeration-1.0.0.jar \
  scan-bundle:file:$BUNDLESBUILD/org.ms123.common.namespace-1.0.0.jar \
  scan-bundle:file:$BUNDLESBUILD/org.ms123.common.exporting-1.0.0.jar \
  scan-bundle:file:$BUNDLESBUILD/org.ms123.common.git-1.0.0.jar \
  scan-bundle:file:$BUNDLESBUILD/org.ms123.common.rpc-1.0.0.jar \
  scan-bundle:file:$BUNDLESBUILD/org.ms123.common.importing-1.0.0.jar \
  scan-bundle:file:$BUNDLESBUILD/org.ms123.common.datamapper-1.0.0.jar \
  scan-bundle:file:$BUNDLESBUILD/org.ms123.common.jetty-1.0.0.jar \
  scan-bundle:file:$BUNDLESBUILD/org.ms123.common.nucleus-1.0.0.jar \
  scan-bundle:file:$BUNDLESBUILD/org.ms123.common.nucleus.api-1.0.0.jar \
  scan-bundle:file:$BUNDLESBUILD/org.ms123.common.reporting-1.0.0.jar \
  scan-bundle:file:$BUNDLESBUILD/org.ms123.common.setting-1.0.0.jar \
  scan-bundle:file:$BUNDLESBUILD/org.ms123.common.setting.api-1.0.0.jar \
  scan-bundle:file:$BUNDLESBUILD/org.ms123.common.permission-1.0.0.jar \
  scan-bundle:file:$BUNDLESBUILD/org.ms123.common.permission.api-1.0.0.jar \
  scan-bundle:file:$BUNDLESBUILD/org.ms123.common.system-1.0.0.jar \
  scan-bundle:file:$BUNDLESBUILD/org.ms123.common.team-1.0.0.jar \
  scan-bundle:file:$BUNDLESBUILD/org.ms123.common.team.api-1.0.0.jar \
  scan-bundle:file:$BUNDLESBUILD/org.ms123.common.utils-1.0.0.jar \
  scan-bundle:file:$BUNDLESBUILD/org.ms123.common.management-1.0.0.jar \
  scan-bundle:file:$BUNDLESBUILD/org.ms123.common.stencil.api-1.0.0.jar \
  scan-bundle:file:$BUNDLESBUILD/org.ms123.common.workflow-1.0.0.jar \
  scan-bundle:file:$BUNDLESBUILD/org.ms123.common.workflow.api-1.0.0.jar \
  scan-bundle:file:$BUNDLESBUILD/org.ms123.common.activiti-1.0.0.jar \
  scan-bundle:file:$BUNDLESBUILD/org.ms123.common.camel-1.0.0.jar \
  scan-bundle:file:$BUNDLESBUILD/org.ms123.common.camel.api-1.0.0.jar \
  scan-bundle:file:$BUNDLESBUILD/org.ms123.common.docbook-1.0.0.jar \
  scan-bundle:file:$BUNDLESBUILD/org.ms123.common.smtp-1.0.0.jar \
	scan-bundle:file:$REPOSITORY/org.apache.sling.commons.compiler-2.0.2.jar \
	scan-bundle:file:$REPOSITORY/ostermillerutils_1_07_00.jar \
  scan-bundle:file:$REPOSITORY/pax-swissbox-extender-1.3.1.jar \
  scan-bundle:file:$REPOSITORY/pax-swissbox-lifecycle-1.3.1.jar \
  scan-bundle:file:$REPOSITORY/pax-logging-logback-1.7.1.jar \
  scan-bundle:file:$REPOSITORY/pax-logging-api-1.7.1.jar \
  scan-bundle:file:$REPOSITORY/commons.compiler-2.6.1.jar \
  scan-bundle:file:$REPOSITORY/phonet4j-1.0.2.bar \
	scan-bundle:file:$REPOSITORY/poi-3.8beta4-all.bar \
	scan-bundle:file:$REPOSITORY/postgresql-9.3-1101.jdbc4.bar  \
  scan-bundle:file:$REPOSITORY/rngom-20100718.bar \
	scan-bundle:file:$REPOSITORY/scala-library.bar \
  scan-bundle:file:$REPOSITORY/secondstring-20140225.bar \
  scan-bundle:file:$REPOSITORY/shiro-aspectj-1.2.0.jar \
  scan-bundle:file:$REPOSITORY/shiro-core-1.2.0.jar \
  scan-bundle:file:$REPOSITORY/freemarker-2.3.15.bar \
  scan-bundle:file:$REPOSITORY/sojo-1.0.0.bar \
	scan-bundle:file:$REPOSITORY/sshd-core-0.12.0.jar \
  scan-bundle:file:$REPOSITORY/tika-core-1.5-SNAPSHOT.jar \
  scan-bundle:file:$REPOSITORY/tika-bundle-1.5-SNAPSHOT.jar \
  scan-bundle:file:$REPOSITORY/xom-1.2.7.jar \
	scan-bundle:file:$REPOSITORY/parboiled-all-1.1.4.bar \
	scan-bundle:file:$REPOSITORY/pegdown-1.2.1.bar \
	scan-bundle:file:$REPOSITORY/fop-all.1.1.bar \
	scan-bundle:file:$REPOSITORY/saxon94.jar \
	scan-bundle:file:$REPOSITORY/woodstox-core-asl-4.1.5.jar \
	scan-bundle:file:$REPOSITORY/stax2-api-3.1.1.jar \
	scan-bundle:file:$REPOSITORY/avalon-framework-4.2.0.bar \
	scan-bundle:file:$REPOSITORY/sweble-all.1.1.0.bar \
	scan-bundle:file:$REPOSITORY/javax.el-3.0-b07.jar \
	scan-bundle:file:$REPOSITORY/javassist-3.18.0-GA.jar \
	scan-bundle:file:$REPOSITORY/UserAgentUtils-1.9-snapshot.bar \
	scan-bundle:file:$REPOSITORY/commons-jxpath-1.3.jar \
	scan-bundle:file:$REPOSITORY/herold-6.1.0.bar  \
	scan-bundle:file:$REPOSITORY/spring-beans-3.2.5.RELEASE.jar \
	scan-bundle:file:$REPOSITORY/spring-context-3.2.5.RELEASE.jar \
	scan-bundle:file:$REPOSITORY/spring-core-3.2.5.RELEASE.jar \
	scan-bundle:file:$REPOSITORY/org.apache.servicemix.bundles.commons-csv-1.0-r706899_5.jar \
	scan-bundle:file:$REPOSITORY/jsp-api-2.0.bar  \
	scan-bundle:file:$REPOSITORY/xstream-1.4.7-ms.jar  \
	scan-bundle:file:$REPOSITORY/xpp3-1.1.4c.bar \
	scan-bundle:file:$REPOSITORY/jettison-1.3.5.jar \
	scan-bundle:file:$REPOSITORY/opencsv-2.3.bar \
	scan-bundle:file:$REPOSITORY/gson-2.2.4.jar \
	scan-bundle:file:$REPOSITORY/xmlbeans-all-2.6.bar \
	scan-bundle:file:$REPOSITORY/httpclient-osgi-4.3.1.jar \
	scan-bundle:file:$REPOSITORY/httpcore-osgi-4.3.jar \
	scan-bundle:file:$REPOSITORY/commons-lang3-3.3.2.jar \
	scan-bundle:file:$REPOSITORY/concurrentlinkedhashmap-lru-1.4.jar  \
	scan-bundle:file:$REPOSITORY/redmine-java-api-1.23.jar  \
	scan-bundle:file:$REPOSITORY/spring-tx-3.2.5.RELEASE.jar \
	scan-bundle:file:$REPOSITORY/spring-jdbc-3.2.5.RELEASE.jar \
	scan-bundle:file:$REPOSITORY/org.everit.osgi.bundles.javax.sql-4.1.0.jar \
	scan-bundle:file:$REPOSITORY/jackson-core-asl-1.9.12.jar \
	scan-bundle:file:$REPOSITORY/quartz-2.2.1.jar  \
	scan-bundle:file:$REPOSITORY/btm-3.0.0-SNAPSHOT.jar \
	scan-bundle:file:$REPOSITORY/xbean-naming-3.18.jar \
	scan-bundle:file:$REPOSITORY/org.apache.karaf.jndi.core-3.0.2.jar \
	scan-bundle:file:$REPOSITORY/org.apache.karaf.jndi.command-2.4.0.jar \
	--executor=script \
	--workingDirectory=${SERVERDIR} \
	--platform=f \
	--nologo=true \
	--shell= \
	--log=ERROR \
	--definitionURL=file:${SRCTOPDIR}/etc/felix.xml \
	--classpath='$SIMPL4DIR/libs/jdt-compiler-3.1.1.jar:$SIMPL4DIR/libs/xml-w3c.jar' \
	--vmOptions="$vmOptions"


rm -f $SRCTOPDIR/etc/felix.xml 
sed -i "s/javax.transaction.xa/dummy/g" $SERVERDIR/felix/config.ini
sed -i "s/javax.transaction/dummy/g" $SERVERDIR/felix/config.ini
sed -i "s/,javax.sql,/,dummy,/g" $SERVERDIR/felix/config.ini
chmod +x $SERVERDIR/run.sh

sed -i 's/startLocalConsole=true/startLocalConsole=$START_CONSOLE/' $SERVERDIR/run.sh
sed -i 's/^java/exec java/' $SERVERDIR/run.sh

sed -i 's/\$SIMPL4DIR/%SIMPL4DIR%/g' $SERVERDIR/run.bat
sed -i 's/\$JETTY_PORT/%JETTY_PORT%/g' $SERVERDIR/run.bat
sed -i 's/\$CONTROL_PORT/%CONTROL_PORT%/g' $SERVERDIR/run.bat
sed -i 's/startLocalConsole=true/startLocalConsole=false/g' $SERVERDIR/run.bat
sed -i 's/org.apache.felix.main.Main.*/org.apache.felix.main.Main 1>..\\log\\run.log 2>\&1/' $SERVERDIR/run.bat

cat >$SERVERDIR/run.bat.tpl <<END-OF-RUNBAT
@echo off
title SIMPFLO 
setx SIMPL4DIR _BASEDIR_
set SIMPL4DIR=_BASEDIR_
set JETTY_PORT=80
set CONTROL_PORT=8070
echo %SIMPL4DIR%\\server
cd %SIMPL4DIR%\\server
rem
END-OF-RUNBAT

cat $SERVERDIR/run.bat >> $SERVERDIR/run.bat.tpl

rm $SERVERDIR/run.bat




