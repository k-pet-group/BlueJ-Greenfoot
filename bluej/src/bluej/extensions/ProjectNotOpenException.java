package bluej.extensions;

/**
 * This exception will be thrown when a reference to a project
 * is no longer valid. The most likely reason is that the 
 * user has closed the project from the GUI.
 * 
 * @version $Id: ProjectNotOpenException.java 1981 2003-05-22 16:35:43Z iau $
 */

/*
 * Author: Damiano Bolla, University of Kent at Canterbury, 2003
 */
public class ProjectNotOpenException extends ExtensionException 
{
  ProjectNotOpenException (String reason) {
      super (reason);
  }
}
