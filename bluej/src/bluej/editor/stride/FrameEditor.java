/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015 Michael Kölling and John Rosenberg 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */

package bluej.editor.stride;


import java.awt.print.PrinterJob;
import java.io.*;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.Tab;

import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;

import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Serializer;
import threadchecker.OnThread;
import threadchecker.Tag;
import bluej.compiler.Diagnostic;
import bluej.debugger.DebuggerField;
import bluej.debugger.DebuggerObject;
import bluej.debugger.DebuggerThread;
import bluej.debugger.DebuggerThreadTreeModel;
import bluej.debugger.gentype.GenTypeClass;
import bluej.editor.Editor;
import bluej.editor.EditorWatcher;
import bluej.editor.TextEditor;
import bluej.parser.AssistContent;
import bluej.parser.AssistContent.CompletionKind;
import bluej.parser.CodeSuggestions;
import bluej.parser.ParseUtils;
import bluej.parser.PrefixCompletionWrapper;
import bluej.parser.SourceLocation;
import bluej.parser.entity.EntityResolver;
import bluej.parser.nodes.ParsedCUNode;
import bluej.parser.symtab.ClassInfo;
import bluej.pkgmgr.JavadocResolver;
import bluej.stride.framedjava.ast.ASTUtility;
import bluej.stride.framedjava.ast.HighlightedBreakpoint;
import bluej.stride.framedjava.ast.JavaFragment.PosInSourceDoc;
import bluej.stride.framedjava.ast.JavaSource;
import bluej.stride.framedjava.ast.Loader;
import bluej.stride.framedjava.elements.CallElement;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.framedjava.elements.CodeElement.LocalParamInfo;
import bluej.stride.framedjava.elements.NormalMethodElement;
import bluej.stride.framedjava.elements.TopLevelCodeElement;
import bluej.stride.framedjava.errors.CodeError;
import bluej.stride.framedjava.frames.DebugInfo;
import bluej.stride.framedjava.frames.DebugVarInfo;
import bluej.stride.framedjava.frames.LocalCompletion;
import bluej.stride.framedjava.frames.LocalTypeCompletion;
import bluej.stride.framedjava.frames.PrimitiveDebugVarInfo;
import bluej.stride.framedjava.frames.ReferenceDebugVarInfo;
import bluej.stride.framedjava.slots.ExpressionSlot;
import bluej.stride.generic.AssistContentThreadSafe;
import bluej.stride.generic.InteractionManager.Kind;
import bluej.utility.Debug;
import bluej.utility.JavaReflective;
import bluej.utility.javafx.JavaFXUtil;

/**
 * FrameEditor implements Editor, the interface to the rest of BlueJ that existed
 * before our frame editor was created.  Because of this, not all of the methods make
 * sense -- see comments throughout the class.
 *
 * Most of the major functionality is actually in FrameEditorTab; this FrameEditor class
 * really just exists to integrate the editor into BlueJ.  Also, FrameEditor can exist without
 * the graphical editor being opened, whereas FrameEditorTab is tied to the graphical aspect.
 */
@OnThread(Tag.Swing)
public class FrameEditor implements Editor
{
    /**
     * Whether the code has been successfully compiled since last edit
     */
    private boolean isCompiled;
    // If the code has been changed since last save (only modify on FX thread):
    // Start true, because we haven't actually saved before, so technically we have changed:
    private boolean changedSinceLastSave = true;
    /**
     * Location of the .stride file
     */
    private File frameFilename;
    private File javaFilename;
    private final FXTabbedEditor fXTabbedEditor;
    private final EntityResolver resolver;
    private final EditorWatcher watcher;
    private final JavadocResolver javadocResolver;
    /**
     * Set to the latest version of the JavaSource.  null if the editor has not yet been opened;
     * you can observe it to see when it becomes non-null if you want to do something when
     * the editor opens.
     */
    private final SimpleObjectProperty<JavaSource> javaSource;
    private bluej.pkgmgr.Package pkg;
    private FrameEditorTab panel;
    /**
     * Whether the editor (this.panel) is open.  Set from FX thread but read from
     * Swing thread; not ideal.
     */
    private final AtomicBoolean panelOpen;
    private final DebugInfo debugInfo = new DebugInfo();
    private HighlightedBreakpoint curBreakpoint;
    private TopLevelCodeElement lastSource;
    /**
     * Errors from compilation to be shown once the editor is opened
     * (and thus we don't have to recompile just because the editor opens)
     */
    private final List<QueuedError> queuedErrors = new ArrayList<>();

    /**
     * A javac compile error.
     */
    @OnThread(Tag.Any)
    private static class QueuedError
    {
        private final long startLine, startColumn, endLine, endColumn;
        private final String message;

        private QueuedError(long startLine, long startColumn, long endLine, long endColumn, String message)
        {
            this.startLine = startLine;
            this.startColumn = startColumn;
            this.endLine = endLine;
            this.endColumn = endColumn;
            this.message = message;
        }
    }

    @OnThread(Tag.FX)
    public FrameEditor(FXTabbedEditor fXTabbedEditor, File frameFilename, File javaFilename, EditorWatcher watcher, EntityResolver resolver, JavadocResolver javadocResolver, bluej.pkgmgr.Package pkg)
    {
        this.frameFilename = frameFilename;
        this.javaFilename = javaFilename;
        this.watcher = watcher;
        this.fXTabbedEditor = fXTabbedEditor;
        this.resolver = resolver;
        this.javadocResolver = javadocResolver;
        this.pkg = pkg;
        this.javaSource = new SimpleObjectProperty<>();
        lastSource = Loader.loadTopLevelElement(frameFilename, resolver);
        panelOpen = new AtomicBoolean();
        ObservableList<Tab> tabs = fXTabbedEditor.tabsProperty();
        tabs.addListener((ListChangeListener<Tab>)c -> panelOpen.set(Boolean.valueOf(tabs.contains(panel))));
    }
    
    @OnThread(Tag.FX)
    private void createPanel(boolean visible, boolean toFront)
    {
        //Debug.message("&&&&&& Creating panel: " + System.currentTimeMillis());
        this.panel = new FrameEditorTab(fXTabbedEditor, resolver, this, lastSource);
        //Debug.message("&&&&&& Adding panel to editor: " + System.currentTimeMillis());
        fXTabbedEditor.addFrameEditor(this.panel, visible, toFront);
        //Debug.message("&&&&&& Done! " + System.currentTimeMillis());
        // Saving Java will trigger any pending actions like jumping to a stack trace location:
        panel.initialisedProperty().addListener((a, b, newVal) -> {
            if (newVal.booleanValue())
            {
                // runLater so that the panel will have been added:
                Platform.runLater(() -> {
                    _saveFX();
                    findLateErrors();
                });

            }
        });
    }

    // Editor methods:

    @Override
    public void close()
    {
        Platform.runLater(() -> {
            if (panel != null)
            {
                lastSource = panel.getSource();
                panel.setWindowVisible(false, false);
            }
        });
    }

    /**
     * Saves the code.  Because this comes from Editor, it must be done on the Swing thread,
     * and is assumed to only return after saving.
     *
     * But because the frame editor is in FX, we must trigger the save on  the FX thread and wait
     * for it.  Thus it's important that nothing ever runs on the FX thread which waits for the Swing
     * thread, because it could deadlock with this code.
     */
    @Override
    @OnThread(Tag.Swing)
    public void save() throws IOException
    {
        final CompletableFuture<Optional<IOException>> q = new CompletableFuture<>();
        Platform.runLater(() -> { q.complete(Optional.ofNullable(_saveFX()));});
        Optional<IOException> e = null;
        try
        {
            e = q.get();
        }
        catch (InterruptedException | ExecutionException e1)
        {
            Debug.reportError(e1);
        }
        if (e.isPresent())
            throw new IOException(e.get());
        
        setSaved();
    }
    
    /**
     * Set the saved/changed status of this buffer to SAVED.
     */
    private void setSaved()
    {
        if (watcher != null) {
            watcher.saveEvent(this);
        }
    }

    /**
     * Saves the code on the FX thread.  If any IOException occurs, it is caught and returned
     * (so it can be re-thrown on the Swing thread).  If all is well, null is returned.
     */
    @OnThread(Tag.FX)
    // Returns null if no exception
    private IOException _saveFX()
    {
        if (!changedSinceLastSave)
            return null;

        try
        {
            // If frame editor is closed, we just need to write the Java code
            if (panel == null || panel.getSource() == null)
            {
                saveJava(lastSource, true);
                return null;
            }
            
            panel.regenerateAndReparse(null);
            TopLevelCodeElement source = panel.getSource();
            
            if (source == null)
                return null; // classFrame not initialised yet

            // Save Frame source:
            FileOutputStream os = new FileOutputStream(frameFilename);
            Serializer s = new Serializer(os);
            s.setLineSeparator("\n");
            s.setIndent(4);
            Element saveEl = source.toXML();
            saveEl.addNamespaceDeclaration("xml", "http://www.w3.org/XML/1998/namespace");
            s.write(new Document(saveEl));
            s.flush();
            os.close();

            saveJava(panel.getSource(), true);
            changedSinceLastSave = false;
        
            panel.saved();
            return null;
        }
        catch (IOException e)
        {
            return e;
        }
    }

    /**
     * Saves the .java file without the "warning: auto-generated" text at the top
     */
    @OnThread(Tag.Swing)
    public void saveJavaWithoutWarning() throws IOException
    {
        final CompletableFuture<Optional<IOException>> q = new CompletableFuture<>();
        Platform.runLater(() -> {
            try
            {
                saveJava(lastSource, false);
                q.complete(Optional.empty());
            } catch (IOException e)
            {
                q.complete(Optional.of(e));
            }
        });
        Optional<IOException> e = null;
        try
        {
            e = q.get();
        }
        catch (InterruptedException | ExecutionException e1)
        {
            Debug.reportError(e1);
        }
        if (e.isPresent())
            throw new IOException(e.get());
    }

    /**
     * @param warning Whether to include the "auto-generated" warning at the top of the file
     */
    @OnThread(Tag.FX)
    private void saveJava(TopLevelCodeElement source, boolean warning) throws IOException
    {
        if (source == null)
            return; // Not fully loaded yet

        FileOutputStream fos = new FileOutputStream(javaFilename);
        OutputStreamWriter w = new OutputStreamWriter(fos, Charset.forName("UTF-8"));
        final JavaSource js = source.toJavaSource(warning);
        String javaString = js.toDiskJavaCodeString();
        w.write(javaString);
        w.close();
        fos.close();
        // Because there may be a listener waiting on javaSource in order to show compiler error,
        // it's important that we first generate the string above, before storing it into the property,
        // to make sure all the source positions have been recorded.
        javaSource.set(js);
    }

    /**
     * Eugh.
     *
     * So: some uses of Editor in the BlueJ code (which predate frames) assume that the program
     * is written in text, in order to modify some aspect.  That doesn't apply well to frames, so
     * really any use of this assumeText() method on a frame editor is wrong.  Ideally, this method
     * should return null or throw an exception (and callers of assumeText adjusted accordingly),
     * but for now it returns a delegate/proxy object which throws exceptions on invalid operations.
     */
    @Override
    public TextEditor assumeText() {
        // I want to annotate this whole class as @OnThread(Tag.Swing) 
        // but on 8u20 it triggers JDK bug JDK-8059531
        // Instead we must annotate each method
        return new TextEditor() {

            @Override
            @OnThread(Tag.Swing)
            public void writeMessage(String msg) { FrameEditor.this.writeMessage(msg); }

            @Override
            @OnThread(Tag.Swing)
            public void showInterface(boolean interfaceStatus) { FrameEditor.this.showInterface(interfaceStatus); }

            @Override
            @OnThread(Tag.Swing)
            public void setVisible(boolean vis) { FrameEditor.this.setVisible(vis); }

            @Override
            @OnThread(Tag.Swing)
            public void setReadOnly(boolean readOnly) { FrameEditor.this.setReadOnly(readOnly); }

            @Override
            @OnThread(Tag.Swing)
            public void setProperty(String propertyKey, Object value) { FrameEditor.this.setProperty(propertyKey, value); }

            @Override
            @OnThread(Tag.Swing)
            public void setCompiled(boolean compiled) { FrameEditor.this.setCompiled(compiled); }

            @Override
            @OnThread(Tag.Swing)
            public void save() throws IOException { FrameEditor.this.save(); }

            @Override
            @OnThread(Tag.Swing)
            public void removeStepMark() { FrameEditor.this.removeStepMark(); }

            @Override
            @OnThread(Tag.Swing)
            public void removeBreakpoints() { FrameEditor.this.removeBreakpoints(); }

            @Override
            @OnThread(Tag.Swing)
            public void reloadFile() { FrameEditor.this.reloadFile(); }

            @Override
            @OnThread(Tag.Swing)
            public void refresh() { FrameEditor.this.refresh(); }

            @Override
            @OnThread(Tag.Swing)
            public void reInitBreakpoints() { FrameEditor.this.reInitBreakpoints(); }

            @Override
            @OnThread(Tag.Any)
            public void printTo(PrinterJob printerJob, boolean printLineNumbers, boolean printBackground) { FrameEditor.this.printTo(printerJob, printLineNumbers, printBackground); }

            @Override
            @OnThread(Tag.Swing)
            public boolean isShowingInterface() { return FrameEditor.this.isShowingInterface(); }

            @Override
            @OnThread(Tag.Swing)
            public boolean isOpen() { return FrameEditor.this.isOpen(); }

            @Override
            @OnThread(Tag.Swing)
            public boolean isReadOnly() { return FrameEditor.this.isReadOnly(); }

            @Override
            @OnThread(Tag.Swing)
            public boolean isModified() { return FrameEditor.this.isModified(); }

            @Override
            @OnThread(Tag.Swing)
            public Object getProperty(String propertyKey) { return FrameEditor.this.getProperty(propertyKey); }

            @Override
            @OnThread(Tag.Swing)
            public void displayMessage(String message, int lineNumber, int column,
                    boolean beep, boolean setStepMark, String help) { FrameEditor.this.displayMessage(message, lineNumber, column, beep, setStepMark, help); }

            @Override
            @OnThread(Tag.Swing)
            public boolean displayDiagnostic(Diagnostic diagnostic, int errorIndex)
            {
                return FrameEditor.this.displayDiagnostic(diagnostic, errorIndex);
            }

            @Override
            @OnThread(Tag.Swing)
            public void close() { FrameEditor.this.close(); }

            @Override
            @OnThread(Tag.Swing)
            public void changeName(String title, String filename, String javaFilename, String docFileName) { FrameEditor.this.changeName(title, filename, javaFilename, docFileName); }
            
            @Override
            @OnThread(Tag.Swing)
            public TextEditor assumeText() { return this; }

            @Override
            @OnThread(Tag.Swing)
            public boolean showFile(String filename, Charset charset, boolean compiled,
                    String docFilename) {
                throw new UnsupportedOperationException();
            }

            @Override
            @OnThread(Tag.Swing)
            public void setText(SourceLocation begin, SourceLocation end, String newText)
                    throws BadLocationException {
                throw new UnsupportedOperationException();

            }

            @Override
            @OnThread(Tag.Swing)
            public void setSelection(int firstlineNumber, int firstColumn,
                    int secondLineNumber, int SecondColumn) {
                throw new UnsupportedOperationException();

            }

            @Override
            @OnThread(Tag.Swing)
            public void setSelection(SourceLocation begin, SourceLocation end) {
                throw new UnsupportedOperationException();

            }

            @Override
            @OnThread(Tag.Swing)
            public void setSelection(int lineNumber, int column, int len) {
                throw new UnsupportedOperationException();

            }

            @Override
            @OnThread(Tag.Swing)
            public void setCaretLocation(SourceLocation location) {
                throw new UnsupportedOperationException();

            }

            @Override
            @OnThread(Tag.Swing)
            public int numberOfLines() {
                throw new UnsupportedOperationException();
            }

            @Override
            @OnThread(Tag.Swing)
            public void insertText(String text, boolean caretBack) {
                throw new UnsupportedOperationException();

            }

            @Override
            @OnThread(Tag.Swing)
            public int getTextLength() {
                throw new UnsupportedOperationException();
            }

            @Override
            @OnThread(Tag.Swing)
            public String getText(SourceLocation begin, SourceLocation end) {
                throw new UnsupportedOperationException();
            }

            @Override
            @OnThread(Tag.Swing)
            public javax.swing.text.Document getSourceDocument() {
                throw new UnsupportedOperationException();
            }

            @Override
            @OnThread(Tag.Swing)
            public SourceLocation getSelectionEnd() {
                throw new UnsupportedOperationException();
            }

            @Override
            @OnThread(Tag.Swing)
            public SourceLocation getSelectionBegin() {
                throw new UnsupportedOperationException();
            }

            @Override
            @OnThread(Tag.Swing)
            public ParsedCUNode getParsedNode() {
                return null; //throw new UnsupportedOperationException();
            }

            @Override
            @OnThread(Tag.Swing)
            public int getOffsetFromLineColumn(SourceLocation location) {
                throw new UnsupportedOperationException();
            }

            @Override
            @OnThread(Tag.Swing)
            public int getLineLength(int line) {
                throw new UnsupportedOperationException();
            }

            @Override
            @OnThread(Tag.Swing)
            public SourceLocation getLineColumnFromOffset(int offset) {
                throw new UnsupportedOperationException();
            }

            @Override
            @OnThread(Tag.Swing)
            public SourceLocation getCaretLocation() {
                throw new UnsupportedOperationException();
            }

            @Override
            @OnThread(Tag.Swing)
            public void clear() {
                throw new UnsupportedOperationException();

            }

            @Override
            @OnThread(Tag.Swing)
            public void compileFinished(boolean successful)
            {
                throw new UnsupportedOperationException();                
            }

            @Override
            @OnThread(Tag.Swing)
            public void insertAppendMethod(bluej.extensions.editor.Editor e, NormalMethodElement method, Consumer<Boolean> after)
            {
                FrameEditor.this.insertAppendMethod(e, method, after);
            }

            @Override
            @OnThread(Tag.Swing)
            public void insertMethodCallInConstructor(bluej.extensions.editor.Editor e, String className, CallElement methodName, Consumer<Boolean> after)
            {
                FrameEditor.this.insertMethodCallInConstructor(e, className, methodName, after);
            }

            @Override
            @OnThread(Tag.FX)
            public FrameEditor assumeFrame()
            {
                return FrameEditor.this;
            }

            @Override
            @OnThread(Tag.Swing)
            public boolean compileStarted()
            {
                return FrameEditor.this.compileStarted();    
            }
            
            @Override
            @OnThread(Tag.Swing)
            public void showNextError()
            {
                FrameEditor.this.showNextError();
            }

            @Override
            @OnThread(Tag.Swing)
            public void cancelFreshState()
            {
                FrameEditor.this.cancelFreshState();
            }

            @Override
            @OnThread(Tag.Swing)
            public void focusMethod(String methodName)
            {
                FrameEditor.this.focusMethod(methodName);
            }
        };
    }


    @Override
    public void reloadFile()
    {
        // TODO Auto-generated method stub
    }

    @Override
    public void refresh()
    {
        // TODO Auto-generated method stub
    }

    @Override
    public void displayMessage(String message, final int lineNumber, int column,
            boolean beep, boolean setStepMark, String help) {
        if (setStepMark) {
            Platform.runLater(() -> {
                setVisibleFX(true, true);
                SwingUtilities.invokeLater(() -> {
                    Debug.message("Hit breakpoint in EditorFrame");
                    //TODO: get thread model, find simulation thread, find top of stack, show locals/members
                    DebuggerThread t = getSimulationThread();
                    HashMap<String, DebugVarInfo> vars = new HashMap<String, DebugVarInfo>();
                    if (t != null) {
                        DebuggerObject currentObject = t.getCurrentObject(0);
                        if (currentObject != null && !currentObject.isNullObject()) {
                            Map<String, Set<String>> restrictedClasses = pkg.getProject().getExecControls().getRestrictedClasses();
                            List<DebuggerField> fields = currentObject.getFields();
                            for (DebuggerField field : fields)
                            {
                                if (! Modifier.isStatic(field.getModifiers())) {
                                    String declaringClass = field.getDeclaringClassName();
                                    Set<String> whiteList = restrictedClasses.get(declaringClass);
                                    if (whiteList == null || whiteList.contains(field.getName())) {
                                        if (field.isReferenceType()) {
                                            vars.put(field.getName(), new ReferenceDebugVarInfo(pkg, null, field));
                                        }
                                        else {
                                            vars.put(field.getName(), new PrimitiveDebugVarInfo(field.getValueString()));
                                        }
                                    }
                                }
                            }
                        }
                    }
                    debugInfo.addVarState(vars);
                    Platform.runLater(() -> {
                        if (curBreakpoint != null) {
                            curBreakpoint.removeHighlight();
                        }
                        curBreakpoint = javaSource.get().handleStop(lineNumber, debugInfo);
                    });
                });});
        }
        else
        {
            //This is a message from a clickable stack trace following an exception
            Debug.message("Will select: " + lineNumber);
            Platform.runLater(() -> JavaFXUtil.onceNotNull(javaSource, js -> js.handleException(lineNumber)));
        }
    }

    private DebuggerThread findThread(DebuggerThreadTreeModel model, Object t, String targetName)
    {
        DebuggerThread thread = model.getNodeAsDebuggerThread(t);
        if (thread != null && targetName.equals(thread.getName())) {
            return thread;
        }
        for (int i = 0; i < model.getChildCount(t); i++)
        {
            DebuggerThread child = findThread(model, model.getChild(t, i), targetName);
            if (child != null) {
                return child;
            }
        }
        return null;
    }

    @SuppressWarnings("unused")
    private void printAndChildren(DebuggerThreadTreeModel model, Object t)
    {
        Debug.message("Thread: " + t.toString());
        for (int i = 0; i < model.getChildCount(t); i++) {
            printAndChildren(model, model.getChild(t, i));
        }
    }

    @Override
    public boolean displayDiagnostic(final Diagnostic diagnostic, int errorIndex)
    {
        // We are on the compile thread, but need to do GUI bits on the FX thread:
        Platform.runLater(() -> {
            // Don't show javac errors if we are not valid for compilation:
            if (panel != null && panel.getSource() != null)
            {
                panel.setWindowVisible(true, false);

                JavaFXUtil.onceNotNull(javaSource, js ->
                        js.handleError((int) diagnostic.getStartLine(), (int) diagnostic.getStartColumn(),
                            (int) diagnostic.getEndLine(), (int) diagnostic.getEndColumn(), diagnostic.getMessage(), true)
                );
            }
            else
            {
                queuedErrors.add(new QueuedError(diagnostic.getStartLine(), diagnostic.getStartColumn(), diagnostic.getEndLine(), diagnostic.getEndColumn(), diagnostic.getMessage()));

            }
        });
        return true;
    }

    @Override
    public void writeMessage(String msg)
    {
      // Not needed yet.
    }

    @Override
    public void removeStepMark()
    {
        // TODO Auto-generated method stub
    }

    @Override
    public void changeName(String title, String filename, String javaFilename, String docFileName)
    {
        this.frameFilename = new File(filename);
        this.javaFilename = new File(javaFilename);
    }

    @Override
    public void setCompiled(boolean compiled)
    {
        isCompiled = compiled;
    }

    @Override
    public void removeBreakpoints() 
    {
        // TODO Auto-generated method stub
    }

    @Override
    public void reInitBreakpoints()
    {
        watcher.clearAllBreakpoints();
        if (javaSource == null) {
            try {
                save();
            }
            catch (IOException e) {
                Debug.reportError(e);
            }
        }
        if (javaSource.get() != null)
        {
            javaSource.get().registerBreakpoints(this, watcher);
        }
    }

    @Override
    public boolean isModified() 
    {
        return !isCompiled;
    }

    @Override
    @OnThread(Tag.Any)
    public void printTo(PrinterJob printerJob, boolean printLineNumbers, boolean printBackground) 
    {
        // TODO Auto-generated method stub
    }

    @Override
    public void setReadOnly(boolean readOnly) 
    {
        // TODO Auto-generated method stub
    }

    @Override
    public boolean isReadOnly() 
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void showInterface(boolean interfaceStatus) 
    {
        // No need to do anything here
        // panel.showWindow();
    }

    @Override
    public boolean isShowingInterface() 
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Object getProperty(String propertyKey) 
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setProperty(String propertyKey, Object value) 
    {
        // TODO Auto-generated method stub
    }

    @Override
    public void setVisible(boolean vis)
    {
        Platform.runLater(() -> setVisibleFX(vis, true));
    }

    @OnThread(Tag.FX)
    private void setVisibleFX(boolean show, boolean bringToFront)
    {
        if (panel == null && show) // No need to create the panel if we don't want to show it
        {
            createPanel(show, bringToFront);
        }

        if (panel != null)
        {
            panel.setWindowVisible(show, bringToFront);
        }


        if (show)
        {
            Platform.runLater(() -> {
                if (!queuedErrors.isEmpty())
                {
                    final ArrayList<QueuedError> queueCopy = new ArrayList<>(queuedErrors);
                    queuedErrors.clear();
                    JavaFXUtil.onceNotNull(javaSource, js -> {
                        for (QueuedError e : queueCopy)
                        {
                            if (!js.handleError((int) e.startLine, (int) e.startColumn,
                                    (int) e.endLine, (int) e.endColumn, e.message, false))
                            {
                                Debug.message("Retrying showing error after saving");
                                Platform.runLater(() -> {
                                    if (_saveFX() == null)
                                    {
                                        boolean result = javaSource.get().handleError((int) e.startLine, (int) e.startColumn, (int) e.endLine, (int) e.endColumn, e.message, true);
                                        Debug.message("Retrying: " + (result ? "success" : "failure"));
                                    }
                                });
                            }
                        }
                        // Eugh.  We need one runLater to account for the fact that adding errors uses a runLater,
                        // and a second to be after any retries above:
                        Platform.runLater(() -> Platform.runLater(() -> panel.updateErrorOverviewBar(false)));
                    });
                }
            });
        }
    }

    @OnThread(Tag.FX)
    public void codeModified()
    {
        isCompiled = false;
        changedSinceLastSave = true;
        SwingUtilities.invokeLater(() -> watcher.modificationEvent(this));
    }
    
    @Override
    @OnThread(Tag.FX)
    public FrameEditor assumeFrame()
    {
        return this;
    }

    @Override
    public boolean isOpen()
    {
        return panelOpen.get();
    }

    public void step()
    {
        DebuggerThread t = getSimulationThread();
        t.step();
    }


    private DebuggerThread getSimulationThread()
    {
        DebuggerThreadTreeModel model = pkg.getDebugger().getThreadTreeModel();
        DebuggerThread t = findThread(model, model.getRoot(), "SimulationThread"); //TODO this makes it Greenfoot specific
        return t;
    }


    public void cont()
    {
        DebuggerThread t = getSimulationThread();
        t.cont();        
    }


    @Override
    public void compileFinished(boolean successful)
    {
        if (panelOpen.get())
        {
            findLateErrors();
            Platform.runLater(() -> panel.compiled());
            reInitBreakpoints();
        }
    }

    @OnThread(Tag.Any)
    private void findLateErrors()
    {
        Platform.runLater(() -> {
            panel.removeOldErrors();
            TopLevelCodeElement el = panel.getSource();
            Stream<CodeElement> allElements = Stream.concat(Stream.of((CodeElement)el), el.streamContained());
            // We must start these futures going on the FX thread
            List<Future<List<CodeError>>> futures = allElements.flatMap(e -> e.findDirectLateErrors(panel)).collect(Collectors.toList());
            // Then wait for them on another thread, and hop back to FX to finish:
            new Thread(() -> {
                try
                {
                    // Wait for all futures:
                    for (Future<List<CodeError>> f : futures)
                        f.get();
                }
                catch (ExecutionException | InterruptedException e)
                {
                    Debug.reportError(e);
                }
                Platform.runLater(() -> panel.updateErrorOverviewBar(false));
            }).start();
        });
    }
        
    @Override
    public boolean compileStarted()
    {
        if (panelOpen.get())
        {
            Platform.runLater(() -> panel.flagErrorsAsOld());
            TopLevelCodeElement el = panel.getSource();
            if (el != null)
            {
                // By using count, we force evaluation of the whole stream:
                return el.findEarlyErrors().count() > 0;
            }
            return true;
        }
        else
        {
            queuedErrors.clear();
            return lastSource.findEarlyErrors().count() > 0;
        }
    }



    public boolean readyToCompile()
    {
        // Hacky proxy for this, but it will do for now:
        return pkg.getDebugger() != null;
    }

    public AssistContent[] getCompletions(TopLevelCodeElement allCode, PosInSourceDoc pos, ExpressionSlot<?> completing, CodeElement codeEl)
    {
        CodeSuggestions suggests = allCode.getCodeSuggestions(pos, completing);
        
        ArrayList<AssistContent> joined = new ArrayList<>();
        if (suggests != null)
        {
            AssistContent[] assists = ParseUtils.getPossibleCompletions(suggests, javadocResolver, null);
            if (assists != null)
                joined.addAll(Arrays.asList(assists));
        }
        
        // We only want to add Greenfoot. suggestions and local var suggestions
        // when they are completing having written only a simple string prefix,
        // not a compound type (like "this.pre" or "Greenfoot.pre" or "getWorld().pre").
        if (suggests != null && suggests.isPlain())
        {    
            // Special case to support completing static methods from Greenfoot class
            
            // TODO in future, only do this if we are importing Greenfoot classes.
            JavaReflective greenfootClassRef = new JavaReflective(pkg.loadClass("greenfoot.Greenfoot"));
            CodeSuggestions greenfootClass = new CodeSuggestions(new GenTypeClass(greenfootClassRef), null, null, true, false);
            AssistContent[] greenfootStatic = ParseUtils.getPossibleCompletions(greenfootClass, javadocResolver, null);
            Arrays.stream(greenfootStatic).filter(ac -> ac.getKind() == CompletionKind.METHOD).forEach(ac -> joined.add(new PrefixCompletionWrapper(ac, "Greenfoot.")));
       
            for (LocalParamInfo v : ASTUtility.findLocalsAndParamsInScopeAt(codeEl, false))
            {
                AssistContent c = LocalCompletion.getCompletion(v.getType(), v.getName(), v.isParam());
                if (c != null)
                    joined.add(c);
            }
        }
        return joined.toArray(new AssistContent[0]);
    }
    
    // Gets the available fields in this class (i.e. those in this class and all superclasses)
    public List<AssistContent> getAvailableMembers(TopLevelCodeElement allCode, PosInSourceDoc pos, Set<CompletionKind> kinds, boolean includeOverridden)
    {
        CodeSuggestions suggests = allCode.getCodeSuggestions(pos, null);
        if (suggests == null)
            return Collections.emptyList();
        List<AssistContent> members;
        if (includeOverridden)
        {
            members = new ArrayList<>();
            // Add it whether overridden or not:
            ParseUtils.getPossibleCompletions(suggests, javadocResolver, (ac, isOverridden) -> members.add(ac));
        }
        else
        {
            AssistContent[] result = ParseUtils.getPossibleCompletions(suggests, javadocResolver, null);
            if (result == null)
                members = Collections.emptyList();
            else
                members = Arrays.asList(result);
        }

        return members.stream().filter(ac -> kinds == null || kinds.contains(ac.getKind())).collect(Collectors.toList());
    }

    @Override
    public void insertAppendMethod(bluej.extensions.editor.Editor e, NormalMethodElement method, Consumer<Boolean> after)
    {
        Platform.runLater(() -> {
            if (panel == null) {
                createPanel(false, false);
            }
            after.accept(panel.insertAppendMethod(method));
        });
    }

    @Override
    public void insertMethodCallInConstructor(bluej.extensions.editor.Editor e, String className, CallElement methodName, Consumer<Boolean> after)
    {
        Platform.runLater(() -> {
            if (panel == null) {
                createPanel(false, false);
            }
            after.accept(panel.insertMethodCallInConstructor(className, methodName));
        });
    }
    
    @OnThread(Tag.FX)
    public TopLevelCodeElement getSource()
    {
        return panel.getSource();
    }

    @OnThread(Tag.Swing)
    public List<AssistContentThreadSafe> getLocalTypes(Class<?> superType, boolean includeSelf, Set<Kind> kinds)
    {
        return pkg.getClassTargets()
                  .stream()
                  .filter(ct -> {
                      if (superType != null)
                      {
                          ClassInfo info = ct.getSourceInfo().getInfoIfAvailable();
                          if (info == null)
                              return false;
                          // This code won't pick up the case where A extends B, and B has "superType"
                          // as a super type, but I'm not sure how we can easily tell that.
                          boolean hasSuperType = false;
                          hasSuperType |= superType.getName().equals(info.getSuperclass());
                          // Check interfaces:
                          hasSuperType |= info.getImplements().stream().anyMatch(s -> superType.getName().equals(s));
                          if (!hasSuperType)
                              return false;
                      }
                      
                      if (ct.isInterface())
                          return kinds.contains(Kind.INTERFACE);
                      else if (ct.isEnum())
                          return kinds.contains(Kind.ENUM);
                      else 
                          return kinds.contains(Kind.CLASS_FINAL) || kinds.contains(Kind.CLASS_NON_FINAL);
                  })
                  .map(ct -> new AssistContentThreadSafe(LocalTypeCompletion.getCompletion(ct)))
                  .collect(Collectors.toList());
    }

    @Override
    public void showNextError()
    {
       Platform.runLater(() -> panel.nextError());
    }

    @Override
    @OnThread(Tag.Swing)
    public void cancelFreshState()
    {
        if (panelOpen.get())
        {
            Platform.runLater(() -> panel.cancelFreshState());
        }
    }

    @Override
    public void focusMethod(String methodName)
    {
        Platform.runLater(() -> {
            if (panel == null) {
                createPanel(true, true);
            }
            panel.focusMethod(methodName);
        });
    }

    public JavadocResolver getJavadocResolver()
    {
        return javadocResolver;
    }

    //    public void changedName(String oldName, String newName)
//    {
//        watcher.changedName(oldName, newName);
//    }    
}
