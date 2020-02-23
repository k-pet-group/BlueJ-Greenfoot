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
import bluej.editor.TextEditor;
import bluej.editor.fixes.Correction;
import bluej.editor.fixes.Correction.CorrectionInfo;
import bluej.editor.fixes.Correction.SimpleCorrectionInfo;
import bluej.editor.fixes.Correction.TypeCorrectionInfo;
import bluej.editor.fixes.EditorFixesManager;
import bluej.editor.fixes.EditorFixesManager.FixSuggestionBase;
import bluej.parser.AssistContent;
import bluej.parser.AssistContent.CompletionKind;
import bluej.parser.AssistContentThreadSafe;
import bluej.editor.fixes.FixSuggestion;
import bluej.editor.flow.FlowEditorPane.ErrorQuery;
import bluej.editor.flow.JavaSyntaxView.ParagraphAttribute;
import bluej.parser.ExpressionTypeInfo;
import bluej.parser.ParseUtils;
import bluej.parser.SourceLocation;
import bluej.parser.nodes.FieldNode;
import bluej.parser.nodes.MethodNode;
import bluej.parser.nodes.NodeTree.NodeAndPosition;
import bluej.parser.nodes.ParsedCUNode;
import bluej.parser.nodes.ParsedNode;
import bluej.pkgmgr.target.ClassTarget;
import bluej.pkgmgr.target.role.Kind;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    private void showErrors(FlowEditor editor, FlowEditorPane sourcePane, int startPos, int endPos, String message, int identifier, Stream<AssistContentThreadSafe> imports)
    {
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

            int errorLine = editor.getLineColumnFromOffset(startPos).getLine();
            int errorLineLength = editor.getLineLength(errorLine - 1);
            SourceLocation startErrorLineSourceLocation = new SourceLocation(errorLine, 1);
            SourceLocation startErrorPosSourceLocation = editor.getLineColumnFromOffset(startPos);
            SourceLocation endErrorLineSourceLocation = new SourceLocation(errorLine, errorLineLength);
            String errorLineText = editor.getText(startErrorLineSourceLocation, endErrorLineSourceLocation);

            // set the quick fix imports if detected an unknown type error...
            if (message.contains("cannot find symbol") && message.contains("class"))
            {
                String typeName = message.substring(message.lastIndexOf(' ') + 1);
                this.message = Config.getString("editor.quickfix.unknownType.errorMsg") + typeName;
                if (possibleImports != null)
                {
                    List<AssistContentThreadSafe> possibleCorrectionsList = possibleImports
                        .filter(ac -> ac.getPackage() != null).collect(Collectors.toList());

                    // Add the fixes: import single class then import package
                    corrections.addAll(possibleCorrectionsList.stream()
                        .filter(ac -> ac.getName().equals(typeName))
                        .flatMap(ac -> Stream.of(new FixSuggestionBase((Config.getString("editor.quickfix.unknownType.fixMsg.class") + ac.getPackage() + "." + ac.getName()), () -> editor.addImportFromQuickFix(ac.getPackage() + "." + ac.getName())),
                            new FixSuggestionBase((Config.getString("editor.quickfix.unknownType.fixMsg.package") + ac.getPackage() + " (for " + ac.getName() + " class)"), () -> editor.addImportFromQuickFix(ac.getPackage() + ".*"))))
                        .collect(Collectors.toList()));

                    // Add a quick fix for correcting to an existing closely spelt type
                    Stream<CorrectionInfo> possibleCorrectionsStream = getPossibleCorrectionsStream(editor, CompletionKind.TYPE, possibleCorrectionsList, typeName);
                    if (possibleCorrectionsStream != null)
                    {
                        corrections.addAll(Correction.winnowAndCreateCorrections(typeName,
                            possibleCorrectionsStream,
                            s -> {
                                // We replace the current error with the simple type name, and if the package is in the name, we add the class in imports.
                                if (!s.contains("."))
                                {
                                    // This case applies to java.lang and classes in the empty package
                                    editor.setText(editor.getLineColumnFromOffset(startPos), editor.getLineColumnFromOffset(endPos), s);
                                }
                                else
                                {
                                    String simpleTypeName = s.substring(s.lastIndexOf('.') + 1);
                                    String packageName = s.substring(0, s.lastIndexOf('.'));
                                    // in the editor, the class name may be preceeded by the package explicitely, if that's the case, we need to maintain the "." before the class name.
                                    if (editor.getText(editor.getLineColumnFromOffset(startPos), editor.getLineColumnFromOffset(startPos + 1)).equals("."))
                                    {
                                        editor.setText(editor.getLineColumnFromOffset(startPos), editor.getLineColumnFromOffset(endPos), "." + simpleTypeName);
                                    }
                                    else
                                    {
                                        editor.setText(editor.getLineColumnFromOffset(startPos), editor.getLineColumnFromOffset(endPos), simpleTypeName);
                                    }
                                    if (!editor.getText(new SourceLocation(1, 1), editor.getLineColumnFromOffset(startPos)).contains("import " + s + ";")
                                        && !editor.getText(new SourceLocation(1, 1), editor.getLineColumnFromOffset(startPos)).contains("import " + packageName + ".*;"))
                                    {
                                        editor.addImportFromQuickFix(s);
                                    }
                                }
                                editor.refresh();
                            },
                            true));
                    }
                }
            }
            // set the quick fix "== instead of =" if :
            // detected the error is "Incompatible types: xx cannot be converted to boolean"
            else if (message.startsWith("incompatible types:") && message.endsWith("cannot be converted to boolean")
                && editor.getText(editor.getLineColumnFromOffset(startPos), editor.getLineColumnFromOffset(startPos + 1)).equals("="))
            {
                // Change the error message to a more meaningful message
                this.message = Config.getString("editor.quickfix.wrongComparisonOperator.errorMsg");
                // Ge the length of this line, but because here the method expects a 0-based value we need to offset.
                String leftCompPart = errorLineText.substring(0, startPos - editor.getOffsetFromLineColumn(startErrorLineSourceLocation));
                String rightCompPart = errorLineText.substring(startPos - editor.getOffsetFromLineColumn(startErrorLineSourceLocation) + 1);
                corrections.add(new FixSuggestionBase(Config.getString("editor.quickfix.wrongComparisonOperator.fixMsg"), () -> {
                    editor.setText(startErrorLineSourceLocation, endErrorLineSourceLocation, (leftCompPart + "==" + rightCompPart));
                    editor.refresh();
                }));
            }
            // set the quick fix correct a wrong variable/declare it:
            // detected the error is "cannot find symbol - variable"
            else if (message.startsWith("cannot find symbol -   variable "))
            {
                // Change the error message to a more meaningful message
                String varName = message.substring(message.lastIndexOf(' ') + 1);
                this.message = Config.getString("editor.quickfix.undeclaredVar.errorMsg") + varName;

                // Add a quick fix for correcting to an existing closely spelt variable
                Stream<CorrectionInfo> possibleCorrectionsStream = getPossibleCorrectionsStream(editor, CompletionKind.FIELD);
                if (possibleCorrectionsStream != null)
                {
                    corrections.addAll(bluej.editor.fixes.Correction.winnowAndCreateCorrections(varName,
                        possibleCorrectionsStream,
                        s -> editor.setText(editor.getLineColumnFromOffset(startPos), editor.getLineColumnFromOffset(endPos), s)));
                }
                // If the variable is in a single line assignment (i.e. a line starting with "<var> = " then we propose declaration
                // locally and at the class level; the type cannot be inferred so we used a placeholder: "_type_".
                if (editor.getText(startErrorLineSourceLocation, endErrorLineSourceLocation).trim().matches("^(" + varName + ")\\s*=[^=]*$"))
                {
                    int indexOfEqualOp = editor.getText(startErrorLineSourceLocation, endErrorLineSourceLocation).indexOf('=');
                    String declarationRightPart = editor.getText(startErrorLineSourceLocation, endErrorLineSourceLocation).substring(indexOfEqualOp + 1).trim();
                    String typePlaceholder = "_type_";

                    // Add suggestion for declaring local variable (not: the error won't be on a assignement made in a class, so we don't need to check where we are)
                    corrections.add(new FixSuggestionBase(Config.getString("editor.quickfix.undeclaredVar.fixMsg.local"), () -> {
                        editor.setText(startErrorPosSourceLocation, endErrorLineSourceLocation, (typePlaceholder + " " + varName + "= " + declarationRightPart));
                        // Select the type placeholder for suggesting the user to fill it...
                        editor.setSelection(startErrorPosSourceLocation, new SourceLocation(errorLine, startErrorPosSourceLocation.getColumn() + typePlaceholder.length()));
                        editor.refresh();
                    }));
                    // Add suggestion for declaring global field
                    corrections.add(new FixSuggestionBase(Config.getString("editor.quickfix.undeclaredVar.fixMsg.class"), () -> {
                        // We try to find the right location to declare the field in the class (last of the fields or just after class declaration if no fields)
                        int newClassFieldPos = getNewClassFieldPos(editor);
                        if (newClassFieldPos > -1)
                        {
                            // We need to find the correction indentation based on the previous line
                            int prevLine = editor.getLineColumnFromOffset(newClassFieldPos - 1).getLine();
                            String prevLineStr = editor.getText(new SourceLocation(prevLine, 1), new SourceLocation(prevLine, editor.getLineLength(prevLine - 1)));
                            Matcher indentMatcher = Pattern.compile("^\\s+").matcher(prevLineStr);
                            String newLineIndentStr = (editor.getText(new SourceLocation(prevLine, 1), new SourceLocation(prevLine, editor.getLineLength(prevLine - 1))).contains(" class ")) ? "    " : "";
                            String indentationStr = (indentMatcher.find()) ? indentMatcher.group(0) : "";
                            editor.setText(editor.getLineColumnFromOffset(newClassFieldPos), editor.getLineColumnFromOffset(newClassFieldPos),
                                (newLineIndentStr + indentationStr + "private " + typePlaceholder + " " + varName + ";\n"));
                            // Select the type placeholder for suggesting the user to fill it...
                            editor.setSelection(new SourceLocation(prevLine + 1, newLineIndentStr.length() + indentationStr.length() + "private ".length() + 1),
                                new SourceLocation(prevLine + 1, newLineIndentStr.length() + indentationStr.length() + "private ".length() + 1 + typePlaceholder.length()));
                            editor.refresh();
                        }
                        else
                        {
                            throw new RuntimeException("Cannot find the position for declaring a new class field.");
                        }
                    }));
                }
            }
            else if (message.startsWith("cannot find symbol -   method "))
            {
                // Change the error message to a more meaningful message
                String methodName = message.substring(message.lastIndexOf(' ') + 1, message.lastIndexOf('('));
                this.message = Config.getString("editor.quickfix.undeclaredMethod.errorMsg") + methodName + "(...)";

                // Add a quick fix for correcting to an existing closely spelt method
                Stream<CorrectionInfo> possibleCorrectionsStream = getPossibleCorrectionsStream(editor, CompletionKind.METHOD);
                if (possibleCorrectionsStream != null)
                {
                    corrections.addAll(bluej.editor.fixes.Correction.winnowAndCreateCorrections(methodName,
                        possibleCorrectionsStream,
                        s -> editor.setText(editor.getLineColumnFromOffset(startPos), editor.getLineColumnFromOffset(endPos), s)));
                    editor.refresh();
                }
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

        /**
         * Gets a position in the class where a new class field can be inserted.
         *
         * @param editor The current editor of the class
         * @return the position where the field can be inserted, -1 if the position cannot be evaluated.
         */
        private int getNewClassFieldPos(FlowEditor editor)
        {
            if (editor == null)
                return -1;

            TextEditor e = editor.assumeText();
            ParsedCUNode pcuNode = e.getParsedNode();

            if (pcuNode == null)
                return -1;

            ParsedNode positionNode = pcuNode.getCurrentPosNode(startPos, 0);
            if (positionNode == null)
                return -1;

            // Find the containing class of the node
            ParsedNode classNode = (positionNode instanceof MethodNode) ? positionNode.getParentNode() : positionNode;
            // Find the last field in that class
            Iterator<NodeAndPosition<ParsedNode>> classNodeChildren = classNode.getChildren(0);
            int posOfNextField = -1;
            int posOfFirstField = 0;
            while (classNodeChildren.hasNext())
            {
                NodeAndPosition<ParsedNode> classNodeChild = classNodeChildren.next();
                if (classNodeChild.getNode() instanceof FieldNode)
                {
                    if (((FieldNode) classNodeChild.getNode()).isFirstFieldNode())
                        posOfFirstField = classNodeChild.getPosition();

                    int offset = classNodeChild.getNode().getAbsoluteEditorPosition();

                    // The position for a new field declaration needs to be found by getting parents offset, the last field position,
                    // the field line (from position) size, and a new line.
                    int lastFieldLine = editor.getLineColumnFromOffset(classNodeChild.getPosition() + offset).getLine();
                    String lastFieldLineStr = editor.getText(new SourceLocation(lastFieldLine, 1), new SourceLocation(lastFieldLine, editor.getLineLength(lastFieldLine - 1)));
                    posOfNextField = offset + posOfFirstField + lastFieldLineStr.replaceAll("^\\s+", "").length() + 1;
                }
            }
            if (posOfNextField > -1)
            {
                // We found the last field in the class
                return posOfNextField;
            }
            else
            {
                // We found no field in the class, so we return the position of the class relative to its potential parent,
                // plus a new line.
                int offset = classNode.getAbsoluteEditorPosition();
                return offset + classNode.getOffsetFromParent() + 1;
            }
        }

        private Stream<CorrectionInfo> getPossibleCorrectionsStream(FlowEditor editor, CompletionKind kind){
            return getPossibleCorrectionsStream(editor, kind, null, null);
        }
        private Stream<CorrectionInfo> getPossibleCorrectionsStream(FlowEditor editor, CompletionKind kind, List<AssistContentThreadSafe> possibleCorrectionAlImports, String errorStr)
        {
            if (editor == null)
                return null;

            TextEditor e = editor.assumeText();
            ParsedCUNode pcuNode = e.getParsedNode();

            if (pcuNode == null)
                return null;

            ExpressionTypeInfo suggests = pcuNode.getExpressionType(startPos, e.getSourceDocument());
            if (suggests == null)
                return null;

            ParsedNode positionNode = pcuNode.getCurrentPosNode(startPos, 0);
            if (positionNode == null)
                return null;

            // Completions are used to get methods and fields/variables;
            // for types we use another method
            if (kind.equals(CompletionKind.TYPE))
            {
                List<AssistContentThreadSafe> types = new ArrayList<>();
                int errorLine = editor.getLineColumnFromOffset(startPos).getLine();
                int errorLineLength = editor.getLineLength(errorLine-1);
                String errorLineStr = editor.getText(new SourceLocation(errorLine  , 1),new SourceLocation(errorLine,errorLineLength));
                String errorFulltypeStrRegeix = "([^ ]*)("+errorStr+")([^ ]*)";
                Pattern pattern = Pattern.compile(errorFulltypeStrRegeix);
                Matcher matcher = pattern.matcher(errorLineStr);
                matcher.find();
                String errorFullTypeStrPrefix = matcher.group(1);
                String errorFullTypeStrSuffix = matcher.group(3);

                // First get project's (package) classes
                if(editor.getWatcher() instanceof ClassTarget)
                {
                    ClassTarget ct = ((ClassTarget) editor.getWatcher());
                    List<AssistContentThreadSafe> projPackClasses = ParseUtils.getLocalTypes(ct.getPackage(), null, Kind.all());
                    //We need to check that the class can actually be added, for example, if the error is on "java.io.tes" (at "tes") and that we have Test class somewhere in the project,
                    //it should not be proposed as a correction because the java.io.Test does not exit...
                    removeCorrectionsTriggeringError(projPackClasses, errorFullTypeStrPrefix);
                    projPackClasses.sort(Comparator.comparing(AssistContentThreadSafe::getName));
                    types.addAll(projPackClasses);
                }
                // Get primitives and "common" classes
                List<AssistContentThreadSafe> commmonTypes = new ArrayList<>();
                // if the type has a prefix (like "java.io") or a suffix, we do not add primitives since they cannot be used
                if (errorFullTypeStrPrefix.length() == 0 && errorFullTypeStrSuffix.length() == 0)
                    commmonTypes.addAll(editor.getEditorFixesManager().getPrimitiveTypes());
                if (possibleCorrectionAlImports !=null){
                    // We manually add java.lang classes as they are not included in the imports
                    try
                    {
                        commmonTypes.addAll(editor.getEditorFixesManager().getJavaLangImports().get().stream().filter(ac -> ac.getPackage() != null).collect(Collectors.toList()));
                    }
                    catch (InterruptedException | ExecutionException ex)
                    {
                        throw new RuntimeException(ex);
                    }
                    // We filter the imports to : commonly used classes and classes imported in the class explicitly
                    commmonTypes.addAll(possibleCorrectionAlImports.stream()
                        .filter(ac -> Correction.isClassInUsualPackagesForCorrections(ac) || editor.getText(new SourceLocation(1,1), editor.getLineColumnFromOffset(startPos)).contains("import "+ac.getPackage()+"."+ac.getName()+";")
                        || editor.getText(new SourceLocation(1,1), editor.getLineColumnFromOffset(startPos)).contains("import "+ac.getPackage()+".*;"))
                        .collect(Collectors.toList()));
                    //We need to check that the class can actually be added, for example, if the error is on "java.io.Strin" (at "Strin") "String" is a possible correction ,
                    //it should not be proposed as a correction because the java.io.String does not exit...
                    removeCorrectionsTriggeringError(commmonTypes, errorFullTypeStrPrefix);
                }
                commmonTypes.sort(Comparator.comparing(AssistContentThreadSafe::getName));
                types.addAll(commmonTypes);
            return types.stream()
                .map(TypeCorrectionInfo::new);
            }
            else
            {
                AssistContent[] values = ParseUtils.getPossibleCompletions(suggests, editor.getProject().getJavadocResolver(), null,
                   (kind.equals(CompletionKind.FIELD) || kind.equals(CompletionKind.LOCAL_VAR)) ? positionNode : null);
                if (values == null)
                    return null;

                // We only propose the possible completion of a same kind request,
                // and distinct values: meaning for variables, the correction is done for a local variable when there is an ambiguity.
                return Arrays.stream(values)
                    .filter(ac -> ac.getKind().equals(kind))
                    .distinct()
                    .flatMap(ac -> Stream.of(ac.getName()))
                    .map(SimpleCorrectionInfo::new);
            }
        }
    }

    /**
     * Removes the types from the list for which a correction in the editor would end up with an error
     * because the correction is a part of type that may mismatch (ex. "java.io.Strin" would be corrected
     * as "java.io.String" and triggers an error). It only checks the prefix.
     *
     * @param possibleCorrectionsList the list of types to work with
     * @param errorFullTypeStrPrefix the prefix of the error type
     */
    private static void removeCorrectionsTriggeringError(List<AssistContentThreadSafe> possibleCorrectionsList, String errorFullTypeStrPrefix)
    {
        if (errorFullTypeStrPrefix.length() > 0)
            possibleCorrectionsList.removeIf(ac -> !(ac.getPackage() + ".").equals(errorFullTypeStrPrefix));
    }


}
