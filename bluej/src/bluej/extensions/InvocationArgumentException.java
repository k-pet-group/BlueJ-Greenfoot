package bluej.extensions;

/**
 * This exception will be thrown when the parameters passed to an invocation
 * do not match the list of arguments of the invocation.
 * 
 * @version $Id: InvocationArgumentException.java 1981 2003-05-22 16:35:43Z iau $
 */

/*
 * Author: Damiano Bolla, University of Kent at Canterbury, 2003
 */
public class InvocationArgumentException extends ExtensionException 
{
  InvocationArgumentException (String reason) {
      super (reason);
  }
}
