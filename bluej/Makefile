##
## Top level Makefile for the BlueJ Development Environment
##
## Usage:
## 	make all	- makes the whole system (grammar, antlr, allclasses)
## 	make allclasses	- makes all bluej classes (but not grammar, antlr)
## 	make		- makes the system with the default compiler
## 	make javac	- makes the system with javac
## 	make jikes	- makes the system with jikes
## 	make grammar	- makes the whole system with jikes
## 	make antlr	- makes the whole system with jikes
## 	make clean	- removes the classes
## 	make dist	- make a distribution
##
## NOTE: the normal make commands (make, make javac, make jikes) do not
## 	make some classes: some classes in "runtime" which are not accesses
##	at compile time are forgotton by those commands. To make those you
##	must use "make allclasses".
##	The Java grammar and the antlr classes are, by default, not re-made.
##	Use "make grammar" and "make antlr" to make those.
##

include $(OS).mk

PKG_VERSION = 101rc1

SUBDIRS = \
	src

INSTALL_IMAGES = \
	images/arrow_black.gif \
	images/arrow_red.gif \
	images/bluej-icon.gif \
	images/bluej-logo.gif \
	images/break.gif \
	images/broken-arrow.gif \
	images/continue.gif \
	images/darrow_black.gif \
	images/darrow_red.gif \
	images/help.gif \
	images/leftarrow.gif \
	images/objectIcon.gif \
	images/objectIcon_blank.gif \
	images/packageIcon.gif \
	images/rightarrow.gif \
	images/step.gif \
	images/step_into.gif \
	images/stop.gif \
	images/terminate.gif \
	images/working-disab.gif \
	images/working-stopped.gif \
	images/working.gif

INSTALL_LIB_FILES = \
	lib/abstract.tmpl \
	lib/antlr.jar \
	lib/applet.tmpl \
	lib/bluej.defs \
	lib/dialogues.english \
	lib/dialogues.german \
	lib/exception.help.english \
	lib/exception.help.german \
	lib/html.tmpl \
	lib/interface.tmpl \
	lib/japplet.tmpl \
	lib/javac.help.english \
	lib/javac.help.german \
	lib/jikes.help.english \
	lib/jikes.help.german \
	lib/jpda.jar \
	lib/labels.english \
	lib/labels.german \
	lib/moe.help.english \
	lib/moe.help.german \
	lib/moe.labels.english \
	lib/moe.labels.german \
	lib/shell.tmpl \
	lib/stdclass.tmpl \
	lib/syslibs.properties

INSTALL_EXAMPLES = \
	examples/appletClock \
	examples/appletdemo \
	examples/debugdemo \
	examples/hello \
	examples/maze \
	examples/people \
	examples/people2 \
	examples/reader \
	examples/simple

SPARC_INSTALL_FILES = \
	lib/sparc

WINDOWS_INSTALL_FILES = \
	lib/win32

LINUX_INSTALL_FILES = \


PACKAGES = \
	package/bluej-$(PKG_VERSION).jar

FINDALL = find . -follow \( -name CVS -a -prune \) -o \( -type f -a -print \)
FINDCVS = find . -name CVS

default all allclasses clean jikes javac grammar antlr doc::
	for f in $(SUBDIRS) ; do \
		$(MAKE) -C $$f $@ ; \
	done

dist: $(PACKAGES)

package/bluej-$(PKG_VERSION).jar: default
	rm -rf install_tmp
	mkdir install_tmp
	mkdir install_tmp/images
	mkdir install_tmp/lib
	mkdir install_tmp/examples
	chmod -R a+rx images

	# install images
	cp -p $(INSTALL_IMAGES) install_tmp/images

	# install lib files
	cp -p $(INSTALL_LIB_FILES) install_tmp/lib

	# install examples
	cp -rp $(INSTALL_EXAMPLES) install_tmp/examples
	( cd install_tmp/examples && rm -rf `$(FINDCVS)` )

	# jar bluej class files into lib/bluej.jar
	( cd classes ; \
	  jar cv0Mf ../install_tmp/lib/bluej.jar `$(FINDALL)` )

	chmod -R a+r install_tmp

	# copy platform directories to lib
	cp -rp lib/sparc install_tmp/lib
	cp -rp lib/win32 install_tmp/lib
	( cd install_tmp/lib && rm -rf `$(FINDCVS)` )

	# bundle the whole bluej distribution together to bluej-dist.jar
	rm -f package/bluej.jar
	( cd install_tmp && jar cvMf ../package/bluej-dist.jar * )

	# compile and run the installer
	( cd package && \
	  javac Installer.java ; \
	  java Installer build installer.props ; \
	  jar cv0mf manifest bluej-$(PKG_VERSION).jar *.class )

	# clean up
	rm -f package/bluej-dist.jar
	rm -rf install_tmp

clean::
	$(RM) $(PACKAGES)
	$(RM) -r install_tmp

.PHONY:
