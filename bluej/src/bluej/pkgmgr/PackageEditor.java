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

import bluej.utility.Utility;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
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
import bluej.utility.javafx.JavaFXUtil;
import bluej.utility.javafx.ResizableCanvas;
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
    private final List<UsesDependency> usesArrows = new ArrayList<>();

    /** all the extends-arrows in a package */
    @OnThread(value = Tag.Any,requireSynchronized = true)
    private final List<Dependency> extendsArrows = new ArrayList<>();

    // Two class layers: one front (for normal classes),
    // and one back (for test classes)
    private final AnchorPane frontClassLayer = new AnchorPane();
    private final AnchorPane backClassLayer = new AnchorPane();
    private Pane selectionLayer = new Pane();
    private final Canvas arrowLayer = new ResizableCanvas();
    private boolean aboutToRepaint = false;

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
        Platform.runLater(() -> {
            JavaFXUtil.addStyleClass(this, "class-diagram");
            frontClassLayer.setBackground(null);
            frontClassLayer.setPickOnBounds(false);
            backClassLayer.setBackground(null);
            JavaFXUtil.addChangeListenerPlatform(arrowLayer.widthProperty(), s -> repaint());
            JavaFXUtil.addChangeListenerPlatform(arrowLayer.heightProperty(), s -> repaint());
            selectionLayer = new Pane();
            // The mouse events occur on us not on the selection layer.
            // We don't want the display getting in the way of mouse events:
            selectionLayer.setMouseTransparent(true);
            javafx.scene.shape.Rectangle rect = selectionController.getMarquee().getRectangle();
            JavaFXUtil.addStyleClass(rect, "marquee");
            selectionLayer.getChildren().add(rect);

            getChildren().addAll(arrowLayer, backClassLayer, frontClassLayer, selectionLayer);
        });
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
        for (Node c : frontClassLayer.getChildren())
        {
            keep.put(c, false);
        }
        for (Node c : backClassLayer.getChildren())
        {
            keep.put(c, false);
        }

        // Add what needs to be added:
        for (Target v : pkg.getVertices())
        {
            if (!keep.containsKey(v.getNode()))
            {
                (v.isFront() ? frontClassLayer : backClassLayer).getChildren().add(v.getNode());
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
                frontClassLayer.getChildren().remove(e.getKey());
                backClassLayer.getChildren().remove(e.getKey());
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

    private static final int ARROW_SIZE = 18; // pixels
    private static final double ARROW_ANGLE = Math.PI / 6; // radians
    private static final double DASHES[] = {5.0f, 2.0f};

    public void repaint()
    {
        if (!aboutToRepaint)
        {
            aboutToRepaint = true;
            JavaFXUtil.runAfterCurrent(this::actualRepaint);
        }
    }

    private void actualRepaint()
    {
        aboutToRepaint = false;
        List<Dependency> extendsDeps;
        List<UsesDependency> usesDeps;
        // Don't hold the monitor too long: access once and take copy.
        synchronized (this)
        {
            extendsDeps = new ArrayList<>(this.extendsArrows);
            usesDeps = new ArrayList<>(this.usesArrows);
        }
        GraphicsContext g = arrowLayer.getGraphicsContext2D();
        g.clearRect(0, 0, arrowLayer.getWidth(), arrowLayer.getHeight());

        for (Dependency d : extendsDeps)
        {
            g.setStroke(Color.BLACK);
            g.setLineWidth(1.0);
            g.setLineDashes();
            Dependency.Line line = d.computeLine();
            double fromY = line.from.getY();
            double fromX = line.from.getX();
            double toY = line.to.getY();
            double toX = line.to.getX();


            double angle = Math.atan2(-(fromY - toY), fromX - toX);

            double arrowJoinX = toX + ((ARROW_SIZE - 2) * Math.cos(angle));
            double arrowJoinY = toY - ((ARROW_SIZE - 2) * Math.sin(angle));

            // draw the arrow head
            double[] xPoints = {toX, toX + ((ARROW_SIZE) * Math.cos(angle + ARROW_ANGLE)),
                    toX + (ARROW_SIZE * Math.cos(angle - ARROW_ANGLE))};
            double[] yPoints = {toY, toY - ((ARROW_SIZE) * Math.sin(angle + ARROW_ANGLE)),
                    toY - (ARROW_SIZE * Math.sin(angle - ARROW_ANGLE))};

            g.strokePolygon(xPoints, yPoints, 3);
            g.strokeLine(fromX, fromY, arrowJoinX, arrowJoinY);
        }

        for (UsesDependency d : usesDeps)
        {
            /*

            private static final BasicStroke dashedUnselected = new BasicStroke(strokeWidthDefault, BasicStroke.CAP_BUTT,
                    BasicStroke.JOIN_MITER, 10.0f, dash1, 0.0f);
            private static final BasicStroke dashedSelected = new BasicStroke(strokeWidthSelected, BasicStroke.CAP_BUTT,
                    BasicStroke.JOIN_MITER, 10.0f, dash1, 0.0f);
            private static final BasicStroke normalSelected = new BasicStroke(strokeWidthSelected);
            private static final BasicStroke normalUnselected = new BasicStroke(strokeWidthDefault);
            */
/*
                double[] dashedStroke, normalStroke;
                boolean isSelected = d.isSelected() && hasFocus;
                if (isSelected) {
                    dashedStroke = dashedSelected;
                    normalStroke = normalSelected;
                }
                else {
                    dashedStroke = dashedUnselected;
                    normalStroke = normalUnselected;
                }*/

            g.setLineDashes(DASHES);
            // These should all be rounded to the nearest integer+0.5 value:
            double src_x = d.getSourceX();
            double src_y = d.getSourceY();
            double dst_x = d.getDestX();
            double dst_y = d.getDestY();

            g.setStroke(Color.BLACK);
            // Draw the end arrow
            int delta_x = d.isEndLeft() ? -10 : 10;

            g.strokeLine(dst_x, dst_y, dst_x + delta_x, dst_y + 4);
            g.strokeLine(dst_x, dst_y, dst_x + delta_x, dst_y - 4);
            g.setLineDashes(DASHES);

            // Draw the start
            double corner_y = src_y + (d.isStartTop() ? -15 : 15);
            g.strokeLine(src_x, corner_y, src_x, src_y);
            src_y = corner_y;

            // Draw the last line segment
            double corner_x = dst_x + (d.isEndLeft() ? -15 : 15);
            g.strokeLine(corner_x, dst_y, dst_x, dst_y);
            dst_x = corner_x;

            // if arrow vertical corner, draw first segment up to corner
            if ((src_y != dst_y) && (d.isStartTop() == (src_y < dst_y))) {
                corner_x = Utility.roundHalf(((src_x + dst_x) / 2) + (d.isEndLeft() ? 15 : -15));
                corner_x = (d.isEndLeft() ? Math.min(dst_x, corner_x) : Math.max(dst_x, corner_x));
                g.strokeLine(src_x, src_y, corner_x, src_y);
                src_x = corner_x;
            }

            // if arrow horiz. corner, draw first segment up to corner
            if ((src_x != dst_x) && (d.isEndLeft() == (src_x > dst_x))) {
                corner_y = Utility.roundHalf(((src_y + dst_y) / 2) + (d.isStartTop() ? 15 : -15));
                corner_y = (d.isStartTop() ? Math.min(src_y, corner_y) : Math.max(src_y, corner_y));
                g.strokeLine(dst_x, corner_y, dst_x, dst_y);
                dst_y = corner_y;
            }

            // draw the middle bit
            g.strokeLine(src_x, src_y, src_x, dst_y);
            g.strokeLine(src_x, dst_y, dst_x, dst_y);
        }
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

    public void toggleSelection(Target element)
    {
        if (!element.isSelected())
            selectionController.addToSelection(element);
        else
            selectionController.removeFromSelection(element);
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
        addEventHandler(MouseEvent.MOUSE_PRESSED, selectionController::mousePressed);
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
        List<? extends Dependency> dependencies = new ArrayList<Dependency>();

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
                    usesArrows.add((UsesDependency)d);
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


    /**
     * Modify the given point to be one of the deined grid points.
     *
     * @param point  The original point
     * @return      A point close to the original which is on the grid.
     */
    public int snapToGrid(int x)
    {
        int steps = x / PackageEditor.GRID_SIZE;
        int new_x = steps * PackageEditor.GRID_SIZE;//new x-coor w/ respect to
        // grid
        return new_x;
    }

    /**
     * Started a move operation on the selection
     */
    public void startedMove()
    {
        for (Target element : selectionController.getSelection())
        {
            if (element.isMoveable())
                element.savePreMovePosition();
        }
    }

    /**
     * Performing a move operation: move by the given amounts
     * from the last time we called startedMove
     */
    public void moveBy(int deltaX, int deltaY)
    {
        for (Target element : selectionController.getSelection())
        {
            if (element.isMoveable())
                element.setPos(Math.max(0, element.getPreMoveX() + deltaX), Math.max(0, element.getPreMoveY() + deltaY));
        }
    }


    /**
     * Started a move operation on the selection
     */
    public void startedResize()
    {
        for (Target element : selectionController.getSelection())
        {
            if (element.isResizable())
                element.savePreResize();
        }
    }

    /**
     * Performing a resize operation: resize by the given amounts
     * from the last time we called startedResize
     */
    public void resizeBy(int deltaWidth, int deltaHeight)
    {
        for (Target element : selectionController.getSelection())
        {
            if (element.isResizable())
                element.setSize(Math.max(40, element.getPreResizeWidth() + deltaWidth), Math.max(20, element.getPreResizeHeight() + deltaHeight));
        }
    }
}
