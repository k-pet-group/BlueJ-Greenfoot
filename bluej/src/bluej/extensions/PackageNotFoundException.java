package bluej.extensions;


/**
 * This exception will be thrown when a reference to a package
 * is no longer valid. The most likely reason is that the 
 * user has deleted the package from the GUI.
 * 
 * @version $Id: PackageNotFoundException.java 1981 2003-05-22 16:35:43Z iau $
 */

/*
 * Author: Damiano Bolla, University of Kent at Canterbury, 2003
 */
public class PackageNotFoundException extends ExtensionException 
{
  PackageNotFoundException (String reason) {
      super (reason);
  }

}
