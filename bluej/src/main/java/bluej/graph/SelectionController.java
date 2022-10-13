/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2013,2014,2016,2017,2018,2020  Michael Kolling and John Rosenberg
 
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

import bluej.Config;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PackageEditor;
import bluej.pkgmgr.target.Target;
import bluej.utility.Utility;
import bluej.utility.javafx.FXPlatformConsumer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

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

    private TraverseStrategy traverseStragegiImpl = new TraverseStrategyImpl();
    private ArrayList<Target> mostRecentSelection = new ArrayList<>();


    /**
     * Create the controller for a given graph editor.
     * @param graphEditor
     * @param graph
     */
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
    public void mousePressed(javafx.scene.input.MouseEvent evt)
    {
        int clickX = (int)evt.getX();
        int clickY = (int)evt.getY();

        if (!isMultiselectionKeyDown(evt)) {
            selection.clear();
        }
        if (isButtonOne(evt)) {
            marquee.start(clickX, clickY);
        }
    }

    /**
     * The mouse was released.
     */
    public void mouseReleased(MouseEvent evt)
    {
        marquee.stop();     // may or may not have had a marquee...
        graphEditor.repaint();
        
        if (moving || resizing) {
            endMove();
            graphEditor.repaint();
        }

        // We deselected the focused class while mouse was dragging,
        // now we need to reselect it if there is a focused class:
        for (Target t : graph.getVertices())
        {
            // If it is focused and selected, it must have been in the marquee
            // so don't ruin the multi-select.
            // We only need to handle the case where it was focused (i.e. was
            // selected pre-marquee) and is not now selected (i.e. was not
            // in the final marquee):
            if (t.isFocused() && !t.isSelected())
            {
                selectOnly(t);
                break;
            }
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
                selection.getSelected().forEach(target -> {
                    if (evt.getTarget().equals(target)) {
                        selection.doubleClick(false);
                        return;
                    }
                });
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
                    /*
                    int deltaX = snapToGrid((int)evt.getX() - dragStartX);
                    int deltaY = snapToGrid((int)evt.getY() - dragStartY);
    
                    if(resizing) {
                        selection.resize(deltaX, deltaY);
                    }
                    else if (moving) {
                        selection.move(deltaX, deltaY);
                    }*/
                }
                graphEditor.repaint();
            }
        }
    }

    // ======= end of MouseMotionListener interface =======

    /**
     * Move the current selection to another selected class, depending on
     * current selection and the key pressed.
     */
    public void navigate(KeyEvent evt)
    {
        Target currentTarget = findSingleVertex();
        currentTarget = traverseStragegiImpl.findNextVertex(graph, currentTarget, evt.getCode());
        selection.selectOnly(currentTarget);
        currentTarget.requestFocus();
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
     * Clear the current selection.  Also remembers the selection being cleared, for a future call
     * to restoreRecentSelectionAndFocus()
     */
    public void clearSelection()
    {
        mostRecentSelection = new ArrayList<>(getSelection()); 
        selection.clear();
    }

    /** 
     * Select all graph vertices.
     */
    public void selectAll()
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
     * Selects the one given target, and no others.
     */
    public void selectOnly(Target target)
    {
        selection.selectOnly(target);
    }

    /**
     * Gets an unordered list of all currently selected targets.
     */
    public List<Target> getSelection()
    {
        return selection.getSelected();
    }

    /**
     * The selection listener will be run every time the selection has changed.
     * @param selectionListener
     */
    public void addSelectionListener(FXPlatformConsumer<Collection<Target>> selectionListener)
    {
        selection.addListener(selectionListener);
    }

    /**
     * Restores the most recently focused items before the last call to clearSelection()
     * @param stillAValidTarget A function to check if a target is still valid (i.e. hasn't been removed)
     * @return true if at least one item in the selection was found, selected, and focused
     */
    public boolean restoreRecentSelectionAndFocus(Predicate<Target> stillAValidTarget)
    {
        // DON'T CALL clearSelection() AS IT WILL OVERWRITE mostRecentSelection!
        selection.clear();
        boolean haveFocused = false;
        for (Target target : mostRecentSelection)
        {
            if (stillAValidTarget.test(target))
            {
                selection.add(target);
                if (!haveFocused)
                {
                    target.getNode().requestFocus();
                    haveFocused = true;
                }
            }
        }
        return !selection.isEmpty();
    }
}
