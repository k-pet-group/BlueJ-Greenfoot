package javablue.gui;

import java.awt.*;

/**
 * A Canvas that displays an image.<p>
 *
 * update() is overridden to call paint() directly, thus 
 * bypassing the default implementation of update() which 
 * erases the background of the canvas before calling paint().
 * This eliminates nasty flashing.<p>
 *
 * @version 1.0, Apr 1 1996
 * @version 1.1, Nov 26, 1996
 *
 * Made ImageCanvas public, added default constructor, and added a 
 * setImage(Image) method for setting the image after construction.  
 * Also added a getPreferredSize() method.
 *
 * @author  David Geary
 * @see     Util
 */
public class ImageCanvas extends Component
{
	public static int CENTER = 0;
	public static int LEFT = -1;
	public static int RIGHT = +1;
	public static int TOP = -1;
	public static int BOTTOM = +1;
	
	private Image image;
	private int valign;
	private int halign;

	public ImageCanvas(Image image, int halign, int valign)
	{
		this.image = image;
		this.halign = halign;
		this.valign = valign;
		
		if(image != null)
			setImage(image);
	}
	
	public ImageCanvas(Image image)
	{
		this(image, CENTER, CENTER);
	}
	
	public ImageCanvas()
	{
		this(null, CENTER, CENTER);
	}
	
	public void paint(Graphics g)
	{
		if(image != null)
		{
			Dimension d = getSize();
			int x, y;
			
			if(halign == LEFT)
				x = 0;
			else if(halign == RIGHT)
				x = d.width - image.getWidth(null);
			else	// CENTER
				x = (d.width - image.getWidth(null)) / 2;
			
			if(valign == TOP)
				y = 0;
			else if(valign == BOTTOM)
				y = d.height - image.getHeight(null);
			else	// CENTER
				y = (d.height - image.getHeight(null)) / 2;
			
			g.drawImage(image, x, y, this);
		}
	}
	
	public void update(Graphics g)
	{
		paint(g);
	}
	
	public void setImage(Image image)
	{
		Util.waitForImage(this, image);
		this.image = image;

		setSize(image.getWidth(this), image.getHeight(this));

		if(isShowing())
			repaint();
	}
	
	public void setAlignment(int halign, int valign)
	{
		this.halign = halign;
		this.valign = valign;

		if(isShowing())
			repaint();
	}
	
	/**
	 * @deprecated as of JDK1.1
	 */
	public Dimension minimumSize()
	{
		if(image != null)
			return new Dimension(image.getWidth(this),
 image.getHeight(this));
		else 
			return new Dimension(0, 0);
	}
	
	public Dimension getMinimumSize()
	{
		return minimumSize();
	}
	
	/**
	 * @deprecated as of JDK1.1
	 */
	public Dimension preferredSize()
	{
		return minimumSize();
	}
	
	public Dimension getPreferredSize()
	{
		return preferredSize();
	}
}
