/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2017,2018  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.guifx.classes;

import bluej.Config;
import bluej.pkgmgr.Project;
import bluej.pkgmgr.target.ClassTarget;
import bluej.pkgmgr.target.Target;
import bluej.utility.javafx.FXRunnable;
import bluej.utility.javafx.JavaFXUtil;
import greenfoot.guifx.GreenfootStage;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.BorderPane;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

/**
 * The class diagram on the right-hand side of the Greenfoot window.
 *
 * For now, this is very primitive, but is useful for implementing other Greenfoot functionality.
 */
public class GClassDiagram extends BorderPane
{
    public static enum GClassType { ACTOR, WORLD, OTHER }
    
    private final ClassDisplaySelectionManager selectionManager = new ClassDisplaySelectionManager();
    // The three groups of classes in the display: World+subclasses, Actor+subclasses, Other
    private final ClassGroup worldClasses;
    private final ClassGroup actorClasses;
    private final ClassGroup otherClasses;
    private final GreenfootStage greenfootStage;
    private Project project;

    /**
     * Construct a GClassDiagram for the given stage.
     */
    public GClassDiagram(GreenfootStage greenfootStage)
    {
        this.greenfootStage = greenfootStage;
        getStyleClass().add("class-diagram");
        this.worldClasses = new ClassGroup(greenfootStage);
        this.actorClasses = new ClassGroup(greenfootStage);
        this.otherClasses = new ClassGroup(greenfootStage);
        setTop(worldClasses);
        setCenter(actorClasses);
        setBottom(otherClasses);
        // Actor classes will expand to fill middle, but content will be positioned at the top of that area:
        BorderPane.setAlignment(actorClasses, Pos.TOP_LEFT);
        BorderPane.setAlignment(otherClasses, Pos.BOTTOM_LEFT);
        // Setting spacing around actorClasses is equivalent to divider spacing:
        BorderPane.setMargin(actorClasses, new Insets(20, 0, 20, 0));
        setMaxWidth(Double.MAX_VALUE);
        setMaxHeight(Double.MAX_VALUE);
        
        setOnContextMenuRequested(e -> {
            e.consume();
            // If they right-click on us, we show new-class and import-class actions:
            ContextMenu contextMenu = new ContextMenu();
            contextMenu.getItems().add(JavaFXUtil.makeMenuItem(
                    Config.getString("new.other.class"),
                    () -> greenfootStage.newNonImageClass(project.getUnnamedPackage(), null), null));
            contextMenu.getItems().add(JavaFXUtil.makeMenuItem(
                    Config.getString("import.action"),
                    () -> greenfootStage.doImportClass(), null));
            contextMenu.show(this, e.getScreenX(), e.getScreenY());
        });
    }
    
    /**
     * Set the project for this class diagram.
     * 
     * @param project  the project whose classes to display (may be null)
     */
    public void setProject(Project project)
    {
        this.project = project;
        if (project != null)
        {
            recalculateGroups();
            setDisable(false);
        }
        else
        {
            worldClasses.setClasses(Collections.emptyList());
            actorClasses.setClasses(Collections.emptyList());
            otherClasses.setClasses(Collections.emptyList());
            setDisable(true);
        }
    }

    /**
     * Looks up list of ClassTargets in the project, and puts them into a tree structure
     * according to their superclass relations, with Actor and World subclasses
     * going into their own group.
     */
    void recalculateGroups()
    {
        ArrayList<ClassTarget> originalClassTargets = project.getUnnamedPackage().getClassTargets();
        
        // Start by mapping everything to false;
        HashMap<ClassTarget, Boolean> classTargets = new HashMap<>();
        for (ClassTarget originalClassTarget : originalClassTargets)
        {
            classTargets.put(originalClassTarget, false);
        }
        // Note that the classTargets list will be modified by each findAllSubclasses call,
        // so the order here is very important.  Actor and World must come before other:
        
        // First, we must take out any World and Actor classes:
        List<GClassNode> worldSubclasses = findAllSubclasses("greenfoot.World", classTargets, GClassType.WORLD);
        GClassNode worldClassesInfo = new GClassNode(GClassType.WORLD, worldSubclasses, selectionManager);
        worldClasses.setClasses(Collections.singletonList(worldClassesInfo));

        List<GClassNode> actorSubclasses = findAllSubclasses("greenfoot.Actor", classTargets, GClassType.ACTOR);
        GClassNode actorClassesInfo = new GClassNode(GClassType.ACTOR, actorSubclasses, selectionManager);
        actorClasses.setClasses(Collections.singletonList(actorClassesInfo));
        
        // All other classes can be found by passing null, see docs on findAllSubclasses:
        otherClasses.setClasses(findAllSubclasses(null, classTargets, GClassType.OTHER));
    }

    /**
     * Finds all subclasses of the given fully-qualified parent class name.  The subclass search
     * is recursive, so if you pass "Grandparent", then both "Parent" and "Child" will be found 
     * and removed.  Any found subclasses will have their boolean changed to true in the given map,
     * and only those that currently map to false will be searched.
     * 
     * @param parentClassName The fully-qualified parent class name to search.  If null, then all classes
     *                        in the classTargets list will be processed and returned.
     * @param classTargets Class targets to search -- only those mapped to false will be searched.  If
     *                     they are processed into a GClassNode, their value will be flipped to true.
     * @return The list of GClassNode at the requested level (there may be a deeper tree inside).
     */
    private List<GClassNode> findAllSubclasses(String parentClassName, Map<ClassTarget, Boolean> classTargets, GClassType type)
    {
        List<GClassNode> curLevel = new ArrayList<>();
        for (Entry<ClassTarget, Boolean> classTargetAndVal : classTargets.entrySet())
        {
            // Ignore anything already mapped to true:
            if (classTargetAndVal.getValue() == true)
                continue;
            
            ClassTarget classTarget = classTargetAndVal.getKey();
            bluej.parser.symtab.ClassInfo classInfo = classTarget.analyseSource();
            String superClass = classInfo == null ? null : classInfo.getSuperclass();
            boolean includeAtThisLevel;
            if (parentClassName == null)
            {
                // We want all classes, but we still want to pick out subclass relations.  Some classes
                // may have a parent class (e.g. java.util.List) that is not in the list of class targets, but
                // the class should still be included at the top-level.  The key test for top-level is:
                //   Is the parent class either null, or not present in the list?

                includeAtThisLevel = superClass == null || !classTargets.keySet().stream().anyMatch(ct -> Objects.equals(ct.getQualifiedName(), superClass));
            }
            else
            {
                // Does it directly inherit from the requested class?
                includeAtThisLevel = Objects.equals(superClass, parentClassName);
            }

            if (includeAtThisLevel)
            {
                // Update processed status before recursing:
                classTargetAndVal.setValue(true);

                List<GClassNode> subClasses = findAllSubclasses(classTarget.getQualifiedName(), classTargets, type);
                curLevel.add(makeClassInfo(classTarget, subClasses, type));
            }
        }
        return curLevel;
    }

    /**
     * Adds a new class to the diagram at the appropriate place, based on its superclass,
     * and returns the constructed class info.
     *
     * @return A class info reference for the class added.
     */
    public GClassNode addClass(ClassTarget classTarget)
    {
        String superClass = null;
        bluej.parser.symtab.ClassInfo info = classTarget.analyseSource();
        if (info != null)
        {
            superClass = info.getSuperclass();
        }
        // The class could be nested within actor or world or other
        // If none of those apply, it will go at top-level of other
        if (superClass != null)
        {
            // It does have a parent class: may be in World, Actor or Other:
            //for (ClassGroup classGroup : Arrays.asList(worldClasses, actorClasses, otherClasses))
            for (GClassType type : GClassType.values())
            {
                ClassGroup classGroup;
                switch (type)
                {
                    case ACTOR:
                        classGroup = actorClasses;
                        break;
                    case WORLD:
                        classGroup = worldClasses;
                        break;
                    case OTHER:
                        classGroup =  otherClasses;
                        break;
                    default:
                        continue; // Should be impossible
                }
                // Look all the way down for the tree for the super class:
                GClassNode classInfo = findAndAdd(classGroup.getLiveClasses(), classTarget, superClass, type);
                if (classInfo != null)
                {
                    classGroup.updateAfterAdd();
                    // Found right place nested within the current group; done:
                    return classInfo;
                }
            }
            // If we fall through here, we do have a parent class, but it's not in the diagram
            // e.g. inheriting from java.util.List
        }
        // Otherwise, add to top of Other:
        GClassNode classInfo = makeClassInfo(classTarget, Collections.emptyList(), GClassType.OTHER);
        otherClasses.getLiveClasses().add(classInfo);
        otherClasses.updateAfterAdd();
        return classInfo;
    }

    /**
     * Looks within the whole tree formed by the list of class info for the right place for classTarget.
     * If found, create a class info for the class target passed, adds it and return it.
     *
     * @param classInfos The tree to search.  The list itself will not be modified.
     * @param classTarget The class to add to the tree.
     * @param classTargetSuperClass The super-class of classTarget
     * @param type The source type of the class added.
     * @return The class info created if right place found and added, null if not.
     */
    private GClassNode findAndAdd(List<GClassNode> classInfos, ClassTarget classTarget, String classTargetSuperClass, GClassType type)
    {
        for (GClassNode classInfo : classInfos)
        {
            if (classInfo.getQualifiedName().equals(classTargetSuperClass))
            {
                GClassNode newClassInfo = makeClassInfo(classTarget, Collections.emptyList(), type);
                classInfo.add(newClassInfo);
                return newClassInfo;
            }
            else
            {
                GClassNode newClassInfo = findAndAdd(classInfo.getSubClasses(), classTarget, classTargetSuperClass, type);
                if (newClassInfo != null)
                {
                    return newClassInfo;
                }
            }
        }
        return null;
    }

    /**
     * Make the GClassNode for a ClassTarget
     */
    protected GClassNode makeClassInfo(ClassTarget classTarget, List<GClassNode> subClasses, GClassType type)
    {
        return new LocalGClassNode(this, classTarget, subClasses, type);
    }

    /**
     * Make a context menu item with the given text and action, and the inbuilt-menu-item
     * style (which shows up as dark-red, and italic on non-Mac)
     */
    public static MenuItem contextInbuilt(String text, FXRunnable action)
    {
        MenuItem menuItem = JavaFXUtil.makeMenuItem(text, action, null);
        JavaFXUtil.addStyleClass(menuItem, ClassTarget.MENU_STYLE_INBUILT);
        return menuItem;
    }

    /**
     * Gets the currently selected class target in the diagram.  May be null if no selection,
     * or if the selection is a class outside the default package (e.g. greenfoot.World)
     */
    public ClassTarget getSelectedClassTarget()
    {
        ClassDisplay selected = selectionManager.getSelected();
        if (selected != null)
        {
            Target target = project.getUnnamedPackage().getTarget(selected.getQualifiedName());
            if (target instanceof ClassTarget)
            {
                return (ClassTarget) target;
            }
        }
        return null;
    }

    /**
     * Gets the selection manager for this class diagram
     */
    public ClassDisplaySelectionManager getSelectionManager()
    {
        return selectionManager;
    }

    /**
     * Gets the GreenfootStage which contains this class diagram
     */
    public GreenfootStage getGreenfootStage()
    {
        return greenfootStage;
    }
}
