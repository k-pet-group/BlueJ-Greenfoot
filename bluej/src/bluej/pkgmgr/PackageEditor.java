package bluej.pkgmgr;

import java.awt.Component;

import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import bluej.debugger.DebuggerObject;
import bluej.debugger.gentype.GenTypeClass;
import bluej.graph.GraphEditor;
import bluej.pkgmgr.actions.NewClassAction;
import bluej.pkgmgr.actions.NewPackageAction;
import bluej.pkgmgr.target.Target;
import bluej.prefmgr.PrefMgr;
import bluej.testmgr.record.InvokerRecord;
import bluej.views.CallableView;

/**
 * Canvas to allow editing of packages
 *
 * @author  Andrew Patterson
 * @version $Id: PackageEditor.java 4905 2007-03-29 06:06:30Z davmac $
 */
public final class PackageEditor extends GraphEditor
{
    private PackageEditorListener listener;
    
    public PackageEditor(Package pkg, PackageEditorListener listener)
    {
        super(pkg);
        this.listener = listener;
    }

    // notify all listeners that have registered interest for
    // notification on this event type.
    protected void fireTargetEvent(PackageEditorEvent e)
    {
        if (listener != null) {
            listener.targetEvent(e);
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
    
    public void popupMenu(int x, int y)
    {
        JPopupMenu menu = createMenu();
        menu.show(this, x, y);
    }

    private JPopupMenu createMenu()
    {
       JPopupMenu menu = new JPopupMenu();
       Action newClassAction = new NewClassAction();
       addMenuItem(menu, newClassAction);
       Action newPackageAction = new NewPackageAction();
       addMenuItem(menu, newPackageAction);
       return menu;
    }
    
    private void addMenuItem(JPopupMenu menu, Action action)
    {
        JMenuItem item = menu.add(action);
        item.setFont(PrefMgr.getPopupMenuFont());
        item.setForeground(envOpColour);
    }
}
