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
public class ImageCanvas extends Component {
    private Image image;

	public ImageCanvas() {
	}
    public ImageCanvas(Image image) {
		Assert.notNull(image);
		setImage(image);
    }
    public void paint(Graphics g) {
		if(image != null) {
        	g.drawImage(image, 0, 0, this);
		}
    }
    public void update(Graphics g) {
        paint(g);
    }
	public void setImage(Image image) {
        Util.waitForImage(this, image);
		this.image = image;

        setSize(image.getWidth(this), image.getHeight(this));

		if(isShowing()) {
			repaint();
		}
	}
	/**
	 * @deprecated as of JDK1.1
	 */
	public Dimension minimumSize() {
		if(image != null) {
			return new Dimension(image.getWidth(this),
		                     	image.getHeight(this));
		}
		else 
			return new Dimension(0,0);
	}
	public Dimension getMinimumSize() {
		return minimumSize();
	}
	/**
	 * @deprecated as of JDK1.1
	 */
	public Dimension preferredSize() {
		return minimumSize();
	}
	public Dimension getPreferredSize() {
		return preferredSize();
	}
}
