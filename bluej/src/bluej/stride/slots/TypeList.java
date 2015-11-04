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

import bluej.stride.generic.FrameCanvas;
import bluej.utility.javafx.FXRunnable;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import bluej.stride.generic.Frame;
import bluej.utility.Utility;
import bluej.utility.javafx.binding.DeepListBinding;

public class TypeList implements SlotValueListener
{
    private final ObservableList<HeaderItem> headerItems = FXCollections.observableArrayList();
    protected final ObservableList<TypeTextSlot> typeSlots = FXCollections.observableArrayList();
    private final SlotLabel prefixLabel;
    private final Supplier<TypeTextSlot> slotGenerator;
    private final Frame parentFrame;
    private final FXRunnable focusOnNext;

    protected TypeList(String label, Frame parentFrame, Supplier<TypeTextSlot> slotGenerator, FXRunnable focusOnNext)
    {
        this.parentFrame = parentFrame;
        this.slotGenerator = slotGenerator;
        this.prefixLabel = new SlotLabel(label);
        this.focusOnNext = focusOnNext;
        
        new DeepListBinding<HeaderItem>(headerItems) {
            
            @Override
            protected Stream<ObservableList<?>> getListenTargets()
            {
                return Stream.of(typeSlots);
            }

            @Override
            protected Stream<HeaderItem> calculateValues()
            {
                if (typeSlots.isEmpty())
                    return Stream.empty();
                
                ArrayList<HeaderItem> commas = new ArrayList<>();
                for (int i = 0; i < typeSlots.size() - 1; i++)
                    commas.add(new SlotLabel(", "));
                
                // Important that we filter the null operators out after interleaving, to preserve ordering:
                return Utility.concat(Stream.of(prefixLabel), Utility.interleave(typeSlots.stream().map(h -> (HeaderItem)h), commas.stream()));
            }
        }.startListening();
    }

    public ObservableList<HeaderItem> getHeaderItems()
    {
        return headerItems;
    }

    public TypeTextSlot addTypeSlot(int index)
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
        return slot.getText();
    }

    public void addTypeSlotAtEnd(String content)
    {
        TypeTextSlot slot = addTypeSlot(typeSlots.size());
        slot.setText(content);
        slot.requestFocus(Focus.LEFT);
    }

    public void setTypes(List<String> types)
    {
        while (typeSlots.size() > 0)
        {
            delete(typeSlots.get(typeSlots.size() - 1));
        }
        types.forEach(this::addTypeSlotAtEnd);
    }
}