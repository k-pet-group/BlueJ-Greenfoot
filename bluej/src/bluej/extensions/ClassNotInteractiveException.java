package bluej.extensions;

/**
 * This exception will be thrown when a class is source-only and cannot be
 * compiled. Normally a BlueJ class is "interactive", meaning that you can interact with it by
 * editing and compiling it. Attempts to compile non-interactive classes
 * will cause this exception to be thrown.
 * 
 * @version $Id: ClassNotInteractiveException.java 1981 2003-05-22 16:35:43Z iau $
 */

/*
 * Author: Damiano Bolla, University of kent at Canterbury, 2003
 */

public class ClassNotInteractiveException extends ExtensionException 
{
  public ClassNotInteractiveException( String reason)
  {
      super(reason);
  }
}
