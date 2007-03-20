package greenfoot.localdebugger;

public class LocalFloatArray extends LocalArray
{
    public LocalFloatArray(float [] array)
    {
        super(array, array.length);
    }
    
    @Override
    public String getValueString(int index)
    {
        return Float.toString(((float []) object)[index]);
    }
    
    @Override
    public boolean instanceFieldIsObject(int slot)
    {
        return false;
    }
}
