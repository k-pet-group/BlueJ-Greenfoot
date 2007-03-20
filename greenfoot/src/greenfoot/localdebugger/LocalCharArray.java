package greenfoot.localdebugger;

public class LocalCharArray extends LocalArray
{
    public LocalCharArray(char [] array)
    {
        super(array, array.length);
    }
    
    @Override
    public String getValueString(int index)
    {
        return Character.toString(((char []) object)[index]);
    }
    
    @Override
    public boolean instanceFieldIsObject(int slot)
    {
        return false;
    }
}
