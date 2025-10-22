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

import java.util.Optional;

/**
 * Metadata for a Kotlin property.
 * 
 * <p>Properties expose getterName/setterName display names for UI:
 * <ul>
 * <li>Getter: property name (e.g., "age")</li>
 * <li>Setter: property name + " =" (e.g., "age =")</li>
 * </ul>
 * 
 * <p>This representation allows the UI to distinguish properties from methods
 * while providing intuitive display names that match Kotlin syntax.
 * 
 * @param name the property name
 * @param hasGetter whether the property has a getterName
 * @param hasSetter whether the property has a setterName
 * @param getterSignature optional getterName method signature
 * @param setterSignature optional setterName method signature
 * @param documentation optional documentation for the property
 */
public record PropertyMetadata(
    String name,
    String type,
    Optional<String> getterName,
    Optional<String> setterName,
    Optional<String> documentation
) implements MemberMetadata {
    
    /**
     * Returns display name for getterName in UI (just the property name).
     * 
     * @return the property name for getterName display
     */
    public String getterDisplay() {
        return name;
    }
    
    /**
     * Returns display name for setterName in UI (property name + " =").
     * 
     * @return the property name with " =" suffix for setterName display
     */
    public String setterDisplay() {
        return name + " =";
    }

    public Optional<String> getterSignature() {
        return this.getterName.map(getterName -> type + " " + getterName + "()");
    }
    public Optional<String> setterSignature() {
        return this.setterName.map(setterName -> "void " + setterName + "(" + type + ")");
    }
}
