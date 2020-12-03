/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2013,2016,2017,2018,2020  Michael Kolling and John Rosenberg
 
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
package bluej.pkgmgr.target;

import bluej.extmgr.ClassExtensionMenu;
import bluej.extmgr.ExtensionsManager;
import bluej.extmgr.ExtensionsMenuManager;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PackageEditor;
import bluej.utility.Debug;
import bluej.utility.javafx.AbstractOperation;
import bluej.utility.javafx.JavaFXUtil;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import javafx.collections.FXCollections;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.AccessibleAttribute;
import javafx.scene.AccessibleRole;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.Cursor;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.Node;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A general target in a package
 * 
 * @author Michael Cahill
 */
public abstract class Target
    implements Comparable<Target>, AbstractOperation.ContextualItem<Target>
{
    static final int DEF_WIDTH = 120;
    static final int DEF_HEIGHT = 70;
    static final int ARR_HORIZ_DIST = 5;
    static final int ARR_VERT_DIST = 10;
    static final int HANDLE_SIZE = 20;
    static final int TEXT_HEIGHT = 16;
    static final int TEXT_BORDER = 4;
    static final int SHAD_SIZE = 4;
    private static final double SHADOW_RADIUS = 3.0;
    protected static final double RESIZE_CORNER_SIZE = 16;

    // Store the position before moving, and size before resizing.
    // Not because we allow cancelling (we don't), but because we move/resize
    // by deltas, so we need to know the start position/size.
    @OnThread(Tag.FXPlatform)
    private int preMoveX;
    @OnThread(Tag.FXPlatform)
    private int preMoveY;
    @OnThread(Tag.FXPlatform)
    private int preResizeWidth;
    @OnThread(Tag.FXPlatform)
    private int preResizeHeight;
    // Keeps track of whether a mouse button press at the current position
    // would be a resize.
    @OnThread(Tag.FXPlatform)
    private boolean pressIsResize;
    // The position of the mouse press (e.g. for positioning/sizing)
    // relative to the pane:
    @OnThread(Tag.FXPlatform)
    private double pressDeltaX;
    @OnThread(Tag.FXPlatform)
    private double pressDeltaY;
    // The currently showing context menu (if any).  Null if no menu showing.
    @OnThread(Tag.FXPlatform)
    private ContextMenu showingContextMenu;

    @OnThread(value = Tag.Any, requireSynchronized = true)
    private String identifierName; // the name handle for this target within
    // this package (must be unique within this
    // package)
    private String displayName; // displayed name of the target
    @OnThread(value = Tag.Any, requireSynchronized = true)
    private Package pkg; // the package this target belongs to

    // Is the current node selected?
    @OnThread(Tag.Any)
    protected boolean selected;
    // Is the current node queued for compilation?
    protected boolean queued;

    // The graphical item in the class diagram
    @OnThread(Tag.FXPlatform)
    protected BorderPane pane = JavaFXUtil.initFX(BorderPane::new);
    // Is the target directly resizable?  Readmes and test classes are not.
    @OnThread(Tag.FX)
    private boolean resizable = true;

    /**
     * Create a new target with default size.
     */
    @OnThread(Tag.FXPlatform)
    public Target(Package pkg, String identifierName, String accessibleTargetType)
    {
        this.pkg = pkg;
        this.identifierName = identifierName;
        this.displayName = identifierName;
        
        pane.setPrefWidth(calculateWidth(new Label(), identifierName, DEF_WIDTH));
        pane.setPrefHeight(DEF_HEIGHT);
        // We set this here rather than via CSS because we vary it dynamically:
        pane.setCursor(Cursor.HAND);
        JavaFXUtil.addStyleClass(pane, "target");
        pane.setEffect(new DropShadow(SHADOW_RADIUS, SHADOW_RADIUS/2.0, SHADOW_RADIUS/2.0, javafx.scene.paint.Color.GRAY));
        
        pane.setFocusTraversable(true);
        updateAccessibleName(accessibleTargetType, null);
        pane.setAccessibleRole(AccessibleRole.BUTTON);
        JavaFXUtil.addFocusListener(pane, hasFocus -> {
            PackageEditor pkgEditor = pkg.getEditor();

            // Editor can be null if we lose focus because window is closing,
            // in which case don't try to do anything with it:
            if (pkgEditor == null)
            {
                return;
            }

            // Here's the logic.  If we are focused after a mouse click,
            // the click listener will already have selected us before the
            // focus.  In which case this guard won't fire because we are selected.
            // This only fires if we received focus some other way while not being selected,
            // which should only be via keyboard traversal (tab, or ctrl-tab),
            // in which case we should cancel the selection and only select the focused item:
            if (hasFocus && !isSelected())
            {
                pkgEditor.selectOnly(this);
            }


            if (!hasFocus)
            {
                pkgEditor.checkForLossOfFocus();
            }
        });

        pane.setOnMouseClicked(e -> {
            if (e.getClickCount() > 1 && e.getButton() == MouseButton.PRIMARY && !e.isPopupTrigger() && e.isStillSincePress())
            {
                doubleClick(e.isShiftDown());
            }
            else if (e.getClickCount() == 1 && e.getButton() == MouseButton.PRIMARY && !e.isPopupTrigger() && e.isStillSincePress())
            {
                // We first check if the user was drawing an extends arrow,
                // in which case a click will finish that off.
                if (!pkg.getEditor().clickForExtends(this, e.getSceneX(), e.getSceneY()))
                {
                    // Not drawing; proceed with normal click handling.

                    // Single left-click.  Is modifier down?
                    if (e.isShiftDown() || e.isShortcutDown())
                    {
                        pkg.getEditor().toggleSelection(this);
                    }
                    else
                    {
                        pkg.getEditor().selectOnly(this);
                    }
                }
                updateCursor(e, false);
                if (isSelected())
                    pane.requestFocus();
            }
            e.consume();
        });
        pane.setOnMouseMoved(e -> {
            updateCursor(e, false);
            pkg.getEditor().setMouseIn(this);
        });
        pane.setOnMouseExited(e -> {
            pkg.getEditor().setMouseLeft(this);
        });

        pane.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY)
            {
                // Dismiss context menu if the user left-clicks on the target:
                // (Usual JavaFX behaviour is to keep the menu showing if you
                // click the menu's parent but I think users will expect it to
                // dismiss if they click anywhere besides the menu.)
                showingMenu(null);
                pressDeltaX = e.getX();
                pressDeltaY = e.getY();
                // Check if it's in the corner (and selected), in which case it will be a resize:
                pressIsResize = isSelected() && cursorAtResizeCorner(e);
                // This will save the positions of everything currently selected,
                // including us (by calling us back via savePreMove/savePreResize):
                if (pressIsResize && isResizable())
                    pkg.getEditor().startedResize();
                else
                    pkg.getEditor().startedMove();
            }
            e.consume();
        });
        pane.setOnMouseDragged(e -> {
            if (e.getButton() == MouseButton.PRIMARY && !pkg.getEditor().isCreatingExtends())
            {
                if (pressIsResize && isResizable())
                {
                    int newWidth = pkg.getEditor().snapToGrid((int) (e.getX() + (preResizeWidth - pressDeltaX)));
                    int newHeight = pkg.getEditor().snapToGrid((int) (e.getY() + (preResizeHeight - pressDeltaY)));
                    pkg.getEditor().resizeBy(newWidth - preResizeWidth, newHeight - preResizeHeight);
                }
                else if (isMoveable())
                {
                    // They didn't select us yet, but we still allow a drag-move.
                    // Select us as they start dragging:
                    if (!isSelected())
                    {
                        pkg.getEditor().selectOnly(this);
                        pkg.getEditor().startedMove();
                    }
                    // Need position relative to the editor to set new position:
                    Point2D p = pkg.getEditor().sceneToLocal(e.getSceneX(), e.getSceneY());
                    int newX = pkg.getEditor().snapToGrid((int) (p.getX() - pressDeltaX));
                    int newY = pkg.getEditor().snapToGrid((int) (p.getY() - pressDeltaY));
                    pkg.getEditor().moveBy(newX - preMoveX, newY - preMoveY);
                    updateCursor(e, true);
                }
            }
            e.consume();
        });
        pane.setOnMouseReleased(e -> {
            pkg.getEditor().endResize();
        });
        pane.setOnKeyTyped(e -> {
            // + or - on the keyboard do a resize:
            if (e.getCharacter().length() > 0 && "+-".contains(e.getCharacter()) && isResizable())
            {
                pkg.getEditor().startedResize();
                int delta = e.getCharacter().equals("+") ? PackageEditor.GRID_SIZE : -PackageEditor.GRID_SIZE;
                pkg.getEditor().resizeBy(delta, delta);
                pkg.getEditor().endResize();

                e.consume();
            }
        });
        pane.setOnKeyPressed(e -> {
            if (isArrowKey(e))
            {
                // Ctrl and arrow keys does a resize:
                if (e.isControlDown())
                {
                    if (isResizable())
                    {
                        // Resize:
                        pkg.getEditor().startedResize();
                        int d = PackageEditor.GRID_SIZE;
                        pkg.getEditor().resizeBy(
                            e.getCode() == KeyCode.LEFT ? -d : (e.getCode() == KeyCode.RIGHT ? d : 0),
                            e.getCode() == KeyCode.UP ? -d : (e.getCode() == KeyCode.DOWN ? d : 0));
                        pkg.getEditor().endResize();
                    }
                }
                else if (e.isShiftDown())
                {
                    // Shift and arrow keys does a move:
                    if (isMoveable())
                    {
                        // Move:
                        pkg.getEditor().startedMove();
                        int d = PackageEditor.GRID_SIZE;
                        pkg.getEditor().moveBy(
                            e.getCode() == KeyCode.LEFT ? -d : (e.getCode() == KeyCode.RIGHT ? d : 0),
                            e.getCode() == KeyCode.UP ? -d : (e.getCode() == KeyCode.DOWN ? d : 0));
                    }
                }
                else
                {
                    // If no modifiers then navigate around the diagram:
                    pkg.getEditor().navigate(e);
                }

                e.consume();
            }
            else if (e.getCode() == KeyCode.ESCAPE)
            {
                // We will still have focus, so rather than
                // clear selection completely, just select us:
                if (pkg.getEditor()!= null)
                {
                    pkg.getEditor().selectOnly(this);
                }
                e.consume();
            }
            else if (e.getCode() == KeyCode.A && !e.isAltDown()) // Allow Ctrl or Cmd, or plain
            {
                pkg.getEditor().selectAll();
                e.consume();
            }
        });

        JavaFXUtil.listenForContextMenu(pane, (x, y) -> {
            // If we are not in the current selection, make us the selection:
            if (!pkg.getEditor().getSelection().contains(Target.this))
            {
                pkg.getEditor().selectOnly(Target.this);
            }
            
            AbstractOperation.MenuItems menuItems = AbstractOperation.getMenuItems(pkg.getEditor().getSelection(), true);
            ContextMenu contextMenu = AbstractOperation.MenuItems.makeContextMenu(Map.of("", menuItems));
            if (pkg.getEditor().getSelection().size() == 1 && pkg.getEditor().getSelection().get(0) instanceof ClassTarget)
            {
                ClassTarget classTarget = (ClassTarget)pkg.getEditor().getSelection().get(0);
                ExtensionsMenuManager menuManager = new ExtensionsMenuManager(contextMenu, ExtensionsManager.getInstance(), new ClassExtensionMenu(classTarget));
                menuManager.addExtensionMenu(getPackage().getProject());
            }
            showingMenu(contextMenu);
            contextMenu.show(pane, x.intValue(), y.intValue());
            return true;
        }, KeyCode.SPACE, KeyCode.ENTER);

        if (pkg == null)
            throw new NullPointerException();
    }

    protected void updateAccessibleName(String accessibleTargetType, String suffix)
    {
        pane.setAccessibleText(getIdentifierName() + (accessibleTargetType != null && !accessibleTargetType.isEmpty() ? " " + accessibleTargetType : "") + (suffix == null ? "" : suffix));
    }

    @OnThread(Tag.FXPlatform)
    private void updateCursor(MouseEvent e, boolean moving)
    {
        if (moving)
        {
            pane.setCursor(Cursor.MOVE);
        }
        else if (isSelected() && isResizable() && cursorAtResizeCorner(e))
        {
            pane.setCursor(Cursor.SE_RESIZE);
        }
        else
        {
            pane.setCursor(Cursor.HAND);

        }
    }

    @OnThread(Tag.FXPlatform)
    protected boolean cursorAtResizeCorner(MouseEvent e)
    {
        // Check if it's in the 45-degree corner in the bottom right:
        return e.getX() + e.getY() >= getWidth() + getHeight() - RESIZE_CORNER_SIZE;
    }

    /**
     * Calculate the width of a target depending on the length of its name and
     * the font used for displaying the name. The size returned is a multiple of
     * 10 (to fit the interactive resizing behaviour).
     * 
     * @param name
     *            the name of the target (may be null).
     * @return the width the target should have to fully display its name.
     */
    @OnThread(Tag.FX)
    protected static int calculateWidth(Labeled node, String name, int minWidth)
    {
        int width = 0;
        if (name != null)
            width = (int)JavaFXUtil.measureString(node, name);
        if ((width + 20) <= minWidth)
            return minWidth;
        else
            // Snap to GRID_SIZE coordinates, at the next coordinate past width + 20.
            // e.g. GRID_SIZE=10, width = 17, snap to 40 (17 + 20 -> next snap).
            return ((width + 20 + (PackageEditor.GRID_SIZE - 1)) / PackageEditor.GRID_SIZE) * PackageEditor.GRID_SIZE;
    }
    
    /**
     * This target has been removed from its package.
     */
    public synchronized void setRemoved()
    {
        // This can be used to detect that a class target has been removed.
        pkg = null;
    }

    /**
     * Load this target's properties from a properties file. The prefix is an
     * internal name used for this target to identify its properties in a
     * properties file used by multiple targets.
     */
    public void load(Properties props, String prefix)
        throws NumberFormatException
    {
        // No super.load, but need to get Vertex properties:
        int xpos = 0;
        int ypos = 0;
        int width = 20; // arbitrary fallback values
        int height = 10;
        
        // Try to get the positional properties in a robust manner.
        try {
            xpos = Math.max(Integer.parseInt(props.getProperty(prefix + ".x")), 0);
            ypos = Math.max(Integer.parseInt(props.getProperty(prefix + ".y")), 0);
            width = Math.max(Integer.parseInt(props.getProperty(prefix + ".width")), 1);
            height = Math.max(Integer.parseInt(props.getProperty(prefix + ".height")), 1);
        }
        catch (NumberFormatException nfe) {}

        setPos(xpos, ypos);
        setSize(width, height);
    }

    /**
     * Save the target's properties to 'props'.
     */
    @OnThread(Tag.FXPlatform)
    public void save(Properties props, String prefix)
    {
        props.put(prefix + ".x", String.valueOf(getX()));
        props.put(prefix + ".y", String.valueOf(getY()));
        props.put(prefix + ".width", String.valueOf(getWidth()));
        props.put(prefix + ".height", String.valueOf(getHeight()));

        props.put(prefix + ".name", getIdentifierName());
    }

    /**
     * Return this target's package (ie the package that this target is
     * currently shown in)
     */
    @OnThread(Tag.Any)
    public synchronized Package getPackage()
    {
        return pkg;
    }

    /**
     * Change the text which the target displays for its label
     */
    public void setDisplayName(String name)
    {
        displayName = name;
    }

    /**
     * Returns the text which the target is displaying as its label
     */
    public String getDisplayName()
    {
        return displayName;
    }

    @OnThread(Tag.Any)
    public synchronized String getIdentifierName()
    {
        return identifierName;
    }

    public synchronized void setIdentifierName(String newName)
    {
        identifierName = newName;
    }

    /*
     * Sets the selected status of this target.  Do not call directly
     * to select us; instead call SelectionController/SelectionSet's methods,
     * which will call this after updating the selected set.
     */
    @OnThread(Tag.FXPlatform)
    public void setSelected(boolean selected)
    {
        this.selected = selected;
        JavaFXUtil.setPseudoclass("bj-selected", selected, pane);
        redraw();
    }

    @OnThread(Tag.FXPlatform)
    protected void redraw()
    {
    }

    /*
     * (non-Javadoc)
     * 
     * @see bluej.graph.Selectable#isSelected()
     */
    @OnThread(Tag.Any)
    public boolean isSelected()
    {
        return selected;
    }

    @OnThread(Tag.FXPlatform)
    public int getX()
    {
        Double leftAnchor = AnchorPane.getLeftAnchor(pane);
        if (leftAnchor == null)
            return 0;
        else
            return (int)(double)leftAnchor;
    }

    @OnThread(Tag.FXPlatform)
    public int getY()
    {
        Double topAnchor = AnchorPane.getTopAnchor(pane);
        if (topAnchor == null)
            return 0;
        else
            return (int)(double)topAnchor;
    }

    @OnThread(Tag.FXPlatform)
    public int getWidth()
    {
        // We use pref width because that's the internally intended width
        // Actual width may be 0 during initialisation so we can't use that.
        return (int)pane.getPrefWidth();
    }

    @OnThread(Tag.FXPlatform)
    public int getHeight()
    {
        return (int)pane.getPrefHeight();
    }

    public boolean isQueued()
    {
        return queued;
    }

    public void setQueued(boolean queued)
    {
        this.queued = queued;
    }

    @OnThread(Tag.FX)
    public boolean isResizable()
    {
        return resizable;
    }

    @OnThread(Tag.FXPlatform)
    public void setResizable(boolean resizable)
    {
        this.resizable = resizable;
    }

    @OnThread(Tag.Any)
    public boolean isSaveable()
    {
        return true;
    }

    public boolean isSelectable()
    {
        return true;
    }

    @OnThread(Tag.FXPlatform)
    public void repaint()
    {
        Package thePkg = getPackage();
        if (thePkg != null) // Can happen during removal
            thePkg.repaint();
    }

    /**
     * We have a notion of equality that relates solely to the identifierName.
     * If the identifierNames's are equal then the Target's are equal.
     */
    @OnThread(Tag.Any)
    public boolean equals(Object o)
    {
        if (o instanceof Target) {
            Target t = (Target) o;
            return this.identifierName.equals(t.identifierName);
        }
        return false;
    }

    @OnThread(Tag.Any)
    public synchronized int hashCode()
    {
        return identifierName.hashCode();
    }

    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    public int compareTo(Target t)
    {
        if (equals(t))
            return 0;

        if (this.getY() < t.getY())
            return -1;
        else if (this.getY() > t.getY())
            return 1;

        if (this.getX() < t.getX())
            return -1;
        else if (this.getX() > t.getX())
            return 1;

        return this.identifierName.compareTo(t.getIdentifierName());
    }

    public String toString()
    {
        return getDisplayName();
    }

    @OnThread(Tag.FXPlatform)
    public void setPos(int x, int y)
    {
        AnchorPane.setTopAnchor(pane, (double)y);
        AnchorPane.setLeftAnchor(pane, (double)x);
        repaint();
    }

    @OnThread(Tag.FXPlatform)
    public void setSize(int width, int height)
    {
        pane.setPrefWidth(width);
        pane.setPrefHeight(height);
        repaint();
    }

    @OnThread(Tag.FXPlatform)
    public abstract void doubleClick(boolean openInNewWindow);

    public abstract void remove();

    @OnThread(Tag.FXPlatform)
    public Node getNode()
    {
        return pane;
    }

    @OnThread(Tag.FXPlatform)
    public boolean isMoveable()
    {
        return false;
    }

    @OnThread(Tag.FXPlatform)
    public void setIsMoveable(boolean b)
    {
        
    }

    /**
     * Is this a front class (true; most of them) or back (false;
     * unit-test classes which always appear behind others)
     */
    @OnThread(Tag.FXPlatform)
    public boolean isFront()
    {
        return true;
    }

    /**
     * Gets the bounds relative to the package editor.
     * @return
     */
    @OnThread(Tag.FXPlatform)
    public Bounds getBoundsInEditor()
    {
        return pane.getBoundsInParent();
    }

    /**
     * Save the current position so that we later know
     * how much to move by (i.e. the delta) when dragging.
     */
    @OnThread(Tag.FXPlatform)
    public void savePreMovePosition()
    {
        preMoveX = getX();
        preMoveY = getY();
    }

    /**
     * The X position before we started the move
     */
    @OnThread(Tag.FXPlatform)
    public int getPreMoveX()
    {
        return preMoveX;
    }

    /**
     * The Y position before we started the move
     */
    @OnThread(Tag.FXPlatform)
    public int getPreMoveY()
    {
        return preMoveY;
    }

    /**
     * Save the current size so that we later know
     * how much to resize by (i.e. the delta).
     */
    @OnThread(Tag.FXPlatform)
    public void savePreResize()
    {
        preResizeWidth = getWidth();
        preResizeHeight = getHeight();
    }

    @OnThread(Tag.FXPlatform)
    public int getPreResizeWidth()
    {
        return preResizeWidth;
    }

    @OnThread(Tag.FXPlatform)
    public int getPreResizeHeight()
    {
        return preResizeHeight;
    }

    @OnThread(Tag.FXPlatform)
    protected final void showingMenu(ContextMenu newMenu)
    {
        if (newMenu != null)
        {
            // Request focus in order to draw selection around us while showing menu:
            requestFocus();
        }
        if (showingContextMenu != null)
        {
            showingContextMenu.hide();
        }
        showingContextMenu = newMenu;
    }

    @OnThread(Tag.FXPlatform)
    private static boolean isArrowKey(KeyEvent evt)
    {
        return evt.getCode() == KeyCode.UP || evt.getCode() == KeyCode.DOWN
            || evt.getCode() == KeyCode.LEFT || evt.getCode() == KeyCode.RIGHT;
    }

    @OnThread(Tag.FXPlatform)
    public boolean isFocused()
    {
        return pane.isFocused();
    }

    @OnThread(Tag.FXPlatform)
    public void requestFocus()
    {
        pane.requestFocus();
    }

    @OnThread(Tag.FXPlatform)
    public void setCreatingExtends(boolean drawingExtends)
    {
        // By default , we darken ourselves:
        pane.setEffect(drawingExtends ? new ColorAdjust(0, 0, -0.2, 0): null);
    }
}
