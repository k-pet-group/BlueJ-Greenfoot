package bluej.extensions;

/**
 * This exception will be thrown when a reference to a class is not longer valid. 
 * The most likely reason for this to happen is that the user has deleted the class
 * using the GUI.
 * 
 * @version $Id: ClassNotFoundException.java 1969 2003-05-21 10:28:27Z damiano $
 */

/*
 * Author: Damiano Bolla, University of kent at Canterbury, 2003
 */
public class ClassNotFoundException extends ExtensionException 
{
  ClassNotFoundException (String reason) {
      super (reason);
      }
}