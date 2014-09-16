@echo off
title SIMPFLO 
setx SIMPL4DIR _BASEDIR_
set SIMPL4DIR=_BASEDIR_
set JETTY_PORT=80
set CONTROL_PORT=8070
echo %SIMPL4DIR%\server
cd %SIMPL4DIR%\server
rem

java -Xmx1500m -XX:MaxPermSize=256m -Djava.security.egd=file:/dev/./urandomx -Dworkspace=%SIMPL4DIR%/workspace -Dorg.osgi.service.http.port=%CONTROL_PORT% -DdisableCheckForReferencesInContentException=true -Dgit.repos=%SIMPL4DIR%/gitrepos -Dgroovy.target.indy=false -Dsimpl4.dir=%SIMPL4DIR% -Djetty.port=%JETTY_PORT% -Dkaraf.startLocalConsole=false -Dkaraf.systemBundlesStartLevel=0 -Dkaraf.startRemoteShell=false -Dfelix.cm.dir=%SIMPL4DIR%/etc/config -Dfelix.fileinstall.dir=%SIMPL4DIR%/gitrepos/.bundles -Dorg.ops4j.pax.logging.DefaultServiceLog.level=ERROR -Ddrools.dialect.java.compiler=JANINO -Dkaraf.shell.init.script=%SIMPL4DIR%/etc/shell.init.script -Dfelix.config.properties=file:felix/config.ini -cp bundles/org.apache.felix.main_4.2.1.jar;%SIMPL4DIR%/libs/jdt-compiler-3.1.1.jar;%SIMPL4DIR%/libs/xml-w3c.jar org.apache.felix.main.Main 1>..\log\run.log 2>&1
