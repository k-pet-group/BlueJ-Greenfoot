package bluej.extensions;

/**
 * This exception will be thrown when a new class is created and not java source file is provided.
 * 
 * @version $Id: MissingJavaFileException.java 2209 2003-10-10 14:02:43Z damiano $
 */

/*
 * Author: Damiano Bolla, University of Kent at Canterbury, 2003
 */
public class MissingJavaFileException extends ExtensionException 
{
  MissingJavaFileException (String reason) {
      super (reason);
  }
}
