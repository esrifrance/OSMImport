@echo off

SET SCRIPT_DIR=%~dp0

PATH %SCRIPT_DIR%/jre/bin;%SCRIPT_DIR%;%PATH%

java -Xmx6g -jar %SCRIPT_DIR%/osmtoolsreader-all-0.5-SNAPSHOT.jar %1 %2 %3 %4 %5 %6 %7 %8 %9

