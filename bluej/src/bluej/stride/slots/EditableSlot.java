/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016,2020 Michael Kölling and John Rosenberg
 
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
package bluej.stride.slots;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import bluej.stride.framedjava.ast.JavaFragment;
import bluej.stride.framedjava.slots.UnderlineContainer;
import bluej.utility.javafx.AbstractOperation;

import javafx.beans.value.ObservableBooleanValue;
import javafx.scene.Node;
import bluej.stride.framedjava.errors.CodeError;
import bluej.stride.framedjava.errors.ErrorShower;
import bluej.stride.framedjava.slots.ExpressionSlot;
import bluej.stride.generic.Frame;
import bluej.stride.generic.RecallableFocus;
import bluej.utility.javafx.ErrorUnderlineCanvas.UnderlineInfo;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * EditableSlot is used to access functionality common to all slots.  EditableSlot extends HeaderItem,
 * but also several other interfaces (see their documentation).
 */
public interface EditableSlot extends HeaderItem, RecallableFocus, UnderlineInfo, ErrorShower, UnderlineContainer
{
    /**
     * Requests focus on the slot, at whatever position makes sense (should not perform a select-all)
     */
    default public void requestFocus() { requestFocus(Focus.LEFT); }

    /**
     * Requests focus at the given position.
     * @param on Where to focus: LEFT, RIGHT or ALL
     * @see bluej.stride.slots.Focus
     */
    public void requestFocus(Focus on);

    /**
     * Called by the editor to indicate that we have lost focus.  We can't just listen to when our components
     * lose focus, because focus may transfer from one component of the slot to another in the same slot (e.g. in
     * expression slots, cursor may move between text fields in the expression), so a component losing focus doesn't
     * always mean the whole slot has lost focus.  The editor keeps track of who has focus, and calls this method
     * on all slots which do not have focus.
     *
     * Note that this method may currently be called on a slot which did not have focus -- it is more like
     * "notifyHasNoFocus" than "youHadFocusButJustLostIt"
     */
    @OnThread(Tag.FXPlatform)
    public void lostFocus();

    /**
     * A property reflecting whether the field is "effectively focused"
     *
     * "Effectively focused" means that either the field has actual JavaFX GUI
     * focus, or code completion is showing for this slot, meaning it doesn't
     * have GUI focus, but for our purposes it is logically the focus owner
     * within the editor.
     */
    public ObservableBooleanValue effectivelyFocusedProperty();

    /**
     * Called to cleanup any state or overlays when the slot is going to be removed.
     * TODO May not be needed any more; might be covered by lostFocus
     */
    public void cleanup();

    /**
     * Called when the whole top level frame has been saved, so slots can perform any necessary updates
     * (e.g. method prompts)
     */
    @OnThread(Tag.FXPlatform)
    public void saved();

    // No need for any implementing classes to further override this:
    default public @Override
    EditableSlot asEditable() { return this; }

    /**
     * Gets the parent Frame of the slot
     * @return The parent frame
     */
    public Frame getParentFrame();

    /**
     * A method used to check/access this slot as an ExpressionSlot (nicer than using cast/instanceof)
     * @return Get this slot as an expression slot (type cast)
     */
    default public ExpressionSlot asExpressionSlot() { return null; }

    /**
     * Checks whether the slot is blank or close enough.  Definition is context-dependent on the slot
     * @return True, if the slot is (essentially) blank
     */
    public boolean isAlmostBlank();

    /**
     * The amount of effort (roughly, keypresses) required to create this slot's content
     *
     * See the documentation of Frame.calculateEffort for more information.
     */
    public int calculateEffort();

    public static enum TopLevelMenu { EDIT, VIEW }

    /**
     * Gets the menu items that might appear in top-level menus or context menu.  If shown in a top-level
     * menu, the key on the Map is used to organise them; if
     * @param contextMenu Whether this is a context menu or top level
     * @return The menu items
     */
    default public Map<TopLevelMenu, AbstractOperation.MenuItems> getMenuItems(boolean contextMenu) { return Collections.emptyMap(); }

    /**
     * Gets the relevant graphical node related to the given error, used for scrolling to the error.
     * By default, just gets the first graphical component in the slot.
     * @param err The error to look for
     * @return The Node where the error is
     */
    @Override
    default public Node getRelevantNodeForError(CodeError err)
    {
        return getComponents().stream().findFirst().orElse(null);
    }

    /**
     * Adds the given error to the slot
     * @param error The error to add
     */
    @OnThread(Tag.FXPlatform)
    public void addError(CodeError error);

    /**
     * Removes any errors that were present during previous calls to flagErrorsAsOld,
     * and have not since been added with addError
     */
    @OnThread(Tag.FXPlatform)
    public void removeOldErrors();

    /**
     * Flags all errors as old.  Generally, the pattern is:
     *  - flagErrorsAsOld
     *  - addError [for all compile errors]
     *  - removeOldErrors [leaves those just added by addError]
     *
     * This avoids an annoying blinking out/in of errors that happens if we just did removeAll/add;
     * this way, an error that is still present, never gets removed
     */
    @OnThread(Tag.FXPlatform)
    public void flagErrorsAsOld();

    /**
     * Gets any errors currently on the slot
     * @return A stream of errors
     */
    @OnThread(Tag.FXPlatform)
    public Stream<CodeError> getCurrentErrors();

    /**
     * Gets the JavaFragment of code that corresponds to this slot
     * @return The Java fragment
     */
    public JavaFragment getSlotElement();

    /**
     * Makes the slots editable/non-editable, e.g. in the case that the surrounding frame is disabled.
     * @param editable True to make this editable
     */
    public void setEditable(boolean editable);

    /**
     * Checks whether the slot is editable (see setEditable, setView), e.g. for determining where to place focus next.
     * @return True if this is editable
     */
    public boolean isEditable();
}
