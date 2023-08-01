@echo off
:server
echo (%time%) server started.
SET BINDIR=%~dp0
CD /D "%BINDIR%"
java -server -Xms1024M -Xmx8192M -XX:MaxHeapFreeRatio=50 -jar  target\NinjaServer.jar
echo (%time%) WARNING: Dang khoi dong lai.
ping 1.1.1.1 -n 1 -w 2000 >nul
goto server