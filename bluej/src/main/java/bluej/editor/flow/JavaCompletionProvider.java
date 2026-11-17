/*
 This file is part of the BlueJ program.
 Copyright (C) 2026  Michael Kolling and John Rosenberg

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import bluej.editor.fixes.SuggestionList;
import bluej.editor.fixes.SuggestionList.SuggestionDetails;
import bluej.editor.fixes.SuggestionList.SuggestionDetailsWithHTMLDoc;
import bluej.editor.fixes.SuggestionList.SuggestionShown;
import bluej.parser.AssistContent;
import bluej.parser.AssistContent.ParamInfo;
import bluej.parser.AssistContentThreadSafe;
import bluej.parser.ExpressionTypeInfo;
import bluej.parser.ParseUtils;
import bluej.parser.SourceLocation;
import bluej.parser.lexer.LocatableToken;
import bluej.parser.nodes.MethodNode;
import bluej.parser.nodes.ParsedCUNode;
import bluej.stride.framedjava.slots.ExpressionCompletionCalculator;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Java code completion, driven by BlueJ's own Java parser and type model. Runs synchronously.
 */
@OnThread(Tag.FXPlatform)
public class JavaCompletionProvider implements CompletionProvider
{
    private static final Pattern GREENFOOT_IMPORT =
        Pattern.compile("(;|^)\\s*import\\s+greenfoot\\s*\\.\\s*(\\*\\s*;|Greenfoot\\s*;)");

    @Override
    public void requestCompletions(FlowEditor editor, CompletionRequest request)
    {
        int caretPosition = request.getCaretPosition();
        ParsedCUNode parser = editor.getParsedNode();
        ExpressionTypeInfo suggests = parser == null ? null : parser.getExpressionType(caretPosition,
                editor.getSourceDocument());
        if (suggests == null) {
            // no completions found. no need to search.
            return;
        }

        List<AssistContent> completionCandidates = new ArrayList<>();

        // Get the static classes for completion suggestions, like System.out.
        // They are only available if nothing (like "x." or "this.") precedes the caret),
        // which is captured in the isPlain variable:
        if (suggests.isPlain()) {
            // To check if Greenfoot.* is available, we check if Greenfoot is imported with a regex before the caret in the file:
            SuggestionList.getStaticClassesCompletion(completionCandidates,
                GREENFOOT_IMPORT.matcher(editor.getText(new SourceLocation(1, 1), editor.getCaretLocation())).find(),
                editor.getProject().getPackage(""),
                editor.getJavadocResolver());
        }

        AssistContent[] possibleCompletions = ParseUtils.getPossibleCompletions(suggests, editor.getJavadocResolver(), null,
            parser.getContainingMethodOrClassNode(caretPosition) instanceof MethodNode m ? m : null, caretPosition);
        if (possibleCompletions != null) {
            Arrays.sort(possibleCompletions, AssistContent.getComparator());
            completionCandidates.addAll(Arrays.asList(possibleCompletions));
        }

        LocatableToken suggestToken = suggests.getSuggestionToken();
        int replaceStart = suggestToken == null ? caretPosition : suggestToken.getPosition();
        request.showResult(new CompletionResult(replaceStart, completionCandidates));
    }

    @Override
    public SuggestionDetails getSuggestionDetails(AssistContent candidate)
    {
        AssistContentThreadSafe ac = new AssistContentThreadSafe(candidate);
        return new SuggestionDetailsWithHTMLDoc(ac.getName(), ExpressionCompletionCalculator.getParamsCompletionDisplay(ac),
            ac.getType(), SuggestionShown.COMMON, ac.getDocHTML());
    }

    @Override
    public CompletionInsertion getInsertion(AssistContent candidate)
    {
        List<ParamInfo> params = candidate.getParams();
        if (params == null) {
            return CompletionInsertion.text(candidate.getName());
        }
        return CompletionInsertion.call(candidate.getName(), params.stream().map(ParamInfo::getDummyName).toList());
    }
}
