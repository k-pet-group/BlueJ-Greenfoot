package greenfoot.localdebugger;

public class LocalLongArray extends LocalArray
{
    public LocalLongArray(long [] array)
    {
        super(array, array.length);
    }
    
    @Override
    public String getValueString(int index)
    {
        return Long.toString(((long [])object)[index]);
    }
    
    @Override
    public boolean instanceFieldIsObject(int slot)
    {
        return false;
    }
}
