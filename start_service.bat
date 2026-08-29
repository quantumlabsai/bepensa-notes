@echo off
setlocal

REM Directorio base de la aplicación
set APP_HOME=%~dp0
set TG-TOKEN=xx
set TG-CHAT-ID=yy

C:\quantumlabs\java\openlogic-openjdk-8u402-b06-windows-64\bin\java ^
-Xms1024m ^
-Xmx1024m ^
-Duser.timezone=America/Mexico_City ^
-Dlog4j2.configurationFile="%APP_HOME%config\log4j2.xml" ^
-server ^
-XX:+CMSParallelRemarkEnabled ^
-XX:+AggressiveOpts ^
-XX:+CMSClassUnloadingEnabled ^
-XX:+IgnoreUnrecognizedVMOptions ^
-cp "%APP_HOME%lib\*;%APP_HOME%config" ^
caudal.core.StarterDSL ^
-c "%APP_HOME%config\config-bepensa.clj"

set EXIT_CODE=%ERRORLEVEL%

endlocal & EXIT /b %EXIT_CODE%
