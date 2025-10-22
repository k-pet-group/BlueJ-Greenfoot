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
package bluej.parser.context;

import java.util.List;

/**
 * Compilation context for Java classes, backed by .ctxt files.
 * 
 * <p>Java classes do not have properties - only methods and fields. This implementation
 * provides access to class metadata extracted from .ctxt files.
 * 
 * @param className the fully qualified class name
 * @param methods the list of methods defined in this class
 * @param fields the list of fields defined in this class
 */
public record JavaContext(
    String className,
    List<MethodMetadata> methods,
    List<FieldMetadata> fields
) implements CompilationUnitContext {
    
    /**
     * Compact constructor with defensive copies of collections.
     */
    public JavaContext {
        methods = List.copyOf(methods);
        fields = List.copyOf(fields);
    }
}