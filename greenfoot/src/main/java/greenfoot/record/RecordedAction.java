/*
 This file is part of the Greenfoot program.
 Copyright (C) 2026  Poul Henriksen and Michael Kolling

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
package greenfoot.record;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import bluej.debugger.gentype.JavaType;
import bluej.parser.kotlin.KotlinParserUtils;
import bluej.stride.framedjava.elements.CallElement;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.framedjava.elements.VarElement;
import bluej.views.KotlinFileFacadeDetector;
import bluej.views.KotlinPropertyAccessorDetector;

import threadchecker.OnThread;
import threadchecker.Tag;

/** A recorded operation, independent of its Java/Stride or Kotlin outer-statement syntax. */
@OnThread(Tag.Any)
interface RecordedAction
{
    /** @return the legacy Java/Stride representation of this operation */
    CodeElement toJava();

    /** @return a native Kotlin statement, with argument expression strings unchanged */
    @OnThread(Tag.FXPlatform)
    String toKotlin();

    /** Argument text and the parameter metadata needed by legacy Java array rendering. */
    record Arguments(List<String> values, List<JavaType> parameterTypes, boolean varArgs)
    {
        /**
         * Takes independent, unmodifiable copies of both lists.
         * @param values argument expressions, including their original whitespace
         * @param parameterTypes the declared parameter types
         * @param varArgs whether the call uses a variable-arity method
         */
        public Arguments
        {
            values = List.copyOf(values);
            parameterTypes = List.copyOf(parameterTypes);
        }

        /**
         * Snapshots the invocation's arrays without changing any expression text.
         * @param args argument expressions, or null for no arguments
         * @param types parameter types, or null when no metadata was supplied
         * @param isVarArgs whether the call uses a variable-arity method
         * @return an independent argument snapshot
         */
        public static Arguments capture(String[] args, JavaType[] types, boolean isVarArgs)
        {
            return new Arguments(args == null ? List.of() : Arrays.asList(args),
                types == null ? List.of() : Arrays.asList(types), isVarArgs);
        }

        private String toJava()
        {
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < values.size(); i++) {
                String arg = values.get(i).trim();
                if (arg.startsWith("{") && arg.endsWith("}")) {
                    String paramTypeName;
                    if (varArgs && i >= parameterTypes.size() - 1) {
                        paramTypeName = parameterTypes.get(parameterTypes.size() - 1).toString();
                        paramTypeName = paramTypeName.substring(0, paramTypeName.length() - 2);
                    }
                    else {
                        paramTypeName = parameterTypes.get(i).toString();
                    }
                    arg = "new " + paramTypeName + " " + arg;
                }
                if (i != 0) {
                    result.append(", ");
                }
                result.append(arg);
            }
            return result.toString();
        }

        private String toKotlin()
        {
            return String.join(", ", values);
        }
    }

    /** Construction of an actor with its already assigned recorder name. */
    record Create(String className, String name, Arguments arguments) implements RecordedAction
    {
        @Override
        public CodeElement toJava()
        {
            return new VarElement(null, className, name, "new " + className + "(" + arguments.toJava() + ")");
        }

        @Override
        public String toKotlin()
        {
            return "val " + kotlinIdentifier(name) + " = " + kotlinType(className) + "(" + arguments.toKotlin() + ")";
        }
    }

    /** A null receiver means the current World; a static receiver is a qualified class name, not a local. */
    record MethodCall(String receiver, boolean staticReceiver, Method method, Arguments arguments)
        implements RecordedAction
    {
        @Override
        public CodeElement toJava()
        {
            String prefix = receiver == null ? "" : receiver + ".";
            return callElement(prefix + method.getName() + "(" + arguments.toJava() + ")");
        }

        @Override
        @OnThread(Tag.FXPlatform)
        public String toKotlin()
        {
            String sourceReceiver = receiver;
            if (staticReceiver && KotlinFileFacadeDetector.isFileFacade(method.getDeclaringClass())) {
                sourceReceiver = method.getDeclaringClass().getPackageName();
            }
            String prefix = sourceReceiver == null || sourceReceiver.isEmpty() ? ""
                : (staticReceiver ? kotlinType(sourceReceiver) : kotlinIdentifier(sourceReceiver)) + ".";
            String property = KotlinPropertyAccessorDetector.getPropertyName(method);
            if (property != null) {
                if (method.getParameterCount() == 0 && arguments.values().isEmpty()) {
                    return prefix + kotlinIdentifier(property);
                }
                if (method.getParameterCount() == 1 && arguments.values().size() == 1) {
                    return prefix + kotlinIdentifier(property) + " = " + arguments.values().get(0);
                }
            }
            return prefix + kotlinIdentifier(method.getName()) + "(" + arguments.toKotlin() + ")";
        }
    }

    /** Placement of a named actor in the current World. */
    record Add(String actor, int x, int y) implements RecordedAction
    {
        @Override
        public CodeElement toJava()
        {
            return callElement("addObject(" + actor + "," + x + "," + y + ")");
        }

        @Override
        public String toKotlin()
        {
            return "addObject(" + kotlinIdentifier(actor) + ", " + x + ", " + y + ")";
        }
    }

    /** Movement of a named actor. */
    record Move(String actor, int x, int y) implements RecordedAction
    {
        @Override
        public CodeElement toJava()
        {
            return callElement(actor + ".setLocation(" + x + "," + y + ")");
        }

        @Override
        public String toKotlin()
        {
            return kotlinIdentifier(actor) + ".setLocation(" + x + ", " + y + ")";
        }
    }

    /** Removal of a named actor from the current World. */
    record Remove(String actor) implements RecordedAction
    {
        @Override
        public CodeElement toJava()
        {
            return callElement("removeObject(" + actor + ")");
        }

        @Override
        public String toKotlin()
        {
            return "removeObject(" + kotlinIdentifier(actor) + ")";
        }
    }

    private static CallElement callElement(String content)
    {
        return new CallElement(content, content);
    }

    private static String kotlinIdentifier(String name)
    {
        return KotlinParserUtils.isCreationIdentifier(name) ? name : "`" + name + "`";
    }

    private static String kotlinType(String type)
    {
        // Escape identifier segments only; do not translate Java types or touch argument expressions.
        return Pattern.compile("\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*").matcher(type)
            .replaceAll(match -> Matcher.quoteReplacement(kotlinIdentifier(match.group())));
    }
}
