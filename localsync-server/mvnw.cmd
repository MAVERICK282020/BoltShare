@REM Maven Wrapper Script for Windows
@REM Generated for LocalSync project

@IF "%__MVNW_ARG0_NAME__%"=="" (SET "JAVA_HOME=%JAVA_HOME%")
@SET WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain
@SET DOWNLOAD_URL="https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar"

java -classpath "%~dp0\.mvn\wrapper\maven-wrapper.jar" ^
     "-Dmaven.multiModuleProjectDirectory=%~dp0" ^
     %WRAPPER_LAUNCHER% %*
