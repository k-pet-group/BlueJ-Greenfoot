package greenfoot.localdebugger;

public class LocalByteArray extends LocalArray
{
    public LocalByteArray(byte [] array)
    {
        super(array, array.length);
    }
    
    @Override
    public String getValueString(int index)
    {
        return Byte.toString(((byte []) object)[index]);
    }
    
    @Override
    public boolean instanceFieldIsObject(int slot)
    {
        return false;
    }
}
