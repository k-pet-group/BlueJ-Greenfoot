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

import bluej.parser.AssistContent.Access;
import bluej.parser.AssistContent.CompletionKind;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * One Kotlin completion suggestion, as plain data that outlives the analysis it came from.
 *
 * @param prefix         text shown and inserted before the name, such as {@code "this."} or
 *                       {@code "Greenfoot."}; empty for none
 * @param name           the declaration's name
 * @param kind           {@link CompletionKind#METHOD} for functions, {@link CompletionKind#FIELD} for
 *                       properties (including Java getters shown as properties, Java fields and enum
 *                       entries), {@link CompletionKind#LOCAL_VAR} or {@link CompletionKind#FORMAL_PARAM}
 * @param type           the function's return type or the variable's type, for display
 * @param parameters     the function's parameters, or null if not a function
 * @param declaringClass fully qualified name of the declaring class, or null if not a class member
 * @param access         the declaration's visibility
 * @param kdoc           the declaration's KDoc comment text, or null (only Kotlin source has KDoc)
 * @param javaMember     for Java-declared methods (and getters shown as properties), what is needed to
 *                       look up their Javadoc; otherwise null
 */
@OnThread(Tag.Any)
public record KotlinCompletionCandidate(String prefix, String name, CompletionKind kind, String type,
                                        List<Parameter> parameters, String declaringClass, Access access,
                                        String kdoc, JavaMember javaMember)
{
    /**
     * A function parameter.
     *
     * @param name         the parameter name (Java library methods may only have {@code p0}, {@code p1}, ...)
     * @param type         the parameter type, for display
     * @param optional     true if a call may omit it (it has a default value, or is a vararg)
     * @param vararg       true for a vararg parameter ({@code type} is then the element type)
     * @param functionType true if its type is a Kotlin function type, so a lambda can be passed
     */
    @OnThread(Tag.Any)
    public record Parameter(String name, String type, boolean optional, boolean vararg, boolean functionType)
    {
    }

    /**
     * Identifies a Java method, for Javadoc lookup.
     *
     * @param className      fully qualified name of the declaring class
     * @param methodName     the Java method name (e.g. {@code getWorld} for the property {@code world})
     * @param parameterTypes erased JVM parameter types, e.g. {@code int} or {@code java.lang.Class}
     */
    @OnThread(Tag.Any)
    public record JavaMember(String className, String methodName, List<String> parameterTypes)
    {
        /**
         * Create a Java member reference; the parameter types are copied.
         */
        public JavaMember
        {
            parameterTypes = List.copyOf(parameterTypes);
        }
    }

    /**
     * Create a candidate; the parameter list (if any) is copied.
     */
    public KotlinCompletionCandidate
    {
        parameters = parameters == null ? null : List.copyOf(parameters);
    }
}
