@ECHO OFF
set BLUEJ=F:\H\BLUEJ
rem set PATH=%BLUEJ%\lib\win32;%PATH%
set CLASSPATH=%BLUEJ%\classes;F:\programming\jdk1.3\lib\tools.jar;%BLUEJ%\lib\antlr.jar
java -Dbluej.home=%BLUEJ% bluej.Main %1
