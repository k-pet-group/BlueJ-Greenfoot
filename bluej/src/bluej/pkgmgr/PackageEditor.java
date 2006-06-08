package bluej.pkgmgr;

import java.awt.Component;

import bluej.debugger.DebuggerObject;
import bluej.debugger.gentype.GenTypeClass;
import bluej.graph.GraphEditor;
import bluej.pkgmgr.target.Target;
import bluej.testmgr.record.InvokerRecord;
import bluej.views.CallableView;

/**
 * Canvas to allow editing of packages
 *
 * @author  Andrew Patterson
 * @version $Id: PackageEditor.java 4345 2006-06-08 06:33:46Z davmac $
 */
public final class PackageEditor extends GraphEditor
{
    public PackageEditor(Package pkg)
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

    public void raiseMethodCallEvent(Object src, CallableView cv)
    {
        fireTargetEvent(
            new PackageEditorEvent(src, PackageEditorEvent.TARGET_CALLABLE, cv));
    }

    public void raiseRemoveTargetEvent(Target t)
    {
        fireTargetEvent(
            new PackageEditorEvent(t, PackageEditorEvent.TARGET_REMOVE));
    }

    public void raiseBenchToFixtureEvent(Target t)
    {
        fireTargetEvent(
            new PackageEditorEvent(t, PackageEditorEvent.TARGET_BENCHTOFIXTURE));
    }

    public void raiseFixtureToBenchEvent(Target t)
    {
        fireTargetEvent(
            new PackageEditorEvent(t, PackageEditorEvent.TARGET_FIXTURETOBENCH));
    }

    public void raiseMakeTestCaseEvent(Target t)
    {
        fireTargetEvent(
            new PackageEditorEvent(t, PackageEditorEvent.TARGET_MAKETESTCASE));
    }

    public void raiseRunTargetEvent(Target t, String name)
    {
        fireTargetEvent(
            new PackageEditorEvent(t, PackageEditorEvent.TARGET_RUN, name));
    }

    public void raiseOpenPackageEvent(Target t, String packageName)
    {
        fireTargetEvent(
            new PackageEditorEvent(t, PackageEditorEvent.TARGET_OPEN,
                                    packageName));
    }

    /**
     * Raise an event to notify that an object should be placed on the oject bench.
     * @param src  The source of the event
     * @param obj    The object to be put on the event
     * @param iType  The "interface" type of the object (declared type, used as a
     *               fallback if the runtime type is not accessible)
     * @param ir   The invoker record for the invocation used to create this object
     */
    public void raisePutOnBenchEvent(Component src, DebuggerObject obj, GenTypeClass iType, InvokerRecord ir)
    {
        fireTargetEvent(
            new PackageEditorEvent(src, PackageEditorEvent.OBJECT_PUTONBENCH, obj, iType, ir));
    }
}
