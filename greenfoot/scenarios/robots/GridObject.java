
import greenfoot.GreenfootWorld;
import greenfoot.GreenfootObject;

/**
 * Class that makes sure that objects move in the grid.
 *  
 */
public class GridObject extends GreenfootObject
{

    public GridObject()
    {

    }

    /**
     * Overridden to round off all locations to grid locations
     */
    public void setLocation(int x, int y)
    {
        super.setLocation(toPixel(toCell(x)), toPixel(toCell(y)));
    }

    public void setCellLocation(int x, int y)
    {
        setLocation(toPixel(x), toPixel(y));
    }

    public int getCellX()
    {
        return toCell(getX());
    }

    public int getCellY()
    {
        return toCell(getY());
    }

    private int toCell(int i)
    {
        return RobotWorld.toCell(i);
    }

    private int toPixel(int i)
    {
        return RobotWorld.toPixel(i);
    }
}