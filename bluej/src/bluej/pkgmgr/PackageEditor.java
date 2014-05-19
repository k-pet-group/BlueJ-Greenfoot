/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2012,2013,2014  Michael Kolling and John Rosenberg 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.pkgmgr;

import java.awt.Component;

import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import bluej.debugger.DebuggerObject;
import bluej.debugger.gentype.GenTypeClass;
import bluej.extmgr.MenuManager;
import bluej.extmgr.PackageExtensionMenu;
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
 */
public final class PackageEditor extends GraphEditor
{
    private PackageEditorListener listener;
    
    /**
     * Construct a package editor for the given package.
     */
    public PackageEditor(Package pkg, PackageEditorListener listener)
    {
        super(pkg);
        this.listener = listener;
    }

    /**
     * Notify listener of an event.
     */
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
    
    /**
     * Notify of some interaction.
     */
    public void recordInteraction(InvokerRecord ir)
    {
        listener.recordInteraction(ir);
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

        Package bluejPackage = (Package) getGraph();
        MenuManager menuManager = new MenuManager(menu);
        menuManager.setMenuGenerator(new PackageExtensionMenu(bluejPackage));
        menuManager.addExtensionMenu(bluejPackage.getProject());

        return menu;
    }
    
    private void addMenuItem(JPopupMenu menu, Action action)
    {
        JMenuItem item = menu.add(action);
        item.setFont(PrefMgr.getPopupMenuFont());
        item.setForeground(envOpColour);
    }
    
    @Override
    public void setPermFocus(boolean focus)
    {
        boolean wasFocussed = hasPermFocus();
        super.setPermFocus(focus);
        if (focus && ! wasFocussed) {
            listener.pkgEditorGotFocus();
        }
        else if (! focus && wasFocussed) {
            listener.pkgEditorLostFocus();
        }
    }
}
