# Makefile for the BlueJ sources

TOP = ..
include $(TOP)/$(OS).mk

# DEFAULT_COMPILER can be either "javac" or "jikes"
DEFAULT_COMPILER=jikes

# compiler for building distribution; can be either "javac" or "jikes"
DIST_COMPILER=javac

JIKES_CLASSPATH = /usr/java/jre/lib/rt.jar:/usr/java/jre/lib/i18n.jar:$(TOP)/lib/jpda.jar:$(TOP)/inspector:$(TOP)/classes
JIKES_OPTS = -O

JAVAC_CLASSPATH = .:$(TOP)/lib/jpda.jar:$(TOP)/classes
JAVAC_OPTS = -deprecation -O -g:none


default: $(DEFAULT_COMPILER)

distcompile: $(DIST_COMPILER)

clean clobber allclasses doc::
	for dir in $(SUBDIRS) ; do \
		$(MAKE) -C $$dir $@ || break ; \
	done

jikes:
	jikes -classpath $(JIKES_CLASSPATH) $(JIKES_OPTS) -d $(TOP)/lib/inspector ObjectReferenceInspector.java MonashImageInspector.java ListInspector.java

javac:
	javac -classpath $(JAVAC_CLASSPATH) $(JAVAC_OPTS) -d $(TOP)/lib/inspector ObjectReferenceInspector.java MonashImageInspector.java ListInspector.java
