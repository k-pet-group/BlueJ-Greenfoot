# Makefile for the BlueJ sources

TOP = ..
include $(TOP)/$(OS).mk

# DEFAULT_COMPILER can be either "javac" or "jikes"
DEFAULT_COMPILER=jikes

JIKES_CLASSPATH = /usr/local/jdk1.2/jre/lib/rt.jar:/usr/local/jdk1.2/jre/lib/i18n.jar:/usr/local/jdk1.2/lib/tools.jar:$(TOP)/lib/jpda.jar:$(TOP)/src:$(TOP)/lib/antlr.jar
JIKES_OPTS = +F

JAVAC_CLASSPATH = .:$(TOP)/lib/jpda.jar:$(TOP)/lib/antlr.jar:/usr/local/jdk1.2/lib/tools.jar
JAVAC_OPTS = -deprecation

ANTLR_CLASSPATH=../../antlr



SUBDIRS = \
	bluej


default: $(DEFAULT_COMPILER)

clean clobber allclasses doc::
	for dir in $(SUBDIRS) ; do \
		$(MAKE) -C $$dir $@ || break ; \
	done

all: grammar antlr allclasses

jikes:
	jikes -classpath $(JIKES_CLASSPATH) $(JIKES_OPTS) -d $(TOP)/classes bluej/Main.java bluej/runtime/Shell.java

javac:
	javac -classpath $(JAVAC_CLASSPATH) $(JAVAC_OPTS) -d $(TOP)/classes bluej/Main.java bluej/runtime/Shell.java

grammar:
	(cd bluej/parser && \
	java -classpath $(ANTLR_CLASSPATH) antlr.Tool java.g )

antlr::
	$(MAKE) -C antlr $@;
