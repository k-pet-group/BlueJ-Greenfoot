include $(OS).mk

PKG_VERSION = 093

SUBDIRS = \
	src

INSTALL_FILES = \
	images/arrow_black.gif \
	images/arrow_red.gif \
	images/continue.gif \
	images/darrow_black.gif \
	images/darrow_red.gif \
	images/bluej-logo.gif \
	images/bluej-icon.gif \
	images/step.gif \
	images/step_into.gif \
	images/stop.gif \
	images/terminate.gif \
	images/working.gif \
	images/working-disab.gif \
	images/working-stopped.gif \
	images/break.gif \
	images/packageIcon.gif \
	images/leftarrow.gif \
	images/rightarrow.gif \
	lib/bluej.jar \
	lib/bluej.defs \
	lib/syslibs.properties \
	lib/moe.defs \
	lib/moehelp.defs \
	lib/interface.tmpl \
	lib/abstract.tmpl \
	lib/interface.tmpl \
	lib/shell.tmpl \
	lib/stdclass.tmpl \
	lib/stdctxt.jar \
	lib/jcvs.jar \
	lib/swing.jar \
	lib/mac.jar \
	lib/motif.jar \
	lib/windows.jar \
	lib/english.defs \
	examples

PACKAGES = \
	package/bluej-$(PKG_VERSION).jar

FINDALL = find . -follow \( -name CVS -a -prune \) -o \( -type f -a -print \)
FINDCVS = find . -name CVS

all dep depend clean clobber jikes grammar::
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
