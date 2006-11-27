package bluej.groupwork.ui;

import java.util.Collections;
import java.util.List;

import javax.swing.AbstractListModel;

/**
 * A list model for the history list.
 * 
 * Because the cells change height depending on their width, we track
 * the heights here and fire a cell changed event if the height changes.
 * It's the responsibility of the cell renderer to let us know when
 * the height changes.
 * 
 * @author Davin McCall
 * @version $Id: HistoryListModel.java 4704 2006-11-27 00:07:19Z bquig $
 */
public class HistoryListModel extends AbstractListModel
{
    private List listData;
    private int [] cellHeights;
    
    public HistoryListModel()
    {
        super();
        listData = Collections.EMPTY_LIST;
    }
    
    /**
     * Set the data to be displayed in the list. 
     * 
     * @param newData A list of HistoryInfo objects.
     */
    public void setListData(List newData)
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
