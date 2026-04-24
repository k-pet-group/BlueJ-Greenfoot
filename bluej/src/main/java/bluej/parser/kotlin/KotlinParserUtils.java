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
package bluej.parser.kotlin;

import java.io.IOException;
import java.io.Reader;

import bluej.parser.nodes.ReparseableDocument;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Shared utility methods for Kotlin parser nodes.
 */
public final class KotlinParserUtils
{
    private KotlinParserUtils()
    {
    }

    /**
     * Read text from a document between two positions.
     *
     * @return the document text, or empty string on error
     */
    @OnThread(Tag.FXPlatform)
    public static String readDocumentText(ReparseableDocument document,
            int start, int end)
    {
        try (Reader reader = document.makeReader(start, end)) {
            return readFully(reader);
        } catch (IOException e) {
            return "";
        }
    }

    /**
     * Read all content from a Reader into a String.
     *
     * @return the full content, or empty string on error
     */
    public static String readFully(Reader reader)
    {
        try {
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[4096];
            int n;
            while ((n = reader.read(buf)) != -1) {
                sb.append(buf, 0, n);
            }
            return sb.toString();
        } catch (IOException e) {
            return "";
        }
    }
}
