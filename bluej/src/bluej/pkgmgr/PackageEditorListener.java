package bluej.pkgmgr;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

/**
 * The listener for PackageEditor events.
 *
 * @author  Andrew Patterson
 * @version $Id: PackageEditorListener.java 505 2000-05-24 05:44:24Z ajp $
 */
public interface PackageEditorListener extends EventListener
{
    void targetEvent(PackageEditorEvent e);
}
