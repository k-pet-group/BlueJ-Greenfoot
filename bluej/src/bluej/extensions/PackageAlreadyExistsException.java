package bluej.extensions;

/**
 * This exception is thrown when there is a request to create a new Package
 * but the package already exists in BlueJ.
 * 
 * @version $Id: PackageAlreadyExistsException.java 2213 2003-10-13 09:55:27Z damiano $
 */

/*
 * Author: Damiano Bolla, University of Kent at Canterbury, 2003
 */
public class PackageAlreadyExistsException extends ExtensionException 
{
    PackageAlreadyExistsException (String reason) {
        super (reason);
    }

}
