package bluej.extensions;

/**
 * This exception will be thrown when there is an exception during an Invocation.
 * The most likely cause of this exception is the user cancelling an object creation
 * or method invocation from the GUI.
 * 
 * @version $Id: InvocationErrorException.java 1970 2003-05-21 10:59:26Z damiano $
 */

/*
 * Author: Damiano Bolla, University of kent at Canterbury, 2003
 */
public class InvocationErrorException extends ExtensionException 
{
  InvocationErrorException (String reason) {
      super (reason);
      }
}