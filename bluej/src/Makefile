# Makefile for the BlueJ sources
SUBDIRS = \
	jdepend \
	bluej

all dep depend clean clobber::
	for dir in $(SUBDIRS) ; do \
		$(MAKE) -C $$dir $@ || break ; \
	done
