@ECHO OFF
set JDK=F:\Programming\JDK1.3
set BLUEJ=%CD%

rem set PATH=%BLUEJ%\lib\win32;%PATH%

set CLASSPATH=%BLUEJ%\classes;%JDK%\lib\dt.jar;%JDK%\lib\tools.jar;%BLUEJ%\lib\antlr.jar
rem ;%BLUEJ%\lib\jpda.jar

%JDK%\bin\java -Dbluej.home=%BLUEJ% bluej.Main %1 %2 %3 %4 %5 %6 %7 %8 %9

pause
