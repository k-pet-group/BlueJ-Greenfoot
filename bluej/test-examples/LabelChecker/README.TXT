------------------------------------------------------------------------
This is the project README file. Here, you should describe your project.
Tell the reader (someone who does not know anything about this project)
all he/she needs to know. The comments should usually include at least:
------------------------------------------------------------------------

PROJECT TITLE: LabelChecker
PURPOSE OF PROJECT: 
This project is aimed to be a convenience tool for anyone localising BlueJ
to a different language.  

VERSION or DATE:
0.1 (15-02-2002)
This is a basic start to the project. It just works on the labels file 
at the moment which contains the UI labels & texts.

HOW TO START THIS PROJECT:
AUTHORS: Bruce Quig
USER INSTRUCTIONS:

To use it in its present state:

Create a LabelChecker object.  The constructor requires the location
of a bluej installation.   Makes sure on Windows systems that double 
backslashes are used eg. "C:\\bluej"".

Then invoke the checkLanguage(String ..) method.  The parameter is the 
name of the language that you want to compare with the master 
language (English).  It will print out any labels that it finds in the
 English labels that it can't find in the other language file.  This 
allows you to check for any new labels that may appear with subsequent 
BlueJ versions.  
 
