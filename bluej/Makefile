include $(OS).mk

PKG_VERSION = 093

SUBDIRS = \
	src

INSTALL_FILES = \
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
	images/packageIcon.gif \
	images/rightarrow.gif \
	images/step.gif \
	images/step_into.gif \
	images/stop.gif \
	images/terminate.gif \
	images/working-disab.gif \
	images/working-stopped.gif \
	images/working.gif \
	lib/abstract.tmpl \
	lib/antlr.jar \
	lib/applet.tmpl \
	lib/bluej.defs \
	lib/bluej.jar \
	lib/english.defs \
	lib/exception.english.help \
	lib/html.tmpl \
	lib/interface.tmpl \
	lib/javac.english.help \
	lib/jbug.jar \
	lib/jikes.english.help \
	lib/moe.defs \
	lib/moehelp.defs \
	lib/shell.tmpl \
	lib/stdclass.tmpl \
	lib/stdctxt.jar \
	lib/syslibs.properties \
	examples

PACKAGES = \
	package/bluej-$(PKG_VERSION).jar

FINDALL = find . -follow \( -name CVS -a -prune \) -o \( -type f -a -print \)
FINDCVS = find . -name CVS

all dep depend clean clobber jikes grammar antlr::
	for f in $(SUBDIRS) ; do \
		$(MAKE) -C $$f $@ ; \
	done

package: dist

dist: $(PACKAGES)

package/bluej-$(PKG_VERSION).jar: package/install.class
	( cd package && \
	  jar cv0Mf bluej-$(PKG_VERSION).jar *.class )

package/install.class: package/bluej.jar
	( cd package && \
	  javac install.java ; \
	  java install build install.props )

# create a files listed in INSTALL_FILES in directory "install/"
# and
package/bluej.jar: $(INSTALL_FILES:%=install/%)
	rm -f $@
	# ( cd install && jar cvMf ../$@ `$(FINDALL)` )
	( cd install && jar cvMf ../$@ * )

install:
	mkdir -p $@

install/images:
	mkdir -p $@
	chmod a+rx $@

install/images/%.gif: install/images
	cp images/`basename $@` $@
	chmod a+r $@

install/lib:
	mkdir -p $@
	chmod a+rx $@

# create install/lib/bluej.jar and add all class files
install/lib/bluej.jar: all install/lib
	( cd classes ; \
	  rm -f ../$@ ; \
	  jar cv0Mf ../$@ `$(FINDALL)` )

# copy the target from "lib/" to "install/lib"
install/lib/%: lib/% install/lib
	cp lib/`basename $@` $@
	chmod a+r $@

.PHONY:
install/examples: install
	rm -rf $@
	cp -rp examples install
	# mkdir $@/bluej
	# cp -r src/* $@/bluej
	( cd $@ && rm -rf `$(FINDCVS)` )

clean::
	$(RM) $(PACKAGES)
	$(RM) -r install

.PHONY:
