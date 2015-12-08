/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015 Michael Kölling and John Rosenberg 
 
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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import bluej.stride.generic.InteractionManager;
import bluej.utility.javafx.FXRunnable;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import bluej.stride.generic.Frame;
import bluej.utility.Utility;
import bluej.utility.javafx.binding.DeepListBinding;

/**
 * A class which manages a list of comma-separated type slots, e.g. an implements
 * list or a throws declaration.
 * 
 * This class holds a list of TypeTextSlot, and handles the use of backspace, delete,
 * and typing commas to shrink/extend the list.
 */
public class TypeList implements SlotValueListener
{
    /**
     * The list of header items to show; this will be a list of type slots with
     * comma slot labels inbetween, and prefixLabel on the front.
     */
    private final ObservableList<HeaderItem> headerItems = FXCollections.observableArrayList();
    /**
     * The list of type slots currently in this type list
     */
    protected final ObservableList<TypeTextSlot> typeSlots = FXCollections.observableArrayList();

    /**
     * A piece of code to generate a new type slot when we need one.  Supplied
     * by our creator (easier to pass lambda than have to subclass TypeList every time).
     */
    private final Supplier<TypeTextSlot> slotGenerator;
    /**
     * The frame which we are contained in.
     */
    private final Frame parentFrame;
    /**
     * A piece of code to focus on the item after this type list.  Called
     * when the user deletes all items in the type list, and we need somewhere
     * else to focus.
     */
    private final FXRunnable focusOnNext;
    /**
     * A reference to the editor, used to trigger recompile when the list changes.
     */
    private InteractionManager editor;
    /**
     * A property keeping track of whether any of the slots in this type list are currently focused.
     */
    private final BooleanProperty focusedProperty = new SimpleBooleanProperty(false);

    /**
     * Constructor.  Protected access because we expect to be subclassed for use anyway.
     * 
     * @param label The label to display before the list, e.g. "implements".  Must not be null.
     * @param parentFrame The frame this type list is contained in
     * @param slotGenerator A piece of code to generate a new TypeTextSlot for this list when needed
     * @param focusOnNext An action to focus on the item after this list, for when the user
     *                    blanks the type list.  (Note that at the moment, this means when the user
     *                    blanks the list using backspace, we focus the item after rather than the
     *                    item before, which is slightly odd.  But type lists are rarely used.)
     * @param editor A reference to the editor, used to notify about recompiles.
     */
    protected TypeList(String label, Frame parentFrame, Supplier<TypeTextSlot> slotGenerator, FXRunnable focusOnNext, InteractionManager editor)
    {
        this.parentFrame = parentFrame;
        this.slotGenerator = slotGenerator;
        this.focusOnNext = focusOnNext;
        this.editor = editor;

        final SlotLabel prefixLabel = new SlotLabel(label);
        
        new DeepListBinding<HeaderItem>(headerItems) {
            
            @Override
            protected Stream<ObservableList<?>> getListenTargets()
            {
                return Stream.of(typeSlots);
            }

            @Override
            protected Stream<HeaderItem> calculateValues()
            {
                // If we have a blank list, we don't even display the prefix label:
                if (typeSlots.isEmpty())
                    return Stream.empty();
                
                // We should probably cache the commas, but never mind:
                ArrayList<HeaderItem> commas = new ArrayList<>();
                for (int i = 0; i < typeSlots.size() - 1; i++)
                    commas.add(new SlotLabel(", "));
                
                // Prefix, followed by type slots interspersed with commas:
                return Utility.concat(Stream.of(prefixLabel), Utility.interleave(typeSlots.stream().map(h -> (HeaderItem)h), commas.stream()));
            }
        }.startListening();

        final ChangeListener<Boolean> focusListener = (a, b, newVal) -> updateFocusedProperty();

        typeSlots.addListener((ListChangeListener<? super TypeTextSlot>) change -> {
            while (change.next())
            {
                if (change.wasAdded())
                {
                    change.getAddedSubList().forEach(slot -> slot.effectivelyFocusedProperty().addListener(focusListener));
                }

                if (change.wasRemoved())
                {
                    change.getRemoved().forEach(slot -> slot.effectivelyFocusedProperty().removeListener(focusListener));
                }
            }
            updateFocusedProperty();
        });
    }

    private void updateFocusedProperty()
    {
        focusedProperty.set(typeSlots.stream().anyMatch(slot -> slot.effectivelyFocusedProperty().getValue()));
    }

    public ObservableList<HeaderItem> getHeaderItems()
    {
        return headerItems;
    }

    /**
     * Add a new type slot before the given index (0 <= index <= typeSlots.size())
     * @return The new type slot, which will just have been added to the typeSlots list.
     */
    private TypeTextSlot addTypeSlot(int index)
    {
        final TypeTextSlot slot = slotGenerator.get();
        
        slot.addValueListener(this);
        slot.addFocusListener(parentFrame);
        slot.addValueListener(SlotTraversalChars.IDENTIFIER);
        
        typeSlots.add(index, slot);
        return slot;
    }

    @Override
    public boolean valueChanged(HeaderItem slot, String oldValue, String newValue,
            FocusParent<HeaderItem> parent)
    {
        // If the user has entered a comma, split the type slot at that point
        // and add a new slot with the second half of the content:
        if (newValue.contains(",")) {
            TypeTextSlot newSlot = addTypeSlot(typeSlots.indexOf(slot) + 1);
            String right = newValue.substring(newValue.indexOf(",") + 1);
            // Hacky way to chop the string after listeners have run:
            Platform.runLater(() -> ((TypeTextSlot)slot).setText(newValue.substring(0, newValue.indexOf(","))));
            newSlot.setText(right);
            newSlot.requestFocus(Focus.LEFT);
            return false;
        }
        return true;
    }

    @Override
    public void backSpacePressedAtStart(HeaderItem slot)
    {
        int index = typeSlots.indexOf(slot);
        // Delete our slot:
        String remainder = delete((TypeTextSlot)slot);
        if (index - 1 >= 0 && index - 1 < typeSlots.size())
        {
            TypeTextSlot prev = typeSlots.get(index - 1);
            prev.setText(prev.getText() + remainder);
            // Iffy way to keep caret in right place:
            prev.requestFocus();
            prev.recallFocus(prev.getText().length() - remainder.length());
        }
        else
        {
            focusOnNext.run();
        }
    }

    @Override
    public void deletePressedAtEnd(HeaderItem slot)
    {
        int index = typeSlots.indexOf(slot);
        // If we're not the last parameter, delete the one after us:
        if (index < typeSlots.size() - 1) {
            String remainder = delete(typeSlots.get(index + 1));
            String prev = typeSlots.get(index).getText();
            typeSlots.get(index).setText(prev + remainder);
            typeSlots.get(index).recallFocus(prev.length());
        }
        // If we are, delete us!
        else {
            delete((TypeTextSlot) slot);
            focusOnNext.run();
        }
    }

    private String delete(TypeTextSlot slot)
    {
        // Remove the formal:
        slot.cleanup();
        typeSlots.remove(slot);
        editor.modifiedFrame(parentFrame);
        return slot.getText();
    }

    public void addTypeSlotAtEnd(String content, boolean requestFocus)
    {
        TypeTextSlot slot = addTypeSlot(typeSlots.size());
        slot.setText(content);
        if (requestFocus)
            slot.requestFocus(Focus.LEFT);
    }

    public void setTypes(List<String> types)
    {
        while (typeSlots.size() > 0)
        {
            delete(typeSlots.get(typeSlots.size() - 1));
        }
        types.forEach(t -> addTypeSlotAtEnd(t, false));
    }
    
    public Stream<TypeTextSlot> getTypeSlots()
    {
        return typeSlots.stream();
    }

    public void ensureAtLeastOneSlot()
    {
        if (typeSlots.isEmpty())
            addTypeSlotAtEnd("", false);
    }

    public void clearIfSingleEmpty()
    {
        if (typeSlots.size() == 1 && typeSlots.get(0).isEmpty())
            delete(typeSlots.get(0));
    }

    public ReadOnlyBooleanProperty focusedProperty()
    {
        return focusedProperty;
    }

    public void removeIndex(int index)
    {
        if (index >= 0 && index < typeSlots.size())
            delete(typeSlots.get(index));
    }
}