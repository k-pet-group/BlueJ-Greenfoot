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
import java.util.Optional;

/**
 * Metadata for a method declaration.
 * 
 * <p>Captures comprehensive method information including signature, parameters,
 * return type, and optional documentation. All collections are immutable.
 * 
 * @param name the method name
 * @param signature the complete method signature
 * @param returnType the return type of the method
 * @param parameters the list of parameter type names
 * @param documentation optional documentation for the method
 */
public record MethodMetadata(
    String name,
    String signature,
    String returnType,
    List<String> parameters,
    Optional<String> documentation
) implements MemberMetadata {
    
    /**
     * Compact constructor with defensive copy of parameters list.
     */
    public MethodMetadata {
        parameters = List.copyOf(parameters);
    }
}