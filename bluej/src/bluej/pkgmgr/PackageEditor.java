package bluej.pkgmgr;

import bluej.graph.GraphEditor;
import bluej.graph.Graph;
import bluej.views.CallableView;

import java.util.Enumeration;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Canvas to allow editing of packages
 *
 * @author  Andrew Patterson
 * @version $Id: PackageEditor.java 505 2000-05-24 05:44:24Z ajp $
 */
public class PackageEditor extends GraphEditor
{
    public PackageEditor(Package pkg, PkgMgrFrame frame)
    {
        super(pkg);
    }

    /**
     * This component will raise PackageEditorEvents when things
     * happen with regards editing. The following functions handle
     * this.
     */

    public void addPackageEditorListener(PackageEditorListener l) {
        listenerList.add(PackageEditorListener.class, l);
    }

    public void removePackageEditorListener(PackageEditorListener l) {
        listenerList.remove(PackageEditorListener.class, l);
    }

    // notify all listeners that have registered interest for
    // notification on this event type.
    protected void fireTargetEvent(PackageEditorEvent e)
    {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i] == PackageEditorListener.class) {
                ((PackageEditorListener)listeners[i+1]).targetEvent(e);
            }
        }
    }

    public void raiseMethodCallEvent(Target t, CallableView cv)
    {
        fireTargetEvent(
            new PackageEditorEvent(t, PackageEditorEvent.TARGET_CALLABLE, cv));
    }

    public void raiseRemoveTargetEvent(Target t)
    {
        fireTargetEvent(
            new PackageEditorEvent(t, PackageEditorEvent.TARGET_REMOVE));
    }

    public void raiseOpenPackageEvent(Target t, String packageName)
    {
        fireTargetEvent(
            new PackageEditorEvent(t, PackageEditorEvent.TARGET_OPEN, packageName));
    }


    public void raiseMessageEvent(String msgId)
    {

    }

    public void raiseMessageTextEvent(String msgId, String text)
    {

    }

   public void raiseErrorEvent(String msgId)
   {

   }


}
