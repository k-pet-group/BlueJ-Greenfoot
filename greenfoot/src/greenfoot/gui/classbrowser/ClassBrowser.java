/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010,2011,2013  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.gui.classbrowser;

import bluej.Config;
import greenfoot.core.GProject;
import greenfoot.gui.GreenfootFrame;
import greenfoot.gui.classbrowser.ClassForest.TreeEntry;
import greenfoot.gui.classbrowser.role.ActorClassRole;
import greenfoot.gui.classbrowser.role.WorldClassRole;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.border.TitledBorder;

/**
 * 
 * This is the component which has all the classes (from the project + the
 * system classes) that is visible in UI. It is responsible for drawing and
 * laying out the classes.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 */
public class ClassBrowser extends JPanel
{
    private ButtonGroup buttonGroup = new ButtonGroup();

    private SelectionManager selectionManager = new SelectionManager();

    private ClassForest worldClasses = new ClassForest();
    private ClassForest greenfootClasses = new ClassForest();
    private ClassForest otherClasses = new ClassForest();
    
    private GProject project;

    private GreenfootFrame frame;

    /**
     * Construct a new ClassBrowser
     */
    public ClassBrowser(GProject project, GreenfootFrame frame)
    {
        this.project = project;
        this.frame = frame;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        setBackground(Color.WHITE);

        worldClasses = new ClassForest();
        greenfootClasses = new ClassForest();
        otherClasses = new ClassForest();
    }

    /**
     * Add a new class to the class browser data structure, without updating
     * the view on screen. The view can be explicitly updated later, using 
     * updateLayout().
     */
    public void quickAddClass(ClassView classView)
    {
        if (classView.getRole() instanceof ActorClassRole) {
            greenfootClasses.add(classView);
        }
        else if (classView.getRole() instanceof WorldClassRole) {           
            worldClasses.add(classView);
        }
        else {
            // everything else
            otherClasses.add(classView);
        }
        buttonGroup.add(classView);
        classView.addSelectionChangeListener(selectionManager);        
    }
    
    /**
     * Add a new class to the class browser.
     * Call only from the Swing event thread.
     */
    public void addClass(ClassView classView)
    {
        quickAddClass(classView);
        classView.select();
        updateLayout();
    }
    
    /**
     * Remove a class from the browser and update the view on screen.
     * 
     * <br>
     * Must be called from the event thread.
     */
    public void removeClass(ClassView classView) 
    {
        // We have to remove the class view from the button group first;
        // otherwise it's deselecting the button doesn't work.
        buttonGroup.remove(classView);
        classView.deselect();

        TreeEntry treeEntry = removeFromForest(classView);
        List<TreeEntry> children = treeEntry.getChildren();
        for (TreeEntry child : children) {
            otherClasses.add(child);
            child.getData().setSuperclass(null);
        }
        
        classView.removeSelectionChangeListener(selectionManager);
        updateLayout();
    }

    private TreeEntry removeFromForest(ClassView classView)
    {
        TreeEntry removedEntry = greenfootClasses.remove(classView);
        if(removedEntry == null) {
            removedEntry = worldClasses.remove(classView);
        }
        if(removedEntry == null) {
            removedEntry = otherClasses.remove(classView);
        }
        return removedEntry;
    }
    

    /**
     * Notify the class browser that a class has changed name
     * @param classView  The classView of the class which name has named
     * @param oldName    The original name of the class
     */
    public void renameClass(ClassView classView, String oldName)
    {
        greenfootClasses.rename(classView, oldName);
        worldClasses.rename(classView, oldName);
        otherClasses.rename(classView, oldName);
    }
    
    /**
     * Arrange and show the class views on screen.
     * Call only from the Swing event thread.
     */
    public void updateLayout()
    {
        greenfootClasses.rebuild();
        worldClasses.rebuild();
        otherClasses.rebuild();
        
        this.removeAll();  // remove current components

        // world Classes

        JComponent worldClassPanel = createClassHierarchyComponent(worldClasses.getRoots(), false);

        JPanel worldFrame = new JPanel();
        ((FlowLayout)worldFrame.getLayout()).setAlignment(FlowLayout.LEFT);
        worldFrame.setBackground(Color.WHITE);
        worldFrame.add(worldClassPanel);
        TitledBorder border = BorderFactory.createTitledBorder(null, Config.getString("browser.border.world"));
        border.setTitleColor(Color.GRAY);
        worldFrame.setBorder(border);

        this.add(worldFrame, BorderLayout.NORTH);

        // simulation classes

        JComponent greenfootClassPanel = createClassHierarchyComponent(greenfootClasses.getRoots(), false);

        if (greenfootClassPanel != null) {
            JPanel objectFrame = new JPanel();
            ((FlowLayout)objectFrame.getLayout()).setAlignment(FlowLayout.LEFT);
            objectFrame.setBackground(Color.WHITE);
            objectFrame.add(greenfootClassPanel);
            border = BorderFactory.createTitledBorder(null, Config.getString("browser.border.actors"));
            border.setTitleColor(Color.GRAY);
            objectFrame.setBorder(border);

            this.add(objectFrame, BorderLayout.CENTER);
        }
        
        if (! otherClasses.getRoots().isEmpty()) {
            JComponent otherClassPanel = createClassHierarchyComponent(otherClasses.getRoots(), false);
            
            if (otherClassPanel != null) {
                JPanel objectFrame = new JPanel();
                ((FlowLayout)objectFrame.getLayout()).setAlignment(FlowLayout.LEFT);
                objectFrame.setBackground(Color.WHITE);
                objectFrame.add(otherClassPanel);
                border = BorderFactory.createTitledBorder(null, Config.getString("browser.border.others"));
                border.setTitleColor(Color.GRAY);
                objectFrame.setBorder(border);
                
                this.add(objectFrame, BorderLayout.SOUTH);
            }
        }
        
        // Don't know why I have to revalidate on the RootPane instead of just
        // the class browser. But that is the only way to make it resize the
        // class browser if a longer class name has been added.
        // Poul 30/10/2006
        JRootPane rootPane = getRootPane();
        if (rootPane != null) {
            getRootPane().revalidate();
        }
    }


    /**
     * Creates a component with the class hierarchy. This method calls itself
     * recursively.
     * 
     * @param roots
     *            All the superclasses (classe hat are NOT subclasses)
     * @param header
     *            A header for specifying the types of classes in this
     *            hierarchy.
     * @return
     */
    private JComponent createClassHierarchyComponent(Collection<TreeEntry> roots,
            boolean isRecursiveCall)
    {
        JComponent hierarchyPanel = new JPanel();
        hierarchyPanel.setOpaque(false);
        hierarchyPanel.setLayout(new BoxLayout(hierarchyPanel, BoxLayout.Y_AXIS));
        
        boolean isFirstSubclass = true; //whether it is the first subclass

        for (Iterator<TreeEntry> iter = roots.iterator(); iter.hasNext();) {
            JComponent classPanel = new JPanel();
            classPanel.setOpaque(false);
            classPanel.setLayout(new BoxLayout(classPanel, BoxLayout.X_AXIS));

            ClassForest.TreeEntry element = iter.next();

            if (!isRecursiveCall) {
                // not recursive: we are at the greenfoot root - no arrows up
            }
            else if (isFirstSubclass && !iter.hasNext()) {
                // first and last subclass: arrow head, no line to bottom
                classPanel.add(new ArrowHeadEnd());
            }
            else if (isFirstSubclass) {
                // first, but not last, subclass: arrow head with line down
                classPanel.add(new ArrowHead());
            }
            else if (!iter.hasNext()) {
                // last subclass: line end
                classPanel.add(new ArrowConnectEnd());
            }
            else {
                // subclass in middle: line to top and bottom
                classPanel.add(new ArrowConnect());
            }

            JComponent classView = element.getData();
            classPanel.add(classView);
            JComponent flowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            flowPanel.add(classPanel);
            flowPanel.setOpaque(false);
            hierarchyPanel.add(flowPanel);
            
            List<TreeEntry> children = element.getChildren();
            if(!children.isEmpty()) {
                JComponent childPanel = new JPanel();
                childPanel.setOpaque(false);
                childPanel.setLayout(new BoxLayout(childPanel, BoxLayout.X_AXIS));

                JComponent child = createClassHierarchyComponent(children, true);
                if (iter.hasNext()) {
                    childPanel.add(new ArrowLine());
                }
                else if (isRecursiveCall) {
                    childPanel.add(new EmptySpace());
                }
                childPanel.add(child);
                flowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
                flowPanel.setOpaque(false);
                flowPanel.add(childPanel);
                hierarchyPanel.add(flowPanel);
            }
            
            isFirstSubclass = false;
        }

        return hierarchyPanel;
    }

    /**
     * Get the selection manager for this class browser.
     */
    public SelectionManager getSelectionManager()
    {
        return selectionManager;
    }

    /**
     * Updates the layout of the ClassBrowser to make sure the given classView
     * is displayed with the correct superclass and in the right section of the
     * classbrowser.
     * <p>
     * Call updateLayout to make the changes visible.
     * 
     * @see #updateLayout()
     * @param classView
     */
    public void consolidateLayout(ClassView classView)
    {
        // Only change the role if we have been initialised
        if (greenfootClasses != null) {
            // Remove it from the forest since it is in the wrong forest.

            TreeEntry removed = removeFromForest(classView);

            // Add it to the right forest.
            if (classView.getRole() instanceof ActorClassRole) {
                greenfootClasses.add(removed);
            }
            else if (classView.getRole() instanceof WorldClassRole) {
                worldClasses.add(removed);
            }
            else {
                // everything else
                otherClasses.add(removed);
            }
        }      
        updateLayout();
    }
    
    public GProject getProject()
    {
        return project;
    }

    /** 
     * Get the parent frame of this class browser.
     */
    public GreenfootFrame getFrame()
    {
        return frame;
    }
}
