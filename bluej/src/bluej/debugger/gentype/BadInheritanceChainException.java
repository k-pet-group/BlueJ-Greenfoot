package bluej.debugger.gentype;

/**
 * Certain methods perform mappings of type parameters between a superclass
 * and subclass. This exception is thrown when the given classes have no
 * inheritance relationship (the "superclass" is not really a superclass of the
 * subclass).
 * 
 * @author Davin McCall
 * @version $Id: BadInheritanceChainException.java 2965 2004-08-31 05:58:15Z davmac $
 */
public class BadInheritanceChainException extends RuntimeException
{

}
