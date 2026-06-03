rem @echo off
rem :loop
C:\quantumlabs\java\openlogic-openjdk-8u402-b06-windows-64\bin\java -Xms1024m -Xmx1024m -Duser.timezone=America/Mexico_City -Dlog4j2.configurationFile=config\log4j2.xml -server -XX:+CMSParallelRemarkEnabled -XX:+AggressiveOpts -XX:+CMSClassUnloadingEnabled -XX:+IgnoreUnrecognizedVMOptions -cp C:\quantumlabs\cauda-rfid\lib\caudal-0.8.4-standalone.jar;C:\quantumlabs\cauda-rfid\config caudal.core.StarterDSL -c C:\quantumlabs\cauda-rfid\config\config-bepensa.clj
rem Espera unos segundos antes de volver a ejecutar el programa
rem timeout /t 10 /nobreak >nul
rem goto loop
