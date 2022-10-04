#! /bin/bash

# Downloads JavaFX modules from Maven and arranges them like the JavaFX SDK

javafxversion=$1
os=$2
modules="javafx-base javafx-controls javafx-fxml javafx-graphics javafx-media javafx-swing javafx-web"
base="https://repo1.maven.org/maven2/org/openjfx/"

mkdir lib
for module in $modules
do
  wget --no-verbose $base$module/$javafxversion/$module-$javafxversion-$os.jar
  mv $module-$javafxversion-$os.jar lib/${module//-/.}.jar
done
