package bluej.extensions;

/**
 * This exception will be thrown when an exception occurs during a method or constructor 
 * invocation. The most likely cause of this exception is the user cancelling a 
 * long-running object construction or method invocation from the GUI.
 * 
 * @version $Id: InvocationErrorException.java 1981 2003-05-22 16:35:43Z iau $
 */

/*
 * Author: Damiano Bolla, University of Kent at Canterbury, 2003
 */
public class InvocationErrorException extends ExtensionException 
{
  InvocationErrorException (String reason) {
      super (reason);
  }
}
