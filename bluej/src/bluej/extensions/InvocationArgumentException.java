package bluej.extensions;

/**
 * This exception will be thrown when the parameters passed to an invocation
 * do not match the list of arguments of the invocation.
 * 
 * @version $Id: InvocationArgumentException.java 1970 2003-05-21 10:59:26Z damiano $
 */

/*
 * Author: Damiano Bolla, University of kent at Canterbury, 2003
 */
public class InvocationArgumentException extends ExtensionException 
{
  InvocationArgumentException (String reason) {
      super (reason);
      }
}