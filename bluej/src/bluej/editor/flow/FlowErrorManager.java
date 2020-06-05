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
import bluej.parser.nodes.*;
import bluej.parser.nodes.NodeTree.NodeAndPosition;
import bluej.pkgmgr.target.role.Kind;
import bluej.utility.JavaUtils;
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

        // prepare for the next compilation (if imports not ready)
        // or retrieve them (imports are ready)
        Utility.runBackground(() -> {
            Stream<AssistContentThreadSafe> imports = efm.getImportSuggestions().values().stream().
                flatMap(Collection::stream);
            Platform.runLater(() -> showErrors(editor, sourcePane, startPos, endPos, message, identifier, imports));
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
        } else
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
        public final int italicMessageStartIndex;
        public final int italicMessageEndIndex;
        public final int identifier;
        public final List<FixSuggestion> corrections = new ArrayList<>();

        private ErrorDetails(FlowEditor editor, int startPos, int endPos, String message, int identifier, Stream<AssistContentThreadSafe> possibleImports)
        {
            this.startPos = startPos;
            this.endPos = endPos;
            this.identifier = identifier;
            int italicMessageStartIndex = -1, italicMessageEndIndex = -1;

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
                            .flatMap(ac -> Stream.of(new FixSuggestionBase((Config.getString("editor.quickfix.unknownType.fixMsg.class") + ac.getPackage() + "." + ac.getName()),
                                            () -> {
                                                editor.setSelection(startErrorPosSourceLocation, startErrorPosSourceLocation);
                                                editor.addImportFromQuickFix(ac.getPackage() + "." + ac.getName());
                                            }),
                                    new FixSuggestionBase((Config.getString("editor.quickfix.unknownType.fixMsg.package") + ac.getPackage() + " (for " + ac.getName() + " class)"),
                                            () -> {
                                                editor.setSelection(startErrorPosSourceLocation, startErrorPosSourceLocation);
                                                editor.addImportFromQuickFix(ac.getPackage() + ".*");
                                            })))
                            .collect(Collectors.toList()));

                    // Add a quick fix for correcting to an existing closely spelt type
                    Stream<CorrectionInfo> possibleCorrectionsStream = getPossibleCorrectionsStream(editor, CompletionKind.TYPE, possibleCorrectionsList, typeName, startPos);
                    if (possibleCorrectionsStream != null)
                    {
                        corrections.addAll(Correction.winnowAndCreateCorrections(typeName,
                                possibleCorrectionsStream,
                                s -> {
                                    editor.setSelection(startErrorPosSourceLocation, startErrorPosSourceLocation);
                                    // We replace the current error with the simple type name, and if the package is in the name, we add the class in imports.
                                    if (!s.contains("."))
                                    {
                                        // This case applies to java.lang and classes in the empty package
                                        editor.setText(editor.getLineColumnFromOffset(startPos), editor.getLineColumnFromOffset(endPos), s);
                                    } else
                                    {
                                        String simpleTypeName = s.substring(s.lastIndexOf('.') + 1);
                                        String packageName = s.substring(0, s.lastIndexOf('.'));
                                        // in the editor, the class name may be preceeded by the package explicitely, if that's the case, we need to maintain the "." before the class name.
                                        if (editor.getText(editor.getLineColumnFromOffset(startPos), editor.getLineColumnFromOffset(startPos + 1)).equals("."))
                                        {
                                            editor.setText(editor.getLineColumnFromOffset(startPos), editor.getLineColumnFromOffset(endPos), "." + simpleTypeName);
                                        } else
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
                    editor.setSelection(startErrorPosSourceLocation, startErrorPosSourceLocation);
                    editor.setText(startErrorLineSourceLocation, endErrorLineSourceLocation, (leftCompPart + "==" + rightCompPart));
                    editor.refresh();
                }));
            }
            // set the quick fix to correct a wrong variable or declare it:
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
                            s -> {
                                editor.setSelection(startErrorPosSourceLocation, startErrorPosSourceLocation);
                                editor.setText(editor.getLineColumnFromOffset(startPos), editor.getLineColumnFromOffset(endPos), s);
                            }));
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
                        NewFieldInfos newFieldInfos = getNewClassFieldPos(editor);
                        int newClassFieldPos = newFieldInfos.getPos();
                        String indentationForNewField = newFieldInfos.getIndentation();
                        if (newClassFieldPos > -1)
                        {
                            // We need to find the correction indentation based on the current last field
                            int prevLine = editor.getLineColumnFromOffset(newClassFieldPos - 1).getLine();
                            editor.setText(editor.getLineColumnFromOffset(newClassFieldPos), editor.getLineColumnFromOffset(newClassFieldPos),
                                    (indentationForNewField + "private " + typePlaceholder + " " + varName + ";\n"));

                            // Select the type placeholder for suggesting the user to fill it...
                            editor.setSelection(new SourceLocation(prevLine + 1, indentationForNewField.length() + "private ".length() + 1),
                                    new SourceLocation(prevLine + 1, indentationForNewField.length() + "private ".length() + 1 + typePlaceholder.length()));
                            editor.refresh();
                        } else
                        {
                            throw new RuntimeException("Cannot find the position for declaring a new class field.");
                        }
                    }));
                }
            }
            // set the quick fix to correct a misspelled method:
            // detected the error is "cannot find symbol - method"
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
                            s -> {
                                editor.setSelection(startErrorPosSourceLocation, startErrorPosSourceLocation);
                                editor.setText(editor.getLineColumnFromOffset(startPos), editor.getLineColumnFromOffset(endPos), s);
                            }));
                    editor.refresh();
                }
            }
            // set the quick fix to add a try/catch statement or throw the exception:
            // detected the error is "unreported exception [...]"
            else if (message.startsWith("unreported exception "))
            {
                // Change the error message to a more meaningful message
                String exceptionType = message.substring("unreported exception ".length(), message.indexOf(';'));
                this.message = Config.getString("editor.quickfix.unreportedException.errorMsg.part1") + exceptionType + Config.getString("editor.quickfix.unreportedException.errorMsg.part2");
                italicMessageStartIndex = this.message.indexOf(exceptionType);
                italicMessageEndIndex = italicMessageStartIndex + exceptionType.length();

                // Corrections need be done once the editor is opened --> if not yet we don't do anything at this stage
                if (editor.getProject() != null)
                {
                    // Add a quick fix for surrounding the current line with try/catch
                    // first we search for a non existing variable name for the exception var in the catch statement
                    // the initial variable name we choose is a gathering of the caps letters of the type, i.e. FileNotFoundException --> fnfe
                    // if the variable name is not available, we append a numerical suffix, starting from 1, until we find a name that hasn't been used.
                    // The search is case insensitive to "force" different variable names and avoid confusion from the user.
                    String exceptionVarNameRoot =  exceptionType.substring(exceptionType.lastIndexOf(".") + 1).replaceAll("[^A-Z]", "").toLowerCase();

                    boolean foundVarName = false;
                    String fileContent = editor.getText(new SourceLocation(1, 1), editor.getLineColumnFromOffset(editor.getTextLength()));
                    int posOfClass = getNewClassFieldPos(editor).pos;
                    int posOfContainingMethod = editor.assumeText().getParsedNode().getContainingMethodOrClassNode(startPos).getAbsoluteEditorPosition();

                    int varSuffix = 0;
                    String exceptionVarName = exceptionVarNameRoot;
                    do
                    {
                        final String exceptionVarNameFinal = exceptionVarName;
                        // look within local variables (need to indicate the search of corrections for the current method position)
                        // and within fields (need to indicate the search of corrections for the class itself)
                        Stream<CorrectionInfo> localVarsCorrInStream = getPossibleCorrectionsStream(editor, CompletionKind.LOCAL_VAR, null, null, posOfContainingMethod);
                        Stream<CorrectionInfo> fieldsCorrInStream = getPossibleCorrectionsStream(editor, CompletionKind.FIELD, null, null, posOfClass);
                        foundVarName = (localVarsCorrInStream.filter(ci -> ci.getCorrectionToCompareWith().equalsIgnoreCase(exceptionVarNameFinal))
                            .findFirst().isPresent())
                            || (fieldsCorrInStream.filter(ci -> ci.getCorrectionToCompareWith().equalsIgnoreCase(exceptionVarNameFinal))
                            .findFirst().isPresent());
                        if (foundVarName)
                        {
                            varSuffix++;
                            exceptionVarName = exceptionVarNameRoot + varSuffix;
                        }
                    } while (foundVarName);

                    // then we can build up the surrounding try/catch string
                    // We find where is the beginning of the statement
                    // IMPORTANT NOTE: the string parts fileContentxxx is a modified version of the source code:
                    //    comments and string literals are obfuscated so be careful when using it.
                    // The reason to do so is to avoid special Java characters (such as ';') contained in a comment or a string literal
                    // to be matched when searching those characters in a code portion.
                    String fileContentBeforeErrorPart = JavaUtils.blankCodeCommentsAndStringLiterals(fileContent.substring(0, startPos),'0');
                    int prevStatementPos = Math.max(fileContentBeforeErrorPart.lastIndexOf('{'), fileContentBeforeErrorPart.lastIndexOf(';'));
                    if (prevStatementPos == -1)
                    {
                        this.italicMessageStartIndex = italicMessageStartIndex;
                        this.italicMessageEndIndex = italicMessageEndIndex;
                        return;
                    }
                    int statementStartPos = prevStatementPos +1;
                    while(statementStartPos < fileContentBeforeErrorPart.length() && Character.isWhitespace(fileContentBeforeErrorPart.charAt(statementStartPos)))
                        statementStartPos++;
                    String fileContentAfterErrorPart = JavaUtils.blankCodeCommentsAndStringLiterals(fileContent.substring(startPos), '0');
                    int statementEndPos = 0;

                    boolean needNewLine = (editor.getLineColumnFromOffset(prevStatementPos).getLine() == errorLine);
                    boolean needSurroundingBrackets = false;
                    boolean foundEnd = false;
                    int searchIndex = 0;
                    int openedBracket = 0;
                    if(fileContentBeforeErrorPart.substring(prevStatementPos,startPos).contains("->")){
                        //if we have a lambda expression without curly brackets, we add them and force a line return
                        //and surround with try catch the all right hand part of the lambda
                        needNewLine = true;
                        needSurroundingBrackets = true;
                        statementStartPos = fileContentBeforeErrorPart.lastIndexOf("->") + "->".length();
                        // the error position is just before the '(' of the method throwing an exception
                        // so we look up for either an ending ')' without opening match or a comma or a semicolon or a colon
                        while (!foundEnd && searchIndex < fileContentAfterErrorPart.length())
                        {
                            char c = fileContentAfterErrorPart.charAt(searchIndex);
                            if (c == '(')
                                openedBracket++;
                            else if (c == ',' || c == ';' || c == ':')
                            {
                                foundEnd = true;
                            } else if (c == ')')
                            {
                                openedBracket--;
                                foundEnd = (openedBracket < 0);
                            }
                            if (foundEnd)
                                statementEndPos = startPos + searchIndex;
                            searchIndex++;
                        }
                    } else
                    {
                        // the error position is just before the '(' of the method throwing an exception
                        // so we look up for a semi colon that is not somewhere inside the method (if lambdas in)
                        while (!foundEnd && searchIndex < fileContentAfterErrorPart.length())
                        {
                            char c = fileContentAfterErrorPart.charAt(searchIndex);
                            if (c == '(')
                                openedBracket++;
                            else if (c == ')')
                            {
                                openedBracket--;
                            }
                            else if (c == ';')
                            {
                                foundEnd = (openedBracket == 0);
                            }
                            if (foundEnd)
                                statementEndPos = startPos + searchIndex + 1;
                            searchIndex++;
                        }

                    }

                    String initIdent = errorLineText.substring(0, errorLineLength - 1  - (errorLineText.replaceAll("^\\s*", "").length()));
                    String newIndentSpacing = "    ";
                    if(needSurroundingBrackets)
                        initIdent += newIndentSpacing;
                    StringBuffer tryCatchString = new StringBuffer(
                            ((needNewLine) ? "\n" : "")
                            + ((needSurroundingBrackets) ? initIdent.substring(0, initIdent.length() - newIndentSpacing.length()) + "{\n" : "")
                            + ((needSurroundingBrackets) ? initIdent : "") + "try\n"
                            + initIdent + "{\n"
                            + initIdent + newIndentSpacing + fileContent.substring(statementStartPos, statementEndPos) + ((needSurroundingBrackets) ? ";" : "") + "\n"
                            + initIdent + "}\n"
                            + initIdent + "catch (" + exceptionType + " " + exceptionVarName + ")\n"
                            + initIdent + "{\n"
                            + initIdent + newIndentSpacing);
                    int posOfCatchStatement = tryCatchString.length();
                    String catchStatement = exceptionVarName + ".printStackTrace();";
                    tryCatchString.append(catchStatement + "\n"
                            + initIdent + "}" + ((needSurroundingBrackets) ? "\n" : "")
                            + ((needSurroundingBrackets) ? initIdent.substring(0, initIdent.length() - newIndentSpacing.length()) + "}" : ""));

                    // and prepare the quick fix
                    int finalStatementEndPos = statementEndPos;
                    int finalStatementStartPos = statementStartPos;
                    corrections.add(new FixSuggestionBase(Config.getString("editor.quickfix.unreportedException.fixMsg.trycatch"), () -> {
                        editor.setSelection(editor.getLineColumnFromOffset(finalStatementStartPos), editor.getLineColumnFromOffset(finalStatementEndPos));
                        editor.setText(editor.getLineColumnFromOffset(finalStatementStartPos), editor.getLineColumnFromOffset(finalStatementEndPos),
                                tryCatchString.toString());
                        editor.refresh();
                        //select the catch statement
                        editor.setSelection(editor.getLineColumnFromOffset(finalStatementStartPos + posOfCatchStatement), editor.getLineColumnFromOffset(finalStatementStartPos + posOfCatchStatement + catchStatement.length()));
                    }));

                    // Add a second quick fix for adding a throws statement, if not in a lambda
                    if (!needSurroundingBrackets)
                    {
                        // first the throws statement is created all if none exist, otherwise we just append the exception
                        int posOfClosingMethodParamsBracket = fileContentBeforeErrorPart.indexOf(")", posOfContainingMethod);
                        int posOfOpeningMethodBodyBracket = fileContentBeforeErrorPart.indexOf('{', posOfContainingMethod);
                        if (posOfClosingMethodParamsBracket == -1 || posOfOpeningMethodBodyBracket == -1)
                        {
                            this.italicMessageStartIndex = italicMessageStartIndex;
                            this.italicMessageEndIndex = italicMessageEndIndex;
                            return;
                        }
                        boolean methodHasThrows = fileContentBeforeErrorPart.substring(posOfClosingMethodParamsBracket, posOfOpeningMethodBodyBracket).contains(" throws ");
                        SourceLocation methodSourceLocation = editor.getLineColumnFromOffset(posOfContainingMethod);
                        boolean openingMethodBodyBracketOnSameLine = (editor.getLineColumnFromOffset(posOfOpeningMethodBodyBracket).getLine() == methodSourceLocation.getLine());
                        String methodSignNoIdent = fileContent.substring(posOfContainingMethod, posOfOpeningMethodBodyBracket);
                        final int posOfNewThrowsAddition = (methodHasThrows)
                                ? posOfContainingMethod + methodSignNoIdent.indexOf(" throws ") + " throws ".length()
                                : (openingMethodBodyBracketOnSameLine) ? posOfOpeningMethodBodyBracket : posOfContainingMethod + methodSignNoIdent.replaceAll("\\s*$", "").length();
                        String throwsStatement =
                                ((Character.isWhitespace(fileContentBeforeErrorPart.charAt(posOfNewThrowsAddition - 1))) ? "" : " ")
                                        + ((methodHasThrows) ? (exceptionType + ",") : ("throws " + exceptionType))
                                        + ((openingMethodBodyBracketOnSameLine) ? " " : "");

                        // and prepare the quick fix
                        corrections.add(new FixSuggestionBase(Config.getString("editor.quickfix.unreportedException.fixMsg.throws"), () -> {
                            int currentLocation = editor.getOffsetFromLineColumn(editor.getCaretLocation());
                            editor.setSelection(editor.getLineColumnFromOffset(posOfNewThrowsAddition), editor.getLineColumnFromOffset(posOfNewThrowsAddition));
                            editor.setText(editor.getLineColumnFromOffset(posOfNewThrowsAddition), editor.getLineColumnFromOffset(posOfNewThrowsAddition), throwsStatement);
                            editor.refresh();
                            //reset the cursor at where the user was at
                            editor.setSelection(editor.getLineColumnFromOffset(currentLocation+throwsStatement.length()),editor.getLineColumnFromOffset(currentLocation+throwsStatement.length()));
                        }));
                    }
                }
            }
            else
            {
                // In the default case, we keep the original error message.
                this.message = message;
            }

            this.italicMessageStartIndex = italicMessageStartIndex;
            this.italicMessageEndIndex = italicMessageEndIndex;
        }

        public boolean containsPosition(int pos)
        {
            return startPos <= pos && pos <= endPos;
        }

        /**
         * Helper class for quick fixes (declaration of a field in the class)
         * Informs the LINE position of a new field to insert in the class,
         * and the indentation expected for the new field
         */
        private final class NewFieldInfos
        {
            private final int pos;
            private final String indentation;

            public NewFieldInfos(int pos, String indentation)
            {
                this.pos = pos;
                this.indentation = indentation;
            }

            public int getPos()
            {
                return pos;
            }

            public String getIndentation()
            {
                return indentation;
            }
        }

        /**
         * Gets a position of the line in the class where a new class field can be inserted.
         *
         * @param editor The current editor of the class
         * @return a NewFieldInfos object with, -1 if the position cannot be evaluated
         */
        private NewFieldInfos getNewClassFieldPos(FlowEditor editor)
        {
            if (editor == null)
                return new NewFieldInfos(-1, null);

            TextEditor e = editor.assumeText();
            ParsedCUNode pcuNode = e.getParsedNode();

            if (pcuNode == null)
                return new NewFieldInfos(-1, null);

            ParsedNode positionNode = pcuNode.getContainingMethodOrClassNode(startPos);
            if (positionNode == null)
                return new NewFieldInfos(-1, null);

            // Find the containing class of the node
            ParsedNode classNode = (positionNode instanceof MethodNode) ? positionNode.getParentNode() : positionNode;
            // Find the last field in that class
            Iterator<NodeAndPosition<ParsedNode>> classNodeChildren = classNode.getChildren(0);
            int posOfNextField = -1;
            int posOfFirstField = 0;
            int posOfSeqField = 0;
            String indentationFromLastField = null;
            while (classNodeChildren.hasNext())
            {
                NodeAndPosition<ParsedNode> classNodeChild = classNodeChildren.next();
                if (classNodeChild.getNode() instanceof FieldNode)
                {
                    boolean isFirstField = ((FieldNode) classNodeChild.getNode()).isFirstFieldNode();
                    if (isFirstField)
                        posOfFirstField = classNodeChild.getPosition();
                    posOfSeqField = classNodeChild.getPosition();

                    int offset = classNodeChild.getNode().getAbsoluteEditorPosition();
                    // The position for a new field declaration needs to be found by getting parents offset, diff with if required the last field position,
                    // the field line (from position) size, and a new line.
                    int lastFieldLine = editor.getLineColumnFromOffset(offset).getLine();
                    String lastFieldLineStr = editor.getText(new SourceLocation(lastFieldLine, 1), new SourceLocation(lastFieldLine, editor.getLineLength(lastFieldLine - 1)));
                    int lastFieldNoIndentLength = lastFieldLineStr.replaceAll("^\\s+", "").length();
                    posOfNextField = offset + lastFieldNoIndentLength + 1;
                    if (!isFirstField)
                        posOfNextField -= (posOfSeqField - posOfFirstField);
                    indentationFromLastField = lastFieldLineStr.substring(0, lastFieldLineStr.length() - lastFieldNoIndentLength);
                }
            }
            if (posOfNextField > -1)
            {
                // We found the last field in the class
                return new NewFieldInfos(posOfNextField, indentationFromLastField);

            } else
            {
                // We found no field in the class, so we return the position of the class,
                // plus a new line, and the default BlueJ indentation
                int classOffset = classNode.getAbsoluteEditorPosition();
                int classLine = editor.getLineColumnFromOffset(classOffset).getLine();
                String classLineStr = editor.getText(new SourceLocation(classLine, 1), new SourceLocation(classLine, editor.getLineLength(classLine - 1)));
                int classLineStrNoTailLength = classLineStr.replaceAll("\\s+$", "").length();
                String tailSpaces = classLineStr.substring(classLineStrNoTailLength);
                int classLineStrNoIdentLength = classLineStr.replaceAll("^\\s+", "").length();

                indentationFromLastField = classLineStr.substring(0, classLineStr.length() - classLineStrNoIdentLength) + "    ";
                return new NewFieldInfos(classOffset + tailSpaces.length() + 1, indentationFromLastField);
            }
        }

        private Stream<CorrectionInfo> getPossibleCorrectionsStream(FlowEditor editor, CompletionKind kind)
        {
            return getPossibleCorrectionsStream(editor, kind, null, null, startPos);
        }

        private Stream<CorrectionInfo> getPossibleCorrectionsStream(FlowEditor editor, CompletionKind kind, List<AssistContentThreadSafe> possibleCorrectionAlImports, String errorStr, int forceLookUpFromPos)
        {
            if (editor == null)
                return null;

            TextEditor e = editor.assumeText();
            ParsedCUNode pcuNode = e.getParsedNode();

            if (pcuNode == null)
                return null;

            ExpressionTypeInfo suggests = pcuNode.getExpressionType(forceLookUpFromPos, e.getSourceDocument());
            if (suggests == null)
                return null;

            ParsedNode positionNode = pcuNode.getContainingMethodOrClassNode(startPos);
            if (positionNode == null)
                return null;

            // Completions are used to get methods and fields/variables;
            // for types we use another method
            if (kind.equals(CompletionKind.TYPE))
            {
                List<AssistContentThreadSafe> types = new ArrayList<>();
                int errorLine = editor.getLineColumnFromOffset(startPos).getLine();
                int errorLineLength = editor.getLineLength(errorLine - 1);
                String errorLineStr = editor.getText(new SourceLocation(errorLine, 1), new SourceLocation(errorLine, errorLineLength));
                Matcher matcher = Pattern.compile("([^ ]*)(" + errorStr + ")([^ ]*)").matcher(errorLineStr);
                matcher.find();
                String errorFullTypeStrPrefix = matcher.group(1);
                String errorFullTypeStrSuffix = matcher.group(3);

                // First get project's (package) classes
                if (editor.getWatcher().getPackage() != null)
                {
                    List<AssistContentThreadSafe> projPackClasses = ParseUtils.getLocalTypes(editor.getWatcher().getPackage(), null, Kind.all());
                    //We need to check that the class can actually be added, for example, if the error is on "java.io.tes" (at "tes") and that we have Test class somewhere in the project,
                    //it should not be proposed as a correction because the java.io.Test does not exist...
                    removeCorrectionsTriggeringError(projPackClasses, errorFullTypeStrPrefix);
                    projPackClasses.sort(Comparator.comparing(AssistContentThreadSafe::getName));
                    types.addAll(projPackClasses);
                }
                // Get primitives and "common" classes
                List<AssistContentThreadSafe> commmonTypes = new ArrayList<>();
                // if the type has a prefix (like "java.io") or a suffix, we do not add primitives since they cannot be used
                if (errorFullTypeStrPrefix.length() == 0 && errorFullTypeStrSuffix.length() == 0)
                    commmonTypes.addAll(editor.getEditorFixesManager().getPrimitiveTypes());
                if (possibleCorrectionAlImports != null)
                {
                    // We manually add java.lang classes as they are not included in the imports
                    commmonTypes.addAll(editor.getEditorFixesManager().getJavaLangImports().stream().filter(ac -> ac.getPackage() != null).collect(Collectors.toList()));
                    
                    // We filter the imports to : commonly used classes and classes imported in the class explicitly
                    commmonTypes.addAll(possibleCorrectionAlImports.stream()
                            .filter(ac -> Correction.isClassInUsualPackagesForCorrections(ac) || editor.getText(new SourceLocation(1, 1), editor.getLineColumnFromOffset(startPos)).contains("import " + ac.getPackage() + "." + ac.getName() + ";")
                                    || editor.getText(new SourceLocation(1, 1), editor.getLineColumnFromOffset(startPos)).contains("import " + ac.getPackage() + ".*;"))
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
            // we distinguish between class fields and method local var: completion as implemented in BlueJ doesn't look up for local fields
            else if (kind.equals(CompletionKind.FIELD) || kind.equals(CompletionKind.METHOD))
            {
                if (editor.getProject() == null)
                    return null;

                AssistContent[] values = ParseUtils.getPossibleCompletions(suggests, editor.getProject().getJavadocResolver(), null, positionNode);
                if (values == null)
                    return null;

                // We only propose the possible completion of a same kind request,
                // and distinct values: meaning for variables, the correction is done for a local variable when there is an ambiguity.
                return Arrays.stream(values)
                        .filter(ac -> ac.getKind().equals(kind))
                        .flatMap(ac -> Stream.of(ac.getName()))
                        .distinct()
                        .map(SimpleCorrectionInfo::new);
            }
            else
            {
                // for local variables, we only look up directly into the method node
                Iterator<NodeTree.NodeAndPosition<ParsedNode>> methodContentIterator = positionNode.getChildren(0);
                if (methodContentIterator.hasNext())
                {
                    NodeTree.NodeAndPosition methodChild = methodContentIterator.next();
                    if (methodChild.getNode() instanceof MethodBodyNode)
                    {
                        // the method should have a body: we look for the variables
                        Iterator<NodeTree.NodeAndPosition<ParsedNode>> methodBodyIterator = ((MethodBodyNode) methodChild.getNode()).getChildren(0);
                        List<String> varNameList = new ArrayList<>();
                        while (methodBodyIterator.hasNext())
                        {
                            NodeTree.NodeAndPosition methodBodyChild = methodBodyIterator.next();
                            // (Note: the FieldNode class actually covers both fields and variables objects)
                            if (methodBodyChild.getNode() instanceof FieldNode)
                            {
                                varNameList.add(((FieldNode) methodBodyChild.getNode()).getName());
                            }
                        }
                        //return the variables we found
                        return varNameList.stream()
                            .distinct()
                            .map(SimpleCorrectionInfo::new);
                    }
                }
                 // if we didn't find anything, then we return an empty stream.
                 return Stream.empty();
            }
        }
    }

    /**
     * Removes the types from the list for which a correction in the editor would end up with an error
     * because the correction is a part of type that may mismatch (ex. "java.io.Strin" would be corrected
     * as "java.io.String" and triggers an error). It only checks the prefix.
     *
     * @param possibleCorrectionsList the list of types to work with
     * @param errorFullTypeStrPrefix  the prefix of the error type
     */
    private static void removeCorrectionsTriggeringError(List<AssistContentThreadSafe> possibleCorrectionsList, String errorFullTypeStrPrefix)
    {
        if (errorFullTypeStrPrefix.length() > 0)
            possibleCorrectionsList.removeIf(ac -> !(ac.getPackage() + ".").equals(errorFullTypeStrPrefix));
    }


}
