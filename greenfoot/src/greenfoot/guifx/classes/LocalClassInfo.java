package greenfoot.guifx.classes;

import bluej.Config;
import bluej.extensions.SourceType;
import bluej.pkgmgr.target.ClassTarget;
import bluej.pkgmgr.target.DependentTarget.State;
import bluej.pkgmgr.target.DependentTarget.TargetListener;
import greenfoot.guifx.GreenfootStage;
import greenfoot.guifx.classes.ClassDiagram.ClassType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

import java.util.List;

/**
 * A version of ClassInfo that handles extra actions and updates for local classes
 * (i.e. classes that are in this project, rather than Actor and World
 * which are imported).
 */
class LocalClassInfo extends ClassInfo implements TargetListener
{
    private ClassDiagram classDiagram;
    private final ClassTarget classTarget;
    private final ClassType type;

    /**
     * Make an instance for the given ClassTarget
     * @param classTarget The ClassTarget to make an instance for
     * @param subClasses The sub-classes of this ClassInfo
     * @param type The type of this class (Actor/World child, or Other)
     */
    public LocalClassInfo(ClassDiagram classDiagram, ClassTarget classTarget, List<ClassInfo> subClasses, ClassType type)
    {
        super(classTarget.getQualifiedName(), classTarget.getBaseName(), classDiagram.getGreenfootStage().getImageForClassTarget(classTarget), subClasses, classDiagram.getSelectionManager());
        this.classDiagram = classDiagram;
        this.classTarget = classTarget;
        this.type = type;
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
    }

    @Override
    public void stateChanged(State newState)
    {
        Paint fill = Color.TRANSPARENT;
        switch (newState)
        {
            case NEEDS_COMPILE:
                fill = ClassTarget.getGreyStripeFill();
                break;
            case HAS_ERROR:
                fill = ClassTarget.getRedStripeFill();
                break;
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
        // The ordering may have changed, easiest thing to do is
        // just recalculate the whole lot:
        classDiagram.recalculateGroups();
    }

    @Override
    public void tidyup()
    {
        classTarget.removeListener(this);
    }

    /**
     * Shows a context menu.
     * @param greenfootStage The GreenfootStage, needed for some actions
     * @param display The ClassDisplay we are showing the menu on.
     * @param e The event that triggered the showing.
     */
    private void showContextMenu(GreenfootStage greenfootStage, ClassDisplay display, ContextMenuEvent e)
    {
        if (curContextMenu != null)
        {
            curContextMenu.hide();
            curContextMenu = null;
        }
        ContextMenu contextMenu = new ContextMenu();
        Class<?> cl = classTarget.getPackage().loadClass(classTarget.getQualifiedName());
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
            contextMenu.getItems().add(ClassDiagram.contextInbuilt(
                    Config.getString(classTarget.hasSourceCode() ? "edit.class" : "show.apidoc"),
                    classTarget::open));
        }

        // Set image:
        if (type == ClassType.ACTOR || type == ClassType.WORLD)
        {
            contextMenu.getItems().add(ClassDiagram.contextInbuilt(Config.getString("select.image"),
                    () -> greenfootStage.setImageFor(classTarget, display)));
        }
        // Inspect:
        contextMenu.getItems().add(classTarget.new InspectAction(true, greenfootStage, display));
        contextMenu.getItems().add(new SeparatorMenuItem());

        // Duplicate:
        if (classTarget.hasSourceCode())
        {
            contextMenu.getItems().add(ClassDiagram.contextInbuilt(Config.getString("duplicate.class"),
                    () -> greenfootStage.duplicateClass(classTarget)));
        }

        // Delete:
        contextMenu.getItems().add(ClassDiagram.contextInbuilt(Config.getString("remove.class"), () -> {
            classTarget.remove();
            // Recalculate class contents after deletion:
            classDiagram.recalculateGroups();
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


        // New subclass:
        contextMenu.getItems().add(ClassDiagram.contextInbuilt(Config.getString("new.sub.class"), () ->
            {
                // TODO check if needed
                // boolean imageClass = superG.isActorClass() || superG.isActorSubclass();
                // imageClass |= superG.isWorldClass() || superG.isWorldSubclass();
                // if (imageClass)
                if (type == ClassType.ACTOR || type == ClassType.WORLD)
                {
                    greenfootStage.newImageSubClassOf(classTarget.getQualifiedName());
                }
                else
                {
                    greenfootStage.newSubClassOf(classTarget.getQualifiedName());
                }
            }));

        // Select item when we show context menu for it:
        classDiagram.getSelectionManager().select(display);
        contextMenu.show(display, e.getScreenX(), e.getScreenY());
        curContextMenu = contextMenu;
    }
}
