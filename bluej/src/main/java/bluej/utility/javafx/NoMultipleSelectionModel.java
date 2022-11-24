/*
 This file is part of the BlueJ program. 
 Copyright (C) 2016,2018 Michael Kölling and John Rosenberg 
 
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
package bluej.utility.javafx;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.MultipleSelectionModel;

/**
 * A class which implements MultipleSelectionModel but never selects anything.
 * 
 * Useful when you want to display information in a listview, but you don't need
 * the selection capability, you just want an easy way to display read-only info.
 */
public class NoMultipleSelectionModel<T> extends MultipleSelectionModel<T>
{
    @Override
    public ObservableList<Integer> getSelectedIndices()
    {
        return FXCollections.emptyObservableList();
    }

    @Override
    public ObservableList<T> getSelectedItems()
    {
        return FXCollections.emptyObservableList();
    }

    @Override
    public void selectIndices(int index, int... indices)
    {

    }

    @Override
    public void selectAll()
    {

    }

    @Override
    public void selectFirst()
    {

    }

    @Override
    public void selectLast()
    {

    }

    @Override
    public void clearAndSelect(int index)
    {

    }

    @Override
    public void select(int index)
    {

    }

    @Override
    public void select(Object obj)
    {

    }

    @Override
    public void clearSelection(int index)
    {

    }

    @Override
    public void clearSelection()
    {

    }

    @Override
    public boolean isSelected(int index)
    {
        return false;
    }

    @Override
    public boolean isEmpty()
    {
        return false;
    }

    @Override
    public void selectPrevious()
    {

    }

    @Override
    public void selectNext()
    {

    }
}
