import greenfoot.GreenfootWorld;
import greenfoot.GreenfootObject;
public class SpiralTurtle extends Turtle
{
  double step=2;
  double size =0;
  double angle=90;
  double maxSize=100;
  
  public SpiralTurtle() {
       penDown();
  }
  
  public void setSize(double newSize) {
      maxSize = newSize;
  }
      
  public void setStep(double newStep) {
      step = newStep;
  }
  
  public void setAngle(double newAngle) {
      angle = newAngle;
  }
  
  public void act()
  {
      if(size>maxSize) {
          return;
      }
      move(size);
      turn(angle);
      size+=step;
  }

}