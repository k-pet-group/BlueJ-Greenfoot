package bluej.extensions;


/**
 * This exception will be thrown when a reference to a package
 * is not longer valid. The most likely reason for this to happen is that the 
 * user has deleted the package from the GUI.
 * 
 * @version $Id: PackageNotFoundException.java 1968 2003-05-21 09:59:49Z damiano $
 */

/*
 * Author: Damiano Bolla, University of kent at Canterbury, 2003
 */
public class PackageNotFoundException extends ExtensionException 
{
  PackageNotFoundException (String reason) {
      super (reason);
      }

}