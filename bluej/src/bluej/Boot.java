package bluej;

import bluej.utility.Debug;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import sun.misc.*;
import javax.swing.*;
import java.awt.*;
import java.io.*;

/**
 * This class is now the starting point of BlueJ.
 * What we are tryng to aim to is to isolate the libraries used by BlueJ by the Extensions.
 * To do this we need to start BlueJ with a ClassLoader that understands about the different libraries.
 * To have such a classloadere inherited by "BlueJ" we need that Main be located by such a classloader.
 * 
 * So, we need a boostrap Class.
 * This Class just sets up the right ClassLoader and then calls the main there, nothing more.
 * A sideeffect is that this class also understands where BlueJ is and where the java runtime is.
 * 
 * @author Damiano Bolla, University of Kent at Canterbury, 2003
 * @version $Id: Boot.java 1997 2003-05-30 12:10:30Z damiano $
 */
public class Boot
  {
  // Needed so I can retrieve the boot class from the rest of the program
  private static Boot instance;

  /**
   * The program starting point is this !
   * In the Manifest it must be written that the Main-Class is bluej.Boot
   */
  public static void main(String[] someArgs)
    {
    instance = new Boot ( someArgs );
    instance.bootBluej();
    }

  /**
   * Returns the Boot instance, so I can get what I need from it.
   * The main reason is so I can return to Andrew all he needs in a clean way.
   */
  public static Boot get()
    {
    return instance;
    }

  // =================== Real Boot starts here =================================
  private String [] args;
  private File javaHomeDir;           // What is returned by the getProperty
  private File bluejLibDir;           // This one is calculated.

  private URL [] bootClassPath;
  private ClassLoader bootLoader;     // The loader this class is loaded with

  private URL [] runtimeClassPath;    // The class path used to run the whole BlueJ
  private URLClassLoader runtimeLoader;  // The class loader we are going to use for the rest of BlueJ  

  /**
   * Retuns the args list passed to the starting program.
   */
  public String [] getArgs()
    {
    return args;
    }

  /**
   * returns the home of the javea we have been sarted with
   */
  public File getJavaHome ()
    {
    return javaHomeDir;
    }

  /**
   * Returns the BlueJ library directory.
   */
  public File getBluejLibDir ()
    {
    return bluejLibDir;
    }
    
  /**
   * returns the runtime classpath. The one used to run BlueJ.
   * Please do not mess with it :-)
   */
  public URL [] getRuntimeClassPath ()
    {
    return runtimeClassPath;
    }

  /**
   * Returns the runtime class path as a String.
   * It is what can be used to start another JVM
   */
  public String getRuntimeClassPathString ()
    {
    StringBuffer result=new StringBuffer(300);

    for ( int index=0; index<runtimeClassPath.length; index++ )
      {
      String filename=runtimeClassPath[index].getFile();
      result.append(filename);
      result.append (";");
      }

    return result.toString();
    }

  /**
   * There is no real need for the constructor to be private, but it is safer.
   * I can only do very little in the constructor...
   */
  private Boot ( String [] someArgs )
    {
    args = someArgs;
    }
    
  /**
   * The starting needs to be outside the constructor othervise the
   * instance is going to be null...
   */
  private void bootBluej ()
    {
    // I store away the boot class path list, I may need it in the future.
    URLClassPath aPath = Launcher.getBootstrapClassPath();
    bootClassPath = aPath.getURLs();

    // I also retrieve the current classLoader, this is the boot loader.
    bootLoader = getClass().getClassLoader();

    // Also the java Home is really needed. NOTE it may not be what you expect
    javaHomeDir = new File (System.getProperty("java.home"));

    // Now we try to get what is the BlueJ lib dir.
    bluejLibDir = calculateBluejLibDir ();
      
    try 
      {
      runtimeClassPath = getLibraryItems();
      runtimeLoader = new URLClassLoader(runtimeClassPath,bootLoader);

      Class theMainClass = Class.forName("bluej.Main",true,runtimeLoader);
      Object theMain = theMainClass.newInstance();
      }
    catch ( Exception exc )
      {
      exc.printStackTrace();
      }
    }


  /**
   * Returns the bluejLibDir by doing some reasoning on a resource we know
   * This does not work curLoader.getResource("/bluej/magic.html")); 
   * Normally what you get is:
   * bootUrl=jar:file:/C:/home/bluej/bluej/lib/bluej.jar!/bluej/Boot.class
   * bootFullName=file:/C:/home/bluej/bluej/lib/bluej.jar!/bluej/Boot.class
   * bootName=file:/C:/home/bluej/bluej/lib/bluej.jar
   * finalName=C:/home/bluej/bluej/lib/bluej.jar
   * Parent=C:\home\bluej\bluej\lib   
   */
  private File calculateBluejLibDir ()
    {
    URL bootUrl = getClass().getResource("Boot.class");
//    System.out.println ("bootUrl="+bootUrl);

    String bootFullName = bootUrl.getFile();
//   System.out.println ("bootFullName="+bootFullName);

    int classIndex = bootFullName.indexOf("!");
    String bootName = bootFullName.substring(0,classIndex);
//    System.out.println ("bootName="+bootName);

    if ( ! bootName.startsWith("file:/") )
      throw new NullPointerException ("bootName does not start with file:/");

    // Let me get rid of the first initial file:/
    String finalName = bootName.substring(6);
//    System.out.println ("finalName="+finalName);
    
    File finalFile = new File (finalName);
    File bluejDir = finalFile.getParentFile();
//    System.out.println ("Parent="+bluejDir);
    
    return bluejDir;
    }

    
  /**
   * returns an array of jar url located into the given directory
   */
  private URL [] getLibraryItems ( ) throws MalformedURLException
    {
    File extDir = new File(bluejLibDir,"ext");

    File[] files = extDir.listFiles();
    if (files == null) 
      throw new NullPointerException ("Boot.getLibraryItems: files==null");

    ArrayList urlList = new ArrayList();
    for (int index = 0; index < files.length; index++) 
      {
      File thisFile = files[index];

      // We do not want to try to make sense of directories
      if (thisFile.isDirectory()) continue;

      // Skip also files that do not end in .jar
      if (! hasValidExtension (thisFile) ) continue;

      // Wow, we like it, lets add it !
      urlList.add(thisFile.toURL());
      }

    // We need to add the tools jar...
    urlList.add(getToolsUrl());
    
    return (URL [])urlList.toArray(new URL[0]);
    }



  /**
   * Try to decide if this filename has the right extension
   */
  private boolean hasValidExtension (File aFile )
    {
    if ( aFile == null ) return false;

    // If it ends in jar it is good.
    if (aFile.getName().endsWith(".jar")) return true;

    // if it ends in zip also 
    if (aFile.getName().endsWith(".zip")) return true;

    return false;
    }


  /**
   * This tryes to get the URL of the tools.jar
   * It will look under lib/tools.jar of the current javaHome
   * and in the parent of it.
   * So, as long as you run with the right java it will find it.
   */
  private URL getToolsUrl () throws MalformedURLException
    {
    File toolsFile = new File (javaHomeDir,"lib/tools.jar");
    if ( toolsFile.canRead() ) return toolsFile.toURL();

    File parentDir = javaHomeDir.getParentFile();
    toolsFile = new File (parentDir,"lib/tools.jar");
    if ( toolsFile.canRead() ) return toolsFile.toURL();

    throw new NullPointerException ("Boot.getToolsUrl: Cannot find tools.jar. javaHome="+javaHomeDir);    
    }

  }
