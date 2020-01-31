/*
 This file is part of the BlueJ program. 
 Copyright (C) 2019,2020  Michael Kolling and John Rosenberg

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
package bluej.editor.flow;

import bluej.Config;
import bluej.editor.fixes.EditorFixesManager;
import bluej.editor.fixes.EditorFixesManager.DoubleEqualFix;
import bluej.editor.fixes.EditorFixesManager.ImportPackageFix;
import bluej.editor.fixes.EditorFixesManager.ImportSingleFix;
import bluej.parser.AssistContentThreadSafe;
import bluej.editor.fixes.FixSuggestion;
import bluej.editor.flow.FlowEditorPane.ErrorQuery;
import bluej.editor.flow.JavaSyntaxView.ParagraphAttribute;
import bluej.parser.SourceLocation;
import bluej.utility.Utility;
import bluej.utility.javafx.FXPlatformConsumer;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.IndexRange;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
     *
     * @param startPos The document position where the error highlight should begin
     * @param endPos   The document position where the error highlight should end
     */
    public void addErrorHighlight(int startPos, int endPos, String message, int identifier)
    {
        if (endPos < startPos)
            throw new IllegalArgumentException("Error ends before it begins: " + startPos + " to " + endPos);
        FlowEditorPane sourcePane = editor.getSourcePane();
        sourcePane.getDocument().addLineAttribute(editor.getSourcePane().getDocument().getLineFromPosition(startPos), ParagraphAttribute.ERROR, true);

        EditorFixesManager efm = editor.getEditorFixesManager();

        // To avoid the interface to hang to display the errors while errors are retrieved,
        // we check the status of the imports to either just display errors without quick fixes,
        // (and launch the imports for a future compilation error highlight) or normal use.
        boolean areimportsReady = efm.areImportsready();
        if (!areimportsReady)
        {
            // imports not yet ready: first display errors without any quick fix
            showErrors(editor, sourcePane, startPos, endPos, message, identifier, null);
        }
        // prepare for the next compilation (if imports not ready)
        // or retrieve them (imports are ready)
        Utility.runBackground(() -> {
            Stream<AssistContentThreadSafe> imports = efm.getImportSuggestions().values().stream().
                flatMap(Collection::stream);
            if (areimportsReady)
            {
                Platform.runLater(() -> showErrors(editor, sourcePane, startPos, endPos, message, identifier, imports));
            }
        });
    }

    private void showErrors(FlowEditor editor, FlowEditorPane sourcePane, int startPos, int endPos, String message, int identifier, Stream<AssistContentThreadSafe> imports){
        errorInfos.add(new FlowErrorManager.ErrorDetails(editor, startPos, endPos, message, identifier, imports));
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
        sourcePane.hideAllErrorUnderlines();
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
        public final List<FixSuggestion> corrections = new ArrayList<>();

        private ErrorDetails(FlowEditor editor, int startPos, int endPos, String message, int identifier, Stream<AssistContentThreadSafe> possibleImports)
        {
            this.startPos = startPos;
            this.endPos = endPos;
            this.identifier = identifier;

            // set the quick fix imports if detected an unknown type error...
            if (message.contains("cannot find symbol") && message.contains("class"))
            {
                String typeName = message.substring(message.lastIndexOf(' ') + 1);
                this.message = Config.getString("editor.quickfix.unknownType.errorMsg") + typeName;
                if (possibleImports != null)
                {
                    corrections.addAll(possibleImports
                        .filter(ac -> ac.getPackage() != null && ac.getName().equals(typeName))
                        .flatMap(ac -> Stream.of(new ImportSingleFix(editor, ac), new ImportPackageFix(editor, ac)))
                        .collect(Collectors.toList()));
                }
            }
            // set the quick fix "== instead of =" if :
            // detected the error is either "Incompatible types: xx cannot be converted to boolean"
            else if (message.startsWith("incompatible types:") && message.endsWith("cannot be converted to boolean")
                && editor.getText(editor.getLineColumnFromOffset(startPos), editor.getLineColumnFromOffset(startPos + 1)).equals("="))
            {
                // Change the error message to a more meaningful message
                this.message = Config.getString("editor.quickfix.wrongComparisonOperator.errorMsg");
                int errorLine = editor.getLineColumnFromOffset(startPos).getLine();
                // Ge the length of this line, but because here the method expects a 0-based value we need to offset.
                int errorLength = editor.getLineLength(errorLine - 1);
                SourceLocation startLineSourceLocation = new SourceLocation(errorLine, 1);
                SourceLocation endLineSourceLocation = new SourceLocation(errorLine, errorLength);
                String errorLineText = editor.getText(startLineSourceLocation, endLineSourceLocation);
                String leftCompPart = errorLineText.substring(0, startPos - editor.getOffsetFromLineColumn(startLineSourceLocation));
                String rightCompPart = errorLineText.substring(startPos - editor.getOffsetFromLineColumn(startLineSourceLocation) + 1);
                corrections.add(new DoubleEqualFix(() -> {
                    editor.setText(startLineSourceLocation, endLineSourceLocation, (leftCompPart + "==" + rightCompPart));
                    editor.refresh();
                }));
            }
            else
            {
                // In the default case, we keep the orignial error message.
                this.message = message;
            }
        }

        public boolean containsPosition(int pos)
        {
            return startPos <= pos && pos <= endPos;
        }
    }

}
