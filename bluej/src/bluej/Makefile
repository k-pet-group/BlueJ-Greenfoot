# Makefile for BlueJ
TOP = ../..
include $(TOP)/$(OS).mk

PKGNAME = bluej
CLASSROOT = $(subst /,$(SL),$(TOP)/classes)
SRCROOT = $(subst /,$(SL),$(TOP)/src)
CTXTROOT = $(TOP)/ctxt
XCLASSPATH = $(TOP)/lib/swing.jar;$$CLASSPATH
CLASSPATH = $(subst ;,$(SEP),$(CLASSROOT);$(subst /,$(SL),$(XCLASSPATH)))
LIB = $(TOP)/lib
JDEPEND = CLASSPATH="$(CLASSPATH)" jdepend
JAVAC = CLASSPATH="$(CLASSPATH)" javac
JAVAC_OPTS = -g -deprecation
#JAVAC_OPTS = -deprecation
# The BlueJ documentation generator (javadoc with right CLASSPATH + opts)
JBDOC = jbdoc

# Top level files
BLUEJ_CLASSES = \
		Config \
		BlueJEvent \
		BlueJEventListener \
		Main
BLUEJ_SRCS = $(BLUEJ_CLASSES:%=%.java)
BLUEJ_SRCS_OS = $(subst /,$(SL),$(BLUEJ_SRCS))
BLUEJ_CLASSFILES = $(BLUEJ_CLASSES:%=$(CLASSROOT)/$(PKGNAME)/%.class)

# Compiler files
COMPILER_CLASSES = \
		CompileObserver \
		Compiler \
		CompilerMessageError \
		CompilerThread \
		ErrorStream \
		JavacCompiler \
		Job \
		JobQueue \
		Queue
COMPILER_SRCS = $(COMPILER_CLASSES:%=compiler/%.java)
COMPILER_SRCS_OS = $(subst /,$(SL),$(COMPILER_SRCS))
COMPILER_CLASSFILES = $(COMPILER_CLASSES:%=$(CLASSROOT)/$(PKGNAME)/compiler/%.class)

JAVAC_CLASSES = \
		BlueJJavacMain \
		JavacWatcher
JAVAC_SRCS = $(JAVAC_CLASSES:%=$(SRCROOT)/sun/tools/javac/%.java)
JAVAC_SRCS_OS = $(subst /,$(SL),$(JAVAC_SRCS))
JAVAC_CLASSFILES = $(JAVAC_CLASSES:%=$(CLASSROOT)/sun/tools/javac/%.class)

JAVADOC_CLASSES = \
		BlueJDocumentationGenerator
JAVADOC_SRCS = $(JAVADOC_CLASSES:%=$(SRCROOT)/sun/tools/javadoc/%.java)
JAVADOC_SRCS_OS = $(subst /,$(SL),$(JAVADOC_SRCS))
JAVADOC_CLASSFILES = $(JAVADOC_CLASSES:%=$(CLASSROOT)/sun/tools/javadoc/%.class)

# Debugger files
DEBUGGER_CLASSES = \
		Debugger \
		DebuggerClassLoader \
		DebuggerObject \
		DebuggerThread \
		DebuggerValue \
		DebuggerWatcher \
		ExecControls \
		Invoker \
		LocalDebugger \
		MachineLoader \
		MethodDialog \
		MethodDialogWatcher \
		ObjectBench \
		ObjectBenchWatcher \
		ObjectViewer \
		ObjectWrapper \
		ResultWatcher \
		SunClassLoader \
		SunDebugger \
		SunObject \
		SunThread \
		SunValue \
		CallHistory
DEBUGGER_SRCS = $(DEBUGGER_CLASSES:%=debugger/%.java)
DEBUGGER_SRCS_OS = $(subst /,$(SL),$(DEBUGGER_SRCS))
DEBUGGER_CLASSFILES = $(DEBUGGER_CLASSES:%=$(CLASSROOT)/$(PKGNAME)/debugger/%.class)

SUN_DEBUG_CLASSES = \
	SunHooks
SUN_DEBUG_SRCS = $(SUN_DEBUG_CLASSES:%=$(SRCROOT)/sun/tools/debug/%.java)
SUN_DEBUG_SRCS_OS = $(subst /,$(SL),$(SUN_DEBUG_SRCS))
SUN_DEBUG_CLASSFILES = $(SUN_DEBUG_CLASSES:%=$(CLASSROOT)/sun/tools/debug/%.class)

# Browser files
BROWSER_CLASSES = \
		AddLibraryDialog \
		AttributeChooser \
		AttributeChooserRenderer \
		ChooseUseDestinationDIalog \
		CodeViewer \
		FindLibraryDialog \
		LibraryChooser \
		LibraryFileFilter \
		LibraryNode \
		LibraryPopupMenu \
		LibraryTreeCellRenderer \
		SystemLibraryNode \
		SystemLibraryPopupMenu \
		UserLibraryNode \
		UserLibraryPopupMenu

BROWSER_SRCS = $(BROWSER_CLASSES:%=browser/%.java)
BROWSER_SRCS_OS = $(subst /,$(SL),$(BROWSER_SRCS))
BROWSER_CLASSFILES = $(BROWSER_CLASSES:%=$(CLASSROOT)/$(PKGNAME)/browser/%.class)

# RED_CLASSES = \
# 		Action \
# 		ActionStack \
# 		Buffer \
# 		BufferPos \
# 		Function \
# 		Info \
# 		Keymap \
# 		Line \
# 		LineFragment \
# 		Messages \
# 		MultiLineLabel \
# 		PrefDialog \
# 		Question \
# 		QuestionDialog \
# 		Red \
# 		RedEditor \
# 		RedEditorManager \
# 		RedVersion \
# 		Screen \
# 		Status \
# 		TextRegion \
# 		UserFunc \
# 		UserFuncID
# RED_SRCS = $(RED_CLASSES:%=editor/red/%.java)
# RED_SRCS_OS = $(subst /,$(SL),$(RED_SRCS))
# RED_CLASSFILES = $(RED_CLASSES:%=$(CLASSROOT)/$(PKGNAME)/editor/red/%.class)

EDITOR_CLASSES = \
		Editor \
		EditorManager \
		EditorWatcher

EDITOR_SRCS = $(EDITOR_CLASSES:%=editor/%.java)
EDITOR_SRCS_OS = $(subst /,$(SL),$(EDITOR_SRCS))
EDITOR_CLASSFILES = $(EDITOR_CLASSES:%=$(CLASSROOT)/$(PKGNAME)/editor/%.class)

MOE_CLASSES = \
		Finder \
		FunctionDialog \
		Info \
		LineNumberLabel \
		MoeActions \
		MoeCaret \
		MoeEditor \
		MoeEditorKit \
		MoeEditorManager \
		MoeParagraphView \
		StatusLabel

MOE_SRCS = $(MOE_CLASSES:%=editor/moe/%.java)
MOE_SRCS_OS = $(subst /,$(SL),$(MOE_SRCS))
MOE_CLASSFILES = $(MOE_CLASSES:%=$(CLASSROOT)/$(PKGNAME)/editor/moe/%.class)

# Graph files
GRAPH_CLASSES = \
		Edge \
		Graph \
		GraphEditor \
		Main \
		TestEdge \
		TestGraph \
		TestVertex \
		Vertex
GRAPH_SRCS = $(GRAPH_CLASSES:%=graph/%.java)
GRAPH_SRCS_OS = $(subst /,$(SL),$(GRAPH_SRCS))
GRAPH_CLASSFILES = $(GRAPH_CLASSES:%=$(CLASSROOT)/$(PKGNAME)/graph/%.class)

# Package manager files
PKGMGR_CLASSES = \
		AboutBlue \
		ClassTarget \
		Dependency \
		EditableTarget \
		ExtendsDependency \
		ImplementsDependency \
		ImportedClassTarget \
		LayoutComparer \
		LibraryBrowserPkgMgrFrame \
		Main \
		NewClassDialog \
		Package \
		PackageTarget \
		PkgFrame \
		PkgMgrFrame \
		PackageCacheMgr \
		Target \
		UsesDependency \
		JBPackageChooser \
		JBPackageFileView
PKGMGR_SRCS = $(PKGMGR_CLASSES:%=pkgmgr/%.java)
PKGMGR_SRCS_OS = $(subst /,$(SL),$(PKGMGR_SRCS))
PKGMGR_CLASSFILES = $(PKGMGR_CLASSES:%=$(CLASSROOT)/$(PKGNAME)/pkgmgr/%.class)

RUNTIME_CLASSES = \
		BlueJClassLoader \
		BlueJRuntime \
		BlueJSecurityManager \
		BooleanResultWrapper \
		ByteResultWrapper \
		CharResultWrapper \
		DoubleResultWrapper \
		FloatResultWrapper \
		IntResultWrapper \
		LongResultWrapper \
		ObjectResultWrapper \
		Shell \
		ShortResultWrapper \
		StringResultWrapper \
		Terminal \
		TerminalCanvas \
		TerminalFrame
RUNTIME_SRCS = $(RUNTIME_CLASSES:%=runtime/%.java)
RUNTIME_SRCS_OS = $(subst /,$(SL),$(RUNTIME_SRCS))
RUNTIME_CLASSFILES = $(RUNTIME_CLASSES:%=$(CLASSROOT)/$(PKGNAME)/runtime/%.class)

# Utility files
UTILITY_CLASSES = \
		AssertionViolationError \
		ClasspathSearcher \
		Comparer \
		Debug \
		FixedMultiLineLabel \
		MultiEnumeration \
		ModelessMessageBox \
		ToggleMessageBox \
		ToggleMessageBoxOwner \
		MultiLineLabel \
		OkayDialog \
		PackageFileFilter \
		SimpleClassLoader \
		SortableVector \
		Utility
UTILITY_SRCS = $(UTILITY_CLASSES:%=utility/%.java)
UTILITY_SRCS_OS = $(subst /,$(SL),$(UTILITY_SRCS))
UTILITY_CLASSFILES = $(UTILITY_CLASSES:%=$(CLASSROOT)/$(PKGNAME)/utility/%.class)

# "Views" files
VIEWS_CLASSES = \
		Comment \
		CommentList \
		ConstructorView \
		FieldView \
		EditorPrintWriter \
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
	$(COMPILER_SRCS_OS) \
	$(DEBUGGER_SRCS_OS) \
	$(SUN_DEBUG_SRCS_OS) \
	$(EDITOR_SRCS_OS) \
	$(GRAPH_SRCS_OS) \
	$(BLUEJ_SRCS_OS) \
	$(BROWSER_SRCS_OS) \
	$(JAVAC_SRCS_OS) \
	$(JAVADOC_SRCS_OS) \
	$(PKGMGR_SRCS_OS) \
	$(MOE_SRCS_OS) \
	$(RUNTIME_SRCS_OS) \
	$(UTILITY_SRCS_OS) \
	$(VIEWS_SRCS_OS)

CLASSFILES = \
	$(COMPILER_CLASSFILES) \
	$(DEBUGGER_CLASSFILES) \
	$(SUN_DEBUG_CLASSFILES) \
	$(EDITOR_CLASSFILES) \
	$(GRAPH_CLASSFILES) \
	$(BLUEJ_CLASSFILES) \
	$(BROWSER_CLASSFILES) \
	$(JAVAC_CLASSFILES) \
	$(JAVADOC_CLASSFILES) \
	$(PKGMGR_CLASSFILES) \
	$(MOE_CLASSFILES) \
	$(RUNTIME_CLASSFILES) \
	$(UTILITY_CLASSFILES) \
	$(VIEWS_CLASSFILES)

#
# include a dependency file if one exists
#
ifeq (.depend,$(wildcard .depend))
all: $(CLASSFILES)
include .depend
else
all: dep
endif

moe: $(MOE_CLASSFILES)

ctxt:
	$(JBDOC) -d $(CTXTROOT) $(SRCS_OS)

.depend depend dep:
	$(JDEPEND) -d $(CLASSROOT) $(JAVAC_OPTS) $(SRCS_OS) > .depend \
	|| $(JDEPEND) -d $(CLASSROOT) $(JAVAC_OPTS) $(SRCS_OS) > .depend 

dep1:
	$(JDEPEND) -d $(CLASSROOT) $(JAVAC_OPTS) $(SRCS_OS) > .depend

clean:
	$(RM) -r $(CLASSROOT)/$(PKGNAME)
	$(RM) -r $(JAVAC_CLASSFILES) $(JAVADOC_CLASSFILES)

clobber: clean
	$(RM) .depend











