last revised: 8/12/2003

Notes on the distribution:

This is a split of the editor source from the BlueJ 
distribution, based on code checked out of the repository at the 
time of the corresponding BlueJ release.

The structure of bluej is reasonably decoupled from the editor
implementation meaning that you can quite easily enhance the editor
and plug it back into bluej.  This release is in response to a number
of requests from users wanting to specialise or enhance the editor to
better suit their uses.

NOTE: The separation of the editor code to a separate library was
completed in BlueJ 1.1.2.  You will need to compile and deploy this
editor with BlueJ 1.1.2 or later as a minimum.  It is likely and 
preferable that you will need to use the matching version of BlueJ.
See information about versioning below.


VERSION

The BlueJ editor source has been numbered the same as the relevant
bluej version from which the editor sourcecode snapshot was taken.
Bluej editor source version 1.1.6 corresponds to the editor code that
is found in BlueJ 1.1.6.  It is envisaged that an editor snapshot will
be released at the same time as each BlueJ version.
 

CONTENTS

license.txt - BlueJ editor license (MIT license)
install.txt - information on how to extend the editor
readme.txt  - this file
build.xml   - a simple build file for the ant build tool 
              

/src
    /bluej -  source for editor and utility classes
    /org   -  source from jedit syntax code used in bluej editor
                

Please read the install.txt file for info on how to extend, build and
deploy the editor.


LICENSE

The license information for this release can be viewed in license.txt.
In a nutshell we have released the source under the MIT license as we
felt it was the most flexible to allow you to do whatever you want
with the code.  We welcome you to post back any enhancements you make
to the editor for possible inclusion in the standard bluej release or
to allow us to provide links to your implementation if you would like
to share with others. Please mail your comments, questions and
improvements to bluej-support@bluej.org.


