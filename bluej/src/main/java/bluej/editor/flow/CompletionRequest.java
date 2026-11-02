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

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * One code completion request made by {@link FlowEditor} to a {@link CompletionProvider}. A request
 * stops being current when the document changes, the caret moves or completion is requested again;
 * results and messages for a request that is no longer current are ignored.
 */
@OnThread(Tag.FXPlatform)
public interface CompletionRequest
{
    /**
     * Get the caret position at the time of the request.
     *
     * @return the caret offset in the document
     */
    int getCaretPosition();

    /**
     * Check whether the request still applies.
     *
     * @return false once the document has changed, the caret has moved or completion was requested again
     */
    boolean isCurrent();

    /**
     * Show the completion popup for the result, if the request is still current. Call at most once.
     *
     * @param result the completions
     */
    void showResult(CompletionResult result);

    /**
     * Show a short message next to the caret, if the request is still current (for example, that
     * completion is still loading or that there is nothing to complete).
     *
     * @param message the message text
     */
    void showMessage(String message);
}
