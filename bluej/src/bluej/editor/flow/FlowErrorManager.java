package bluej.editor.flow;

import bluej.editor.flow.FlowEditorPane.ErrorQuery;
import bluej.editor.flow.JavaSyntaxView.ParagraphAttribute;
import bluej.parser.SourceLocation;
import bluej.utility.Utility;
import bluej.utility.javafx.FXPlatformConsumer;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.IndexRange;
import org.fxmisc.richtext.model.TwoDimensional.Bias;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class FlowErrorManager implements ErrorQuery
{
    private final ObservableList<ErrorDetails> errorInfos = FXCollections.observableArrayList();
    private FlowEditor editor;
    private Consumer<Boolean> setNextErrorEnabled;
    /**
     * Construct a new FlowErrorManager to manage error display for the specified editor instance.
     * The new manager should be set as the document listener so that it receives notification
     * of parser errors as they occur.
     */
    public FlowErrorManager(FlowEditor editor, Consumer<Boolean> setNextErrorEnabled)
    {
        this.editor = editor;
        this.setNextErrorEnabled = setNextErrorEnabled;
    }

    /**
     * Add a compiler error highlight.
     * @param startPos  The document position where the error highlight should begin
     * @param endPos    The document position where the error highlight should end
     */
    public void addErrorHighlight(int startPos, int endPos, String message, int identifier)
    {
        if (endPos < startPos)
            throw new IllegalArgumentException("Error ends before it begins: " + startPos + " to " + endPos);
        FlowEditorPane sourcePane = editor.getSourcePane();
        sourcePane.getDocument().addLineAttribute(editor.getSourcePane().getDocument().getLineFromPosition(startPos), ParagraphAttribute.ERROR, true);
        errorInfos.add(new FlowErrorManager.ErrorDetails(startPos, endPos, message, identifier));
        setNextErrorEnabled.accept(true);
        editor.updateHeaderHasErrors(true);
        sourcePane.repaint();
    }

    /**
     * Remove any existing compiler error highlight.
     */
    public void removeAllErrorHighlights()
    {
        FlowEditorPane sourcePane = editor.getSourcePane();
        sourcePane.getDocument().removeLineAttributeThroughout(ParagraphAttribute.ERROR);
        errorInfos.clear();
        setNextErrorEnabled.accept(false);
        editor.updateHeaderHasErrors(false);
        sourcePane.repaint();
    }

    public void listenForErrorChange(FXPlatformConsumer<List<FlowErrorManager.ErrorDetails>> listener)
    {
        errorInfos.addListener((ListChangeListener<? super FlowErrorManager.ErrorDetails>) c -> listener.accept(Collections.unmodifiableList(errorInfos)));
    }

    // Returns null if there is no next error.
    public FlowErrorManager.ErrorDetails getNextErrorPos(int from)
    {
        int lowestDist = Integer.MIN_VALUE; // Negative means before the given position
        FlowErrorManager.ErrorDetails next = null;

        for (FlowErrorManager.ErrorDetails err : errorInfos)
        {
            // If error is before the given position, it will be a negative distance
            // If error is ahead, it will be a positive distance
            // If we are within the error, the position will also show up negative,
            // which means we will treat it as low priority, and advance to next error instead
            final int dist = err.startPos - from;

            if (next == null
                    // If the current best is before the position, ours is better if either
                    // it's after the position, or it's even further before
                    || (lowestDist <= 0 && (dist > 0 || dist <= lowestDist))
                    // If the current best is after the position, ours is better only if
                    // we are earlier
                    || (lowestDist > 0 && dist > 0 && dist <= lowestDist))
            {
                next = err;
                lowestDist = dist;
            }
        }
        return next;
    }

    /**
     * Notify the error manager of a change to the document.
     */
    public void documentContentChanged()
    {
        setNextErrorEnabled.accept(false);
    }

    /**
     * Get the error code (or message) at a particular document position.
     * If there are multiple errors at the same position it will return the 
     * right most error at that position.
     */
    public FlowErrorManager.ErrorDetails getErrorAtPosition(int pos)
    {
        return errorInfos.stream()
                .filter(e -> e.containsPosition(pos))
                .reduce((first, second) -> second)
                .orElse(null);
    }

    /**
     * Returns null if no error on that line
     */
    @OnThread(Tag.FXPlatform)
    public FlowErrorManager.ErrorDetails getErrorOnLine(int lineIndex)
    {
        final int lineStart = editor.getOffsetFromLineColumn(new SourceLocation(lineIndex + 1, 1));
        if (lineIndex + 1 >= editor.numberOfLines())
        {
            return errorInfos.stream().filter(e -> e.endPos >= lineStart).findFirst().orElse(null);
        }
        else
        {
            int lineEnd = editor.getOffsetFromLineColumn(new SourceLocation(lineIndex + 2, 1));
            return errorInfos.stream().filter(e -> e.startPos <= lineEnd && e.endPos >= lineStart).findFirst().orElse(null);
        }
    }
    
    public List<IndexRange> getErrorUnderlines()
    {
        return Utility.mapList(errorInfos, e -> new IndexRange(e.startPos, e.endPos));
    }

    public boolean hasErrorHighlights()
    {
        return !errorInfos.isEmpty();
    }

    public static class ErrorDetails
    {
        public final int startPos;
        public final int endPos;
        public final String message;
        public final int identifier;
        private ErrorDetails(int startPos, int endPos, String message, int identifier)
        {
            this.startPos = startPos;
            this.endPos = endPos;
            this.message = message;
            this.identifier = identifier;
        }

        public boolean containsPosition(int pos)
        {
            return startPos <= pos && pos <= endPos;
        }
    }

}
