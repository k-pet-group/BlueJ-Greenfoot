package greenfoot.localdebugger;

public class LocalBooleanArray extends LocalArray
{
    public LocalBooleanArray(boolean [] array)
    {
        super(array, array.length);
    }
    
    @Override
    public String getValueString(int index)
    {
        return Boolean.toString(((boolean []) object)[index]);
    }

    @Override
    public boolean instanceFieldIsObject(int slot)
    {
        return false;
    }
}
