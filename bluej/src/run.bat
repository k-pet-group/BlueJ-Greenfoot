@ECHO OFF
set JDK=C:\JDK1.3
set BLUEJ=C:\bsrc\BLUEJ

set PATH=%BLUEJ%\lib\win32;%PATH%

set CLASSPATH=%BLUEJ%\classes;%JDK%\lib\dt.jar;%JDK%\lib\tools.jar;%BLUEJ%\lib\antlr.jar

%JDK%\bin\java -Dbluej.home=%BLUEJ% bluej.Main %1
