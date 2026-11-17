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

import bluej.editor.fixes.SuggestionList.SuggestionDetails;
import bluej.parser.AssistContent;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Language-specific code completion for {@link FlowEditor}. The editor owns the popup and
 * the text insertion; a provider decides what to suggest, how each suggestion is shown,
 * and what choosing it inserts.
 */
@OnThread(Tag.FXPlatform)
public interface CompletionProvider
{
    /**
     * Compute completions at the request's caret position.
     *
     * <p>The provider shows its result through the request, on the FX thread, at most once. A
     * synchronous provider may do so before returning. If there is nothing to complete, the provider
     * may show nothing at all (as Java completion does) or show a message.</p>
     *
     * @param editor  the editor requesting completion
     * @param request where to deliver the result
     */
    void requestCompletions(FlowEditor editor, CompletionRequest request);

    /**
     * Describe how a candidate from this provider appears in the completion popup.
     *
     * @param candidate a candidate from a {@link CompletionResult} produced by this provider
     * @return the popup row for the candidate
     */
    SuggestionDetails getSuggestionDetails(AssistContent candidate);

    /**
     * Describe the text inserted when a candidate from this provider is chosen.
     *
     * @param candidate a candidate from a {@link CompletionResult} produced by this provider
     * @return the text to insert in place of the typed prefix
     */
    CompletionInsertion getInsertion(AssistContent candidate);
}
