package bluej.pkgmgr;

import bluej.views.CallableView;

import java.awt.event.*;
import javax.swing.*;

/**
 * An action representing an initiation of a call on a method or
 * a constructor by the user.
 *
 * @author  Andrew Patterson
 * @version $Id$
 */
public class CallAction extends AbstractAction
{
    private CallableView cv;
    private Target t;
    private PackageEditor ped;

    public CallAction(String menu, PackageEditor ped, Target t, CallableView cv)
    {
        super(menu);
        this.ped = ped;
        this.cv = cv;
        this.t = t;
    }

    public void actionPerformed(ActionEvent e)
    {
        // not possible? ajp 23/5/02
        //        if (state != S_NORMAL) {
        //            Debug.reportError("Can't instantiate modified class");
        //            return;
        //        }

        ped.raiseMethodCallEvent(t, cv);
    }
}
