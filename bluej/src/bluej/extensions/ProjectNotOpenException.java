package bluej.extensions;

/**
 * This exception will be thrown when a reference to a project
 * is no longer valid. The most likely reason is that the 
 * user has closed the project from the GUI.
 * 
 * @version $Id: ProjectNotOpenException.java 2314 2003-11-10 14:49:48Z damiano $
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
