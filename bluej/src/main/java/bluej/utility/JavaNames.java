/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2010,2016  Michael Kolling and John Rosenberg 
 
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
package bluej.utility;

import java.io.*;
import java.util.*;

/**
 * Some generally useful utility methods to do with dealing with
 * java names.
 *
 * @author  Andrew Patterson
 */
public class JavaNames
{
    private static Set<String> javaKeywords;
    
    static {
        javaKeywords = new HashSet<String>();
        String[] keywords = new String[] {"abstract", "assert", "boolean", "break", "byte",
                "case", "catch", "char", "class", "const", "continue", "default", "do",
                "double", "else", "enum", "extends", "final", "finally", "float", "for",
                "goto", "if", "implements", "import", "instanceof", "int", "interface",
                "long", "native", "new", "package", "private", "protected", "public",
                "return", "short", "static", "strictfp", "super", "switch", "synchronized",                 
                "this", "throw", "throws", "transient", "try", "void", "volatile", "while",
                "false", "null", "true"
        };
        
        
        Collections.addAll(javaKeywords, keywords);
    }
    
    /**
     * Check whether a string is a valid Java identifier
     */
    public static boolean isIdentifier(String str)
    {
        if (str.length() == 0) {
            return false;
        }
        
        if (!Character.isJavaIdentifierStart(str.charAt(0))) {
            return false;
        }
        
        for (int i=1; i < str.length(); i++) {
            if (! Character.isJavaIdentifierPart(str.charAt(i))) {
                return false;
            }
        }
        
        if (isJavaKeyword(str)) {
            return false;
        }

        return true;
    }

    /**
     * Check whether a string is valid Java qualified identifier
     * ie "java.util" or "util" or "com.sun.test" or the empty string
     * but not ".java" or "java..util" or "com.sun.".
     */
    public static boolean isQualifiedIdentifier(String str)
    {
        if (str.length() == 0) {
            return true;
        }

        StringTokenizer st = new StringTokenizer(str, ".");

        while(st.hasMoreTokens()) {
            if(!JavaNames.isIdentifier(st.nextToken())) {
                return false;
            }
        }

        return true;
    }

    /**
     * Strips package prefix's from full class name. This works only for
     * class names, not generic types.
     *
     * @return the stripped class name.
     */
    public static String stripPrefix(String fullClassName)
    {
        if(fullClassName != null) {
            int index = fullClassName.lastIndexOf(".");
            if(index >= 0) {
                return fullClassName.substring(++index);
            }
        }

        return fullClassName;
    }

    /**
     * Strip the given suffix (such as ".java" or ".class") from the given name
     */
    public static String stripSuffix(String name, String suffix)
    {
        int s = name.lastIndexOf(suffix);

        if(s > 0 && (s == name.length() - suffix.length())) {
            return name.substring(0, s);
        }
        else {
            return name;
        }
    }

    /**
     * Return the base item from a fully qualified Java name.
     *
     * java.util.ArrayList --> ArrayList
     * java.util           --> util
     * ""                  --> ""
     */
    public static String getBase(String qualifiedName)
    {
        int index = qualifiedName.lastIndexOf(".");
        if(index >= 0)
            return qualifiedName.substring(++index);

        return qualifiedName;
    }

    /**
     * Return the prefix (all but the base name) from a
     * fully qualified Java name. Examples:
     *
     * <pre>
     * java.util.ArrayList   returns   java.util
     * ""                    returns   ""
     * ArrayList             returns   ""
     * </pre>
     */
    public static String getPrefix(String qualifiedName)
    {
        if(qualifiedName == null) {
            throw new NullPointerException();
        }

        int index = qualifiedName.lastIndexOf(".");
        if(index > 0) {
            return qualifiedName.substring(0, index);
        }
        else {
            return "";
        }
    }

    /**
     * Convert a filename into a fully qualified Java name
     * by considering the filename relative to a base directory.
     * Returns null if the file is outside the base
     * directory.
     *
     * <p>For example, if baseDir is /foo/bar and the f parameter
     * is /foo/bar/p1/s1/TestName.java, the result will be
     * "p1.s1.TestName".
     *
     * <p>The behaviour of this function is not guaranteed if
     * you pass in a directory name. It is meant for filenames
     * like /foo/bar/p1/s1/TestName.java
     * 
     * <p>Makes no guarantee that the returned name is a valid
     * Java identifier (ie. some of the directory names used
     * may not be valid java identifiers but no check is made
     * for this).
     */
    public static String convertFileToQualifiedName(File baseDir, File f)
    {
        File pathFile = f;
        if (! pathFile.isAbsolute()) {
            pathFile = new File(baseDir, pathFile.getPath());
        }
        File parent = null;
        String name = "";

        while((parent = pathFile.getParentFile()) != null) {
            if(pathFile.equals(baseDir)) {
                return name;
            }

            if (name == "") {
                name = pathFile.getName();

                int firstDot = name.indexOf('.');
                if (firstDot >= 0) {
                    name = name.substring(0, firstDot);
                }
            }
            else {
                name = pathFile.getName() + "." + name;
            }

            pathFile = parent;
        }

        return null;
    }
    
    /**
     * Convert a qualifed name to a file. This is mostly only useful for
     * packages, as other files have a filename extension with a dot in it.
     */
    public static File convertQualifiedNameToFile(String name, File root)
    {
        int n = 0;
        int i;
        
        File f = root;
        
        i = name.indexOf('.', n);
        while (i != -1) {
            String namePart = name.substring(n, i);
            f = new File(f, namePart);
            n = i + 1;
            i = name.indexOf('.', n);
        }
        
        return new File(f, name.substring(n));
    }

    /**
     * Fix up Java class names as returned by Class.getName()
     *
     * <p>The Class.getName() functions are okay for non-array
     * classes (we don't need to do anything for them), but are in a funny
     * format for arrays. "String[]", for example, is shown as
     * "[Ljava.lang.String;". See the Class.getName() documentation for
     * details. Here, we transform the array names into standard Java syntax.
     */
    public static String typeName(String className)
    {
        if(!(className.charAt(0) == '['))
            return className;

        String name = "";
        while (className.startsWith("[")) {
            className = className.substring(1);
            name = name + "[]";
        }
        switch (className.charAt(0)) {
            case 'L' : name = className.substring(1, className.length()-1)
                       + name;
        break;
            case 'B' : name = "byte" + name;
            break;
            case 'C' : name = "char" + name;
            break;
            case 'D' : name = "double" + name;
            break;
            case 'F' : name = "float" + name;
            break;
            case 'I' : name = "int" + name;
            break;
            case 'J' : name = "long" + name;
            break;
            case 'S' : name = "short" + name;
            break;
            case 'Z' : name = "boolean" + name;
            break;
        }
        return name;
    }
    
    /**
     * Combine two identifiers, such as a package and a class name, to produce a
     * qualified name. This works correctly even if either of the identifiers is
     * empty (or null).
     */
    public static String combineNames(String firstPart, String secondPart)
    {
        if (firstPart == null || firstPart.length() == 0) {
            return secondPart;
        }
        else if (secondPart == null || secondPart.length() == 0) {
            return firstPart;
        }
        else {
            return firstPart + "." + secondPart;
        }
    }

    /**
     * From an array type, figure out the type of the elements in the array. For
     * instance, if you have an array type of "Integer[]" this method will
     * return "Integer".
     * 
     * @param arrayType A string describing the array type. For instance "Integer[]".
     */
    public static String getArrayElementType(String arrayType)
    {
        return JavaNames.stripSuffix(arrayType, "[]");
    }
    
    /**
     * Check whether the given string is a Java keyword / reserved word.
     */
    public static boolean isJavaKeyword(String word)
    {
        return javaKeywords.contains(word);
    }
}
