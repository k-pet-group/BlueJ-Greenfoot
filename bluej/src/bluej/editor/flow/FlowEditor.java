package bluej.editor.flow;

import bluej.compiler.CompileType;
import bluej.compiler.Diagnostic;
import bluej.debugger.DebuggerThread;
import bluej.editor.TextEditor;
import bluej.editor.moe.ScopeColorsBorderPane;
import bluej.editor.stride.FXTabbedEditor;
import bluej.editor.stride.FlowFXTab;
import bluej.editor.stride.FrameEditor;
import bluej.parser.SourceLocation;
import bluej.parser.nodes.ParsedCUNode;
import bluej.parser.nodes.ReparseableDocument;
import bluej.parser.symtab.ClassInfo;
import bluej.prefmgr.PrefMgr.PrintSize;
import bluej.stride.framedjava.elements.CallElement;
import bluej.stride.framedjava.elements.NormalMethodElement;
import bluej.utility.Debug;
import bluej.utility.javafx.FXPlatformConsumer;
import bluej.utility.javafx.FXRunnable;
import javafx.print.PrinterJob;
import javafx.scene.control.Menu;
import javafx.scene.image.Image;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

public class FlowEditor extends ScopeColorsBorderPane implements TextEditor
{
    private final FlowEditorPane flowEditorPane = new FlowEditorPane("");
    private final Document document = flowEditorPane.getDocument();
    private final JavaSyntaxView javaSyntaxView = new JavaSyntaxView(flowEditorPane, this);
    private final FetchTabbedEditor fetchTabbedEditor;
    private final FlowFXTab fxTab = new FlowFXTab(this, "TODOFLOW Title");

    public static interface FetchTabbedEditor
    {
        FXTabbedEditor getFXTabbedEditor(boolean newWindow);
    }

    // TODOFLOW remove this once all its callers are implemented.
    private final class UnimplementedException extends RuntimeException {}


    public FlowEditor(FetchTabbedEditor fetchTabbedEditor)
    {
        this.fetchTabbedEditor = fetchTabbedEditor;
        setCenter(flowEditorPane);
    }

    public void requestEditorFocus()
    {
        flowEditorPane.requestFocus();
    }

    public void notifyVisibleTab(boolean visible)
    {
        throw new UnimplementedException();
    }

    public void cancelFreshState()
    {
        throw new UnimplementedException();
    }

    public void setParent(FXTabbedEditor parent, boolean partOfMove)
    {
        throw new UnimplementedException();
    }

    public List<Menu> getFXMenu()
    {
        throw new UnimplementedException();
    }

    @Override
    public boolean showFile(String filename, Charset charset, boolean compiled, String docFilename)
    {
        try
        {
            document.replaceText(0, document.getLength(), Files.readString(new File(filename).toPath(), charset));
            javaSyntaxView.enableParser(false);
            return true;
        }
        catch (IOException e)
        {
            Debug.reportError(e);
            return false;
        }
    }

    @Override
    public void clear()
    {
        document.replaceText(0, document.getLength(), "");
    }

    @Override
    public void insertText(String text, boolean caretBack)
    {
        int startPos = Math.min(flowEditorPane.getCaretPosition(), flowEditorPane.getAnchorPosition());
        flowEditorPane.replaceSelection(text);
        if (caretBack)
        {
            flowEditorPane.positionCaret(startPos);
        }
    }

    @Override
    public void setSelection(SourceLocation begin, SourceLocation end)
    {
        flowEditorPane.positionCaret(document.getPosition(end));
        flowEditorPane.positionAnchor(document.getPosition(begin));
    }

    @Override
    public SourceLocation getCaretLocation()
    {
        return document.makeSourceLocation(flowEditorPane.getCaretPosition());
    }

    @Override
    public void setCaretLocation(SourceLocation location)
    {
        flowEditorPane.positionCaret(document.getPosition(location));
    }

    @Override
    public SourceLocation getSelectionBegin()
    {
        return document.makeSourceLocation(Math.min(flowEditorPane.getCaretPosition(), flowEditorPane.getAnchorPosition()));
    }

    @Override
    public SourceLocation getSelectionEnd()
    {
        return document.makeSourceLocation(Math.max(flowEditorPane.getCaretPosition(), flowEditorPane.getAnchorPosition()));
    }

    @Override
    public String getText(SourceLocation begin, SourceLocation end)
    {
        return document.getContent(document.getPosition(begin), document.getPosition(end));
    }

    @Override
    public void setText(SourceLocation begin, SourceLocation end, String newText)
    {
        document.replaceText(document.getPosition(begin), document.getPosition(end), newText);
    }

    @Override
    public SourceLocation getLineColumnFromOffset(int offset)
    {
        return document.makeSourceLocation(offset);
    }

    @Override
    public int getOffsetFromLineColumn(SourceLocation location)
    {
        return document.getPosition(location);
    }

    @Override
    public int getLineLength(int line)
    {
        return document.getLineLength(line);
    }

    @Override
    public int numberOfLines()
    {
        return document.getLineCount();
    }

    @Override
    public int getTextLength()
    {
        return document.getLength();
    }

    @Override
    public ParsedCUNode getParsedNode()
    {
        return javaSyntaxView.getRootNode();
    }

    @Override
    public ReparseableDocument getSourceDocument()
    {
        throw new UnimplementedException();
    }

    @Override
    public void reloadFile()
    {
        throw new UnimplementedException();
    }

    @Override
    public void setEditorVisible(boolean vis, boolean openInNewWindow)
    {
        // TODOFLOW put pack the commented parts of this method.
        
        if (vis)
        {
            //checkBracketStatus();

            /*
            if (sourceIsCode && !compiledProperty.get() && sourceDocument.notYetShown)
            {
                // Schedule a compilation so we can find and display any errors:
                scheduleCompilation(CompileReason.LOADED, CompileType.ERROR_CHECK_ONLY);
            }
            */

            // Make sure caret is visible after open:
            //sourcePane.requestFollowCaret();
            //sourcePane.layout();
        }
        FXTabbedEditor fxTabbedEditor = fetchTabbedEditor.getFXTabbedEditor(false);
        /*
        if (fxTabbedEditor == null)
        {
            if (openInNewWindow)
            {
                fxTabbedEditor = defaultFXTabbedEditor.get().getProject().createNewFXTabbedEditor();
            }
            else
            {
                fxTabbedEditor = defaultFXTabbedEditor.get();
            }
        }
        else
        {
            // Checks if the editor of the selected target is already opened in a tab inside another window,
            // then do not open it in a new window unless the tab is closed.
            if (openInNewWindow && !fxTabbedEditor.containsTab(fxTab) )
            {
                fxTabbedEditor = defaultFXTabbedEditor.get().getProject().createNewFXTabbedEditor();
            }
        }
        */

        if (vis)
        {
            fxTabbedEditor.addTab(fxTab, vis, true);
        }
        fxTabbedEditor.setWindowVisible(vis, fxTab);
        if (vis)
        {
            fxTabbedEditor.bringToFront(fxTab);
            /*
            if (callbackOnOpen != null)
            {
                callbackOnOpen.run();
            }
            */

            // Allow recalculating the scopes:
            //sourceDocument.notYetShown = false;

            // Make sure caret is visible after open:
            //sourcePane.requestFollowCaret();
            //sourcePane.layout();
        }
    }

    @Override
    public boolean isOpen()
    {
        throw new UnimplementedException();
    }

    @Override
    public void save() throws IOException
    {
        throw new UnimplementedException();
    }

    @Override
    public void close()
    {
        throw new UnimplementedException();
    }

    @Override
    public void refresh()
    {
        throw new UnimplementedException();
    }

    @Override
    public void displayMessage(String message, int lineNumber, int column)
    {
        throw new UnimplementedException();
    }

    @Override
    public boolean displayDiagnostic(Diagnostic diagnostic, int errorIndex, CompileType compileType)
    {
        throw new UnimplementedException();
    }

    @Override
    public boolean setStepMark(int lineNumber, String message, boolean isBreak, DebuggerThread thread)
    {
        throw new UnimplementedException();
    }

    @Override
    public void writeMessage(String msg)
    {
        throw new UnimplementedException();
    }

    @Override
    public void removeStepMark()
    {
        throw new UnimplementedException();
    }

    @Override
    public void changeName(String title, String filename, String javaFilename, String docFileName)
    {
        throw new UnimplementedException();
    }

    @Override
    public void setCompiled(boolean compiled)
    {
        throw new UnimplementedException();
    }

    @Override
    public boolean compileStarted(int compilationSequence)
    {
        throw new UnimplementedException();
    }

    @Override
    public void compileFinished(boolean successful, boolean classesKept)
    {
        throw new UnimplementedException();
    }

    @Override
    public void removeBreakpoints()
    {
        throw new UnimplementedException();
    }

    @Override
    public void reInitBreakpoints()
    {
        throw new UnimplementedException();
    }

    @Override
    public boolean isModified()
    {
        throw new UnimplementedException();
    }

    @Override
    public FXRunnable printTo(PrinterJob printerJob, PrintSize printSize, boolean printLineNumbers, boolean printBackground)
    {
        throw new UnimplementedException();
    }

    @Override
    public void setReadOnly(boolean readOnly)
    {
        throw new UnimplementedException();
    }

    @Override
    public boolean isReadOnly()
    {
        throw new UnimplementedException();
    }

    @Override
    public void showInterface(boolean interfaceStatus)
    {
        if (interfaceStatus)
            throw new UnimplementedException();
    }

    @Override
    public Object getProperty(String propertyKey)
    {
        throw new UnimplementedException();
    }

    @Override
    public void setProperty(String propertyKey, Object value)
    {
        throw new UnimplementedException();
    }

    @Override
    public TextEditor assumeText()
    {
        return this;
    }

    @Override
    public FrameEditor assumeFrame()
    {
        return null;
    }

    @Override
    public void insertAppendMethod(NormalMethodElement method, FXPlatformConsumer<Boolean> after)
    {
        throw new UnimplementedException();
    }

    @Override
    public void insertMethodCallInConstructor(String className, CallElement methodCall, FXPlatformConsumer<Boolean> after)
    {
        throw new UnimplementedException();
    }

    @Override
    public void focusMethod(String methodName, List<String> paramTypes)
    {
        throw new UnimplementedException();
    }

    @Override
    public void setExtendsClass(String className, ClassInfo classInfo)
    {
        throw new UnimplementedException();
    }

    @Override
    public void removeExtendsClass(ClassInfo classInfo)
    {
        throw new UnimplementedException();
    }

    @Override
    public void addImplements(String interfaceName, ClassInfo classInfo)
    {
        throw new UnimplementedException();
    }

    @Override
    public void addExtendsInterface(String interfaceName, ClassInfo classInfo)
    {
        throw new UnimplementedException();
    }

    @Override
    public void removeExtendsOrImplementsInterface(String interfaceName, ClassInfo classInfo)
    {
        throw new UnimplementedException();
    }

    @Override
    public void removeImports(List<String> importTargets)
    {
        throw new UnimplementedException();
    }

    @Override
    public void setHeaderImage(Image image)
    {
        throw new UnimplementedException();
    }
}
