package bluej.utility;

import bluej.Config;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Some generally useful utility methods to do with dealing with
 * java names.
 *
 * @author  Andrew Patterson
 * @version $Id: JavaNames.java 505 2000-05-24 05:44:24Z ajp $
 */
public class JavaNames
{
    /**
     * Check whether a string is a valid Java identifier
     */
    public static boolean isIdentifier(String str)
    {
        if (str.length() == 0)
            return false;
        if (!Character.isJavaIdentifierStart(str.charAt(0)))
            return false;
        for (int i=1; i < str.length(); i++)
            if (! Character.isJavaIdentifierPart(str.charAt(i)))
                return false;

        return true;
    }

    /**
     * Check whether a string is valid Java qualified identifier
     * ie java.util or util or com.sun.test
     * but not .java or java..util to com.sun.
     */
    public static boolean isQualifiedIdentifier(String str)
    {
        if (str.length() == 0)
            return false;

        StringTokenizer st = new StringTokenizer(str, ".");

        while(st.hasMoreTokens())
        {
            if(!JavaNames.isIdentifier((String)(st.nextToken())))
                return false;
        }

        return true;
    }

    /**
     * Strips package prefix's from full class name.
     *
     * @return the stripped class name.
     */
    public static String stripPrefix(String fullClassName)
    {
        if(fullClassName != null) {
            int index = fullClassName.lastIndexOf(".");
            if(index >= 0)
                return fullClassName.substring(++index);
        }

        return fullClassName;
    }

    /**
     * java.util.ArrayList --> ArrayList
     * java.util           --> util
     * ""                  --> ""
     */
    public static String getBase(String qualifiedName)
    {
        if(qualifiedName == null)
            throw new NullPointerException();

        int index = qualifiedName.lastIndexOf(".");
        if(index >= 0)
            return qualifiedName.substring(++index);

        return qualifiedName;
    }

    /**
     * java.util.ArrayList --> java.util
     * ""                  --> ""
     * ArrayList           --> ""
     */
    public static String getPrefix(String qualifiedName)
    {
        if(qualifiedName == null)
            throw new NullPointerException();

        int index = qualifiedName.lastIndexOf(".");
        if(index > 0)
            return qualifiedName.substring(0, index);

        return "";
    }

    /**
     * typeName - a utility function to fix up Java class names. Class names
     *  as returned by the Class.getName() functions are okay for non-array
     *  classes (we don't need to do anything for them), but are in a funny
     *  format for arrays. "String[]", for example, is shown as
     *  "[Ljava.lang.String;". See the Class.getName() documentation for
     *  details. Here, we transform the array names into standard Java syntax.
     */
    public static String typeName(String className)
    {
        if( !(className.charAt(0) == '['))
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

    public static String stripSuffix(String name, String suffix)
    {
        int s = name.lastIndexOf(suffix);

        if(s > 0)
            return name.substring(0, s);
        else
            return name;
    }
}
