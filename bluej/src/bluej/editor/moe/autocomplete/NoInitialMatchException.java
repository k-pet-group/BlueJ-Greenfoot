package bluej.editor.moe.autocomplete;

/**
 * This exception is thrown by the constructor of a MoeDropDownList
 * if no initial matches are found.  This prevents an empty list
 * from being displayed.
 *
 * @version 1.2 (10/03/2003)
 *
 * @author Chris Daly (<A href="mailto:cdd1@ukc.ac.uk">cdd1@ukc.ac.uk</A>),
 *         Darren Link (<A href="mailto:drl1@ukc.ac.uk">drl1@ukc.ac.uk</A>),
 *         Mike Stewart (<A href="mailto:mjs7@ukc.ac.uk">mjs7@ukc.ac.uk</A>)
 */
public class NoInitialMatchException extends Exception{
  public NoInitialMatchException(String message){
    super(message);
  }
}
