package bluej.extensions;

/**
 * @author Clive Miller
 * @version $Id: MenuListener.java 1459 2002-10-23 12:13:12Z jckm $
 */
public interface MenuListener
{
    /**
     * A menu has been invoked by the user
     * @param source the BMenuItem responsible for raising the call
     * @param pkg the package frame from which the menu was invoked.
     * This may be empty, which should be checked with 
     * {@link bluej.extensions.BPackage#isEmptyFrame() isEmptyFrame}.
     */
    public void menuInvoked (Object source, BPackage pkg);
}