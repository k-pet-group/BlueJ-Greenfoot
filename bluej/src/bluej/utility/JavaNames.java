package bluej.utility;

import java.io.*;
import java.util.*;

/**
 * Some generally useful utility methods to do with dealing with
 * java names.
 *
 * @author  Andrew Patterson
 * @version $Id: JavaNames.java 1819 2003-04-10 13:47:50Z fisker $
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
     * ie java.util or util or com.sun.test or the empty string
     * but not .java or java..util to com.sun.
     */
    public static boolean isQualifiedIdentifier(String str)
    {
        if (str.length() == 0)
            return true;

        StringTokenizer st = new StringTokenizer(str, ".");

        while(st.hasMoreTokens()) {
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

    public static String stripSuffix(String name, String suffix)
    {
        int s = name.lastIndexOf(suffix);

        if(s > 0)
            return name.substring(0, s);
        else
            return name;
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
        if(qualifiedName == null)
            throw new NullPointerException();

        int index = qualifiedName.lastIndexOf(".");
        if(index >= 0)
            return qualifiedName.substring(++index);

        return qualifiedName;
    }

    /**
     * Return the prefix (all but the base name) from a
     * fully qualified Java name.
     *
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
     * Convert a filename into a fully qualified Java name
     * by considering the filename relative to a base directory.
     * Returns null if the file is outside the base
     * directory.
     *
     * The behaviour of this function is not guaranteed if
     * you pass in a directory name. It is meant for filenames
     * like /foo/bar/p1/s1/TestName.java
     *
     * An example of its use is if your baseDir was the
     * directory /foo/bar and you passed in
     * /foo/bar/p1/s1/TestName.java the function would
     * return p1.s1.TestName
     *
     * Makes no guarantee that the returned name is a valid
     * Java identifier (ie. some of the directory names used
     * may not be valid java identifiers but no check is made
     * for this).
     */
    public static String convertFileToQualifiedName(File baseDir, File f)
    {
        try {
            File pathFile = f.getCanonicalFile();
            File parent = null;
            String name = "";

            while((parent = pathFile.getParentFile()) != null) {
                if(pathFile.equals(baseDir)) {
                    return name;
                }

                if (name == "") {
                    name = pathFile.getName();

                    int firstDot;

                    if((firstDot = name.indexOf('.')) >= 0) {
                        name = name.substring(0, firstDot);
                    }
                }
                else {
                    name = pathFile.getName() + "." + name;
                }

                pathFile = parent;
            }
        }
        catch(IOException ioe) { }

        return null;
    }

    /**
     * Fix up Java class names as returned by Class.getName()
     *
     * The Class.getName() functions are okay for non-array
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
}
