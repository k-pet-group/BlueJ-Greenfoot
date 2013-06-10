#! /bin/sh

javac -cp bluej-editor.jar *.java && java -cp bluej-editor.jar:mysql-connector-java-5.1.17-bin.jar:. Viewer
