@ECHO OFF
rem Windows batch file to build bluej on ajp's system
rem -------------------------------------------------
rem note: if not using jdk1.3, jpda.jar needs to be added

rem set JDK=F:\programming\jdk1.2
rem set JPDA=;%JDK%\lib\jpda.jar

set JDK=C:\jdk1.3
set JPDA=

set BLUEJ=c:\bsrc\bluej

set JIKES_PATH=%BLUEJ%\src;%BLUEJ%\lib\antlr.jar;%JDK%\jre\lib\rt.jar;%JDK%\jre\lib\i18n.jar;%JDK%\lib\dt.jar;%JDK%\lib\tools.jar%JPDA%
set JIKES_OPTS=-nowarn -depend +P +F +E -g -Xstdout

jikes -classpath %JIKES_PATH% -d %BLUEJ%\classes %JIKES_OPTS% bluej\Main.java bluej\runtime\Shell.java
