package bluej.extensions;

/**
 * This exception will be thrown when an Extension that has been unloaded
 * from BlueJ still tries to access methods of the BlueJ class.
 * If the Extension terminates its activities when the Extension.terminate() 
 * method is called then this exception will never be thrown.
 * 
 * @version $Id: ExtensionUnloadedException.java 1971 2003-05-21 12:30:12Z damiano $
 */

/*
 * Author: Damiano Bolla, University of kent at Canterbury, 2003
 */

public class ExtensionUnloadedException extends RuntimeException 
{
}