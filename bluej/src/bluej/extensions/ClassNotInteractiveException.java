package bluej.extensions;

/**
 * This exception will be thrown when a Class is a source only Class and cannot be
 * compiled. Normally a BClass under BlueJ is an interactive one, meaning that you can interact with it by
 * editing it and compile. It may happen that the BClass ends up in a state where the
 * above operation cannod be done and in this case this Exception will be thrown.
 * 
 * @version $Id: ClassNotInteractiveException.java 1977 2003-05-22 10:25:47Z damiano $
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