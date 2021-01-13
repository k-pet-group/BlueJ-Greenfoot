/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016,2017,2018,2019,2020,2021 Michael Kölling and John Rosenberg
 
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


import bluej.Config;
import bluej.collect.DiagnosticWithShown;
import bluej.collect.StrideEditReason;
import bluej.compiler.CompileReason;
import bluej.compiler.CompileType;
import bluej.compiler.Diagnostic;
import bluej.debugger.DebuggerThread;
import bluej.debugger.gentype.GenTypeClass;
import bluej.editor.Editor;
import bluej.editor.EditorWatcher;
import bluej.editor.TextEditor;
import bluej.editor.fixes.EditorFixesManager;
import bluej.parser.nodes.ReparseableDocument;
import bluej.pkgmgr.target.role.Kind;
import bluej.prefmgr.PrefMgr.PrintSize;
import bluej.parser.AssistContent;
import bluej.parser.AssistContent.CompletionKind;
import bluej.parser.ExpressionTypeInfo;
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
import bluej.stride.framedjava.frames.LocalCompletion;
import bluej.stride.framedjava.slots.ExpressionSlot;
import bluej.parser.AssistContentThreadSafe;
import bluej.utility.Debug;
import bluej.utility.JavaReflective;
import bluej.utility.Utility;
import bluej.utility.javafx.FXPlatformConsumer;
import bluej.utility.javafx.FXPlatformRunnable;
import bluej.utility.javafx.FXRunnable;
import bluej.utility.javafx.JavaFXUtil;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.print.PrinterJob;
import javafx.scene.image.Image;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * FrameEditor implements Editor, the interface to the rest of BlueJ that existed
 * before our frame editor was created.  Because of this, not all of the methods make
 * sense -- see comments throughout the class.
 *
 * Most of the major functionality is actually in FrameEditorTab; this FrameEditor class
 * really just exists to integrate the editor into BlueJ.  Also, FrameEditor can exist without
 * the graphical editor being opened, whereas FrameEditorTab is tied to the graphical aspect.
 */
@OnThread(Tag.FXPlatform)
public class FrameEditor implements Editor
{
    /** Whether the code has been successfully compiled since last edit */
    @OnThread(Tag.FXPlatform) private boolean isCompiled;
    
    // If the code has been changed since last save:
    @OnThread(Tag.FXPlatform) private boolean changedSinceLastSave = false;
    // The code at point of last save (only modify on FX thread)
    @OnThread(Tag.FX) private String lastSavedSource = null;
    // The generated Java code at point of last save:
    @OnThread(Tag.FX) private SaveJavaResult lastSavedJava = null;
    
    /** Location of the .stride file */
    private File frameFilename;
    private File javaFilename;
    
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
    /** The Editor Quick Fixes manager associated with this Editor */
    @OnThread(Tag.Any)
    private final EditorFixesManager editorFixesMgr;

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
    @OnThread(Tag.Any) private final FXPlatformRunnable callbackOnOpen;
    
    @OnThread(value = Tag.Any, requireSynchronized = true)
    private List<Integer> latestBreakpoints = Collections.emptyList();
    /**
     * When a compile starts, we set this to false.  When a compile finishes,
     * if it's false, we look for late errors and flip it to true.
     * compileFinished can get called multiple times for one compile,
     * so this prevents us looking for late errors twice.
     */
    @OnThread(Tag.FXPlatform)
    private boolean foundLateErrorsForMostRecentCompile;
    /**
     * Paired with foundLateErrorsForMostRecentCompile, this is the identifier
     * for the most recent compile for data recording purposes, to allow the late
     * errors to be associated with the most recent compile which triggered them.
     */
    private int mostRecentCompileIdentifier = -1;

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
    public FrameEditor(File frameFilename, File javaFilename, EditorWatcher watcher, EntityResolver resolver,
                       JavadocResolver javadocResolver, bluej.pkgmgr.Package pkg, FXPlatformRunnable callbackOnOpen)
    {
        this.frameFilename = frameFilename;
        this.javaFilename = javaFilename;
        this.watcher = watcher;
        this.resolver = resolver;
        this.javadocResolver = javadocResolver;
        this.pkg = pkg;
        this.javaSource = new SimpleObjectProperty<>();
        this.callbackOnOpen = callbackOnOpen;
        this.editorFixesMgr = new EditorFixesManager(watcher.getPackage().getProject().getImports());
        lastSource = Loader.loadTopLevelElement(frameFilename, resolver, pkg.getQualifiedName());
    }

    /**
     * Create a frame editor tab.
     *
     * @param visible Whether to make the FXTabbedEditor window visible
     * @param toFront Whether to bring the tab to the front (i.e. select the tab)
     * @param openInNewWindow if this is true, the editor opens in a new window
     */
    @OnThread(Tag.FXPlatform)
    private void createPanel(boolean visible, boolean toFront, boolean openInNewWindow)
    {
        this.panel = new FrameEditorTab(pkg.getProject(), resolver, this, lastSource);
        if (visible)
        {
            if (openInNewWindow)
            {
                // This calls initialiseFX:
                pkg.getProject().createNewFXTabbedEditor().addTab(this.panel, visible, toFront);
            }
            else
            {
                // This calls initialiseFX:
                pkg.getProject().getDefaultFXTabbedEditor().addTab(this.panel, visible, toFront);
            }

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
                    saveFX();
                    // No relevant other compilation, so use -1 as identifier:
                    findLateErrors(-1);
                });

            }
        });
        debugInfo.bindVarVisible(panel.debugVarVisibleProperty());
    }

    // Editor methods:

    @Override
    public void close()
    {
        if (panel != null)
        {
            lastSource = panel.getSource();
            panel.setWindowVisible(false, false);
            panel.cleanup();
            panel = null;
        }
    }

    @Override
    public void save() throws IOException
    {
        if (changedSinceLastSave)
        {
            SaveResult result = saveFX();
            if (result.exception != null)
            {
                throw new IOException(result.exception);
            }
            
            if (watcher != null)
            {
                watcher.recordStrideEdit(result.javaResult.javaSourceStringContent,
                        result.savedSource, null);
            }
        }
        else if (lastSavedJava == null)
        {
            // If we haven't generated Java yet, we should do so:
            lastSavedJava = saveJava(lastSource, true);
        }
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
     * Saves the code, if it has been modified since it was last saved. If any IOException occurs,
     * it is caught and returned; otherwise, the saved XML source is returned.<p>
     * 
     * The Java source is also generated if it is stale or has not yet been generated.
     */
    @OnThread(Tag.FXPlatform)
    private SaveResult saveFX()
    {
        try
        {
            if (!changedSinceLastSave && lastSavedSource != null)
            {
                if (lastSavedJava == null)
                {
                    lastSavedJava = saveJava(lastSource, true);
                }
                return new SaveResult(lastSavedSource, lastSavedJava);
            }
            
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
            try (FileOutputStream os = new FileOutputStream(frameFilename))
            {
                Utility.serialiseCodeTo(source.toXML(), os);
            }

            lastSavedJava = saveJava(panel.getSource(), true);
            changedSinceLastSave = false;
            lastSavedSource = Utility.serialiseCodeToString(source.toXML());
        
            setSaved();
            panel.saved();
            lastSource = panel.getSource();
            return new SaveResult(lastSavedSource, lastSavedJava);
        }
        catch (IOException e)
        {
            return new SaveResult(e);
        }
    }

    /**
     * Saves the .java file without the "warning: auto-generated" text at the top
     */
    @OnThread(Tag.FXPlatform)
    public void saveJavaWithoutWarning() throws IOException
    {
        saveJava(lastSource, false);
    }

    private class SaveJavaResult
    {
        private final JavaSource javaSource;
        private final String javaSourceStringContent;
        private final LocationMap xpathLocations;

        public SaveJavaResult(JavaSource javaSource, String javaSourceStringContent, LocationMap xpathLocations)
        {
            this.javaSource = javaSource;
            this.javaSourceStringContent = javaSourceStringContent;
            this.xpathLocations = xpathLocations;
        }
    }

    /**
     * @param warning Whether to include the "auto-generated" warning at the top of the file
     */
    @OnThread(Tag.FXPlatform)
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

        return new SaveJavaResult(js, javaString, source.toXML().buildLocationMap());
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
        // I want to annotate this whole class as @OnThread(Tag.FXPlatform)
        // but on 8u20 it triggers JDK bug JDK-8059531
        // Instead we must annotate each method
        return new TextEditor() {

            @Override
            @OnThread(Tag.FXPlatform)
            public void setLastModified(long millisSinceEpoch)
            {
                FrameEditor.this.setLastModified(millisSinceEpoch);
            }

            @Override
            @OnThread(Tag.FXPlatform)
            public void writeMessage(String msg) { FrameEditor.this.writeMessage(msg); }

            @Override
            @OnThread(Tag.FXPlatform)
            public void showInterface(boolean interfaceStatus) { FrameEditor.this.showInterface(interfaceStatus); }

            @Override
            @OnThread(Tag.FXPlatform)
            public void setEditorVisible(boolean vis, boolean openInNewWindow)
            {
                FrameEditor.this.setEditorVisible(vis, openInNewWindow);
            }

            @Override
            @OnThread(Tag.FXPlatform)
            public void setReadOnly(boolean readOnly) { FrameEditor.this.setReadOnly(readOnly); }

            @Override
            @OnThread(Tag.FXPlatform)
            public void setProperty(String propertyKey, Object value) { FrameEditor.this.setProperty(propertyKey, value); }
            
            @Override
            @OnThread(Tag.FXPlatform)
            public void setCompiled(boolean compiled) { FrameEditor.this.setCompiled(compiled); }

            @Override
            @OnThread(Tag.FXPlatform)
            public void save() throws IOException { FrameEditor.this.save(); }

            @Override
            @OnThread(Tag.FXPlatform)
            public void removeStepMark() { FrameEditor.this.removeStepMark(); }

            @Override
            @OnThread(Tag.FXPlatform)
            public void removeBreakpoints() { FrameEditor.this.removeBreakpoints(); }

            @Override
            @OnThread(Tag.FXPlatform)
            public void reloadFile() { FrameEditor.this.reloadFile(); }

            @Override
            @OnThread(Tag.FXPlatform)
            public void refresh() { FrameEditor.this.refresh(); }

            @Override
            @OnThread(Tag.FXPlatform)
            public void reInitBreakpoints() { FrameEditor.this.reInitBreakpoints(); }

            @Override
            @OnThread(Tag.FXPlatform)
            public FXRunnable printTo(PrinterJob printerJob, PrintSize printSize, boolean printLineNumbers, boolean printBackground, PrintProgressUpdate progressUpdate) { return FrameEditor.this.printTo(printerJob, printSize, printLineNumbers, printBackground, progressUpdate); }

            @Override
            @OnThread(Tag.FXPlatform)
            public boolean isOpen() { return FrameEditor.this.isOpen(); }

            @Override
            @OnThread(Tag.FXPlatform)
            public boolean isReadOnly() { return FrameEditor.this.isReadOnly(); }

            @Override
            @OnThread(Tag.FXPlatform)
            public boolean isModified() { return FrameEditor.this.isModified(); }

            @Override
            @OnThread(Tag.FXPlatform)
            public Object getProperty(String propertyKey) { return FrameEditor.this.getProperty(propertyKey); }

            @Override
            @OnThread(Tag.FXPlatform)
            public void displayMessage(String message, int lineNumber, int column) { FrameEditor.this.displayMessage(message, lineNumber, column); }

            @Override
            @OnThread(Tag.FXPlatform)
            public boolean displayDiagnostic(Diagnostic diagnostic, int errorIndex, CompileType compileType)
            {
                return FrameEditor.this.displayDiagnostic(diagnostic, errorIndex, compileType);
            }
            
            @Override
            @OnThread(Tag.FXPlatform)
            public boolean setStepMark(int lineNumber, String message,
                    boolean isBreak, DebuggerThread thread)
            {
                return FrameEditor.this.setStepMark(lineNumber, message, isBreak, thread);
            }

            @Override
            @OnThread(Tag.FXPlatform)
            public void close() { FrameEditor.this.close(); }

            @Override
            @OnThread(Tag.FXPlatform)
            public void changeName(String title, String filename, String javaFilename, String docFileName) { FrameEditor.this.changeName(title, filename, javaFilename, docFileName); }
            
            @Override
            @OnThread(Tag.FXPlatform)
            public TextEditor assumeText() { return this; }

            @Override
            @OnThread(Tag.FXPlatform)
            public boolean showFile(String filename, Charset charset, boolean compiled,
                    String docFilename) {
                throw new UnsupportedOperationException();
            }

            @Override
            @OnThread(Tag.FXPlatform)
            public void setText(SourceLocation begin, SourceLocation end, String newText) {
                throw new UnsupportedOperationException();

            }

            @Override
            @OnThread(Tag.FXPlatform)
            public void setSelection(SourceLocation begin, SourceLocation end) {
                throw new UnsupportedOperationException();

            }

            @Override
            @OnThread(Tag.FXPlatform)
            public void setCaretLocation(SourceLocation location) {
                throw new UnsupportedOperationException();

            }

            @Override
            @OnThread(Tag.FXPlatform)
            public int numberOfLines() {
                throw new UnsupportedOperationException();
            }

            @Override
            @OnThread(Tag.FXPlatform)
            public void insertText(String text, boolean caretBack) {
                throw new UnsupportedOperationException();

            }

            @Override
            @OnThread(Tag.FXPlatform)
            public int getTextLength() {
                throw new UnsupportedOperationException();
            }

            @Override
            @OnThread(Tag.FXPlatform)
            public String getText(SourceLocation begin, SourceLocation end) {
                throw new UnsupportedOperationException();
            }

            @Override
            @OnThread(Tag.FXPlatform)
            public ReparseableDocument getSourceDocument() {
                throw new UnsupportedOperationException();
            }

            @Override
            @OnThread(Tag.FXPlatform)
            public SourceLocation getSelectionEnd() {
                throw new UnsupportedOperationException();
            }

            @Override
            @OnThread(Tag.FXPlatform)
            public SourceLocation getSelectionBegin() {
                throw new UnsupportedOperationException();
            }

            @Override
            @OnThread(Tag.FXPlatform)
            public ParsedCUNode getParsedNode() {
                return null; //throw new UnsupportedOperationException();
            }

            @Override
            @OnThread(Tag.FXPlatform)
            public int getOffsetFromLineColumn(SourceLocation location) {
                throw new UnsupportedOperationException();
            }

            @Override
            @OnThread(Tag.FXPlatform)
            public int getLineLength(int line) {
                throw new UnsupportedOperationException();
            }

            @Override
            @OnThread(Tag.FXPlatform)
            public SourceLocation getLineColumnFromOffset(int offset) {
                throw new UnsupportedOperationException();
            }

            @Override
            @OnThread(Tag.FXPlatform)
            public SourceLocation getCaretLocation() {
                throw new UnsupportedOperationException();
            }

            @Override
            @OnThread(Tag.FXPlatform)
            public void clear() {
                throw new UnsupportedOperationException();

            }

            @Override
            @OnThread(Tag.FXPlatform)
            public void compileFinished(boolean successful, boolean classesKept)
            {
                throw new UnsupportedOperationException();                
            }

            @Override
            @OnThread(Tag.FXPlatform)
            public void insertAppendMethod(NormalMethodElement method, FXPlatformConsumer<Boolean> after)
            {
                FrameEditor.this.insertAppendMethod(method, after);
            }

            @Override
            @OnThread(Tag.FXPlatform)
            public void insertMethodCallInConstructor(String className, CallElement methodName, FXPlatformConsumer<Boolean> after)
            {
                FrameEditor.this.insertMethodCallInConstructor(className, methodName, after);
            }

            @Override
            @OnThread(Tag.FXPlatform)
            public FrameEditor assumeFrame()
            {
                return FrameEditor.this;
            }

            @Override
            @OnThread(Tag.FXPlatform)
            public boolean compileStarted(int compilationSequence)
            {
                return FrameEditor.this.compileStarted(compilationSequence);
            }

            @Override
            @OnThread(Tag.FXPlatform)
            public void focusMethod(String methodName, List<String> paramTypes)
            {
                FrameEditor.this.focusMethod(methodName, paramTypes);
            }

            @Override
            @OnThread(Tag.FXPlatform)
            public void setExtendsClass(String className, ClassInfo classInfo)
            {
                FrameEditor.this.setExtendsClass(className, classInfo);
            }

            @Override
            @OnThread(Tag.FXPlatform)
            public void addImplements(String className, ClassInfo classInfo)
            {
                FrameEditor.this.addImplements(className, classInfo);
            }

            @Override
            @OnThread(Tag.FXPlatform)
            public void removeExtendsClass(ClassInfo classInfo)
            {
                FrameEditor.this.removeExtendsClass(classInfo);
            }

            @Override
            @OnThread(Tag.FXPlatform)
            public void addExtendsInterface(String interfaceName, ClassInfo classInfo)
            {
                FrameEditor.this.addExtendsInterface(interfaceName, classInfo);
            }

            @Override
            @OnThread(Tag.FXPlatform)
            public void removeExtendsOrImplementsInterface(String interfaceName, ClassInfo classInfo)
            {
                FrameEditor.this.removeExtendsOrImplementsInterface(interfaceName, classInfo);
            }

            @Override
            public EditorFixesManager getEditorFixesManager(){
                return FrameEditor.this.editorFixesMgr;
            }

            @Override
            @OnThread(Tag.FXPlatform)
            public void addImportFromQuickFix(String importName)
            {
                FrameEditor.this.addImportFromQuickFix(importName);
            }

            @Override
            @OnThread(Tag.FXPlatform)
            public void removeImports(List<String> importTargets)
            {
                FrameEditor.this.removeImports(importTargets);
            }

            @Override
            @OnThread(Tag.FXPlatform)
            public void setHeaderImage(Image image)
            {
                FrameEditor.this.setHeaderImage(image);
            }

            @Override
            @OnThread(Tag.FXPlatform)
            public void removeErrorHighlights()
            {
                FrameEditor.this.removeErrorHighlights();
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
        JavaFXUtil.onceNotNull(javaSource, js -> JavaFXUtil.runNowOrLater(() -> {
            setVisibleFX(true, true, false);
            js.handleException(lineNumber);
        }));
    }

    @Override
    public boolean displayDiagnostic(final Diagnostic diagnostic, int errorIndex, CompileType compileType)
    {
        if (lastSavedJava != null && lastSavedJava.javaSource != null && lastSavedJava.xpathLocations != null)
        {
            JavaFragment fragment = lastSavedJava.javaSource.findError((int)diagnostic.getStartLine(), (int)diagnostic.getStartColumn(), (int)diagnostic.getEndLine(), (int)diagnostic.getEndColumn(), diagnostic.getMessage());
            if (fragment != null)
            {
                String xpath = lastSavedJava.xpathLocations.locationFor(fragment);
                int start = fragment.getErrorStartPos((int)diagnostic.getStartLine(), (int)diagnostic.getStartColumn());
                int end = fragment.getErrorEndPos((int)diagnostic.getEndLine(), (int)diagnostic.getEndColumn());
                if (xpath != null)
                    diagnostic.setXPath(xpath, start, end);
            }
        }


        // Don't show javac errors if we are not valid for compilation:
        if (panel != null && panel.getSource() != null)
        {
            JavaFXUtil.onceNotNull(javaSource, js ->
                    js.handleError((int) diagnostic.getStartLine(), (int) diagnostic.getStartColumn(),
                        (int) diagnostic.getEndLine(), (int) diagnostic.getEndColumn(), diagnostic.getMessage(), diagnostic.getIdentifier())
            );
        }
        else
        {
            queuedErrors.add(new QueuedError(diagnostic.getStartLine(), diagnostic.getStartColumn(), diagnostic.getEndLine(), diagnostic.getEndColumn(), diagnostic.getMessage(), diagnostic.getIdentifier()));
        }

        if (compileType.showEditorOnError())
        {
            setVisibleFX(true, true, false);
        }
        return false;
    }
    
    @Override
    public boolean setStepMark(int lineNumber, String message, boolean isBreak,
            DebuggerThread thread)
    {
        // Disable Stride debugger:
        if (true)
            return true;
        /*
        removeStepMark();
        setVisibleFX(true, true, false);
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
        debugInfo.addVarState(vars, execHistory.size());
        panel.showDebuggerControls(thread);
        if (curBreakpoint != null) {
            curBreakpoint.removeHighlight();
            curBreakpoint = null;
        }

        // We only want to show the step mark once the panel is initialised,
        // which we may need to wait for if we're only now showing the editor
        // for the first time:
        JavaFXUtil.onceTrue(panel.initialisedProperty(), b -> {
            try
            {
                JavaSource js = javaSource.get();
                if (js == null)
                {
                    js = saveJava(lastSource, true).javaSource;
                }
                curBreakpoint = js.handleStop(lineNumber, debugInfo);
                if (curBreakpoint.isBreakpointFrame())
                {
                    getPackage().getProject().getDebugger().runOnEventHandler(() -> thread.step());
                }
                else
                {
                    if (execHistory.isEmpty() || execHistory.get(execHistory.size() - 1) != curBreakpoint)
                    {
                        execHistory.add(curBreakpoint);
                    }
                    panel.redrawExecHistory(execHistory);
                }
            }
            catch (IOException ioe)
            {
                Debug.reportError("Exception attempting to save Java source for Stride class", ioe);
            }
        });
        */
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

        if (javaSource.get() == null) {
            IOException e = saveFX().exception;
            if (e != null)
                Debug.reportError(e);
        }
        if (javaSource.get() != null)
        {
            JavaSource latestSource = this.javaSource.get();
            watcher.clearAllBreakpoints();
            List<Integer> breaks = latestSource.registerBreakpoints(this, watcher);
            synchronized (this)
            {
                latestBreakpoints = breaks;
            }
        }
    }

    @Override
    public boolean isModified() 
    {
        return changedSinceLastSave;
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public FXRunnable printTo(PrinterJob job, PrintSize printSize, boolean printLineNumbers, boolean printBackground, PrintProgressUpdate progressUpdate)
    {
        CompletableFuture<Boolean> inited = new CompletableFuture<>();
        if (panel == null)
        {
            setVisibleFX(true, false, false);
        }
        JavaFXUtil.onceTrue(panel.initialisedProperty(), init -> {
            inited.complete(init);

        });
        return () -> {
            try
            {
                inited.get();
                // If we try to print off-thread
                // then things get seriously messed up for the editors
                // afterwards.  So we hop back to platform thread to print:
                CompletableFuture<Boolean> done = new CompletableFuture<>();
                JavaFXUtil.runPlatformLater(() -> {
                    job.printPage(panel.getSource().getFrame().getNode());
                    done.complete(true);
                });
                done.get();
            }
            catch (InterruptedException | ExecutionException e)
            {
                Debug.reportError(e);
            }
        };

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
    public void setEditorVisible(boolean vis, boolean openInNewWindow)
    {
        setVisibleFX(vis, true, openInNewWindow);
    }

    @OnThread(Tag.FXPlatform)
    private void setVisibleFX(boolean show, boolean bringToFront, boolean openInNewWindow)
    {
        if (panel == null && show) // No need to create the panel if we don't want to show it
        {
            createPanel(show, bringToFront, openInNewWindow);
        }

        if (panel != null)
        {
            if (openInNewWindow && !panel.isWindowVisible())
            {
                panel.setParent(pkg.getProject().createNewFXTabbedEditor(), true);
            }
            panel.setWindowVisible(show, bringToFront);
            if (callbackOnOpen != null && show)
                callbackOnOpen.run();
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
                    Exception ex = saveFX().exception;
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
                                js.handleError((int) e.startLine, (int) e.startColumn,
                                        (int) e.endLine, (int) e.endColumn, e.message, e.identifier);
                            });
                        }
                        // We need to use runLater to account for the fact that adding errors uses a runLater:
                        JavaFXUtil.runPlatformLater(() -> panel.updateErrorOverviewBar(false));
                    });
                }
            }));
        }
    }

    public void codeModified()
    {
        changedSinceLastSave = true;
        isCompiled = false;
        watcher.modificationEvent(this);
        watcher.scheduleCompilation(false, CompileReason.MODIFIED, CompileType.ERROR_CHECK_ONLY);
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
        if (panel != null && panel.isWindowVisible())
        {
            if (!foundLateErrorsForMostRecentCompile)
            {
                foundLateErrorsForMostRecentCompile = true;
                findLateErrors(mostRecentCompileIdentifier);
                // Shouldn't use the same one twice anyway as we are guarded by the boolean flag:
                mostRecentCompileIdentifier = -1;
            }
            panel.compiled();
        }

        reInitBreakpoints();
    }

    @OnThread(Tag.FXPlatform)
    private void findLateErrors(int compilationIdentifier)
    {
        panel.removeOldErrors();
        TopLevelCodeElement el = panel.getSource();
        if (el == null)
            return;
        Stream<CodeElement> allElements = Stream.concat(Stream.of((CodeElement)el), el.streamContained());
        LocationMap rootPathMap = el.toXML().buildLocationMap();
        // We must start these futures going on the FX thread
        List<Future<List<DirectSlotError>>> futures = allElements.flatMap(e -> e.findDirectLateErrors(panel, rootPathMap)).collect(Collectors.toList());
        // Then wait for them on another thread
        new Thread() {
            @Override
            @OnThread(Tag.Worker)
            public void run()
            {
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
                Platform.runLater(() -> {
                    panel.updateErrorOverviewBar(false);
                    List<DiagnosticWithShown> diagnostics = Utility.mapList(allLates, e -> e.toDiagnostic(javaFilename.getName(), frameFilename));
                    watcher.recordLateErrors(diagnostics, compilationIdentifier);
                });
            }
        }.start();
    }
        
    @Override
    public boolean compileStarted(int compilationSequence)
    {
        foundLateErrorsForMostRecentCompile = false;
        mostRecentCompileIdentifier = compilationSequence;
        if (panel != null)
            panel.flagErrorsAsOld();
        else
            queuedErrors.clear();
        // Note lastSourceRef may refer to a stale source, but this shouldn't cause any
        // significant issues.  In fact, it probably makes sense to use the source at
        // point of last save, rather than any modifications in the window since.
        return earlyErrorCheck(lastSource.findEarlyErrors(), compilationSequence);
    }

    /**
     * Given a stream of early errors, records them and returns true if there were any errors (i.e. if the stream was non-empty)
     */
    //package-visible
    @OnThread(Tag.FXPlatform)
    boolean earlyErrorCheck(Stream<SyntaxCodeError> earlyErrors, int compilationIdentifier)
    {
        List<SyntaxCodeError> earlyList = earlyErrors.collect(Collectors.toList());
        List<DiagnosticWithShown> diagnostics = Utility.mapList(earlyList, e -> e.toDiagnostic(javaFilename.getName(), frameFilename));
        watcher.recordEarlyErrors(diagnostics, compilationIdentifier);
        return !earlyList.isEmpty();
    }

    public AssistContent[] getCompletions(TopLevelCodeElement allCode, PosInSourceDoc pos, ExpressionSlot<?> completing, CodeElement codeEl)
    {
        ExpressionTypeInfo suggests = allCode.getCodeSuggestions(pos, completing);
        
        ArrayList<AssistContent> joined = new ArrayList<>();
        if (suggests != null)
        {
            AssistContent[] assists = ParseUtils.getPossibleCompletions(suggests, javadocResolver, null, null);
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
                ExpressionTypeInfo greenfootClass = new ExpressionTypeInfo(new GenTypeClass(greenfootClassRef), null, null, true, false);
                AssistContent[] greenfootStatic = ParseUtils.getPossibleCompletions(greenfootClass, javadocResolver, null, null);
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
        ExpressionTypeInfo suggests = allCode.getCodeSuggestions(pos, null);
        if (suggests == null)
            return Collections.emptyList();
        List<AssistContent> members;
        if (includeOverridden)
        {
            members = new ArrayList<>();
            // Add it whether overridden or not:
            ParseUtils.getPossibleCompletions(suggests, javadocResolver, (ac, isOverridden) -> members.add(ac), null);
        }
        else
        {
            AssistContent[] result = ParseUtils.getPossibleCompletions(suggests, javadocResolver, null, null);
            if (result == null)
                members = Collections.emptyList();
            else
                members = Arrays.asList(result);
        }

        return members.stream().filter(ac -> kinds == null || kinds.contains(ac.getKind())).collect(Collectors.toList());
    }

    @Override
    public void insertAppendMethod(NormalMethodElement method, FXPlatformConsumer<Boolean> after)
    {
        if (panel == null)
        {
            createPanel(false, false, false);
        }
        panel.insertAppendMethod(method, after);
        codeModified();
    }

    @Override
    public void insertMethodCallInConstructor(String className, CallElement methodName, FXPlatformConsumer<Boolean> after)
    {
        if (panel == null)
        {
            createPanel(false, false, false);
        }
        panel.insertMethodCallInConstructor(methodName, after);
        codeModified();
    }

    @Override
    @OnThread(Tag.Any)
    public EditorFixesManager getEditorFixesManager(){
        return editorFixesMgr;
    }

    public boolean containsImport(String importName)
    {
        return panel.containsImport(importName);
    }

    @Override
    public void addImportFromQuickFix(String importName)
    {
        panel.addImport(importName);
    }

    @Override
    public void removeImports(List<String> importTargets)
    {
        if (panel == null)
        {
            createPanel(false, false, false);
        }
        panel.removeImports(importTargets);
        codeModified();
    }

    /**
     * Set the header image (in the tab header) for this editor
     * @param image The image to use (any size).
     */
    @Override
    public void setHeaderImage(Image image)
    {
        if (panel != null)
        {
            panel.setHeaderImage(image);
        }
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public void setLastModified(long millisSinceEpoch)
    {
        // We don't keep track of disk modification time, so nothing to do.
    }

    @OnThread(Tag.FX)
    public TopLevelCodeElement getSource()
    {
        return panel.getSource();
    }

    @OnThread(Tag.FXPlatform)
    public List<AssistContentThreadSafe> getLocalTypes(Class<?> superType, Set<Kind> kinds)
    {
        return ParseUtils.getLocalTypes(pkg, superType, kinds);
    }

    public void showNextError()
    {
       panel.nextError();
    }

    @Override
    public void focusMethod(String methodName, List<String> paramTypes)
    {
        if (panel == null) {
            createPanel(true, true, false);
        }
        panel.focusMethod(methodName);
    }

    public JavadocResolver getJavadocResolver()
    {
        return javadocResolver;
    }

    @OnThread(Tag.Any)
    public EditorWatcher getWatcher()
    {
        return watcher;
    }

    @OnThread(Tag.FXPlatform)
    public void recordEdits(StrideEditReason reason)
    {
        SaveResult result = saveFX();
        if (result.exception == null)
        {
            watcher.recordStrideEdit(result.javaResult.javaSourceStringContent, result.savedSource, reason);
        }
        else
            Debug.reportError(result.exception);
    }

    @Override
    public void addImplements(String className, ClassInfo classInfo)
    {
        if (panel == null) {
            createPanel(false, false, false);
        }
        JavaFXUtil.onceTrue(panel.initialisedProperty(), p -> panel.addImplements(className));
    }

    @Override
    public void setExtendsClass(String className, ClassInfo classInfo)
    {
        if (panel == null) {
            createPanel(false, false, false);
        }
        JavaFXUtil.onceTrue(panel.initialisedProperty(), p -> panel.addExtends(className));
    }

    @Override
    public void removeExtendsClass(ClassInfo classInfo)
    {
        if (panel == null) {
            createPanel(false, false, false);
        }
        JavaFXUtil.onceTrue(panel.initialisedProperty(), p -> panel.removeExtendsClass());
    }

    @Override
    public void addExtendsInterface(String interfaceName, ClassInfo classInfo)
    {
        if (panel == null) {
            createPanel(false, false, false);
        }
        JavaFXUtil.onceTrue(panel.initialisedProperty(), p -> panel.addExtends(interfaceName));
    }

    @Override
    public void removeExtendsOrImplementsInterface(String interfaceName, ClassInfo classInfo)
    {
        if (panel == null) {
            createPanel(false, false, false);
        }
        JavaFXUtil.onceTrue(panel.initialisedProperty(), p -> panel.removeExtendsOrImplementsInterface(interfaceName));
    }
    
    /**
     * Get the package of the class being edited by this editor.
     */
    public bluej.pkgmgr.Package getPackage()
    {
        return pkg;
    }

    @Override
    public void removeErrorHighlights()
    {
        if (panel != null)
        {
            panel.flagErrorsAsOld();
            panel.removeOldErrors();
        }
    }
}
