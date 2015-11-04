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


class MethodParametersPane
{
    /*
    public MethodParametersPane(JVMInteractionManager editor, HeadedFrame parentFrame, CodeFrame<? extends CodeElement> codeParentFrame, String stylePrefix)
    {
        this.editor = editor;
        this.parentFrame = parentFrame;
        this.codeParentFrame = codeParentFrame;
        if (parentFrame != codeParentFrame)
            throw new IllegalArgumentException("codeFrame and codeParentFrame should be identical");
        this.stylePrefix = stylePrefix;

        setAlignment(Pos.CENTER_LEFT);
        setMinHeight(5);

        open.getStyleClass().add("bracket-label");
        getChildren().add(open);
        Label spacer = new Label(" ");
        spacer.setOpacity(0.0);
        spacer.setCursor(Cursor.TEXT);
        spacer.setOnMouseClicked(e -> {
                addSlot(null).requestFocus(Focus.LEFT);
                e.consume();
        });
        getChildren().add(spacer);
        comma.getStyleClass().add("comma-label");
        close.getStyleClass().add("bracket-label");
        getChildren().add(close);
    }
    */
    /*    
    public abstract SLOT addSlot(SLOT after);
    
    protected final <F extends SlotFragment> TextSlot<F> createTextSlot(String promptText, F value, SlotFragmentFactory<F> fragmentFactory, CompletionCalculator completionCalc)
    {
        TextSlot<F> textSlot = new TextSlot<>(editor, parentFrame, codeParentFrame, fragmentFactory, completionCalc, stylePrefix);
        textSlot.addValueListener(new SlotTraversalChars());
        textSlot.setPromptText(promptText);
        textSlot.setText(value);
        textSlot.addValueListener(this);
        return textSlot;
    }
    
    protected SLOT insertSlot(SLOT after, SLOT slot)
    {
        slots.add(after == null ? slots.size() : slots.indexOf(after) + 1, slot);
        
        int indexToAdd;
        if (after == null) {
            indexToAdd = getChildren().size() - 1; // Before spacer
        }
        else {
            indexToAdd = getChildren().indexOf(after.getNode()) + 1;
        }
        if (slots.size() > 1) {
            getChildren().add(indexToAdd, comma);
            indexToAdd += 1;
        }
        getChildren().add(indexToAdd, slot.getNode());
        return slot;
    }
    
    @Override
    public boolean valueChanged(Slot slot, String oldValue, String newValue, SlotParent parent)
    {
        if (newValue.contains(",")) {
            addSlot(getWrapperSlot(slot)).requestFocus(Focus.LEFT);
            return false;
        }
        return true;
    }
    
    private void deleteSlot(SLOT slot, int index)
    {
        if (index > 0) {
            // TODO change it
            SLOT prevSlot = slots.get(index - 1);
            // Copy remaining content into previous slot:
            mergeTwoSlotsContents(slot, prevSlot);
            prevSlot.requestFocus(Focus.RIGHT);
        }

        // Destroy the slot
        int slotIndex = getChildren().indexOf(slot.getNode());
        getChildren().remove(slotIndex);
        // Remove the comma
        if (slotIndex > 2) {
            getChildren().remove(slotIndex - 1);
        }
        
        // Remove the slot from the slot parent and params:
        slots.remove(slot);
    }

    protected abstract void mergeTwoSlotsContents(SLOT slot, SLOT prevSlot);
    
    public void deleteFirstSlot()
    {
        if (slots.size() > 0) {
            // TODO check it in typeParams
            deleteSlot(slots.get(0), 0);
        }
    }
    
    @Override
    public void backSpacePressedAtStart(Slot slot)
    {
        SLOT wrapper = getWrapperSlot(slot);
        int index = slots.indexOf(wrapper);
        // Delete our slot:
        deleteSlot(wrapper, index);
    }
    
    @Override
    public void deletePressedAtEnd(Slot slot)
    {
        // If the cursor is on the last slot of the the wrapper
        if ( isLastInTheWrapper(slot) ) {
            SLOT wrapper = getWrapperSlot(slot);
            int index = slots.indexOf(wrapper);
            // If we're not the last parameter, delete the one after us:
            if (index < slots.size() - 1) {
                deleteSlot(slots.get(index + 1), index + 1);
            }
            // If we are, delete us!
            else {
                deleteSlot(wrapper, index);
            }
        }
    }
    
    protected abstract boolean isLastInTheWrapper(Slot slot);

    abstract protected SLOT getWrapperSlot(Slot slot);

    public List<String> getTexts()
    {
        List<String> paramsText = new ArrayList<String>();
        slots.forEach(p -> paramsText.add(p.getText()));
        return paramsText;
    }
    
    public List<ParamFragment> getSlotElement()
    {
        return slots.stream().map(PairParameterSlot::getSlotElement).collect(Collectors.toList());
    }
    
    public void focusSlot(int index)
    {
        slots.get(index).requestFocus(Focus.LEFT);
    }
    
    public int slotCount()
    {
        return slots.size();
    }
    
    public boolean isEmpty()
    {
        if (slots.size() == 1 && slots.get(0).isEmpty()) {
            return true;
        }
        return false;
    }

    @Override
    public void requestFocus()
    {
        if (slots.isEmpty()) {
            addSlot(null);
        }
        slots.get(0).requestFocus(Focus.LEFT);
    }
    
    abstract public void checkForEmptySlot();

    public Stream<EditableSlot> getHeaderItems()
    {
        return slots.stream().flatMap(PairParameterSlot::getHeaderItems);
    }
    */
}
