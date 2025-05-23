@echo off

setlocal

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~n0
set WRAPPER_JAR=%DIRNAME%gradle\wrapper\gradle-wrapper.jar

if not exist "%WRAPPER_JAR%" (
  echo Could not find Gradle Wrapper jar. Please run 'gradle wrapper' to generate it.
  exit /b 1
)

set DEFAULT_JVM_OPTS=-Xmx64m -Xms64m

set CLASSPATH=%WRAPPER_JAR%

java %DEFAULT_JVM_OPTS% -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*