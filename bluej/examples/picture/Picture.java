
/**
 * This class represents a simple picture. You can draw the picture using
 * the draw method. But wait, there's more: being an electronic picture, it
 * can be changed. You can set it to black-and-white display and back to
 * colours (only after it's been drawn, of course).
 *
 * This class was written as an early example for teaching Java with BlueJ.
 * 
 * @author	Michael Kolling
 * @version 1.0  (15 July 2000)
 */
public class Picture
{
	Square wall;
	Square window;
	Triangle roof;
	Circle sun;

	/**
	 * Constructor for objects of class Picture
	 */
	public Picture()
	{
		// nothing to do... instance variables are automatically set to null
	}

	/**
	 * Draw this picture.
	 */
	public void draw()
	{
		wall = new Square();
		wall.moveVertical(80);
		wall.changeSize(100);

		window = new Square();
		window.changeColor("black");
		window.moveHorizontal(20);
		window.moveVertical(100);

		roof = new Triangle();	
		roof.changeSize(50, 140);
		roof.moveHorizontal(60);
		roof.moveVertical(70);

		sun = new Circle();
		sun.changeColor("yellow");
		sun.moveHorizontal(180);
		sun.moveVertical(-10);
		sun.changeSize(60);
	}

	/**
	 * change this picture to black/white display
	 */
	public void setBlackAndWhite()
	{
		if(wall != null)   // only if it's painted already...
		{
			wall.changeColor("black");
			window.changeColor("white");
			roof.changeColor("black");
			sun.changeColor("black");
		}
	}

	/**
	 * change this picture to black/white display
	 */
	public void setColour()
	{
		if(wall != null)   // only if it's painted already...
		{
			wall.changeColor("red");
			window.changeColor("black");
			roof.changeColor("green");
			sun.changeColor("yellow");
		}
	}
}
