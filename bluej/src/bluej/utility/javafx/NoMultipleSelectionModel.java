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
public class NoMultipleSelectionModel<T> extends MultipleSelectionModel
{
    private ObservableList emptyList = FXCollections.observableArrayList();

    @Override
    public ObservableList<Integer> getSelectedIndices()
    {
        return emptyList;
    }

    @Override
    public ObservableList<T> getSelectedItems()
    {
        return emptyList;
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
