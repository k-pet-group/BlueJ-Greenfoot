##
## Makefile for BlueJ sources (part of BlueJ system)
##

TOP = ../..
include $(TOP)/$(OS).mk

PKGNAME = bluej
CLASSROOT = $(subst /,$(SL),$(TOP)/classes)
SRCROOT = $(subst /,$(SL),$(TOP)/src)
CTXTROOT = $(TOP)/ctxt
XCLASSPATH = $$CLASSPATH
#CLASSPATH = $(subst ;,$(SEP),$(CLASSROOT);$(subst /,$(SL),$(XCLASSPATH)))

CLASSPATH = ..:$(TOP)/lib/jpda.jar:$(TOP)/lib/antlr.jar:/usr/local/jdk1.2/lib/tools.jar

LIB = $(TOP)/lib
JDEPEND = CLASSPATH="$(CLASSPATH)" jdepend
JAVAC = CLASSPATH="$(CLASSPATH)" javac
JAVAC_OPTS = -deprecation
# The BlueJ documentation generator (javadoc with right CLASSPATH + opts)
JBDOC = jbdoc
JAVADOC = CLASSPATH="$(CLASSPATH)" javadoc
JAVADOC_OPTS = -author -private -sourcepath $(SRCROOT)
DOC_DIR = $(TOP)/doc


# Top level files
BLUEJ_CLASSES = \
		BlueJEvent \
		BlueJEventListener \
		Config \
		Main
BLUEJ_SRCS = $(BLUEJ_CLASSES:%=%.java)
BLUEJ_SRCS_OS = $(subst /,$(SL),$(BLUEJ_SRCS))
BLUEJ_CLASSFILES = $(BLUEJ_CLASSES:%=$(CLASSROOT)/$(PKGNAME)/%.class)


# Browser files
BROWSER_CLASSES = \
		AttributeChooser \
		ChooseUseDestinationDIalog \
		CodeViewer \
		FindLibraryDialog \
		LibraryChooser \
		LibraryNode \
		LibraryTreeCellRenderer \
		StatusBarOwner
BROWSER_SRCS = $(BROWSER_CLASSES:%=browser/%.java)
BROWSER_SRCS_OS = $(subst /,$(SL),$(BROWSER_SRCS))
BROWSER_CLASSFILES = $(BROWSER_CLASSES:%=$(CLASSROOT)/$(PKGNAME)/browser/%.class)


# Class Manager files
CLASSMGR_CLASSES = \
		ClassMgr \
		ClassMgrDialog \
		ClassPath \
		ClassPathEntry \
		ClassPathTableModel
CLASSMGR_SRCS = $(CLASSMGR_CLASSES:%=classmgr/%.java)
CLASSMGR_SRCS_OS = $(subst /,$(SL),$(CLASSMGR_SRCS))
CLASSMGR_CLASSFILES = $(CLASSMGR_CLASSES:%=$(CLASSROOT)/$(PKGNAME)/classmgr/%.class)


# Compiler files
COMPILER_CLASSES = \
		CompileObserver \
		Compiler \
		CompilerMessageError \
		CompilerThread \
		JavacCompiler \
		JavacCompilerInternal \
		JikesCompiler \
		Job \
		JobQueue
COMPILER_SRCS = $(COMPILER_CLASSES:%=compiler/%.java)
COMPILER_SRCS_OS = $(subst /,$(SL),$(COMPILER_SRCS))
COMPILER_CLASSFILES = $(COMPILER_CLASSES:%=$(CLASSROOT)/$(PKGNAME)/compiler/%.class)


# Debugger files
DEBUGGER_CLASSES = \
		CallHistory \
		Debugger \
		DebuggerClassLoader \
		DebuggerObject \
		DebuggerThread \
		ExceptionDescription \
		ExecControlButtonModel \
		ExecControls \
		Invoker \
		MachineLoader \
		MethodDialog \
		MethodDialogWatcher \
		ObjectBench \
		ObjectBenchWatcher \
		ObjectViewer \
		ObjectWrapper \
		ResultWatcher
DEBUGGER_SRCS = $(DEBUGGER_CLASSES:%=debugger/%.java)
DEBUGGER_SRCS_OS = $(subst /,$(SL),$(DEBUGGER_SRCS))
DEBUGGER_CLASSFILES = $(DEBUGGER_CLASSES:%=$(CLASSROOT)/$(PKGNAME)/debugger/%.class)


# classes for JDI (Java Debug Interface) debugger implementation
JDI_DEBUG_CLASSES = \
		JdiArray \
		JdiClassLoader \
		JdiDebugger \
		JdiObject \
		JdiThread \
		VMEventHandler
JDI_DEBUG_SRCS = $(JDI_DEBUG_CLASSES:%=debugger/jdi/%.java)
JDI_DEBUG_SRCS_OS = $(subst /,$(SL),$(JDI_DEBUG_SRCS))
JDI_DEBUG_CLASSFILES = $(JDI_DEBUG_CLASSES:%=$(CLASSROOT)/$(PKGNAME)/debugger/jdi/%.class)


# editor files
EDITOR_CLASSES = \
		Editor \
		EditorManager \
		EditorWatcher

EDITOR_SRCS = $(EDITOR_CLASSES:%=editor/%.java)
EDITOR_SRCS_OS = $(subst /,$(SL),$(EDITOR_SRCS))
EDITOR_CLASSFILES = $(EDITOR_CLASSES:%=$(CLASSROOT)/$(PKGNAME)/editor/%.class)


# moe files
MOE_CLASSES = \
		Finder \
		FunctionDialog \
		Info \
		LineNumberLabel \
		MoeActions \
		MoeBoxView \
		MoeCaret \
		MoeEditor \
		MoeEditorKit \
		MoeEditorManager \
		MoeParagraphView \
		Preferences \
		StatusLabel
MOE_SRCS = $(MOE_CLASSES:%=editor/moe/%.java)
MOE_SRCS_OS = $(subst /,$(SL),$(MOE_SRCS))
MOE_CLASSFILES = $(MOE_CLASSES:%=$(CLASSROOT)/$(PKGNAME)/editor/moe/%.class)


# Graph files
GRAPH_CLASSES = \
		Edge \
		Graph \
		GraphEditor \
		Vertex
GRAPH_SRCS = $(GRAPH_CLASSES:%=graph/%.java)
GRAPH_SRCS_OS = $(subst /,$(SL),$(GRAPH_SRCS))
GRAPH_CLASSFILES = $(GRAPH_CLASSES:%=$(CLASSROOT)/$(PKGNAME)/graph/%.class)


# Parser files
PARSER_CLASSES = \
		ClassParser \
		JavaLexer \
		JavaToken \
		JavaTokenTypes
PARSER_SRCS = $(PARSER_CLASSES:%=parser/%.java)
PARSER_SRCS_OS = $(subst /,$(SL),$(PARSER_SRCS))
PARSER_CLASSFILES = $(PARSER_CLASSES:%=$(CLASSROOT)/$(PKGNAME)/parser/%.class)


# Symtab files
SYMTAB_CLASSES = \
		ArrayDef \
		BlockDef \
		ClassDef \
		ClassInfo \
		Definition \
		DummyClass \
		HasImports \
		IndentingPrintWriter \
		JavaHashtable \
		JavaStack \
		JavaVector \
		LabelDef \
		MethodDef \
		MultiDef \
		Occurrence \
		PackageDef \
		PrimitiveDef \
		Reportable \
		ScopedDef \
		StringTable \
		SymbolTable \
		TypedDef \
		VariableDef
SYMTAB_SRCS = $(SYMTAB_CLASSES:%=parser/symtab/%.java)
SYMTAB_SRCS_OS = $(subst /,$(SL),$(SYMTAB_SRCS))
SYMTAB_CLASSFILES = $(SYMTAB_CLASSES:%=$(CLASSROOT)/$(PKGNAME)/parser/symtab/%.class)


# Package manager files
PKGMGR_CLASSES = \
		AboutBlue \
		AppletClassRole \
		ClassRole \
		ClassTarget \
		Dependency \
		DirectoryFilter \
		EditableTarget \
		ExtendsDependency \
		ImplementsDependency \
		ImportedClassTarget \
		JavaSourceFilter \
		JBPackageChooser \
		JBPackageFileView \
		LayoutComparer \
		LibraryBrowserPkgMgrFrame \
		Main \
		NewClassDialog \
		Package \
		PackageCacheMgr \
		PackageTarget \
		PkgFrame \
		PkgMgrFrame \
		RunAppletDialog \
		SourceInfo \
		StdClassRole \
		Target \
		UsesDependency
PKGMGR_SRCS = $(PKGMGR_CLASSES:%=pkgmgr/%.java)
PKGMGR_SRCS_OS = $(subst /,$(SL),$(PKGMGR_SRCS))
PKGMGR_CLASSFILES = $(PKGMGR_CLASSES:%=$(CLASSROOT)/$(PKGNAME)/pkgmgr/%.class)


# Runtime files
RUNTIME_CLASSES = \
		BlueJSecurityManager \
		BooleanResultWrapper \
		ByteResultWrapper \
		CharResultWrapper \
		DoubleResultWrapper \
		ExecServer \
		ExitException \
		FloatResultWrapper \
		IntResultWrapper \
		LongResultWrapper \
		ObjectResultWrapper \
		Shell \
		ShortResultWrapper \
		StringResultWrapper \
		TerminateException
RUNTIME_SRCS = $(RUNTIME_CLASSES:%=runtime/%.java)
RUNTIME_SRCS_OS = $(subst /,$(SL),$(RUNTIME_SRCS))
RUNTIME_CLASSFILES = $(RUNTIME_CLASSES:%=$(CLASSROOT)/$(PKGNAME)/runtime/%.class)


# Terminal files
TERMINAL_CLASSES = \
		Terminal \
		TerminalButtonModel \
		TerminalCanvas
TERMINAL_SRCS = $(TERMINAL_CLASSES:%=terminal/%.java)
TERMINAL_SRCS_OS = $(subst /,$(SL),$(TERMINAL_SRCS))
TERMINAL_CLASSFILES = $(TERMINAL_CLASSES:%=$(CLASSROOT)/$(PKGNAME)/terminal/%.class)


# Utility files
UTILITY_CLASSES = \
		AssertionViolationError \
		Comparer \
		Debug \
		FixedMultiLineLabel \
		ModelessMessageBox \
		MultiEnumeration \
		MultiLineLabel \
		Queue \
		SortableVector \
		ToggleMessageBox \
		ToggleMessageBoxOwner \
		Utility
UTILITY_SRCS = $(UTILITY_CLASSES:%=utility/%.java)
UTILITY_SRCS_OS = $(subst /,$(SL),$(UTILITY_SRCS))
UTILITY_CLASSFILES = $(UTILITY_CLASSES:%=$(CLASSROOT)/$(PKGNAME)/utility/%.class)


# "Views" files
VIEWS_CLASSES = \
		Comment \
		CommentList \
		ConstructorView \
		EditorPrintWriter \
		FieldView \
		FormattedPrintWriter \
		LabelPrintWriter \
		MemberView \
		MethodView \
		UnFormattedPrintWriter \
		View \
		ViewFilter
VIEWS_SRCS = $(VIEWS_CLASSES:%=views/%.java)
VIEWS_SRCS_OS = $(subst /,$(SL),$(VIEWS_SRCS))
VIEWS_CLASSFILES = $(VIEWS_CLASSES:%=$(CLASSROOT)/$(PKGNAME)/views/%.class)


SRCS_OS = \
	$(BLUEJ_SRCS_OS) \
	$(BROWSER_SRCS_OS) \
	$(CLASSMGR_SRCS_OS) \
	$(COMPILER_SRCS_OS) \
	$(DEBUGGER_SRCS_OS) \
	$(JDI_DEBUG_SRCS_OS) \
	$(EDITOR_SRCS_OS) \
	$(MOE_SRCS_OS) \
	$(GRAPH_SRCS_OS) \
	$(PARSER_SRCS_OS) \
	$(SYMTAB_SRCS_OS) \
	$(PKGMGR_SRCS_OS) \
	$(RUNTIME_SRCS_OS) \
	$(TERMINAL_SRCS_OS) \
	$(UTILITY_SRCS_OS) \
	$(VIEWS_SRCS_OS)

CLASSFILES = \
	$(BLUEJ_CLASSFILES) \
	$(BROWSER_CLASSFILES) \
	$(CLASSMGR_CLASSFILES) \
	$(COMPILER_CLASSFILES) \
	$(DEBUGGER_CLASSFILES) \
	$(JDI_DEBUG_CLASSFILES) \
	$(EDITOR_CLASSFILES) \
	$(MOE_CLASSFILES) \
	$(GRAPH_CLASSFILES) \
	$(PARSER_CLASSFILES) \
	$(SYMTAB_CLASSFILES) \
	$(PKGMGR_CLASSFILES) \
	$(RUNTIME_CLASSFILES) \
	$(TERMINAL_CLASSFILES) \
	$(UTILITY_CLASSFILES) \
	$(VIEWS_CLASSFILES)

PACKAGE_NAMES = \
	bluej \
	bluej.browser \
	bluej.classmgr \
	bluej.compiler \
	bluej.debugger \
	bluej.debugger.jdi \
	bluej.editor \
	bluej.editor.moe \
	bluej.graph \
	bluej.parser \
	bluej.parser.symtab \
	bluej.pkgmgr \
	bluej.runtime \
	bluej.terminal \
	bluej.utility \
	bluej.views

#
# include a dependency file if one exists
#
#ifeq (.depend,$(wildcard .depend))
#all: $(CLASSFILES)
#include .depend
#else
#all: dep
#endif

all allclasses::
	$(JAVAC) $(JAVAC_OPTS) -d ../../classes $(SRCS_OS)


moe: $(MOE_CLASSFILES)

ctxt:
	$(JBDOC) -d $(CTXTROOT) $(SRCS_OS)

doc:
	$(JAVADOC) $(JAVADOC_OPTS) -d $(DOC_DIR) $(PACKAGE_NAMES)

clean:
	$(RM) -r $(CLASSROOT)/$(PKGNAME)
