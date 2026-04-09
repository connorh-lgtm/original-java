Place the following JAR files in this directory before building:

  servlet-api-3.1.jar    - Java Servlet API 3.1 (provided by container at runtime)
  gson-2.8.0.jar         - Google JSON serialization library
  sqlite-jdbc-3.20.0.jar - SQLite JDBC driver
  joda-time-2.9.jar      - Date/time library (unused, but someone added it "just in case")

Download links:
  servlet-api:  https://repo1.maven.org/maven2/javax/servlet/javax.servlet-api/3.1.0/javax.servlet-api-3.1.0.jar
  gson:         https://repo1.maven.org/maven2/com/google/code/gson/gson/2.8.0/gson-2.8.0.jar
  sqlite-jdbc:  https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.20.0.1/sqlite-jdbc-3.20.0.1.jar
  joda-time:    https://repo1.maven.org/maven2/joda-time/joda-time/2.9/joda-time-2.9.jar

TODO: We really should use Maven or Gradle instead of manually managing JARs.
      Every new developer has to download these by hand.
      Version conflicts are tracked in a spreadsheet on the shared drive.
