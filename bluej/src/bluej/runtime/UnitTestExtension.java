/*
 This file is part of the BlueJ program.
 Copyright (C) 2019  Michael Kolling and John Rosenberg

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
package bluej.runtime;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class implements InvocationInterceptor (exposed by JUnit 5),
 * so we can get access to the parameters passed with dynamic tests
 * invocation.
 */
public class UnitTestExtension implements InvocationInterceptor
{
    // The method parameters are listed in this list to be read by other classes
    private static ArrayList<String> argsAsStrList = new ArrayList<String>();

    private void doIntereceptionMethod(InvocationInterceptor.Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable
    {
        // First we clear the list from potential previous calls
        argsAsStrList.clear();

        // Retrieve the method's parameter so we can show something meaningful in BlueJ's tests list.
        List<Object> args = invocationContext.getArguments();
        for (Object arg : args)
        {
            // We evaluate the types of the parameters and display the paramters
            argsAsStrList.add(getArgStrRepresentation(arg));
        }
        invocation.proceed();
    }

    @Override
    public void interceptTestMethod(InvocationInterceptor.Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable
    {
        doIntereceptionMethod(invocation, invocationContext, extensionContext);
    }

    @Override
    public void interceptTestTemplateMethod(InvocationInterceptor.Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable
    {
        doIntereceptionMethod(invocation, invocationContext, extensionContext);
    }

    /**
     * Evaluates the types of the parameter and returns a representation value as such:
     * null value -> "null"
     * a numeric object --> a string value obtained with toString()
     * a string object --> a string wrapping the string value between double quotes
     * a character object --> a string wrapping the string value between double quotes
     * a boolean object --> a string value obtained with toString()
     * any other object (that is not an array) --> a string value in the format "type name" (name obtained with toString())
     * an array object--> a string value in the format "[val1, val2, ..., valN]"
     *
     * @param arg the Object encapsulating a parameter
     * @return a String objet containing the representation of arg.
     */
    private String getArgStrRepresentation(Object arg)
    {
        if (arg == null)
        {
            return "null";
        }
        if (arg instanceof Number || arg instanceof Boolean)
        {
            return arg.toString();
        }
        if (arg instanceof CharSequence)
        {
            return ("\"" + arg.toString() + "\"");
        }
        if (arg instanceof Character)
        {
            return ("'" + arg.toString() + "'");
        }
        if (arg instanceof Iterable || arg.getClass().isArray())
        {
            ArrayList<String> arrayElementsStrList = new ArrayList<String>();
            if (arg instanceof Iterable)
            {
                ((Iterable) arg).forEach(o -> arrayElementsStrList.add(getArgStrRepresentation(o)));
            }
            else
            {
                int arraySize = Array.getLength(arg);
                for (int arrayIndex = 0; arrayIndex < arraySize; arrayIndex++)
                {
                    arrayElementsStrList.add(getArgStrRepresentation(Array.get(arg, arrayIndex)));
                }
            }
            return ("[" + String.join(", ", arrayElementsStrList) + "]");
        }
        else
        {
            return (arg.getClass().getCanonicalName() + " " + arg.toString());
        }
    }

    public static ArrayList<String> getArgsAsStrList()
    {
        return argsAsStrList;
    }
}
