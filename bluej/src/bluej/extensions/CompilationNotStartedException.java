package bluej.extensions;

/**
 * This exception will be thrown when a compile request cannot be started.
 * The most likely reason for a compilation to abort is that BlueJ is currently
 * executing some class code.
 *
 * @version $Id: CompilationNotStartedException.java 1990 2003-05-27 09:54:17Z damiano $
 */

/*
 * Author: Damiano Bolla, University of Kent at Canterbury, 2003
 */
public class CompilationNotStartedException extends ExtensionException 
{
  CompilationNotStartedException (String reason) {
      super (reason);
  }

}
