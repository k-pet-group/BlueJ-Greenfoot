# Makefile for the BlueJ sources

TOP = ../..
include $(TOP)/$(OS).mk

all antlr::
	javac -classpath . antlr/*.java
	mkjar
	mv antlr.jar $(TOP)/lib