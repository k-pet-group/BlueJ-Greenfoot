package bluej.editor.moe.autocomplete.parser;

import java.util.*;
import java.io.*;
import java.net.*;
import java.lang.reflect.*;

import bluej.classmgr.*;
import bluej.editor.moe.autocomplete.Debug;

/**
 * This class provides some methods to enable the Parser to
 * find class objects. It enables the project root to be set
 * so that a ProjectClassLoader can be constructed.
 *
 * This class was taken from version 0.3 of SpeedJava
 * and has been modified to find classes written in the
 * current BlueJ project.
 *
 * @version 1.2 (10/03/2003)
 *
 * @author Chris Daly (<A href="mailto:cdd1@ukc.ac.uk">cdd1@ukc.ac.uk</A>),
 *         Darren Link (<A href="mailto:drl1@ukc.ac.uk">drl1@ukc.ac.uk</A>),
 *         Mike Stewart (<A href="mailto:mjs7@ukc.ac.uk">mjs7@ukc.ac.uk</A>)
 */
public class ClassUtilities {

  private static final Object syncToken = new Object();

    private static File projRootDir = null;

    /**
     * This method can be used to set the location
     * of the project root so that the ClassLoader
     * can find classes in the current BlueJ project
     *
     * THIS METHOD IS NOT SPEEDJAVA CODE
     *
     * @param projRoot The root of the current BlueJ project.
     */
    public static void setProjectRoot(File projRoot){
        projRootDir = projRoot;
    }

    /**
     * This method can be used to find a class object
     * using a full class name.  The method creates
     * a ProjectClassLoader upon each call so that
     * the latest version of the class is found.
     * The ProjectClassLoader uses the project root
     * to find classes written in the current project
     * in BlueJ
     *
     * THIS METHOD HAS BEEN ADAPTED FROM THE SPEEDJAVA CODE
     *
     * @param name a full class name (e.g. java.lang.String)
     * @return the found class. (Will be null if not found)
     */
  public static Class findClass(String name)
                              throws java.lang.Throwable{
    synchronized (syncToken) {
            Debug.printParserMessage("");
            Debug.printParserMessage("-------------------------------------------------------");
            Debug.printParserMessage("ClassUtilities.findClass=" + name);
      ProjectClassLoader newLoader = ClassMgr.getProjectLoader(projRootDir);
            Class found = findClass(name, false, newLoader);
            Debug.printParserMessage("ClassUtilities.foundClass=" + found);
            Debug.printParserMessage("-------------------------------------------------------");
            Debug.printParserMessage("");
      return found;
    }
  }

    /**
     * THIS METHOD IS SPEEDJAVA CODE
     */
  private static Class findClass(String name,
                                   boolean flag,
                               ClassLoader loader)
                                   throws java.lang.Throwable{
      Class cls = Class.forName(name,flag,loader);
      return cls;
  }

}