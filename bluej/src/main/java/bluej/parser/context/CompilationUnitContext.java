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
 * Sealed interface for compilation unit contexts supporting both Java and Kotlin classes.
 * 
 * <p>This interface provides language-agnostic access to class metadata. Language-specific
 * features (e.g., Kotlin properties) are only accessible after type checking via pattern matching.
 * 
 * <p>Design rationale: Properties are intentionally excluded from this interface to avoid
 * exposing Kotlin-specific features to Java contexts. Consumers must use pattern matching
 * to access properties on {@link KotlinContext} instances.
 * 
 * @see JavaContext
 * @see KotlinContext
 */
public sealed interface CompilationUnitContext 
    permits JavaContext, KotlinContext {
    
    /**
     * Returns the fully qualified class name.
     * 
     * @return the fully qualified class name
     */
    String className();
    
    /**
     * Returns the list of methods defined in this class.
     * 
     * @return an immutable list of method metadata
     */
    List<MethodMetadata> methods();
    
    /**
     * Returns the list of fields defined in this class.
     * 
     * @return an immutable list of field metadata
     */
    List<FieldMetadata> fields();
}