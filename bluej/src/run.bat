@ECHO OFF
set JDK=C:\JDK1.3
set BLUEJ=C:\bsrc\bluej

set CLASSPATH=%BLUEJ%\lib\bluej.jar;%BLUEJ%\classes;%JDK%\lib\dt.jar;%JDK%\lib\tools.jar;%BLUEJ%\lib\antlr.jar

%JDK%\bin\java bluej.Main %1
