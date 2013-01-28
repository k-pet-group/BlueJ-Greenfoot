#! /bin/sh

javac -cp bluej-editor.jar:diffutils-1.2.1.jar *.java
java -cp bluej-editor.jar:diffutils-1.2.1.jar:mysql-connector-java-5.1.17-bin.jar:. Viewer
