@ECHO OFF
set JDK=F:\Programming\JDK1.2
set BLUEJ=F:\H\BLUEJ

set PATH=%BLUEJ%\lib\win32;%PATH%

set CLASSPATH=%BLUEJ%\classes;%JDK%\lib\dt.jar;%JDK%\lib\tools.jar;%BLUEJ%\lib\antlr.jar;%BLUEJ%\lib\jpda.jar

%JDK%\bin\java -Dbluej.home=%BLUEJ% bluej.Main %1
