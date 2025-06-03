/*
 This file is part of the BlueJ program.
 Copyright (C) 2025  Michael Kolling and John Rosenberg

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
package bluej.compiler;

/**
 * The englishMessage always English, the localisedMessage is localised into the BlueJ-set language if possible.
 * If the BlueJ-set language is English, or there's no localisation for that language, the second one might be English too.
 *
 * Note that it's assumed throughout the code that neither is null;
 * if you don't have a localised one, just put the English one in there.
 * If you don't have an English message, put the localised one in there.
 * (You should never have neither!)
 */
public record DiagnosticMessage(String englishMessage, String localisedMessage)
{
    // Although these two methods do the same thing, it's much clearer in the source
    // code to give them different names.  Ideally we wouldn't have the fromEnglish
    // method, only fromLocalised.
    public static DiagnosticMessage fromEnglish(String msg)
    {
        return new DiagnosticMessage(msg, msg);
    }

    public static DiagnosticMessage fromLocalised(String msg)
    {
        return new DiagnosticMessage(msg, msg);
    }
}
