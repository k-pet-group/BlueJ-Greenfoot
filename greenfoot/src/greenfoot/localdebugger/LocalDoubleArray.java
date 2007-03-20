package greenfoot.localdebugger;

public class LocalDoubleArray extends LocalArray
{
    public LocalDoubleArray(double [] array)
    {
        super(array, array.length);
    }
    
    @Override
    public String getValueString(int index)
    {
        return Double.toString(((double []) object)[index]);
    }
    
    @Override
    public boolean instanceFieldIsObject(int slot)
    {
        return false;
    }
}
