package bluej.pkgmgr;

import java.util.*;

/**
 * The listener for PackageEditor events.
 *
 * @author  Andrew Patterson
 * @version $Id: PackageEditorListener.java 1819 2003-04-10 13:47:50Z fisker $
 */
public interface PackageEditorListener extends EventListener
{
    void targetEvent(PackageEditorEvent e);
}
