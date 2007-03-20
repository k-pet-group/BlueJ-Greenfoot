package greenfoot.localdebugger;

public class LocalIntArray extends LocalArray
{
    public LocalIntArray(int [] array)
    {
        super(array, array.length);
    }
    
    @Override
    public String getValueString(int index)
    {
        return Integer.toString(((int []) object)[index]);
    }

    @Override
    public boolean instanceFieldIsObject(int slot)
    {
        return false;
    }
}
