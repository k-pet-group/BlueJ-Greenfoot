/*
 This file is part of the BlueJ program. 
 Copyright (C) 2019,2020,2021,2022  Michael Kolling and John Rosenberg

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
import bluej.parser.lexer.JavaLexer;
import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.lexer.LocatableToken;
import bluej.parser.nodes.*;
import bluej.parser.nodes.NodeTree.NodeAndPosition;
import bluej.pkgmgr.target.role.Kind;
import bluej.utility.Debug;
import bluej.utility.Utility;
import bluej.utility.javafx.FXPlatformConsumer;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.IndexRange;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.io.StringReader;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static bluej.utility.JavaUtils.blankCodeCommentsAndStringLiterals;

public class FlowErrorManager implements ErrorQuery
{
    private final ObservableList<ErrorDetails> errorInfos = FXCollections.observableArrayList();
    private final FlowEditor editor;

    /**
     * Construct a new FlowErrorManager to manage error display for the specified editor instance.
     * The new manager should be set as the document listener so that it receives notification
     * of parser errors as they occur.
     */
    public FlowErrorManager(FlowEditor editor)
    {
        this.editor = editor;
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

    public ObservableList<ErrorDetails> getObservableErrorList()
    {
        return errorInfos;
    }

    public static class ErrorDetails
    {
        public final int startPos;
        public final int endPos;
        public final String message;
        private int italicMessageStartIndex;
        private int italicMessageEndIndex;
        public final int identifier;
        public final List<FixSuggestion> corrections = new ArrayList<>();

        private ErrorDetails(FlowEditor editor, int startPos, int endPos, String message, int identifier, Stream<AssistContentThreadSafe> possibleImports)
        {
            this.startPos = startPos;
            this.endPos = endPos;
            this.identifier = identifier;
            int italicMessageStartIndex = -1, italicMessageEndIndex = -1;

            try
            {
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
                    message = Config.getString("editor.quickfix.unknownType.errorMsg") + typeName;
                    if (possibleImports != null)
                    {
                        List<AssistContentThreadSafe> possibleCorrectionsList = possibleImports
                            .filter(ac -> ac.getPackage() != null).collect(Collectors.toList());
    
                        // Add the fixes: import single class then import package
                        // We don't show package import when the typeName suggest an inner class
                        corrections.addAll(possibleCorrectionsList.stream()
                            .filter(ac -> ac.getName().equals(typeName) && ac.getDeclaringClass() == null)
                            .flatMap(ac -> Stream.of(new FixSuggestionBase((Config.getString("editor.quickfix.unknownType.fixMsg.class")
                                    + ac.getPackage() + "." + ac.getName()),
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
                                correctionElements -> doTypeQuickFix(editor, startErrorPosSourceLocation, correctionElements, typeName),
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
                    message = Config.getString("editor.quickfix.wrongComparisonOperator.errorMsg");
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
                    message = Config.getString("editor.quickfix.undeclaredVar.errorMsg") + varName;
    
                    // Add a quick fix for correcting to an existing closely spelt variable
                    Stream<CorrectionInfo> possibleCorrectionsStream = getPossibleCorrectionsStream(editor, CompletionKind.FIELD);
                    if (possibleCorrectionsStream != null)
                    {
                        corrections.addAll(bluej.editor.fixes.Correction.winnowAndCreateCorrections(varName,
                            possibleCorrectionsStream,
                            correctionElements ->
                            {
                                editor.setSelection(startErrorPosSourceLocation, startErrorPosSourceLocation);
                                editor.setText(editor.getLineColumnFromOffset(startPos), editor.getLineColumnFromOffset(endPos), correctionElements.getPrimaryElement());
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
                    String methodName = message.substring("cannot find symbol -   method ".length(), message.lastIndexOf('('));
                    message = Config.getString("editor.quickfix.undeclaredMethod.errorMsg") + methodName + message.substring(message.lastIndexOf('('));
    
                    // Add a quick fix for correcting to an existing closely spelt method
                    Stream<CorrectionInfo> possibleCorrectionsStream = getPossibleCorrectionsStream(editor, CompletionKind.METHOD);
                    if (possibleCorrectionsStream != null)
                    {
                        corrections.addAll(bluej.editor.fixes.Correction.winnowAndCreateCorrections(methodName,
                            possibleCorrectionsStream,
                            correctionElements ->
                            {
                                editor.setSelection(startErrorPosSourceLocation, startErrorPosSourceLocation);
                                editor.setText(editor.getLineColumnFromOffset(startPos), editor.getLineColumnFromOffset(endPos), correctionElements.getPrimaryElement());
                            }));
                        editor.refresh();
                    }
                }
                // set the quick fix to add a try/catch statement or throw the exception:
                // detected the error is "unreported exception [...]"
                else if (message.startsWith("unreported exception "))
                {
                    // Change the error message to a more meaningful message
                    String exceptionQualifiedNameType = message.substring("unreported exception ".length(), message.indexOf(';'));
                    message = Config.getString("editor.quickfix.unreportedException.errorMsg.part1") + exceptionQualifiedNameType + Config.getString("editor.quickfix.unreportedException.errorMsg.part2");
                    String messageFinal = message;
    
                    // If the exception type is already imported, we don't need to use the qualified name in the corrections:
                    // so we check if imports contains that type, and if so, use a simple type name.
                    // We try to retrieve the AssistContent object for the suggested type in order to make the right checkup 
                    // against imports of the current class.
                    Future<List<AssistContentThreadSafe>> futureImports = editor.getEditorFixesManager().scanImports(exceptionQualifiedNameType);
                    Utility.runBackground(() -> {
                            try
                            {
                                List<AssistContentThreadSafe> matchTypeACList = futureImports.get();
                                Platform.runLater(() -> {
                                    // The actual correction type String we will use might not be the qualified name if the import is already there for that type (or type in java.lang)
                                    // initial value is set to qualified name
                                    String exceptionTypeForCorrection = exceptionQualifiedNameType;
    
                                    // There shouldn't be more than one type returned here using the fully qualified name.. 
                                    // so we use the first one if at least one is returned.
                                    if (matchTypeACList.size() > 0)
                                    {
                                        AssistContentThreadSafe matchTypeAC = matchTypeACList.get(0);
                                        //Now we have a way to check if the type is already imported (or part of java.lang)
                                        if (editor.checkTypeIsImported(matchTypeAC, false))
                                            exceptionTypeForCorrection = matchTypeAC.getName();
                                    }
                                    // Corrections need be done once the editor is opened --> if not yet we don't do anything at this stage
                                    if (editor.getProject() != null)
                                    {
                                        // Add a quick fix for surrounding the current line with try/catch
                                        // first we search for a non existing variable name for the exception var in the catch statement
                                        // the initial variable name we choose is a gathering of the caps letters of the type, i.e. FileNotFoundException --> fnfe
                                        // if the variable name is not available, we append a numerical suffix, starting from 1, until we find a name that hasn't been used.
                                        // The search is case insensitive to "force" different variable names and avoid confusion from the user.
                                        // (in case we cannot extract any upper case letter from the type (which could happen with wrongly named user defined types) we 
                                        // return a generic variable name "e"...)
                                        String exceptionVarNameRoot =
                                            exceptionQualifiedNameType.substring(((exceptionQualifiedNameType.contains(".")) ? (exceptionQualifiedNameType.lastIndexOf(".") + 1) : 0)).replaceAll("[^A-Z]", "").toLowerCase();
                                        if (exceptionVarNameRoot.length() == 0)
                                            exceptionVarNameRoot = "e";
    
                                        boolean foundVarName = false;
                                        String fileContent = editor.getText(new SourceLocation(1, 1), editor.getLineColumnFromOffset(editor.getTextLength()));
                                        int posOfClass = getNewClassFieldPos(editor).pos;
                                        int posOfContainingMethod = editor.getParsedNode().getContainingMethodOrClassNode(startPos).getAbsoluteEditorPosition();
    
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
                                        String fileContentBeforeErrorPart = blankCodeCommentsAndStringLiterals(fileContent.substring(0, startPos), '0');
                                        int prevStatementPos = Math.max(Math.max(fileContentBeforeErrorPart.lastIndexOf('{'), fileContentBeforeErrorPart.lastIndexOf('}')), fileContentBeforeErrorPart.lastIndexOf(';'));
                                        if (prevStatementPos == -1)
                                        {
                                            this.italicMessageStartIndex = messageFinal.indexOf(exceptionQualifiedNameType);
                                            this.italicMessageEndIndex = messageFinal.indexOf(exceptionQualifiedNameType) + exceptionQualifiedNameType.length();
                                            return;
                                        }
                                        int statementStartPos = prevStatementPos + 1;
                                        while (statementStartPos < fileContentBeforeErrorPart.length() && Character.isWhitespace(fileContentBeforeErrorPart.charAt(statementStartPos)))
                                            statementStartPos++;
                                        String fileContentAfterErrorPart = blankCodeCommentsAndStringLiterals(fileContent.substring(startPos), '0');
                                        int statementEndPos = 0;
    
                                        boolean needSurroundingBrackets = false;
                                        // First, we check if the error is inside a control statement, for example "if(...)", "for(...)"
                                        // because if that is the case, we do not show the try/catch fix at all
                                        if (!editor.getParsedNode().isCurrentlyInControlStatement(startPos))
                                        {
    
                                            boolean needNewLine = (editor.getLineColumnFromOffset(prevStatementPos).getLine() == errorLine);
                                            boolean foundEnd = false;
                                            int searchIndex = 0;
                                            int openedBracket = 0;
                                            if (fileContentBeforeErrorPart.substring(prevStatementPos, startPos).contains("->"))
                                            {
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
                                                        //we only consider these characters ends the lambda if they're not
                                                        //inside the method that returns an expection (i.e. within its arguments)
                                                        foundEnd = (openedBracket == 0);
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
                                                    } else if (c == ';')
                                                    {
                                                        foundEnd = (openedBracket == 0);
                                                    }
                                                    if (foundEnd)
                                                        statementEndPos = startPos + searchIndex + 1;
                                                    searchIndex++;
                                                }
                                            }
    
                                            // If we couldn't find the end of the erroneous statement, 
                                            // or we are inside a control statement (e.g. if, for ...),
                                            // then we don't propose to add a try/catch statement
                                            if (foundEnd)
                                            {
                                                String initIdent = errorLineText.substring(0, errorLineLength - 1 - (errorLineText.replaceAll("^\\s*", "").length()));
                                                String unchangedInitIdent = initIdent;
                                                String newIndentSpacing = "    ";
                                                if (needSurroundingBrackets)
                                                    initIdent += newIndentSpacing;
                                                StringBuffer tryCatchString = new StringBuffer(
                                                    ((needNewLine) ? "\n" : "")
                                                        + ((needSurroundingBrackets) ? unchangedInitIdent + "{\n" : "")
                                                        + ((needSurroundingBrackets) ? initIdent : "") + "try\n"
                                                        + initIdent + "{\n"
                                                        + initIdent + newIndentSpacing + fileContent.substring(statementStartPos, statementEndPos) + ((needSurroundingBrackets) ? ";" : "") + "\n"
                                                        + initIdent + "}\n"
                                                        + initIdent + "catch (" + exceptionTypeForCorrection + " " + exceptionVarName + ")\n"
                                                        + initIdent + "{\n"
                                                        + initIdent + newIndentSpacing);
                                                int posOfCatchStatement = tryCatchString.length();
                                                String catchStatement = exceptionVarName + ".printStackTrace();";
                                                tryCatchString.append(catchStatement + "\n"
                                                    + initIdent + "}" + ((needSurroundingBrackets) ? "\n" : "")
                                                    + ((needSurroundingBrackets) ? unchangedInitIdent + "}" : ""));
    
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
                                            }
                                        }
    
                                        // Add a second quick fix for adding a throws statement, if not in a lambda
                                        if (!needSurroundingBrackets)
                                        {
                                            // first the throws statement is created all if none exist, otherwise we just append the exception
                                            int posOfClosingMethodParamsBracket = fileContentBeforeErrorPart.indexOf(")", posOfContainingMethod);
                                            int posOfOpeningMethodBodyBracket = fileContentBeforeErrorPart.indexOf('{', posOfClosingMethodParamsBracket);
                                            if (posOfClosingMethodParamsBracket == -1 || posOfOpeningMethodBodyBracket == -1)
                                            {
                                                this.italicMessageStartIndex = messageFinal.indexOf(exceptionQualifiedNameType);
                                                this.italicMessageEndIndex = messageFinal.indexOf(exceptionQualifiedNameType) + exceptionQualifiedNameType.length();
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
                                                    + ((methodHasThrows) ? (exceptionTypeForCorrection + ",") : ("throws " + exceptionTypeForCorrection))
                                                    + ((openingMethodBodyBracketOnSameLine) ? " " : "");
    
                                            // and prepare the quick fix
                                            corrections.add(new FixSuggestionBase(Config.getString("editor.quickfix.unreportedException.fixMsg.throws"), () -> {
                                                int currentLocation = editor.getOffsetFromLineColumn(editor.getCaretLocation());
                                                editor.setSelection(editor.getLineColumnFromOffset(posOfNewThrowsAddition), editor.getLineColumnFromOffset(posOfNewThrowsAddition));
                                                editor.setText(editor.getLineColumnFromOffset(posOfNewThrowsAddition), editor.getLineColumnFromOffset(posOfNewThrowsAddition), throwsStatement);
                                                editor.refresh();
                                                //reset the cursor at where the user was at
                                                editor.setSelection(editor.getLineColumnFromOffset(currentLocation + throwsStatement.length()), editor.getLineColumnFromOffset(currentLocation + throwsStatement.length()));
                                            }));
                                        }
                                    }
                                });
                            } catch (InterruptedException | ExecutionException e)
                            {
                                Debug.reportError(e);
                            }
                        }
                    );
                }
                // In the default case, we keep the original error message.
            }
            catch (Exception ex)
            {
                Debug.reportError(ex);
            }

            this.message = message;
            this.italicMessageStartIndex = italicMessageStartIndex;
            this.italicMessageEndIndex = italicMessageEndIndex;
        }

        private void doTypeQuickFix(FlowEditor editor, SourceLocation startErrorPosSourceLocation, Correction.CorrectionElements correctionElements, String typeName) {

            // We always try to replace only the type (which might be as outerType.innerType)
            // So one of the following cases can happen:
            // - the type is not an inner type --> we replace by the correct type and import it if not already imported
            // - the type is an inner type and not fully qualified) -->
            //       if the inner class is imported as such (either package.Outer.* or package.Outer.Inner), we use the innerClass type
            //       otherwise, we use the Outer.Inner format (and import it if necessary)

            String correctionType = correctionElements.getPrimaryElement();
            String correctionPackage = (correctionElements.getSecondaryElements().length > 0) ? correctionElements.getSecondaryElements()[0] : "";
            boolean isInnerClass = correctionType.contains(".");
            String fullyQualifiedNameCorrectionType = (correctionPackage.length() > 0) ? correctionPackage + "." + correctionType : "";
            String fullyQualifiedNameCorrectionOuterType = (isInnerClass) ? correctionPackage + "." + correctionType.substring(0, correctionType.lastIndexOf(".")) : "";
            AtomicBoolean isCorrectionTypeImportedAtomic = new AtomicBoolean(false); //we will check this later
            AtomicBoolean isCorrectionOuterTypeImportedAtomic = new AtomicBoolean(false); //we will check this later as well if required
            
            Future<List<AssistContentThreadSafe>> futureCorrectionTypeImport = editor.getEditorFixesManager().scanImports(fullyQualifiedNameCorrectionType);
            Future<List<AssistContentThreadSafe>> futureCorrectionOuterTypeImport = (isInnerClass) ? editor.getEditorFixesManager().scanImports(fullyQualifiedNameCorrectionOuterType) : null;
            Utility.runBackground(() -> {
                try
                {
                    //Beforehand, we just check if the correction type and (if required) the correction outer type are imported already.
                    List<AssistContentThreadSafe> matchCorrectionTypeACList = futureCorrectionTypeImport.get();
                    List<AssistContentThreadSafe> matchCorrectionOuterTypeACList = (futureCorrectionOuterTypeImport != null) ? futureCorrectionOuterTypeImport.get() : new ArrayList<>();
                    Platform.runLater(() ->
                    {
                        //There shouldn't be more than one type returned using the fully qualified name.. 
                        // so we use the first one if at least one is returned.
                        if (matchCorrectionTypeACList.size() > 0)
                        {
                            AssistContentThreadSafe matchCorrectionTypeAC = matchCorrectionTypeACList.get(0);
                            //Now we have a way to check if the type is already imported 
                            //(we check that this type is strictly imported to make sure we don't get it via it's outer class if applies)
                            isCorrectionTypeImportedAtomic.set(editor.checkTypeIsImported(matchCorrectionTypeAC, true));
                        }
                        if (matchCorrectionOuterTypeACList.size() > 0)
                        {
                            AssistContentThreadSafe matchCorrectionOuterTypeAC = matchCorrectionOuterTypeACList.get(0);
                            //Now we have a way to check if the type is already imported
                            isCorrectionOuterTypeImportedAtomic.set(editor.checkTypeIsImported(matchCorrectionOuterTypeAC, false));
                        }

                        // Here we find where to substring the whole code up to the error
                        // to get only the full type name part before the error (ex: "java.util.")
                        String codeBeforeError = editor.getText(new SourceLocation(1, 1), startErrorPosSourceLocation);
                        //just for making sure we don't mess up when the listed characters token mentioned later are in a comment or string literal,
                        //we blank the literals and comments. Note that this keeps the comments token, and remove line breaks.
                        codeBeforeError = blankCodeCommentsAndStringLiterals(codeBeforeError, ' ');
                        // find with our best possible where the declaration type starts: it follows either:
                        // - '/' for a comment, '{' for a block, ';' for after another statement, '(' or ',' for an argument,
                        // - operators
                        int startOfFullType = -1;
                        int currentSearchIndex = codeBeforeError.length() - 1;
                        List<Character> searchTokens = Arrays.asList('/', '{', ';', '(', ',', '?', '+', '-', '*', '%', '&', '|', ':', '!', '<', '>', '=', '^');
                        while (startOfFullType == -1 && currentSearchIndex >= 0)
                        {
                            if (searchTokens.contains(codeBeforeError.charAt(currentSearchIndex)))
                            {
                                //if the token if found then we set the start of the full type name right after
                                startOfFullType = currentSearchIndex + 1;
                            }
                            currentSearchIndex--;
                        }
                        //now remove the parts before the declaration
                        if (startOfFullType > -1)
                        {
                            codeBeforeError = codeBeforeError.substring(startOfFullType);
                        }
                        //finally, we remove potential keywords (such as modifiers) by search for 2 consecutive
                        //token (i.e. not separated by a dot) - whenever we find a dot, we keep the token before
                        //and all subsequent tokens: they're part of the full type name.
                        //In the process we keep the full type name parts in a list that we can use later
                        //and we keep the position information of the beginning of the type declaration.
                        //** If the error starts at "." then we need to get information related to the end of the pretoken right
                        String codeAfterError = editor.getText(startErrorPosSourceLocation, editor.getLineColumnFromOffset(editor.getTextLength()));
                        JavaLexer l = new JavaLexer(new StringReader((codeAfterError.startsWith(".")) ? (codeBeforeError + ".") : codeBeforeError));
                        List<String> fullTypePreTokens = new ArrayList<>();
                        boolean feedPreTokens = false;
                        LocatableToken lastToken = null;
                        int fullTypePrePos = -1;
                        LocatableToken t = null;
                        
                        for (t = l.nextToken(); t.getType() != JavaTokenTypes.EOF; t = l.nextToken())
                        {
                            if (feedPreTokens && t.getType() != JavaTokenTypes.DOT)
                            {
                                fullTypePreTokens.add(t.getText());
                                continue;
                            }

                            if (!feedPreTokens && t.getType() == JavaTokenTypes.DOT)
                            {
                                //found a dot --> we save the previous token
                                fullTypePreTokens.add(lastToken.getText());
                                //most likely the position will need to be offset from where we started looking at the type in the code.
                                fullTypePrePos = (startOfFullType > -1) ? startOfFullType + lastToken.getPosition() : lastToken.getPosition();
                                feedPreTokens = true;
                                continue;
                            }

                            //save that token for next iteration to be able to use it if we start feeding the array
                            lastToken = t;
                        }

                        // now we can look how to replace the erroneous type
                        //Two cases can happen:
                        //- there is nothing about the type before the error --> replace at the error position
                        //- there is something about the type before the error ---> replace where the type actually starts (and if there was a package, we remove it)
                        //BECAUSE the "endPos" (ending position of the error) may be on the next line since we don't show
                        //multiline errors, we don't use the ending position of the error to select the text. Instead, we search
                        //for where the wrong type is after the startPos and use that position (past the wrong type of course)
                        int endOfWrongTypePos = startPos + codeAfterError.indexOf(typeName) + typeName.length();
                        if (fullTypePreTokens.size() > 0)
                        {
                            editor.setSelection(editor.getLineColumnFromOffset(fullTypePrePos), editor.getLineColumnFromOffset(fullTypePrePos));
                            editor.setText(editor.getLineColumnFromOffset(fullTypePrePos), editor.getLineColumnFromOffset(endOfWrongTypePos), (isInnerClass && isCorrectionTypeImportedAtomic.get()) ? correctionType.substring(correctionType.lastIndexOf(".") + 1) : correctionType);
                        } 
                        else
                        {
                            editor.setSelection(startErrorPosSourceLocation, startErrorPosSourceLocation);
                            // if we have an inner type and that the inner type is strictly imported, we use the import type instead of the composed "outer.inner" type
                            editor.setText(editor.getLineColumnFromOffset(startPos), editor.getLineColumnFromOffset(endOfWrongTypePos), 
                                (isInnerClass && isCorrectionTypeImportedAtomic.get()) ? correctionType.substring(correctionType.lastIndexOf(".") + 1) : correctionType);
                        }

                        //and now that we've done the replacement, we check if an import is required
                        if (correctionPackage.length() > 0 && ((!isInnerClass && !isCorrectionTypeImportedAtomic.get())
                            || (isInnerClass && !isCorrectionTypeImportedAtomic.get() && !isCorrectionOuterTypeImportedAtomic.get())))
                        {
                            //for nested class, we only need to import the declaring class as we never 
                            //import the inner class directly (as mentioned before, we replace by the inner only if already imported)
                            String importTypeFullName =
                                correctionPackage
                                    + "."
                                    + ((isInnerClass)
                                    ? correctionType.substring(0, correctionType.lastIndexOf("."))
                                    : correctionType);

                            editor.addImportFromQuickFix(importTypeFullName);
                            editor.refresh();
                        }
                    });            
                } 
                catch (InterruptedException | ExecutionException e)
                {
                    Debug.reportError(e);
                }
            });
        }

        public boolean containsPosition(int pos)
        {
            return startPos <= pos && pos <= endPos;
        }
        
        public int getItalicMessageStartIndex()
        {
            return italicMessageStartIndex;
        }
        
        public int getItalicMessageEndIndex()
        {
            return italicMessageEndIndex;
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
                // First get project's (package) classes
                if (editor.getWatcher().getPackage() != null)
                {
                    List<AssistContentThreadSafe> projPackClasses = ParseUtils.getLocalTypes(editor.getWatcher().getPackage(), null, Kind.all());
                    projPackClasses.sort(Comparator.comparing(AssistContentThreadSafe::getName));
                    types.addAll(projPackClasses);
                }
                // Get primitives and "common" classes
                List<AssistContentThreadSafe> commmonTypes = new ArrayList<>();
                commmonTypes.addAll(editor.getEditorFixesManager().getPrimitiveTypes());
                if (possibleCorrectionAlImports != null)
                {
                    // We manually add java.lang classes as they are not included in the imports
                    commmonTypes.addAll(editor.getEditorFixesManager().getJavaLangImports().stream().filter(ac -> ac.getPackage() != null).collect(Collectors.toList()));
                    
                    // We filter the imports to : commonly used classes and classes imported in the class explicitly
                    commmonTypes.addAll(possibleCorrectionAlImports.stream()
                            .filter(ac -> Correction.isClassInUsualPackagesForCorrections(ac)
                                || editor.checkTypeIsImported(ac, false))
                            .collect(Collectors.toList()));
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
                while (methodContentIterator.hasNext())
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
}
