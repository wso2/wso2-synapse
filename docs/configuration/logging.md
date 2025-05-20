# log4j Configuration

```properties
# log4j2.properties
status = error
name = PropertiesConfig
monitorInterval = 30

# Appenders
appenders = console

appender.console.type = Console
appender.console.name = STDOUT
appender.console.layout.type = PatternLayout
appender.console.layout.pattern = [%d{yyyy-MM-dd HH:mm:ss.SSS}] %-5level {%C{1}} - %msg%n

# Root logger
rootLogger.level = info
rootLogger.appenderRefs = stdout
rootLogger.appenderRef.stdout.ref = STDOUT
```