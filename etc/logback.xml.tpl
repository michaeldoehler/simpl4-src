<configuration>

  <appender name="FILE" class="ch.qos.logback.core.FileAppender">
    <file>_LOGDIR_/simpl4.log</file>
    <append>true</append>

   <!-- encoders are assigned the type
         ch.qos.logback.classic.encoder.PatternLayoutEncoder by default -->
    <encoder>
      <pattern>%-4relative [%thread] %-5level %logger{35} - %msg%n</pattern>
    </encoder>
  </appender>

<!--appender name="DB" class="ch.qos.logback.classic.db.DBAppender">
    <connectionSource class="ch.qos.logback.core.db.DriverManagerConnectionSource">
      <driverClass>org.h2.jdbcx.JdbcDataSource</driverClass>
      <url>jdbc:h2:file:/tmp/dbh2</url>
      <user>sa</user>
      <password>sa</password>
    </connectionSource>
  </appender-->

<logger name="org" level="INFO"/>
<logger name="DataNucleus" level="WARN"/>
<logger name="org.apache.camel.impl.RouteService" level="INFO"/>
<logger name="org.apache.camel.processor" level="INFO"/>
<logger name="org.apache.camel.management" level="WARN"/>
<logger name="org.apache.camel.support" level="WARN"/>
<logger name="org.apache.camel.component.http4.HttpComponent" level="WARN"/>
<logger name="org.apache.camel.core.osgi.OsgiCamelContextHelper" level="INFO"/>
<logger name="org.apache.camel.processor.interceptor" level="INFO"/>
<logger name="org.apache.camel" level="DEBUG"/>
<logger name="org.ms123.common.workflow" level="DEBUG"/>


<!--logger name="org.apache.camel.core.osgi" level="DEBUG"/-->
<logger name="org.milyn.javabean" level="DEBUG"/>
<logger name="org.activiti.engine.impl.bpmn.behavior" level="DEBUG"/>
<logger name="org.activiti.engine.impl.pvm.runtime" level="DEBUG"/>
<logger name="org.ms123.common.datamapper" level="DEBUG"/>
  <root level="INFO">
    <appender-ref ref="FILE" />
  </root>
</configuration>
