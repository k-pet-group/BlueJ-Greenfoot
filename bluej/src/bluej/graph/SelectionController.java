/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2013,2014,2016  Michael Kolling and John Rosenberg 
 
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
package bluej.graph;

import java.awt.Rectangle;
import java.util.Iterator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

import bluej.Config;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PackageEditor;
import bluej.pkgmgr.target.ClassTarget;
import bluej.pkgmgr.target.Target;
import bluej.utility.Utility;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * This class controls the selection (the set of selected elements in the graph).
 * To do this, it maintains a selection set, a marquee (a graphical selection rectangle)
 * and a rubber band (for drawing new edges). Both the marquee and the rubber band can
 * be inactive.
 */
@OnThread(Tag.FXPlatform)
public class SelectionController
{
    private final PackageEditor graphEditor;
    private final Package graph;
    
    private final Marquee marquee;
    private final SelectionSet selection;   // Contains the elements that have been selected

    private boolean moving = false; 
    private boolean resizing = false; 

    private int dragStartX;
    private int dragStartY;

    private int keyDeltaX;
    private int keyDeltaY;

    private TraverseStrategy traverseStragegiImpl = new TraverseStrategyImpl();

    
    /**
     * Create the controller for a given graph editor.
     * @param graphEditor
     * @param graph
     */
    @OnThread(Tag.Any)
    public SelectionController(PackageEditor graphEditor)
    {
        this.graphEditor = graphEditor;
        this.graph = graphEditor.getPackage();
        selection = new SelectionSet(Utility.filterList(graph.getVertices(), Target::isSelected));
        marquee = new Marquee(graph, selection);

    }

    /**
     * A mouse-pressed event. Analyse what we should do with it.
     */
    public void mousePressed(javafx.scene.input.MouseEvent evt, Target clickedElement)
    {
        graphEditor.requestFocus();
        int clickX = (int)evt.getX();
        int clickY = (int)evt.getY();

        notifyPackage(clickedElement);
        
        if (clickedElement == null) {                           // nothing hit
            if (!isMultiselectionKeyDown(evt)) {
                selection.clear();
            }
            if (isButtonOne(evt))
                marquee.start(clickX, clickY);
        }
        else if (isButtonOne(evt)) {                            // clicked on something
            if (isMultiselectionKeyDown(evt)) {
                // a class was clicked, while multiselectionKey was down.
                if (clickedElement.isSelected()) {
                    selection.remove(clickedElement);
                }
                else {
                    selection.add(clickedElement);
                }
            }
            else {
                // a class was clicked without multiselection
                if (! clickedElement.isSelected()) {
                    selection.selectOnly(clickedElement);
                }
            }

            if(isDrawingDependency()) {
                //if (clickedElement instanceof Target)
                    //rubberBand = new RubberBand(clickX, clickY, clickX, clickY);
            }
            else {
                dragStartX = clickX;
                dragStartY = clickY;

                if(clickedElement.isHandle(clickX, clickY)) {
                    resizing = true;
                }
                else {
                    moving = true;                        
                }
            }
        }
    }

    /**
     * The mouse was released.
     */
    public void mouseReleased(MouseEvent evt)
    {
        if (isDrawingDependency()) {
            //Target selectedElement = graph.findGraphElement(evt.getX(), evt.getY());
            //notifyPackage(selectedElement);
            graphEditor.repaint();
        }

        marquee.stop();     // may or may not have had a marquee...
        graphEditor.repaint();
        
        if(moving || resizing) {
            endMove();
            graphEditor.repaint();
        }
    }
    
    /**
     * A mouse-clicked event. This is only interesting if it was a double
     * click. If so, inform every element in the current selection.
     */
    public void mouseClicked(MouseEvent evt)
    {
        if (isButtonOne(evt)) {
            if (evt.getClickCount() > 1) {
                selection.doubleClick();
            }
        }
    }

    /**
     * The mouse was dragged - either draw a marquee or move some classes.
     */
    public void mouseDragged(MouseEvent evt)
    {
        if (isButtonOne(evt)) {
            if (marquee.isActive()) {
                marquee.move((int)evt.getX(), (int)evt.getY());
                graphEditor.repaint();
            }
            else
            {
                if(! selection.isEmpty()) {
                    int deltaX = snapToGrid((int)evt.getX() - dragStartX);
                    int deltaY = snapToGrid((int)evt.getY() - dragStartY);
    
                    if(resizing) {
                        selection.resize(deltaX, deltaY);
                    }
                    else if (moving) {
                        selection.move(deltaX, deltaY);
                    }
                }
                graphEditor.repaint();
            }
        }
    }

    // ======= end of MouseMotionListener interface =======

    // ======= KeyListener interface =======

    /**
     * A key was pressed in the graph editor.
     */
    public void keyPressed(KeyEvent evt)
    {
        boolean handled = true; // assume for a start that we are handling the
                                // key here

        if (isArrowKey(evt)) {
            if (evt.isControlDown()) {      // resizing
                if(!resizing)
                    startKeyboardResize();
                setKeyDelta(evt);
                selection.resize(keyDeltaX, keyDeltaY);
            }
            else if (evt.isShiftDown()) {   // moving targets
                if(!moving)
                    startKeyboardMove();
                setKeyDelta(evt);
                selection.move(keyDeltaX, keyDeltaY);
            }
            else {                          // navigate the diagram
                navigate(evt);
            }
        }

        else if (isPlusOrMinusKey(evt)) {
            resizeWithFixedRatio(evt);
        }

        // post context menu
        else if (evt.getCode() == KeyCode.SPACE || evt.getCode() == KeyCode.ENTER || evt.getCode() == KeyCode.CONTEXT_MENU) {
            postMenu();
        }

        // 'A' (with any or no modifiers) selects all
        else if (evt.getCode() == KeyCode.A) {
            selectAll();
        }

        // Escape removes selections
        else if (evt.getCode() == KeyCode.ESCAPE) {
            if(moving || resizing) {
                endMove();
            }
            clearSelection();
        }

        else {
            handled = false;
        }

        if (handled)
            evt.consume();

        graphEditor.repaint();
    }

    
    /**
     * A key was released. Check whether a key-based move or resize operation
     * has ended.
     */
    public void keyReleased(KeyEvent evt)
    {
        if(moving && (!evt.isShiftDown())) {    // key-based moving stopped
            selection.moveStopped();
            moving = false;
        }
        else if(resizing && (!evt.isControlDown())) {    // key-based moving stopped
            selection.moveStopped();
            resizing = false;
        }
        graphEditor.repaint();
    }

    // ======= end of KeyListener interface =======


    private void notifyPackage(Target element)
    {
        if (element != null && element instanceof ClassTarget)
            ((Package)graph).targetSelected((Target)element);
        else
            ((Package)graph).targetSelected(null);
    }
    
    /**
     * Tell whether the package is currently drawing a dependency.
     */
    @OnThread(Tag.FXPlatform)
    public boolean isDrawingDependency()
    {
        return graphEditor.isDrawingDependency();
    }

    
    private static boolean isArrowKey(KeyEvent evt)
    {
        return evt.getCode() == KeyCode.UP || evt.getCode() == KeyCode.DOWN
                || evt.getCode() == KeyCode.LEFT || evt.getCode() == KeyCode.RIGHT;
    }

    /**
     * Move the current selection to another selected class, depending on
     * current selection and the key pressed.
     */
    private void navigate(KeyEvent evt)
    {
        Target currentTarget = findSingleVertex();
        currentTarget = traverseStragegiImpl.findNextVertex(graph, currentTarget, evt.getCode());
        selection.selectOnly(currentTarget);
    }

    /**
     * Prepare a key-based move operation.
     */
    private void startKeyboardMove()
    {
        keyDeltaX = 0;
        keyDeltaY = 0;
        moving = true;
    }
    
    /**
     * End a move or resize gesture.
     *
     */
    private void endMove() 
    {
        selection.moveStopped();
        moving = false;
        resizing = false;
    }
    
    /**
     * Prepare a key-based resize operation.
     */
    private void startKeyboardResize()
    {
        keyDeltaX = 0;
        keyDeltaY = 0;
        resizing = true;
    }
     
    /**
     * Move all targets according to the supplied key.
     */
    private void setKeyDelta(KeyEvent evt)
    {
        switch(evt.getCode()) {
            case UP: {
                keyDeltaY -= PackageEditor.GRID_SIZE;
                break;
            }
            case DOWN: {
                keyDeltaY += PackageEditor.GRID_SIZE;
                break;
            }
            case LEFT: {
                keyDeltaX -= PackageEditor.GRID_SIZE;
                break;
            }
            case RIGHT: {
                keyDeltaX += PackageEditor.GRID_SIZE;
                break;
            }
        }
    }

    /**
     * Is the pressed key a plus or minus key?
     */
    private boolean isPlusOrMinusKey(KeyEvent evt)
    {
        return "+-".contains(evt.getCharacter());
    }

    private void resizeWithFixedRatio(KeyEvent evt)
    {
        int delta = (evt.getCharacter().equals("+") ? PackageEditor.GRID_SIZE : -PackageEditor.GRID_SIZE);
        selection.resize(delta, delta);
        selection.moveStopped();
    }
    
    /**
     * Post the context menu of one selected element of the current selection.
     * If any dependencies are selected, show the menu for one of those. Otherwise
     * show the menu for a randomly chosen target.
     */
    private void postMenu()
    {
        // if not, choose a target
        Target vertex = selection.getAnyVertex();
        if(vertex != null) {
            selection.selectOnly(vertex);
            int x = vertex.getX() + vertex.getWidth() - 20;
            int y = vertex.getY() + 20;
            postMenu(vertex, x, y);
        }
    }

    
    /**
     * Post the context menu for a given element at the given screen position.
     */
    private void postMenu(Target element, int x, int y)
    {
        element.popupMenu(x, y, graphEditor);
    }


    /**
     * Return the marquee of this conroller.
     */
    public Marquee getMarquee()
    {
        return marquee;
    }

    
    private Target findSingleVertex()
    {
        Target vertex = selection.getAnyVertex();

        // if there is no selection we select an existing vertex
        if (vertex == null) {
            vertex = graph.getVertices().get(0);
        }
        return vertex;
    }


    /**
     * Clear the current selection.
     */
    public void clearSelection()
    {
        selection.clear();
    }

    /** 
     * Select all graph vertices.
     */
    private void selectAll()
    {
        for (Target t : graph.getVertices())
            selection.add(t);
    }
    
    /**
     * Clear the current selection.
     */
    public void removeFromSelection(Target element)
    {
        selection.remove(element);
    }

    /**
     * Add to the current selection
     * @param element
     */
    public void addToSelection(Target element)
    {
        selection.add(element);
    }
   
    /**
     * Check whether this mouse event was from button one.
     * (Ctrl-button one on MacOS does not count - that posts the menu
     * so we consider that button two.)
     */
    private boolean isButtonOne(MouseEvent evt)
    {
        return !evt.isPopupTrigger() && evt.getButton() == MouseButton.PRIMARY;
    }

    /**
     * Check whether the key used for multiple selections is down.
     */
    private boolean isMultiselectionKeyDown(MouseEvent evt)
    {
        if (Config.isMacOS()) {
            return evt.isShiftDown() || evt.isMetaDown();
        }
        else {
            return evt.isShiftDown() || evt.isControlDown();
        }

    }

    /**
     * Modify the given point to be one of the deined grid points.
     * 
     * @param point  The original point
     * @return      A point close to the original which is on the grid.
     */
    private int snapToGrid(int x)
    {
        int steps = x / PackageEditor.GRID_SIZE;
        int new_x = steps * PackageEditor.GRID_SIZE;//new x-coor w/ respect to
                                                  // grid
        return new_x;
    }

    public void selectOnly(Target target)
    {
        selection.selectOnly(target);
    }
}
