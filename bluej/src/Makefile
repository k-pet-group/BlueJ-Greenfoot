# Makefile for the BlueJ sources

TOP = ..
include $(TOP)/$(OS).mk

JIKES_CLASSPATH = $(TOP)/lib/swing.jar:$(CLASSPATH)
JIKES_OPTS = +P +F
# JIKES_OPTS = +P +F -deprecation	# -deprecation not supported by jikes yet

SUBDIRS = \
	jdepend \
	bluej

all dep depend clean clobber::
	for dir in $(SUBDIRS) ; do \
		$(MAKE) -C $$dir $@ || break ; \
	done

jikes:
	jikes -classpath $(JIKES_CLASSPATH) -d $(TOP)/classes $(JIKES_OPTS) bluej/Main.java
