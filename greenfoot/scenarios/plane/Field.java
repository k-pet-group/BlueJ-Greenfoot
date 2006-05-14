import greenfoot.World;
import greenfoot.Actor;

import java.awt.Color;

public class Field extends World
{
    /**
     * Creates a new field
     */
    public Field() {
        super(600,600,1);
        getBackground().setColor(new Color(50,150,50));
        getBackground().fill();
    }
}