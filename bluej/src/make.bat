@ECHO OFF
rem Windows batch file to build bluej on ajp's system
rem -------------------------------------------------
rem note: if not using jdk1.3, jpda.jar needs to be added

set JDK=F:\programming\jdk1.3
set BLUEJ=F:\H\bluej

set JIKES_PATH=%BLUEJ%\src;%BLUEJ%\lib\antlr.jar;%JDK%\jre\lib\rt.jar;%JDK%\jre\lib\i18n.jar;%JDK%\lib\tools.jar
set JIKES_OPTS=-nowarn -depend +P +F -deprecation +E -g 

jikes -classpath %JIKES_PATH% -d %BLUEJ%\classes %JIKES_OPTS% bluej\Main.java bluej\runtime\Shell.java
