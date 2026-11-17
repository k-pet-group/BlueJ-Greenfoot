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
package bluej.parser.kotlin.analysis;

import java.util.List;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Completion suggestions for one position in a Kotlin file.
 *
 * @param replaceStart offset in the original text where the identifier being typed starts; choosing
 *                     a candidate replaces the text from here to the caret
 * @param candidates   the suggestions, unsorted (may be empty)
 */
@OnThread(Tag.Any)
public record KotlinCompletionResult(int replaceStart, List<KotlinCompletionCandidate> candidates)
{
    /**
     * Create a result; the candidate list is copied.
     */
    public KotlinCompletionResult
    {
        candidates = List.copyOf(candidates);
    }
}
