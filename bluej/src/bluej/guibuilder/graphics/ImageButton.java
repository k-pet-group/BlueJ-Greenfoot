package bluej.guibuilder.graphics;

import java.awt.*;
import java.awt.image.FilteredImageSource;
import java.awt.event.*;
import java.util.Vector;
import javablue.gui.image.BlackAndWhiteFilter;

/**
 * An Image painted in a Canvas, bordered by a ThreeDRectangle.
 * <p>
 *
 * ImageButtons have two constructors, both of which take an 
 * Image, along with a default constructor.  The Image passed 
 * to a constructor must not be null; this is enforced by 
 * assertions.<p>
 *
 * Using the default constructor creates an image button with no
 * image specified.  This is to allow flexibility when creating
 * image buttons, but it should be noted that it's the 
 * developer's * responsibility to call setImage(Image) before 
 * the image button is painted, or an assertion will be thrown.
 *
 * Default border thickness is 2 pixels - thickness may be set 
 * at construction time only.<p>
 *
 * Event handling is delegated to an ImageButtonListener.  By 
 * default, all ImageButtons are fitted with an instance of 
 * SpringyImageButtonListener, however, 
 * setListener(ImageButtonListener) may be used to fit an
 * ImageButton with a different derivation of 
 * ImageButtonListener after construction.<p>
 *
 * ImageButtons ensure that their Images are completely loaded 
 * before they are displayed.<p>
 * 
 * Drawn either raised or inset, current state may be queried 
 * via the isRaised() method.<p>
 *
 * setEnabled(false) disables response to input and repaints 
 * the image with a black and white version.  setEnabled(true) 
 * restores the original image and enables response to input.
 *
 * @version 1.0, Apr 1 1996
 * @version 1.1, Nov 8 1996
 * @version 1.2, Dec 19 1996
 *
 * 1.1:.........................................................
 * Added default constructor and a constructor taking an Image
 * and an int (for thickness).
 *
 * Added static methods for getting/setting default thickness.
 *
 * Changed preferredSize instance variable to prefSize to work
 * around bug in Microsoft compiler.
 *
 * Switched the implementations of setBlackWhitePercent() and
 * getBleachPercent() which somehow got switched previously.
 *
 * 1.2:.........................................................
 * Event handling upgrade to 1.1.  Image buttons can be
 * constructed with or without an ImageButtonListener.  If no
 * ImageButtonListener is specified at construction time,
 * the image button is equipped with a
 * SpringyImageButtonListener.  The image button's listener can
 * also be changed after construction, by invoking
 * setListener(ImageButtonListener)
 * 
 * Image buttons fire action events; to register an action
 * listener, use ImageButton.setActionListener(ActionListener).
 * Image buttons employ an instance of ActionListener and
 * AWTEventMulticaster to manage the registration of action 
 * listeners and to fire action events to all currently 
 * registered action listeners.
 *
 * Since an ImageButton is a Canvas, it will fire mouse and
 * mouse motion events that can be listened to by others.
 *
 * ImageButton uses a BlackAndWhiteFilter to drain the color out
 * of the button when it is disabled.  You can set the degree
 * to which the color is washed out by calling the static
 * setBlackWhitePercent() method.
 *
 * @author  David Geary
 * @see     ThreeDRectangle
 * @see     ImageButtonListener
 * @see     SpringyImageButtonListener
 * @see     StickyImageButtonListener
 * @see     gjt.test.ImageButtonTest
 */
public class ImageButton extends Component {
    private static BlackAndWhiteFilter _bwFilter;
    private static int                 _offset           = 1;
    private static int                 _defaultThickness = 2;

    public  ThreeDRectangle border  = new ThreeDRectangle(this);
    private boolean             isDisabled = false;
	private boolean             armed      = false;
    private Dimension           prefSize   = new Dimension(0,0);
    private int                 thickness;
    private Image               image, disabledImage;
	private ImageButtonListener listener;
	private ActionListener      actionListener;
	private Bubble              bubble;
	private String              bubbleText;
	private BubbleThread        thread;
	private long                bubbleInterval = 1000;

	public static void setDefaultThickness(int defThickness) {
		_defaultThickness = defThickness;
	}
	public static int getDefaultThickness() {
		return _defaultThickness;
	}
	public ImageButton() {
		this((Image)null);
	}
    public ImageButton(Image image) {
        this(image, _defaultThickness);
    }
    public ImageButton(Image image, String bubble) {
        this(image, _defaultThickness);
		this.setBubbleHelp(bubble);
    }
	public ImageButton(Image image, int thickness) {
		this(image, thickness, (ImageButtonListener)null);
    }
	public ImageButton(Image image, int thickness, String bubble) {
		this(image, thickness, (ImageButtonListener)null);
		this.setBubbleHelp(bubble);
    }
    public ImageButton(String image) {
		this(Toolkit.getDefaultToolkit().getImage(image), _defaultThickness);
    }
    public ImageButton(String image, String bubble) {
		this(Toolkit.getDefaultToolkit().getImage(image), _defaultThickness);
		this.setBubbleHelp(bubble);
    }
    public ImageButton(String image, int thickness) {
		this(Toolkit.getDefaultToolkit().getImage(image), thickness, (ImageButtonListener)null);
    }
    public ImageButton(String image, int thickness, String bubble) {
		this(Toolkit.getDefaultToolkit().getImage(image), thickness, (ImageButtonListener)null);
		this.setBubbleHelp(bubble);
    }
    public ImageButton(String image, int thickness, ImageButtonListener listener) {
		this(Toolkit.getDefaultToolkit().getImage(image), thickness, listener);
    }
    public ImageButton(String image, int thickness, ImageButtonListener listener, String bubble) {
		this(Toolkit.getDefaultToolkit().getImage(image), thickness, listener);
		this.setBubbleHelp(bubble);
    }
	public ImageButton(Image image, int thickness, ImageButtonListener listener, String bubble) {
		this(image, thickness, listener);
		this.setBubbleHelp(bubble);
	}
	public ImageButton(Image image, int thickness, ImageButtonListener listener) {
		setThickness(thickness);

		if(image != null)
        	setImage(image);

		if(listener != null)
			setListener(listener);
		else
			setListener(new SpringyImageButtonListener());

		addMouseListener(new MouseAdapter() {
			public void mouseEntered(MouseEvent event) {
				if(bubbleText != null) {
					if(bubbleInterval == 0) {
						showBubbleHelp();
					}
					else {
						thread = new BubbleThread(ImageButton.this);
						thread.start();
					}
				}
			}
			public void mousePressed(MouseEvent event) {
				if(bubble != null && bubble.isShowing()) {
					bubble.dispose();
				}
				if(thread != null && thread.isAlive())
					thread.stop();
			}
			public void mouseExited(MouseEvent event) {
				if(bubble != null && bubble.isShowing()) {
					bubble.dispose();
				}
				if(thread != null && thread.isAlive())
					thread.stop();
			}
		});
	}
	void showBubbleHelp() {
		if(bubbleText != null) {
			Dimension size = getSize();
	    	Point scrnLoc = getLocationOnScreen();

			if(bubble == null)
				bubble = new Bubble(this, bubbleText);

			bubble.setLocation(scrnLoc.x, 
			                   scrnLoc.y + size.height + 2);
			bubble.setVisible(true);			
		}
	}
	public void setBubbleHelp(String bubbleText) {
		this.bubbleText = bubbleText;
	}
	public void setBubbleInterval(long interval) {
		bubbleInterval = interval;
	}
	public long getBubbleInterval() {
		return bubbleInterval;
	}
	// Provision for programatically manipulating image
	// buttons. All functionality is delegated to listener,
	// because the listener defines what it means to be armed
	// or active.
	public void activate  () { listener.activate(this);   }
	public void arm       () { listener.arm(this);        }
	public void disarm    () { listener.disarm(this);     }

	public void setThickness(int thickness) {
        Assert.notFalse(thickness > 0); 
		this.thickness = thickness;
        border.setThickness(this.thickness = thickness);  
		if(isShowing()) {
			invalidate();
			getParent().validate();
		}
	}
	public void setArmed(boolean armed) {
		this.armed = armed;
	}
	public boolean isArmed() {
		return armed;
	}
    public void setImage(String image) {
    	this.setImage(Toolkit.getDefaultToolkit().getImage(image));
    }
    public void setImage(Image image) {
		Assert.notNull(image);
        Util.waitForImage(this, this.image = image);
		if(isShowing()) {
			invalidate();
			getParent().validate();
		}
    }
	public ImageButtonListener getListener() {
		return listener;
	}
	public void setListener(ImageButtonListener l) {
		if(listener != null) {
			removeMouseListener(listener);
			removeMouseMotionListener(listener);
		}
		listener = l;
		addMouseListener(listener);
		addMouseMotionListener(listener);
	}
	/**
	 * @deprecated as of JDK1.1
	 */
	public Dimension minimumSize() {
        return prefSize; 
	}
    public Dimension getMinimumSize() { 
		return minimumSize();
    }
	/**
	 * @deprecated as of JDK1.1
	 */
	public Dimension preferredSize() {
		Util.waitForImage(this, image);
        prefSize.width  = image.getWidth (this) + (2*thickness);
        prefSize.height = image.getHeight(this) + (2*thickness);
        return prefSize;                
    }
    public Dimension getPreferredSize() { 
		return preferredSize();
	}
    public boolean isRaised() { 
		return border.isRaised();    
	}
	public void setRaised() {
		border.raise();
		if(isShowing()) repaint();
	}
    public boolean isInset() { 
		return ! border.isRaised();
	}
	public void setInset() {
		border.inset();
		if(isShowing()) repaint();
	}
    public boolean isDisabled() { 
		return isDisabled;           
	}
	/**
	 * @deprecated as of JDK1.1
	 */
	public void enable() {
        isDisabled = false; 
		if(isShowing()) repaint();
	}
	/**
	 * @deprecated as of JDK1.1
	 */
	public void disable() {
        isDisabled = true;  

        if(disabledImage == null) 
        	createDisabledImage();

		if(isShowing()) 
			repaint();
    }
    public void setEnabled(boolean enable) { 
		if(enable) enable();
		else       disable();
	}
	/**
	 * @deprecated as of JDK1.1
	 */
	public void resize(int w, int h) {
        setBounds(getLocation().x, getLocation().y, w, h);
    }
    public void setSize(int w, int h) { 
		resize(w,h);
	}
	/**
	 * @deprecated as of JDK1.1
	 */
	public void reshape(int x, int y, int w, int h) {
		// compiler will issue deprecation warning, but we can't call
		// super.setBounds()!
        super.reshape(x,y,w,h);
        border.setSize(w,h);
    }
    public void setBounds(int x, int y, int w, int h) { 
		reshape(x,y,w,h);
	}
    public void paint(Graphics g) {
        if(isRaised()) paintRaised();
        else           paintInset ();
    }
	public void paintInset() {
        Point     upperLeft = findUpperLeft();
		Graphics  g         = getGraphics();
        Image     image     = isDisabled() ? 
                              disabledImage : this.image;
        Dimension size      = getSize();

		Assert.notNull(image);

        if(g != null) {
            border.clearInterior();
            g.drawImage(image, 
                        upperLeft.x + thickness + _offset, 
                        upperLeft.y + thickness + _offset,this);

            // g.setColor(getBackground().darker());
            g.setColor(getBackground());
            for(int i=0; i < _offset; ++i) {
                g.drawLine(thickness+i,thickness+i,
                           size.width-thickness-i,thickness+i);
                g.drawLine(thickness+i,thickness+i,
                           thickness+i,size.height-thickness-i);
            }
            border.paintInset();
			g.dispose();
        }
    }
    public void paintRaised() {
        Point    upperLeft = findUpperLeft();
		Graphics  g        = getGraphics();
        Image    image     = isDisabled() ? 
                             disabledImage : this.image;

		Assert.notNull(image);

        if(g != null) {
            border.clearInterior();
            g.drawImage(image, upperLeft.x + thickness, 
                               upperLeft.y + thickness, this);
            border.paintRaised();
			g.dispose();
        }
    }
	public void processActionEvent() {
		if(actionListener != null) {
			actionListener.actionPerformed(
				new ActionEvent(this, 
				                ActionEvent.ACTION_PERFORMED,
							    "ImageButton Action"));
		}
		setArmed(false);
	}
	public synchronized void addActionListener( 
									ActionListener listener) {
		actionListener = 
			AWTEventMulticaster.add(actionListener, listener);
    }
    public synchronized void removeActionListener(
									ActionListener listener) {
		actionListener = 
			AWTEventMulticaster.remove(actionListener, listener);
    }
    private void createDisabledImage() {
		Assert.notNull(image);

        if(_bwFilter == null)
            _bwFilter = new BlackAndWhiteFilter();

        FilteredImageSource fis = 
            new FilteredImageSource(image.getSource(), 
                                    _bwFilter);

        Util.waitForImage(this, disabledImage=createImage(fis));
    }
    private Point findUpperLeft() {
        Dimension size = getSize();
		Dimension p = getPreferredSize();

        return new Point((size.width/2) - 
                         (p.width/2), 
                         (size.height/2) - 
                         (p.height/2));
    }
}
class BubbleThread extends Thread {
	ImageButton button;
	public BubbleThread(ImageButton button) {
		this.button = button;
	}
	public void run() {
		long    start = System.currentTimeMillis();
		boolean done = false;

		while(!done) {
			long delta = System.currentTimeMillis() - start;
			if(delta > button.getBubbleInterval()) {
				button.showBubbleHelp();
				done = true;
			}
		}
	}
}
