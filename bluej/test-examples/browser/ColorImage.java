import java.applet.Applet;
import java.awt.image.*;
import java.awt.*;

/**
 * An applet to perform real time color cycling on an image.
 *
 * This source code has been made available for instructional
 * purposes.  Please do not abuse my good will by trying to claim
 * it as your own.
 *
 * @author  Jason Marshall
 * @version 1.0 17 May 1996
 */

public class ColorImage extends Panel implements Runnable {

    /**
     *Thread controlling the morph.
     **/

    Thread controlThread = null;

    /**
     * The image actually drawn
     **/
    Image morphedImage;

    /**
     * The original image
     **/
    Image original;

    MediaTracker tracker;

    // Pixel stores
    int pixels[];
    int sourcePixels[];

    PixelGrabber grabber;

    /**
     * color channel modifiers for the color morphing filters.
     **/

    int red, green, blue;
    int redInc = 2;
    int greenInc = 3;
    int blueInc = 4;

    /**
     * Storage for each color channel of the original image.  Used in constructing
     * the new pixels
     **/

    int redPixels[];
    int bluePixels[];
    int greenPixels[];
    int alphaPixels[];

    /**
     * location of the image
     **/

    String imgName;

    /**
     * Window height and width
     **/

    int height;
    int width;

    int delay;

    /**
     * Performs a 'color safe' (does't over- or under-saturate the pixel
     * components) color alteration on the pixels of the source image and
     * store the values in the pixel buffer.
     **/

    void colorMorphPixels() {
	int w, h, i, a, r, g, b;
	for (w = 0, i = 0; w < width; w++) {
	    for (h = 0; h < height; h++, i++) {
		r = redPixels[i] + red;
		if ((r & 0xffffff00) != 0)
		    r = (Math.max(Math.min(r, 255), 0));
		g = greenPixels[i] + green;
		if ((g & 0xffffff00) != 0)
		    g = (Math.max(Math.min(g, 255), 0));
		b = bluePixels[i] + blue;
		if ((b & 0xffffff00) != 0)
		    b = (Math.max(Math.min(b, 255), 0));
		a = 0xff;
		pixels[i] = ((a << 24)
			    | (r << 16) | (g << 8) | b);
		}
	    }
	}



    public Dimension getPreferredSize()
    { Dimension mysize = new Dimension( 0, 0 );
	if (morphedImage != null) {
          mysize.height = morphedImage.getHeight( this );
          mysize.width = morphedImage.getWidth( this );

        }
       
      return mysize;
    }

    public ColorImage( String image, int delayTime ) {
	
	if ( image != null) {
	    imgName = image;
	    }
	else
	    imgName = "text.gif";

	delay = delayTime;
	}

    /**
     * Calculate the next color offset to use
     **/

    void nextColorIncriment() {
	if ((red > 64) || (red < -32))
	    redInc = -redInc;
	red += redInc;
	if ((green > 64) || (green < -32))
	    greenInc = -greenInc;
	green += greenInc;
	if ((blue > 64) || (blue < -32))
	    blueInc = -blueInc;
	blue += blueInc;
	}


    public void paint(Graphics g) {
	if (morphedImage != null) {
	    synchronized (morphedImage) {
		if (morphedImage != null)
		    g.drawImage(morphedImage, 0, 0, this);
		}
	    }
	}

    public void run() {
        if (original == null) {
            // Acquire the desired image
            //            original = getImage(getDocumentBase(), imgName);
            	original = Toolkit.getDefaultToolkit().getImage( imgName );
            // wait for it to arrive
            tracker = new MediaTracker(this);
            tracker.addImage(original, 0);
            original.getHeight(this);
            try {
                tracker.waitForID(0);
            	}
            catch (InterruptedException e) {
                return;
            }

            // establish pixel buffer for image manipulations
            height = size().height;
            width = size().width;
            pixels = new int[width * height];
            sourcePixels = new int[width * height];
            redPixels = new int[width * height];
            greenPixels = new int[width * height];
            bluePixels = new int[width * height];
            alphaPixels = new int[width * height];

        	    ColorModel cm = ColorModel.getRGBdefault();

            grabber = new PixelGrabber(original, 0, 0,
                                Math.min(width, original.getWidth(this)),
                                Math.min(height, original.getHeight(this)),
                                sourcePixels, 0, width);
            try {
                grabber.grabPixels();
            		}
            catch (InterruptedException e) {
                return;
            }
            if ((grabber.status() & ImageObserver.ABORT) != 0) {
                System.err.println("image fetch aborted");
                return;
		            }

	    int n = width * height;
	    for (int i = 0; i < n; i++) {
		redPixels[i] = cm.getRed(sourcePixels[i]);
		greenPixels[i] = cm.getGreen(sourcePixels[i]);
		bluePixels[i] = cm.getBlue(sourcePixels[i]);
		alphaPixels[i] = cm.getAlpha(sourcePixels[i]);
		}
	    }

	// create the MemoryImageSource associated with the pixel buffer
	MemoryImageSource imageSource = new MemoryImageSource(width, height,
					ColorModel.getRGBdefault(), pixels,
					0, width);

	while (controlThread != null) {
	    // do any setup for new image creation.
	    nextColorIncriment();

	    // create the next image
	    colorMorphPixels();
	    morphedImage = createImage(imageSource);

	    // request that it be drawn.
	    repaint();
	    try {
		controlThread.sleep(delay);
		}
	    catch (InterruptedException ie) {
		}
	    }
	}

    public void stop() {
	if (controlThread != null) {
	    controlThread.stop();
	    controlThread = null;
	    }
	}

    public void start() {
	if (controlThread == null) {
	    controlThread = new Thread(this);
	    controlThread.setPriority(Thread.NORM_PRIORITY - 1);
	    controlThread.start();
	    }
	}

    public void update(Graphics g) {
	paint(g);
	}


    public static void main( String args[] )
    { Frame f = new Frame();
       f.setSize( 300, 300 );
      ColorImage ci = new ColorImage( "Logo.gif", 140 );
      ci.setSize( 50, 50 );
      f.add( ci );
      ci.start();
      f.setVisible( true );
    }
}
