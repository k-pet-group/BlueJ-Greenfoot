import greenfoot.World;
import greenfoot.Actor;

/**
 * A turtle that draws a spiral.
 */
public class SpiralTurtle extends Turtle
{
    double step=2;
    double size =0;
    double angle=90;
    
    public SpiralTurtle() {
        penDown();
    }
      
    /**
     * The step size used to increase the size of the spiral
     */
    public void setStep(double newStep) {
        step = newStep;
    }
    
    /**
     * The angle the turtle turns when changing direction.
     */
    public void setAngle(double newAngle) {
        angle = newAngle;
    }
    
    public void act()
    {
        move(size);
        turn(angle);
        size+=step;
    }
}