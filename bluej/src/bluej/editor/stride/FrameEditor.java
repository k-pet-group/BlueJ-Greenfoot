/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016,2017 Michael Kölling and John Rosenberg
 
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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;

import bluej.Config;
import bluej.collect.DiagnosticWithShown;
import bluej.collect.StrideEditReason;
import bluej.compiler.CompileReason;
import bluej.compiler.CompileType;
import bluej.compiler.Diagnostic;
import bluej.debugger.DebuggerField;
import bluej.debugger.DebuggerObject;
import bluej.debugger.DebuggerThread;
import bluej.debugger.gentype.GenTypeClass;
import bluej.editor.Editor;
import bluej.editor.EditorWatcher;
import bluej.editor.TextEditor;
import bluej.extensions.SourceType;
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
import bluej.stride.framedjava.ast.JavaFragment;
import bluej.stride.framedjava.ast.JavaFragment.PosInSourceDoc;
import bluej.stride.framedjava.ast.JavaSource;
import bluej.stride.framedjava.ast.Loader;
import bluej.stride.framedjava.elements.CallElement;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.framedjava.elements.CodeElement.LocalParamInfo;
import bluej.stride.framedjava.elements.LocatableElement.LocationMap;
import bluej.stride.framedjava.elements.NormalMethodElement;
import bluej.stride.framedjava.elements.TopLevelCodeElement;
import bluej.stride.framedjava.errors.DirectSlotError;
import bluej.stride.framedjava.errors.SyntaxCodeError;
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
import bluej.utility.Utility;
import bluej.utility.javafx.JavaFXUtil;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import threadchecker.OnThread;
import threadchecker.Tag;

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
    /** Whether the code has been successfully compiled since last edit */
    @OnThread(Tag.Swing) private boolean isCompiled;
    
    // If the code has been changed since last save (only modify on FX thread):
    // Start true, because we haven't actually saved before, so technically we have changed:
    @OnThread(Tag.FX) private boolean changedSinceLastSave = true;
    // The code at point of last save (only modify on FX thread)
    @OnThread(Tag.FX) private String lastSavedSource = null;
    // Only touch on FX thread:
    @OnThread(Tag.FX) private SaveJavaResult lastSavedJavaFX = null;
    // Only touch on Swing thread:
    @OnThread(Tag.Swing) private SaveJavaResult lastSavedJavaSwing = null;
    
    /** Location of the .stride file */
    @OnThread(Tag.Any) private final ReadWriteLock filenameLock = new ReentrantReadWriteLock();
    // These fields should only be used with the lock above, but once locked, can be accessed from any thread:
    @OnThread(Tag.Any) private File frameFilename;
    @OnThread(Tag.Any) private File javaFilename;
    
    @OnThread(Tag.FX) private final EntityResolver resolver;
    private final EditorWatcher watcher;
    private final JavadocResolver javadocResolver;
    
    /**
     * Set to the latest version of the JavaSource.  null if the editor has not yet been opened;
     * you can observe it to see when it becomes non-null if you want to do something when
     * the editor opens.
     */
    @OnThread(Tag.FX) private final SimpleObjectProperty<JavaSource> javaSource;
    private final bluej.pkgmgr.Package pkg;
    @OnThread(Tag.FX) private FrameEditorTab panel;
    private final DebugInfo debugInfo = new DebugInfo();
    @OnThread(Tag.FXPlatform) private HighlightedBreakpoint curBreakpoint;
    @OnThread(Tag.FXPlatform) private final List<HighlightedBreakpoint> execHistory = new ArrayList<>();

    /** Stride source at last save. Assigned on FX thread only, readable on any thread. */
    private volatile TopLevelCodeElement lastSource;
    
    /**
     * Errors from compilation to be shown once the editor is opened
     * (and thus we don't have to recompile just because the editor opens)
     */
    @OnThread(Tag.FX) private final List<QueuedError> queuedErrors = new ArrayList<>();

    /**
     * A callback to call (on the Swing thread) when this editor is opened.
     * Callback can be accessed from any thread to be queued up, but should always
     * be passed to SwingUtilities.invokeLater
     */
    @OnThread(Tag.Any) private final Runnable callbackOnOpen;
    
    @OnThread(value = Tag.Any, requireSynchronized = true)
    private List<Integer> latestBreakpoints = Collections.emptyList();

    @OnThread(Tag.Any)
    public synchronized List<Integer> getBreakpoints()
    {
        return new ArrayList<>(latestBreakpoints);
    }

    /**
     * A javac compile error.
     */
    @OnThread(Tag.Any)
    private static class QueuedError
    {
        private final long startLine, startColumn, endLine, endColumn;
        private final String message;
        private final int identifier;

        private QueuedError(long startLine, long startColumn, long endLine, long endColumn, String message, int identifier)
        {
            this.startLine = startLine;
            this.startColumn = startColumn;
            this.endLine = endLine;
            this.endColumn = endColumn;
            this.message = message;
            this.identifier = identifier;
        }
    }

    @OnThread(Tag.FX)
    public FrameEditor(File frameFilename, File javaFilename, EditorWatcher watcher, EntityResolver resolver, JavadocResolver javadocResolver, bluej.pkgmgr.Package pkg, Runnable callbackOnOpen)
    {
        this.frameFilename = frameFilename;
        this.javaFilename = javaFilename;
        this.watcher = watcher;
        this.resolver = resolver;
        this.javadocResolver = javadocResolver;
        this.pkg = pkg;
        this.javaSource = new SimpleObjectProperty<>();
        this.callbackOnOpen = callbackOnOpen;
        lastSource = Loader.loadTopLevelElement(frameFilename, resolver);
    }
    
    @OnThread(Tag.FXPlatform)
    private void createPanel(boolean visible, boolean toFront)
    {
        //Debug.message("&&&&&& Creating panel: " + System.currentTimeMillis());
        this.panel = new FrameEditorTab(pkg.getProject(), resolver, this, lastSource);
        //Debug.message("&&&&&& Adding panel to editor: " + System.currentTimeMillis());
        if (visible)
        {
            // This calls initialiseFX:
            pkg.getProject().getDefaultFXTabbedEditor().addTab(this.panel, visible, toFront);
        }
        else
        {
            // This is ok to call multiple times:
            this.panel.initialiseFX();
        }
        //Debug.message("&&&&&& Done! " + System.currentTimeMillis());
        // Saving Java will trigger any pending actions like jumping to a stack trace location:
        panel.initialisedProperty().addListener((a, b, newVal) -> {
            if (newVal)
            {
                // runLater so that the panel will have been added:
                JavaFXUtil.runPlatformLater(() -> {
                    _saveFX();
                    findLateErrors();
                });

            }
        });
        debugInfo.bindVarVisible(panel.debugVarVisibleProperty());
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
                panel.cleanup();
                panel = null;
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
        final CompletableFuture<SaveResult> q = new CompletableFuture<>();
        Platform.runLater(() -> { q.complete(_saveFX());});
        SaveResult result = null;
        try
        {
            result = q.get();
        }
        catch (InterruptedException | ExecutionException e1)
        {
            Debug.reportError(e1);
        }
        // result can be null if an exception occurred completing the future
        if (result != null && result.exception != null)
            throw new IOException(result.exception);
        
        setSaved();
        if (watcher != null)
            watcher.recordEdit(SourceType.Stride, result.savedSource, true);
        if (result.javaResult != null)
            this.lastSavedJavaSwing = result.javaResult;
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

    private static class SaveResult
    {
        private final IOException exception;
        private final String savedSource;
        private final SaveJavaResult javaResult;

        public SaveResult(IOException exception)
        {
            this.exception = exception;
            this.savedSource = null;
            this.javaResult = null;
        }

        public SaveResult(String savedSource, SaveJavaResult javaResult)
        {
            this.savedSource = savedSource;
            this.javaResult = javaResult;
            this.exception = null;
        }
    }

    /**
     * Saves the code on the FX thread.  If any IOException occurs, it is caught and returned
     * (so it can be re-thrown on the Swing thread).  Otherwise, the saved XML source is returned.
     * Null is never returned
     */
    @OnThread(Tag.FXPlatform)
    private SaveResult _saveFX()
    {
        if (!changedSinceLastSave) {
            return new SaveResult(lastSavedSource, lastSavedJavaFX);
        }

        try
        {
            // If frame editor is closed, we just need to write the Java code
            if (panel == null || panel.getSource() == null)
            {
                SaveJavaResult javaResult = saveJava(lastSource, true);
                return new SaveResult(Utility.serialiseCodeToString(lastSource.toXML()), javaResult);
            }

            panel.regenerateAndReparse();
            TopLevelCodeElement source = panel.getSource();
            
            if (source == null)
                return new SaveResult(Utility.serialiseCodeToString(lastSource.toXML()), null); // classFrame not initialised yet

            // Save Frame source:
            Lock readLock = filenameLock.readLock();            
            readLock.lock();
            try (FileOutputStream os = new FileOutputStream(frameFilename)) {
                Utility.serialiseCodeTo(source.toXML(), os);
            }
            finally {
                readLock.unlock();
            }

            lastSavedJavaFX = saveJava(panel.getSource(), true);
            changedSinceLastSave = false;
            lastSavedSource = Utility.serialiseCodeToString(source.toXML());
        
            panel.saved();
            lastSource = panel.getSource();
            return new SaveResult(lastSavedSource, lastSavedJavaFX);
        }
        catch (IOException e)
        {
            return new SaveResult(e);
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

    private class SaveJavaResult
    {
        private final JavaSource javaSource;
        private final LocationMap xpathLocations;

        public SaveJavaResult(JavaSource javaSource, LocationMap xpathLocations)
        {
            this.javaSource = javaSource;
            this.xpathLocations = xpathLocations;
        }
    }

    /**
     * @param warning Whether to include the "auto-generated" warning at the top of the file
     */
    @OnThread(Tag.FX)
    private SaveJavaResult saveJava(TopLevelCodeElement source, boolean warning) throws IOException
    {
        if (source == null)
            return null; // Not fully loaded yet

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

        SwingUtilities.invokeLater(() -> watcher.recordEdit(SourceType.Java, javaString, true));

        return new SaveJavaResult(js, source.toXML().buildLocationMap());
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
            public void displayMessage(String message, int lineNumber, int column) { FrameEditor.this.displayMessage(message, lineNumber, column); }

            @Override
            @OnThread(Tag.Swing)
            public boolean displayDiagnostic(Diagnostic diagnostic, int errorIndex, CompileType compileType)
            {
                return FrameEditor.this.displayDiagnostic(diagnostic, errorIndex, compileType);
            }
            
            @Override
            @OnThread(Tag.Swing)
            public void setStepMark(int lineNumber, String message,
                    boolean isBreak, DebuggerThread thread)
            {
                FrameEditor.this.setStepMark(lineNumber, message, isBreak, thread);
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
            public void compileFinished(boolean successful, boolean classesKept)
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

            @Override
            @OnThread(Tag.Swing)
            public void setExtendsClass(String className, ClassInfo classInfo)
            {
                FrameEditor.this.setExtendsClass(className, classInfo);
            }

            @Override
            @OnThread(Tag.Swing)
            public void addImplements(String className, ClassInfo classInfo)
            {
                FrameEditor.this.addImplements(className, classInfo);
            }

            @Override
            @OnThread(Tag.Swing)
            public void removeExtendsClass(ClassInfo classInfo)
            {
                FrameEditor.this.removeExtendsClass(classInfo);
            }

            @Override
            @OnThread(Tag.Swing)
            public void addExtendsInterface(String interfaceName, ClassInfo classInfo)
            {
                FrameEditor.this.addExtendsInterface(interfaceName, classInfo);
            }

            @Override
            @OnThread(Tag.Swing)
            public void removeExtendsOrImplementsInterface(String interfaceName, ClassInfo classInfo)
            {
                FrameEditor.this.removeExtendsOrImplementsInterface(interfaceName, classInfo);
            }

            @Override
            @OnThread(Tag.Swing)
            public void removeImports(List<String> importTargets)
            {
                FrameEditor.this.removeImports(importTargets);
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
    public void displayMessage(String message, final int lineNumber, int column)
    {
        //This is a message from a clickable stack trace following an exception
        Platform.runLater(() -> JavaFXUtil.onceNotNull(javaSource, js -> JavaFXUtil.runNowOrLater(() -> {
            setVisibleFX(true, true);
            js.handleException(lineNumber);
        })));
    }

    @Override
    public boolean displayDiagnostic(final Diagnostic diagnostic, int errorIndex, CompileType compileType)
    {
        if (lastSavedJavaSwing != null && lastSavedJavaSwing.javaSource != null && lastSavedJavaSwing.xpathLocations != null)
        {
            JavaFragment fragment = lastSavedJavaSwing.javaSource.findError((int)diagnostic.getStartLine(), (int)diagnostic.getStartColumn(), (int)diagnostic.getEndLine(), (int)diagnostic.getEndColumn(), diagnostic.getMessage(), true);
            if (fragment != null)
            {
                String xpath = lastSavedJavaSwing.xpathLocations.locationFor(fragment);
                int start = fragment.getErrorStartPos((int)diagnostic.getStartLine(), (int)diagnostic.getStartColumn());
                int end = fragment.getErrorEndPos((int)diagnostic.getEndLine(), (int)diagnostic.getEndColumn());
                if (xpath != null)
                    diagnostic.setXPath(xpath, start, end);
            }
        }

        // We are on the Swing EDT, but need to do GUI bits on the FX thread:
        Platform.runLater(() -> {
            // Don't show javac errors if we are not valid for compilation:
            if (panel != null && panel.getSource() != null)
            {
                JavaFXUtil.onceNotNull(javaSource, js ->
                        js.handleError((int) diagnostic.getStartLine(), (int) diagnostic.getStartColumn(),
                            (int) diagnostic.getEndLine(), (int) diagnostic.getEndColumn(), diagnostic.getMessage(), true, diagnostic.getIdentifier())
                );
            }
            else
            {
                queuedErrors.add(new QueuedError(diagnostic.getStartLine(), diagnostic.getStartColumn(), diagnostic.getEndLine(), diagnostic.getEndColumn(), diagnostic.getMessage(), diagnostic.getIdentifier()));
            }
            
            if (compileType.showEditorOnError())
            {
                setVisibleFX(true, true);
            }
        });
        return false;
    }
    
    @Override
    public void setStepMark(int lineNumber, String message, boolean isBreak,
            DebuggerThread thread)
    {
        removeStepMark();
        Platform.runLater(() -> {
            setVisibleFX(true, true);
            SwingUtilities.invokeLater(() -> {
                HashMap<String, DebugVarInfo> vars = new HashMap<String, DebugVarInfo>();
                if (thread != null) {
                    DebuggerObject currentObject = thread.getCurrentObject(0);
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
                Platform.runLater(() -> {
                    debugInfo.addVarState(vars, execHistory.size());
                    panel.showDebuggerControls(thread);
                    if (curBreakpoint != null) {
                        curBreakpoint.removeHighlight();
                        curBreakpoint = null;
                    }
                    try {
                        JavaSource js = javaSource.get();
                        if (js == null) {
                            js = saveJava(lastSource, true).javaSource;
                        }
                        curBreakpoint = js.handleStop(lineNumber, debugInfo);
                        if (curBreakpoint.isBreakpointFrame())
                        {
                            SwingUtilities.invokeLater(() -> thread.step());
                        }
                        else
                        {
                            if (execHistory.isEmpty() || execHistory.get(execHistory.size() - 1) != curBreakpoint)
                                execHistory.add(curBreakpoint);
                            panel.redrawExecHistory(execHistory);
                        }
                    }
                    catch (IOException ioe) {
                        Debug.reportError("Exception attempting to save Java source for Stride class", ioe);
                    }
                });
            });
        });
    }

    @Override
    public void writeMessage(String msg)
    {
      // Not needed yet.
    }

    @Override
    public void removeStepMark()
    {
        /*
        Platform.runLater(() -> {
            if (debugInfo != null)
                debugInfo.hideAllDisplays();
        });
        */
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
        Platform.runLater(() -> {
            if (javaSource.get() == null) {
                IOException e = _saveFX().exception;
                if (e != null)
                    Debug.reportError(e);
            }
            if (javaSource.get() != null)
            {
                JavaSource latestSource = this.javaSource.get();
                SwingUtilities.invokeLater(() -> {
                    watcher.clearAllBreakpoints();
                    List<Integer> breaks = latestSource.registerBreakpoints(this, watcher);
                    synchronized (this)
                    {
                        latestBreakpoints = breaks;
                    }
                });
            }
        });
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
        javafx.print.PrinterJob job = javafx.print.PrinterJob.createPrinterJob();
        if (job.showPrintDialog(null)) {
            Platform.runLater(() -> {
                if (job != null) {
                    boolean success = job.printPage(panel.getSource().getFrame().getNode());
                    if (success) {
                        job.endJob();
                    }
                }
            });
        }
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

    @OnThread(Tag.FXPlatform)
    private void setVisibleFX(boolean show, boolean bringToFront)
    {
        if (panel == null && show) // No need to create the panel if we don't want to show it
        {
            createPanel(show, bringToFront);
        }

        if (panel != null)
        {
            panel.setWindowVisible(show, bringToFront);
            if (callbackOnOpen != null && show)
                SwingUtilities.invokeLater(callbackOnOpen);
        }


        if (show)
        {
            // We wait to try to show errors until the frame has finished loading, because
            // otherwise we won't have the correct references to work out where the error should be
            // shown.
            // We need this runLater to make sure we only run after we've finished setting up the editor
            // (and it case it uses any runLaters)
            panel.withTopLevelFrame(f -> JavaFXUtil.runPlatformLater(() -> {
                // We only need to worry about queued errors from the latest compile:
                if (!queuedErrors.isEmpty())
                {
                    // First, save, so that the AST elements all have the correct references back to
                    // the GUI frames which generated them:
                    Exception ex = _saveFX().exception;
                    if (ex != null)
                    {
                        Debug.reportError(ex);
                        return;
                    }
                    // Take a copy of the queue so we can leave it empty, but still use the queue in our listener:
                    final ArrayList<QueuedError> queueCopy = new ArrayList<>(queuedErrors);
                    queuedErrors.clear();
                    // We can only show the errors once the Java source is available.  I believe it will now
                    // (after r15373) actually always be available, but that means the code will just run immediately so that's fine:

                    JavaFXUtil.onceNotNull(javaSource, js -> {
                        for (QueuedError e : queueCopy)
                        {
                            // Use runlater because we might be mid-save, so need to wait for current code to finish:
                            JavaFXUtil.runPlatformLater(() -> {
                                if (!js.handleError((int) e.startLine, (int) e.startColumn,
                                        (int) e.endLine, (int) e.endColumn, e.message, false, e.identifier))
                                {
                                    Debug.message("Could not display queued error");
                                }
                            });
                        }
                        // We need to use runLater to account for the fact that adding errors uses a runLater:
                        JavaFXUtil.runPlatformLater(() -> panel.updateErrorOverviewBar(false));
                    });
                }
            }));
        }
    }

    @OnThread(Tag.FX)
    public void codeModified()
    {
        changedSinceLastSave = true;
        SwingUtilities.invokeLater(() -> {
            isCompiled = false;
            watcher.modificationEvent(this);
            watcher.scheduleCompilation(false, CompileReason.MODIFIED, CompileType.ERROR_CHECK_ONLY);
        });
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
        return false;
    }

    @Override
    public void compileFinished(boolean successful, boolean classesKept)
    {
        Platform.runLater(() -> {
            if (panel != null && panel.isWindowVisible())
            {
                findLateErrors();
                panel.compiled();
            }
        });

        reInitBreakpoints();
    }

    @OnThread(Tag.FXPlatform)
    private void findLateErrors()
    {
        panel.removeOldErrors();
        TopLevelCodeElement el = panel.getSource();
        if (el == null)
            return;
        Stream<CodeElement> allElements = Stream.concat(Stream.of((CodeElement)el), el.streamContained());
        LocationMap rootPathMap = el.toXML().buildLocationMap();
        // We must start these futures going on the FX thread
        List<Future<List<DirectSlotError>>> futures = allElements.flatMap(e -> e.findDirectLateErrors(panel, rootPathMap)).collect(Collectors.toList());
        // Then wait for them on another thread, and hop back to FX to finish:
        Utility.runBackground(() -> {
            ArrayList<DirectSlotError> allLates = new ArrayList<>();
            try
            {
                // Wait for all futures:
                for (Future<List<DirectSlotError>> f : futures)
                    allLates.addAll(f.get());
            }
            catch (ExecutionException | InterruptedException e)
            {
                Debug.reportError(e);
            }
            Platform.runLater(() -> panel.updateErrorOverviewBar(false));
            List<DiagnosticWithShown> diagnostics = Utility.mapList(allLates, e -> e.toDiagnostic(javaFilename.getName(), frameFilename));
            SwingUtilities.invokeLater(() -> watcher.recordLateErrors(diagnostics));
        });
    }
        
    @Override
    public boolean compileStarted()
    {
        Platform.runLater(() -> {
            if (panel != null)
                panel.flagErrorsAsOld();
            else
                queuedErrors.clear();
        });
        // Note lastSourceRef may refer to a stale source, but this shouldn't cause any
        // significant issues.  In fact, it probably makes sense to use the source at
        // point of last save, rather than any modifications in the window since.
        return earlyErrorCheck(lastSource.findEarlyErrors());
    }

    /**
     * Given a stream of early errors, records them and returns true if there were any errors (i.e. if the stream was non-empty)
     */
    //package-visible
    @OnThread(Tag.Any)
    boolean earlyErrorCheck(Stream<SyntaxCodeError> earlyErrors)
    {
        List<SyntaxCodeError> earlyList = earlyErrors.collect(Collectors.toList());
        List<DiagnosticWithShown> diagnostics = Utility.mapList(earlyList, e -> e.toDiagnostic(javaFilename.getName(), frameFilename));
        SwingUtilities.invokeLater(() -> watcher.recordEarlyErrors(diagnostics));
        return !earlyList.isEmpty();
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

            if (Config.isGreenfoot())
            {
                // TODO in future, only do this if we are importing Greenfoot classes.
                JavaReflective greenfootClassRef = new JavaReflective(pkg.loadClass("greenfoot.Greenfoot"));
                CodeSuggestions greenfootClass = new CodeSuggestions(new GenTypeClass(greenfootClassRef), null, null, true, false);
                AssistContent[] greenfootStatic = ParseUtils.getPossibleCompletions(greenfootClass, javadocResolver, null);
                Arrays.stream(greenfootStatic).filter(ac -> ac.getKind() == CompletionKind.METHOD).forEach(ac -> joined.add(new PrefixCompletionWrapper(ac, "Greenfoot.")));
            }

            for (LocalParamInfo v : ASTUtility.findLocalsAndParamsInScopeAt(codeEl, false, false))
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
            panel.insertAppendMethod(method, after);
        });
    }

    @Override
    public void insertMethodCallInConstructor(bluej.extensions.editor.Editor e, String className, CallElement methodName, Consumer<Boolean> after)
    {
        Platform.runLater(() -> {
            if (panel == null) {
                createPanel(false, false);
            }
            panel.insertMethodCallInConstructor(className, methodName, after);
        });
    }

    @Override
    public void removeImports(List<String> importTargets)
    {
        Platform.runLater(() -> {
            if (panel == null) {
                createPanel(false, false);
            }
            panel.removeImports(importTargets);
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

    public void showNextError()
    {
       Platform.runLater(() -> panel.nextError());
    }

    @Override
    @OnThread(Tag.Swing)
    public void cancelFreshState()
    {
        Platform.runLater(() -> {
            if (panel != null)
                panel.cancelFreshState();
        });
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

    @OnThread(Tag.Any)
    public EditorWatcher getWatcher()
    {
        return watcher;
    }

    @OnThread(Tag.FXPlatform)
    public void recordEdits(StrideEditReason reason)
    {
        SaveResult result = _saveFX();
        if (result.exception == null)
        {
            SwingUtilities.invokeLater(() -> {
                watcher.recordEdit(SourceType.Stride, result.savedSource, true, reason);
            });
        }
        else
            Debug.reportError(result.exception);
    }

    @Override
    public void addImplements(String className, ClassInfo classInfo)
    {
        Platform.runLater(() -> {
            if (panel == null) {
                createPanel(false, false);
            }
            JavaFXUtil.onceTrue(panel.initialisedProperty(), p -> panel.addImplements(className));
        });
    }

    @Override
    public void setExtendsClass(String className, ClassInfo classInfo)
    {
        Platform.runLater(() -> {
            if (panel == null) {
                createPanel(false, false);
            }
            JavaFXUtil.onceTrue(panel.initialisedProperty(), p ->
                JavaFXUtil.runPlatformLater( () -> panel.addExtends(className))
            );
        });
    }

    @Override
    public void removeExtendsClass(ClassInfo classInfo)
    {
        Platform.runLater(() -> {
            if (panel == null) {
                createPanel(false, false);
            }
            JavaFXUtil.onceTrue(panel.initialisedProperty(), p -> panel.removeExtendsClass());
        });
    }

    @Override
    public void addExtendsInterface(String interfaceName, ClassInfo classInfo)
    {
        Platform.runLater(() -> {
            if (panel == null) {
                createPanel(false, false);
            }
            JavaFXUtil.onceTrue(panel.initialisedProperty(), p -> panel.addExtends(interfaceName));
        });
    }

    @Override
    public void removeExtendsOrImplementsInterface(String interfaceName, ClassInfo classInfo)
    {
        Platform.runLater(() -> {
            if (panel == null) {
                createPanel(false, false);
            }
            JavaFXUtil.onceTrue(panel.initialisedProperty(), p -> panel.removeExtendsOrImplementsInterface(interfaceName));
        });
    }
}
