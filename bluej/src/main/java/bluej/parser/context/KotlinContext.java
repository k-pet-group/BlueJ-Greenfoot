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
 * Compilation context for Kotlin classes, backed by bytecode metadata.
 * 
 * <p>Kotlin classes have properties in addition to methods and fields.
 * Access properties via pattern matching after type check.
 * 
 * <p>This implementation extracts metadata from Kotlin bytecode, providing
 * access to language-specific features like properties that are not present
 * in pure Java classes.
 * 
 * @param className the fully qualified class name
 * @param methods the list of methods defined in this class
 * @param fields the list of fields defined in this class
 * @param properties the list of Kotlin properties defined in this class
 */
public record KotlinContext(
    String className,
    List<MethodMetadata> methods,
    List<FieldMetadata> fields,
    List<PropertyMetadata> properties
) implements CompilationUnitContext {
    
    /**
     * Compact constructor with defensive copies of collections.
     */
    public KotlinContext {
        methods = List.copyOf(methods);
        fields = List.copyOf(fields);
        properties = List.copyOf(properties);
    }
}