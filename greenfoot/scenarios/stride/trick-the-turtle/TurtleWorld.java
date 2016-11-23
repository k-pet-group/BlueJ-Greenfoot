// WARNING: This file is auto-generated and any changes to it will be overwritten
import java.util.*;
import greenfoot.*;

/**
 * Create the turtle world. 
 * Our world has a size of 560x560 cells, where every cell is just 1 pixel.
 */
public class TurtleWorld extends World
{

    /**
     * 
     */
    public TurtleWorld()
    {
        super(560, 560, 1);
        prepare();
    }

    /**
     * Prepare the world for the start of the program.
     * That is: create the initial objects and add them to the world.
     */
    private void prepare()
    {
        Lettuce lettuce =  new  Lettuce();
        addObject(lettuce, 115, 506);
        Lettuce lettuce2 =  new  Lettuce();
        addObject(lettuce2, 255, 495);
        Lettuce lettuce3 =  new  Lettuce();
        addObject(lettuce3, 491, 489);
        Lettuce lettuce4 =  new  Lettuce();
        addObject(lettuce4, 394, 325);
        Lettuce lettuce5 =  new  Lettuce();
        addObject(lettuce5, 84, 341);
        Lettuce lettuce6 =  new  Lettuce();
        addObject(lettuce6, 243, 252);
        Lettuce lettuce7 =  new  Lettuce();
        addObject(lettuce7, 191, 411);
        Lettuce lettuce8 =  new  Lettuce();
        addObject(lettuce8, 466, 148);
        Lettuce lettuce9 =  new  Lettuce();
        addObject(lettuce9, 352, 71);
        Lettuce lettuce10 =  new  Lettuce();
        addObject(lettuce10, 124, 75);
        Lettuce lettuce11 =  new  Lettuce();
        addObject(lettuce11, 64, 197);
        Lettuce lettuce12 =  new  Lettuce();
        addObject(lettuce12, 225, 148);
        Lettuce lettuce13 =  new  Lettuce();
        addObject(lettuce13, 344, 454);
        Lettuce lettuce14 =  new  Lettuce();
        addObject(lettuce14, 378, 198);
        Snake snake =  new  Snake();
        addObject(snake, 435, 453);
        Snake snake2 =  new  Snake();
        addObject(snake2, 93, 259);
        Snake snake3 =  new  Snake();
        addObject(snake3, 467, 61);
        Turtle turtle =  new  Turtle();
        addObject(turtle, 273, 337);
    }
}
