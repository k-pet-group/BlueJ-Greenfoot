This directory contains the original Javadoc doclet from OpenJDK 6.

Certain modifications have been made. Primarily, the package names have
been changed to avoid any conflicts which may otherwise occur.

It's necessary to use this doclet when running BlueJ with Java 7,
as the Java 7 doclet produces HTML which is too modern for the JEditorPane/
HTMLEditorKit combination to render correctly, thereby making the BlueJ
editor's "documentation" view unusable.

The license for these sources is the GNU General Public License Version 2
with the classpath exception - the same as the license for the main BlueJ
sources.
