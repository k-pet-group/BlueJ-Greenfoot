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
package bluej.editor.flow;

import bluej.parser.entity.EntityResolver;
import bluej.parser.nodes.ParsedCUNode;
import bluej.parser.symtab.ClassInfo;

/**
 * Strategy interface that encapsulates language-specific behavior for
 * {@link FlowEditor}. Each supported language (Java, Kotlin) provides an
 * implementation that handles parser creation and source text manipulation
 * for class declarations.
 *
 * <p>This replaces the {@code boolean isKotlin} flag that previously caused
 * branching throughout FlowEditor with polymorphic dispatch.</p>
 */
public interface FlowLanguageSupport
{
    /**
     * Create the root parse tree node for this language.
     *
     * @param resolver the entity resolver for symbol resolution (may be null
     *                 for languages that don't support entity resolution)
     * @return a root {@link ParsedCUNode} (or subclass) for the language
     */
    ParsedCUNode createRootNode(EntityResolver resolver);

    /**
     * Insert or replace the superclass declaration in a class source file.
     *
     * @param editor    the editor to modify
     * @param className the superclass name to set
     * @param info      parsed class info with selection positions
     */
    void setExtendsClass(FlowEditor editor, String className, ClassInfo info);

    /**
     * Remove the superclass declaration from a class source file.
     *
     * @param editor the editor to modify
     * @param info   parsed class info with selection positions
     */
    void removeExtendsClass(FlowEditor editor, ClassInfo info);

    /**
     * Add an interface to the class's implemented interfaces list.
     *
     * @param editor        the editor to modify
     * @param interfaceName the interface name to add
     * @param info          parsed class info with selection positions
     */
    void addImplements(FlowEditor editor, String interfaceName, ClassInfo info);

    /**
     * For interface types: add to the extends/supertypes list.
     *
     * @param editor        the editor to modify
     * @param interfaceName the interface name to add
     * @param info          parsed class info with selection positions
     */
    void addExtendsInterface(FlowEditor editor, String interfaceName, ClassInfo info);
}
