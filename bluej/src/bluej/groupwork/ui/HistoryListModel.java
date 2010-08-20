/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010  Michael Kolling and John Rosenberg 
 
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
package bluej.groupwork.ui;

import java.util.Collections;
import java.util.List;

import javax.swing.AbstractListModel;

import bluej.groupwork.HistoryInfo;

/**
 * A list model for the history list.
 * 
 * Because the cells change height depending on their width, we track
 * the heights here and fire a cell changed event if the height changes.
 * It's the responsibility of the cell renderer to let us know when
 * the height changes.
 * 
 * @author Davin McCall
 */
public class HistoryListModel extends AbstractListModel
{
    private List<HistoryInfo> listData;
    private int [] cellHeights;
    
    public HistoryListModel()
    {
        super();
        listData = Collections.emptyList();
    }
    
    /**
     * Set the data to be displayed in the list. 
     * 
     * @param newData A list of HistoryInfo objects.
     */
    public void setListData(List<HistoryInfo> newData)
    {
        int endIndex = listData.size() - 1;
        if (endIndex >= 0) {
            fireIntervalRemoved(this, 0, endIndex);
        }
        
        cellHeights = new int[newData.size()];
        for (int i = 0; i < cellHeights.length; i++) {
            cellHeights[i] = -1;
        }
        
        listData = newData;
        endIndex = listData.size() - 1;
        if (endIndex >= 0) {
            fireIntervalAdded(this, 0, endIndex);
        }
    }
    
    /**
     * Specify the cell height at a certain index. If the cell height
     * has changed, the list will be notified.
     * 
     * @param index   The index of the cell
     * @param height  The height of the specified cell
     */
    public void setCellHeight(final int index, int height)
    {
        int oldHeight = cellHeights[index];
        cellHeights[index] = height;
        if (oldHeight != -1 && oldHeight != height) {
            fireContentsChanged(this, index, index);
        }
    }
    
    /**
     * All the renderer to flag that a cell needs to change size.
     * @param index  The cell index
     */
    public void changeChell(int index)
    {
        fireContentsChanged(this, index, index);
    }
    
    public Object getElementAt(int index)
    {
        return listData.get(index);
    }
    
    public int getSize()
    {
        return listData.size();
    }
}
