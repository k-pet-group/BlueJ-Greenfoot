/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2018,2019  Poul Henriksen and Michael Kolling 
 
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
import bluej.debugger.gentype.Reflective;
import bluej.editor.Editor;
import bluej.extensions.SourceType;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.target.ClassTarget;
import bluej.pkgmgr.target.DependentTarget.State;
import bluej.pkgmgr.target.DependentTarget.TargetListener;
import bluej.utility.javafx.JavaFXUtil;
import greenfoot.guifx.GreenfootStage;
import greenfoot.guifx.classes.GClassDiagram.GClassType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.image.Image;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.io.File;
import java.lang.reflect.Modifier;
import java.util.List;

/**
 * A version of GClassNode that handles extra actions and updates for local classes
 * (i.e. classes that are in this project, rather than Actor and World
 * which are imported).
 */
public class LocalGClassNode extends GClassNode implements TargetListener
{
    private final GClassType type;
    private GClassDiagram classDiagram;
    private final ClassTarget classTarget;
    private String imageFilename;
    
    /**
     * Make an instance for the given ClassTarget. The image will be retrieved from the project
     * properties.
     * 
     * @param classTarget The ClassTarget to make an instance for
     * @param subClasses The sub-classes of this GClassNode
     * @param type The type of this class (Actor/World child, or Other)
     */
    public LocalGClassNode(GClassDiagram classDiagram, ClassTarget classTarget,
            List<GClassNode> subClasses, GClassType type)
    {
        super(getImageForClass(classTarget, type), subClasses, classDiagram.getSelectionManager());
        this.imageFilename = classTarget.getPackage().getLastSavedProperties()
                .getProperty("class." + classTarget.getQualifiedName() + ".image");
        this.classDiagram = classDiagram;
        this.classTarget = classTarget;
        this.type = type;
        setImageForEditor();
    }

    @Override
    public String getQualifiedName()
    {
        return classTarget.getQualifiedName();
    }

    @Override
    public String getDisplayName()
    {
        return classTarget.getBaseName();
    }

    /**
     * Get the image for a class, if any. A class "inherits" its super-class image if it does not
     * have a specific image set. May return null.
     * @param classTarget The ClassTarget to get image for.
     * @param type The source type of this class node.
     * @return The image for the class or null if it has no image.
     */
    private static Image getImageForClass(ClassTarget classTarget, GClassType type)
    {
        if (type == GClassType.OTHER)
        {
            return null;
        }
        return JavaFXUtil.loadImage(getImageFilename(classTarget));
    }
    
    /**
     * Returns a file name for the image of the first class in the given class' class hierarchy
     * that has an image set.
     */
    private static File getImageFilename(ClassTarget ct)
    {
        String className = ct.getQualifiedName();
        Reflective type = ct.getTypeReflective();
        Package pkg = ct.getPackage();
        
        do {
            String imageFileName = pkg.getLastSavedProperties()
                    .getProperty("class." + className + ".image");
            
            if (imageFileName != null)
            {
                File imageDir = new File(pkg.getProject().getProjectDir(), "images");
                return new File(imageDir, imageFileName);
            }
            
            if (type != null)
            {
                type = type.getSuperTypesR().stream().filter(t -> !t.isInterface()).findFirst().orElse(null);
                className = (type != null) ? type.getName() : null;
            }
        }
        while (type != null);
        
        return null;
    }
    
    /**
     * Setup the given ClassDisplay with custom actions
     */
    @Override
    protected void setupClassDisplay(GreenfootStage greenfootStage, ClassDisplay display)
    {
        display.setOnContextMenuRequested(e -> {
            e.consume();
            showContextMenu(greenfootStage, display, e);
        });
        display.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2)
            {
                classTarget.open();
            }
        });
        // We only want to listen once our display exists:
        classTarget.addListener(this);
        // Make sure we correctly show the initial state:
        stateChanged(classTarget.getState());
    }

    @Override
    public void editorOpened()
    {
        setImageForEditor();
    }

    /**
     * Set the header image for the editor, if it has been opened.
     */
    private void setImageForEditor()
    {
        Editor editor = classTarget.getEditorIfOpen();
        if (editor != null)
        {
            editor.setHeaderImage(image);
        }
    }

    @Override
    protected void setImage(Image newImage)
    {
        super.setImage(newImage);
        setImageForEditor();
    }

    @Override
    public void stateChanged(State newState)
    {
        Paint fill;
        switch (newState)
        {
            case NEEDS_COMPILE:
                fill = ClassTarget.getGreyStripeFill();
                break;
            case HAS_ERROR:
                fill = ClassTarget.getRedStripeFill();
                break;
            default:
                fill = Color.TRANSPARENT;
        }
        display.setStripePattern(fill);
        // If we've become uncompiled, let the main window know we've been modified:
        if (newState != State.COMPILED)
        {
            classDiagram.getGreenfootStage().classModified();
        }
    }

    @Override
    public void renamed(String newName)
    {
        classDiagram.getGreenfootStage().saveAndMirrorClassImageFilename(newName, getImageFilename());
        
        // The ordering may have changed, easiest thing to do is
        // just recalculate the whole lot:
        classDiagram.recalculateGroups();
    }

    @Override
    public void tidyup()
    {
        super.tidyup();
        classTarget.removeListener(this);
    }

    /**
     * Shows a context menu.
     * @param greenfootStage The GreenfootStage, needed for some actions
     * @param display The ClassDisplay we are showing the menu on.
     * @param e The event that triggered the showing.
     */
    @OnThread(Tag.FXPlatform)
    private void showContextMenu(GreenfootStage greenfootStage, ClassDisplay display, ContextMenuEvent e)
    {
        if (curContextMenu != null)
        {
            curContextMenu.hide();
            curContextMenu = null;
            // Must hide our parent node's context menu manually:
            classDiagram.hideContextMenu();
        }
        ContextMenu contextMenu = new ContextMenu();
        
        Class<?> cl = null;
        if (classTarget.isCompiled())
        {
            // Only load the class if it is compiled:
            cl = classTarget.getPackage().loadClass(classTarget.getQualifiedName());
        }
        
        // Update mouse position from menu, so that if the user clicks new Crab(),
        // it appears where the mouse is now, rather than where the mouse was before the menu was shown.
        // We must use screen X/Y here, because the scene is the menu, not GreenfootStage,
        // so scene X/Y wouldn't mean anything useful to GreenfootStage:
        contextMenu.getScene().setOnMouseMoved(ev -> greenfootStage.setLatestMousePosOnScreen(ev.getScreenX(), ev.getScreenY()));

        if (cl != null)
        {
            if (classTarget.getRole().createClassConstructorMenu(contextMenu.getItems(), classTarget, cl))
            {
                // If any items were added, add divider afterwards:
                contextMenu.getItems().add(new SeparatorMenuItem());
            }

            if (classTarget.getRole().createClassStaticMenu(contextMenu.getItems(), classTarget, cl))
            {
                // If any items were added, add divider afterwards:
                contextMenu.getItems().add(new SeparatorMenuItem());
            }
        }
        else
        {
            MenuItem menuItem = new MenuItem(Config.getString("classPopup.needsCompile"));
            menuItem.setDisable(true);
            contextMenu.getItems().add(menuItem);
            contextMenu.getItems().add(new SeparatorMenuItem());
        }


        // Open editor:
        if (classTarget.hasSourceCode() || classTarget.getDocumentationFile().exists())
        {
            contextMenu.getItems().add(GClassDiagram.contextInbuilt(
                    Config.getString(classTarget.hasSourceCode() ? "edit.class" : "show.apidoc"),
                    classTarget::open));
        }

        // Set image:
        if (type == GClassType.ACTOR || type == GClassType.WORLD)
        {
            contextMenu.getItems().add(GClassDiagram.contextInbuilt(Config.getString("select.image"),
                    () -> greenfootStage.setImageFor(this)));
        }
        // Inspect:
        contextMenu.getItems().add(classTarget.new InspectAction(cl != null, display));
        contextMenu.getItems().add(new SeparatorMenuItem());

        // Duplicate:
        if (classTarget.hasSourceCode())
        {
            contextMenu.getItems().add(GClassDiagram.contextInbuilt(Config.getString("duplicate.class"),
                    () -> greenfootStage.duplicateClass(this, classTarget)));
        }

        // Delete:
        contextMenu.getItems().add(GClassDiagram.contextInbuilt(Config.getString("remove.class"), () -> {
            classTarget.remove();
            // Recalculate class contents after deletion:
            classDiagram.recalculateGroups();
            // Check after updating class diagram, as that will
            // check if any user classes remain:
            greenfootStage.fireWorldRemovedCheck(classTarget);
        }));


        // Convert to Java/Stride
        if (classTarget.getSourceType() == SourceType.Stride)
        {
            contextMenu.getItems().add(classTarget.new ConvertToJavaAction(greenfootStage));
        }
        else if (classTarget.getSourceType() == SourceType.Java &&
                classTarget.getRole() != null && classTarget.getRole().canConvertToStride())
        {
            contextMenu.getItems().add(classTarget.new ConvertToStrideAction(greenfootStage));
        }

        // Show "new subclass" only if the class is not final:
        boolean isFinal = false;
        if (cl != null)
        {
            isFinal = Modifier.isFinal(cl.getModifiers());
        }
        else
        {
            Reflective ctReflective = classTarget.getTypeReflective();
            if (ctReflective != null)
            {
                isFinal = ctReflective.isFinal();
            }
        }
        
        // New subclass:
        if (! isFinal)
        {
            contextMenu.getItems().add(GClassDiagram.contextInbuilt(Config.getString("new.sub.class"),
                    () -> greenfootStage.newSubClassOf(classTarget.getQualifiedName(), type)));
        }

        // Select item when we show context menu for it:
        classDiagram.getSelectionManager().select(display);
        contextMenu.show(display, e.getScreenX(), e.getScreenY());
        curContextMenu = contextMenu;
    }
    
    @Override
    public String getImageFilename()
    {
        return imageFilename;
    }
    
    /**
     * Set the image filename for this class node. The displayed image will be changed to match.
     */
    public void setImageFilename(String newImageFilename)
    {
        this.imageFilename = newImageFilename;

        if (newImageFilename != null)
        {
            File imageDir = new File(classTarget.getPackage().getProject().getProjectDir(), "images");
            File imageFile = new File(imageDir, imageFilename);
            setImage(JavaFXUtil.loadImage(imageFile));
        }
        else 
        {
            setImage(null);
        }
    }

    /**
     * Get the class target of this class
     */
    public ClassTarget getClassTarget()
    {
        return classTarget;
    }
}
