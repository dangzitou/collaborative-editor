@echo off
cd /d "%~dp0..\server"
echo Starting Backend...
mvn spring-boot:run "-Dspring-boot.run.jvmArguments=-Dfile.encoding=GBK"
pause