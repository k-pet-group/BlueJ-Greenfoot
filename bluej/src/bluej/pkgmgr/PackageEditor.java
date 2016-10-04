/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2012,2013,2014,2016  Michael Kolling and John Rosenberg 
 
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

import java.awt.Rectangle;
import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Window;

import bluej.debugger.DebuggerObject;
import bluej.debugger.gentype.GenTypeClass;
import bluej.extensions.BDependency;
import bluej.extensions.event.DependencyEvent;
import bluej.extmgr.ExtensionsManager;
import bluej.extmgr.MenuManager;
import bluej.extmgr.PackageExtensionMenu;
import bluej.graph.SelectionController;
import bluej.pkgmgr.actions.NewClassAction;
import bluej.pkgmgr.actions.NewPackageAction;
import bluej.pkgmgr.dependency.Dependency;
import bluej.pkgmgr.dependency.UsesDependency;
import bluej.pkgmgr.target.DependentTarget;
import bluej.pkgmgr.target.Target;
import bluej.prefmgr.PrefMgr;
import bluej.testmgr.record.InvokerRecord;
import bluej.views.CallableView;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Canvas to allow editing of packages
 *
 * @author  Andrew Patterson
 */
@OnThread(Tag.FXPlatform)
public final class PackageEditor extends StackPane
{
    private static final int RIGHT_PLACEMENT_MIN = 300;
    private static final int WHITESPACE_SIZE = 10;
    /**  The grid resolution for graph layout. */
    public static final int GRID_SIZE = 10;
    
    private final PkgMgrFrame pmf;
    private final PackageEditorListener listener;
    private final Package pkg;
    private final SelectionController selectionController;
    private boolean hasPermFocus;
    private final SimpleBooleanProperty showExtends = new SimpleBooleanProperty();
    private final SimpleBooleanProperty showUses = new SimpleBooleanProperty();

    /** all the uses-arrows in a package */
    @OnThread(value = Tag.Any,requireSynchronized = true)
    private final List<Dependency> usesArrows = new ArrayList<>();

    /** all the extends-arrows in a package */
    @OnThread(value = Tag.Any,requireSynchronized = true)
    private final List<Dependency> extendsArrows = new ArrayList<>();

    private final AnchorPane classLayer = new AnchorPane();

    /**
     * Construct a package editor for the given package.
     */
    @OnThread(Tag.Any)
    public PackageEditor(PkgMgrFrame pmf, Package pkg, PackageEditorListener listener)
    {
        this.pmf = pmf;
        this.pkg = pkg;
        this.listener = listener;
        this.selectionController = new SelectionController(this);
        Platform.runLater(() -> {getChildren().add(classLayer);});
    }

    /**
     * Notify listener of an event.
     */
    @OnThread(Tag.Swing)
    protected void fireTargetEvent(PackageEditorEvent e)
    {
        if (listener != null) {
            listener.targetEvent(e);
        }
    }

    @OnThread(Tag.Swing)
    public void raiseMethodCallEvent(Object src, CallableView cv)
    {
        fireTargetEvent(
            new PackageEditorEvent(src, PackageEditorEvent.TARGET_CALLABLE, cv));
    }

    @OnThread(Tag.Swing)
    public void raiseRemoveTargetEvent(Target t)
    {
        fireTargetEvent(
            new PackageEditorEvent(t, PackageEditorEvent.TARGET_REMOVE));
    }

    @OnThread(Tag.Swing)
    public void raiseBenchToFixtureEvent(Target t)
    {
        fireTargetEvent(
            new PackageEditorEvent(t, PackageEditorEvent.TARGET_BENCHTOFIXTURE));
    }

    @OnThread(Tag.Swing)
    public void raiseFixtureToBenchEvent(Target t)
    {
        fireTargetEvent(
            new PackageEditorEvent(t, PackageEditorEvent.TARGET_FIXTURETOBENCH));
    }

    @OnThread(Tag.Swing)
    public void raiseMakeTestCaseEvent(Target t)
    {
        fireTargetEvent(
            new PackageEditorEvent(t, PackageEditorEvent.TARGET_MAKETESTCASE));
    }

    @OnThread(Tag.Swing)
    public void raiseRunTargetEvent(Target t, String name)
    {
        fireTargetEvent(
            new PackageEditorEvent(t, PackageEditorEvent.TARGET_RUN, name));
    }

    @OnThread(Tag.Swing)
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
    @OnThread(Tag.Swing)
    public void raisePutOnBenchEvent(Window src, DebuggerObject obj, GenTypeClass iType, InvokerRecord ir)
    {
        fireTargetEvent(
            new PackageEditorEvent(src, PackageEditorEvent.OBJECT_PUTONBENCH, obj, iType, ir));
    }
    
    /**
     * Notify of some interaction.
     */
    @OnThread(Tag.Swing)
    public void recordInteraction(InvokerRecord ir)
    {
        listener.recordInteraction(ir);
    }
    
    public void popupMenu(int x, int y)
    {
        //JPopupMenu menu = createMenu();
        //menu.show(this, x, y);
    }

    @OnThread(Tag.Swing)
    private JPopupMenu createMenu()
    {
        JPopupMenu menu = new JPopupMenu();
        Action newClassAction = new NewClassAction(pmf);
        addMenuItem(menu, newClassAction);
        Action newPackageAction = new NewPackageAction(pmf);
        addMenuItem(menu, newPackageAction);

        MenuManager menuManager = new MenuManager(menu);
        menuManager.setMenuGenerator(new PackageExtensionMenu(pkg));
        menuManager.addExtensionMenu(pkg.getProject());

        return menu;
    }

    @OnThread(Tag.Swing)
    private void addMenuItem(JPopupMenu menu, Action action)
    {
        JMenuItem item = menu.add(action);
        item.setFont(PrefMgr.getPopupMenuFont());
        //item.setForeground(envOpColour);
    }
    
    public void setPermFocus(boolean focus)
    {
        boolean wasFocussed = hasPermFocus;
        this.hasPermFocus = focus;
        if (focus && ! wasFocussed) {
            listener.pkgEditorGotFocus();
        }
        else if (! focus && wasFocussed) {
            listener.pkgEditorLostFocus();
        }
    }

    /**
     * Check whether the editor has focus within its parent.
     */
    public boolean hasPermFocus()
    {
        return hasPermFocus;
    }

    @OnThread(Tag.FXPlatform)
    public Window getFXWindow()
    {
        return pmf.getFXWindow();
    }

    /**
     * Finds the Edge that covers the coordinate x,y and is visible. If no
     * (visible) edge is found, null is returned.
     *
     * @param x
     *            the x coordinate
     * @param y
     *            the y coordinate
     * @return an edge at that position, or null
     */
    /*
    private Dependency findEdge(int x, int y)
    {
        Dependency element = null;
        for (Iterator<? extends Dependency> it = getEdges(); it.hasNext();) {
            element = it.next();
            if (element.isVisible() && element.contains(x, y)) {
                return element;
            }
        }
        return null;
    }
    */

    /**
     * Position the given vertex nicely in the graph. Thsi usually means that it
     * will be placed somewhere near the top where it does not overlap with
     * existing vertices.
     *
     * @param t
     *            The vertex to place.
     */
    public void findSpaceForVertex(Target t)
    {
        Area a = new Area();

        for (Target vertex : pkg.getVertices())
        {
            // lets discount the vertex we are adding from the space
            // calculations
            if (vertex != t) {
                Rectangle vr = new Rectangle(vertex.getX(), vertex.getY(), (int)vertex.getWidth(), (int)vertex.getHeight());
                a.add(new Area(vr));
            }
        }
        
        double minWidth = getMinWidth();
        double minHeight = getMinHeight();

        if (RIGHT_PLACEMENT_MIN > minWidth)
            minWidth = RIGHT_PLACEMENT_MIN;

        Rectangle targetRect = new Rectangle((int)t.getWidth() + WHITESPACE_SIZE * 2, (int)t.getHeight() + WHITESPACE_SIZE * 2);

        for (int y = 0; y < (2 * minHeight); y += 10) {
            for (int x = 0; x < (minWidth - t.getWidth() - 2 * WHITESPACE_SIZE); x += 10) {
                targetRect.setLocation(x, y);
                if (!a.intersects(targetRect)) {
                    t.setPos(x + 10, y + 10);
                    return;
                }
            }
        }

        t.setPos(10, (int)minHeight + 10);
    }

    public void graphChanged()
    {
        HashMap<Node, Boolean> keep = new HashMap<>();

        // We assume all components currently in the graph belong to vertices.
        // We first mark all of them as no longer needed:
        for (Node c : classLayer.getChildren())
        {
            keep.put(c, false);
        }

        // Add what needs to be added:
        for (Target v : pkg.getVertices())
        {
            if (!keep.containsKey(v.getNode()))
            {
                classLayer.getChildren().add(v.getNode());
                //v.getComponent().addFocusListener(focusListener);
                //v.getComponent().addFocusListener(selectionController);
                //v.getComponent().addKeyListener(selectionController);
                if (v.isSelected()) {
                    selectionController.addToSelection(v);
                }
            }
            // If it's in the vertices, keep it:
            keep.put(v.getNode(), true);
        }
        // Remove what needs to be removed (i.e. what we didn't see in the vertices):
        for (Map.Entry<Node, Boolean> e : keep.entrySet())
        {
            if (e.getValue().booleanValue() == false)
            {
                classLayer.getChildren().remove(e.getKey());
                //e.getKey().removeFocusListener(focusListener);
                //e.getKey().removeFocusListener(selectionController);
                //e.getKey().removeKeyListener(selectionController);
            }
        }
        
        //TODO make sure removed items aren't still in the selection

        repaint();
    }
    
    public void graphClosed()
    {
        
    }

    public void repaint()
    {
        //TODO redraw edges on canvas
    }


    /**
     * Clear the set of selected classes. (Nothing will be selected after this.)
     */
    public void clearSelection()
    {
        selectionController.clearSelection();
    }


    /**
     * Add to the current selection
     * @param element the element to add
     */
    public void addToSelection(Target element)
    {
        selectionController.addToSelection(element);
    }

    @OnThread(Tag.Any)
    public Package getPackage()
    {
        return pkg;
    }

    /**
     * Start our mouse listener. This is not done in the constructor, because we want 
     * to give others (the PkgMgrFrame) the chance to listen first.
     */
    public void startMouseListening()
    {
        addEventHandler(MouseEvent.MOUSE_DRAGGED, selectionController::mouseDragged);
        addEventHandler(MouseEvent.MOUSE_CLICKED, selectionController::mouseClicked);
        addEventHandler(MouseEvent.MOUSE_PRESSED, e -> selectionController.mousePressed(e, null));
        addEventHandler(MouseEvent.MOUSE_RELEASED, selectionController::mouseReleased);
        addEventHandler(KeyEvent.KEY_PRESSED, selectionController::keyPressed);
        addEventHandler(KeyEvent.KEY_RELEASED, selectionController::keyReleased);
    }

    public void scrollTo(Target target)
    {
        //TODO
    }

    public boolean isDrawingDependency()
    {
        return false;
    }


    @OnThread(Tag.FXPlatform)
    public synchronized Collection<Dependency> getEdges()
    {
        List<Dependency> deps = new ArrayList<>();

        if (isShowUses())
            deps.addAll(usesArrows);
        if (isShowExtends())
            deps.addAll(extendsArrows);

        return deps;
    }

    /**
     * Get the selected Dependencies.
     *
     * @return The currently selected dependency or null.
     */
    /*
    @OnThread(Tag.FXPlatform)
    public Dependency getSelectedDependency()
    {
        for (Iterator<? extends Dependency> it = getEdges(); it.hasNext();) {
            Dependency edge = it.next();
            if (edge.isSelected()) {
                return edge;
            }
        }
        return null;
    }
    */



    /**
     * Returns the {@link Dependency} with the specified <code>origin</code>,
     * <code>target</code> and <code>type</code> or <code>null</code> if there
     * is no such dependency.
     *
     * @param origin
     *            The origin of the dependency.
     * @param target
     *            The target of the dependency.
     * @param type
     *            The type of the dependency (there may be more than one
     *            dependencies with the same origin and target but different
     *            types).
     * @return The {@link Dependency} with the specified <code>origin</code>,
     *         <code>target</code> and <code>type</code> or <code>null</code> if
     *         there is no such dependency.
     */
    @OnThread(Tag.Any)
    public synchronized Dependency getDependency(DependentTarget origin, DependentTarget target, BDependency.Type type)
    {
        List<Dependency> dependencies = new ArrayList<Dependency>();

        switch (type) {
            case USES :
                dependencies = usesArrows;
                break;
            case IMPLEMENTS :
            case EXTENDS :
                dependencies = extendsArrows;
                break;
            case UNKNOWN :
                // If the type of the dependency is UNKNOWN, the requested
                // dependency does not exist anymore. In this case the method
                // returns null.
                return null;
        }

        for (Dependency dependency : dependencies) {
            DependentTarget from = dependency.getFrom();
            DependentTarget to = dependency.getTo();

            if (from.equals(origin) && to.equals(target)) {
                return dependency;
            }
        }

        return null;
    }

    /**
     * Add a dependancy in this package. The dependency is also added to the
     * individual targets involved.
     */
    @OnThread(Tag.Swing)
    // package-visible; see Package.addDependency proxy
    void addDependency(Dependency d, boolean recalc)
    {
        DependentTarget from = d.getFrom();
        DependentTarget to = d.getTo();

        if (from == null || to == null) {
            // Debug.reportError("Found invalid dependency - ignored.");
            return;
        }

        synchronized (this)
        {
            if (d instanceof UsesDependency)
            {
                int index = usesArrows.indexOf(d);
                if (index != -1)
                {
                    ((UsesDependency)usesArrows.get(index)).setFlag(true);
                    return;
                }
                else
                    usesArrows.add(d);
            }
            else
            {
                if (extendsArrows.contains(d))
                    return;
                else
                    extendsArrows.add(d);
            }
        }

        Platform.runLater(() -> {
            from.addDependencyOut(d, recalc);
            to.addDependencyIn(d, recalc);
        });

        // Inform all listeners about the added dependency
        DependencyEvent event = new DependencyEvent(d, pkg, DependencyEvent.Type.DEPENDENCY_ADDED);
        ExtensionsManager.getInstance().delegateEvent(event);
    }

    /**
     * Remove a dependency from this package. The dependency is also removed
     * from the individual targets involved.
     */
    @OnThread(Tag.Swing)
    public void removeDependency(Dependency d, boolean recalc)
    {
        synchronized (this)
        {
            if (d instanceof UsesDependency)
                usesArrows.remove(d);
            else
                extendsArrows.remove(d);
        }

        DependentTarget from = d.getFrom();
        DependentTarget to = d.getTo();
        
        Platform.runLater(() -> {
            from.removeDependencyOut(d, recalc);
            to.removeDependencyIn(d, recalc);
        });

        // Inform all listeners about the removed dependency
        DependencyEvent event = new DependencyEvent(d, pkg, DependencyEvent.Type.DEPENDENCY_REMOVED);
        ExtensionsManager.getInstance().delegateEvent(event);
    }

    public void setShowUses(boolean state)
    {
        showUses.set(state);
    }

    public void setShowExtends(boolean state)
    {
        showExtends.set(state);
    }

    public boolean isShowUses()
    {
        return showUses.get();
    }

    public boolean isShowExtends()
    {
        return showExtends.get();
    }


    @OnThread(Tag.Any)
    public synchronized List<Dependency> getUsesArrows()
    {
        return new ArrayList<>(usesArrows);
    }

    public void clearState()
    {
        // TODO
    }

    public void doNewInherits()
    {
        // TODO
    }

    public boolean doRemoveDependency()
    {
        return false;
    }

    public BooleanProperty showUsesProperty()
    {
        return showUses;
    }

    public BooleanProperty showInheritsProperty()
    {
        return showExtends;
    }

    public void selectOnly(Target target)
    {
        selectionController.selectOnly(target);
    }
}
