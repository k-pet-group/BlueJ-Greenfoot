package bluej.extensions;

/**
 * This exception will be thrown when a reference to a class is no longer valid. 
 * The most likely reason is that the user has deleted the class
 * using the GUI.
 * 
 * @version $Id: ClassNotFoundException.java 1981 2003-05-22 16:35:43Z iau $
 */

/*
 * Author: Damiano Bolla, University of Kent at Canterbury, 2003
 */
public class ClassNotFoundException extends ExtensionException 
{
  ClassNotFoundException (String reason) {
      super (reason);
  }
}
