/*
 This file is part of the BlueJ program.
 Copyright (C) 1999-2009,2014,2016,2017,2018,2020  Michael Kolling and John Rosenberg

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

import bluej.extensions2.SourceType;

/**
 * The type of source code being edited.
 */
public enum FlowSource {
    Java,       // Java source code
    Kotlin,     // Kotlin source code
    PlainText;  // Plain text (not source code)

    /**
     * Creates a FlowSource from the given SourceType.
     * @param sourceType the source type to convert
     * @return the corresponding FlowSource
     */
    public static FlowSource fromSourceType(SourceType sourceType) {
        return switch (sourceType) {
            case Java -> Java;
            case Kotlin -> Kotlin;
            default -> PlainText;
        };
    }

    public SourceType toSourceType() {
        return switch (this) {
            case Java -> SourceType.Java;
            case Kotlin -> SourceType.Kotlin;
            case PlainText -> SourceType.NONE;
        };
    }
}