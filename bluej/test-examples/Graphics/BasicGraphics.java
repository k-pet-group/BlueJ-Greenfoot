
package java.lancs ;

/**
 * Graphics objects for practical classes (Java 1.1 version)
 * @author Roger Garside/Richard Cardoe
 * @version Last Rewritten: 24/Sept/97
 */

import java.awt.* ;
import java.awt.event.* ;

/*
 * class to hold details about the shape to draw
 */
class BasicShape
    {
    // name of the shape - RECTANGLE, OVAL, etc.
    int shape ;
    // dimensions of the shape
    int x, y, w, h ;
    // colour of the shape
    Color colour ;

    // constructor to initialise the variables to default values
    public BasicShape()
        {
        shape = -1 ;
	x = -1 ;
	y = -1 ;
	w = -1 ;
	h = -1 ;
        colour = Color.green ;
        } // end of constructor method

    // constructor to initialise the variables to specifier values
    public BasicShape(int sh, int x1, int y1, int w1, int h1, Color col)
        {
        shape = sh ;
	x = x1 ;
	y = y1 ;
	w = w1 ;
	h = h1 ;
        colour = col ;
        } // end of constructor method
    } // end of class BasicShape


/*
 * a canvas to draw on
 */
class BasicCanvas extends Canvas
    {
    BasicGraphics parent ;

    // constructor method
    public BasicCanvas(BasicGraphics p)
	{
	parent = p ;
	} // end of constructor method

    // called when class is initialised to put window on the screen
    // or when window needs to be redrawn
    public void paint(Graphics g)
        {
        Dimension d = getSize() ;
        int cx = d.width / 2,
	    cy = d.height /2 ;
        g.setColor(Color.black) ;
        g.drawRect(1, 1, d.width - 3, d.height - 3) ;
	int yy = 25 ;
	while (yy < d.height)
	    {
	    if (yy % 100 == 0)
		{
	        g.drawLine(1, yy, 11, yy) ;
	        g.drawLine(d.width - 13, yy, d.width - 3, yy) ;
		}
	    else
		{
	        g.drawLine(1, yy, 6, yy) ;
	        g.drawLine(d.width - 8, yy, d.width - 3, yy) ;
		}
	    yy += 25 ;
	    }
	int xx = 25 ;
	while (xx < d.width)
	    {
	    if (xx % 100 == 0)
		{
	        g.drawLine(xx, 1, xx, 11) ;
	        g.drawLine(xx, d.height - 13, xx, d.height - 3) ;
		}
	    else
		{
	        g.drawLine(xx, 1, xx, 6) ;
	        g.drawLine(xx, d.height - 8, xx, d.height - 3) ;
		}
	    xx += 25 ;
	    }

        for (int i = 0 ; i < parent.noOfShapes ; i++)
            {
            g.setColor(parent.shapeList[i].colour) ;

            if (parent.shapeList[i].shape == BasicGraphics.RECTANGLE)
                {
	        g.drawRect(parent.shapeList[i].x, parent.shapeList[i].y, 
		           parent.shapeList[i].w, parent.shapeList[i].h) ;
                }
            else if (parent.shapeList[i].shape == BasicGraphics.FILLED_RECTANGLE)
                {
	        g.fillRect(parent.shapeList[i].x, parent.shapeList[i].y, 
		           parent.shapeList[i].w, parent.shapeList[i].h) ;
                }
            else if (parent.shapeList[i].shape == BasicGraphics.OVAL)
                {
	        g.drawOval(parent.shapeList[i].x, parent.shapeList[i].y, 
		           parent.shapeList[i].w, parent.shapeList[i].h) ;
                }
            else if (parent.shapeList[i].shape == BasicGraphics.FILLED_OVAL)
                {
	        g.fillOval(parent.shapeList[i].x, parent.shapeList[i].y, 
		           parent.shapeList[i].w, parent.shapeList[i].h) ;
                }
            else if ((parent.shapeList[i].shape == BasicGraphics.TRIANGLE) ||
	             (parent.shapeList[i].shape == BasicGraphics.FILLED_TRIANGLE))
		{
		int x1 = parent.shapeList[i].x ;
		int y1 = parent.shapeList[i].y ;
		int w1 = parent.shapeList[i].w ;
		int h1 = parent.shapeList[i].h ;

		Polygon p = new Polygon() ;
		p.addPoint(x1, y1 + h1) ;
		p.addPoint(x1 + w1, y1 + h1) ;
		p.addPoint(x1 + (w1 / 2), y1) ;
		p.addPoint(x1, y1 + h1) ;

                if (parent.shapeList[i].shape == BasicGraphics.TRIANGLE)
		    g.drawPolygon(p) ;
                else
		    g.fillPolygon(p) ;
		}
            }
        } // end of method paint

    } // end of class BasicCanvas


/*
 * class to draw simple shapes in a window
 */   
public class BasicGraphics extends Frame implements ActionListener
    {
    // maximum width of window
    private static final int MAX_WIDTH = 600 ;

    // maximum height of window
    private static final int MAX_HEIGHT = 400 ;

    /**
     * definition of a rectangle shape
     */
    public static final int RECTANGLE = 1 ;
  
    /**
     * definition of an oval shape
     */
    public static final int OVAL = 2 ;

    /**
     * definition of a triangle shape
     */
    public static final int TRIANGLE = 3 ;

    /**
     * definition of a filled-in rectangle
     */
    public static final int FILLED_RECTANGLE = 4 ;

    /**
     * definition of a filled-in oval
     */
    public static final int FILLED_OVAL = 5 ;

    /**
     * definition of a filled-in triangle
     */
    public static final int FILLED_TRIANGLE = 6 ;

    BasicShape[] shapeList = new BasicShape[50];

    int noOfShapes = 0;

    private BasicShape newShape = new BasicShape();

    private Button quit ;

    /**
     * constructor to lay out the window
     */
    public BasicGraphics()
        {
        setTitle("BasicGraphics Window") ;
        setSize(MAX_WIDTH, MAX_HEIGHT + 50) ;

        BasicCanvas c = new BasicCanvas(this) ;
        add("Center", c) ;  

        Panel p = new Panel() ;
        p.setLayout(new FlowLayout()) ;
        quit = new Button("Quit") ;
        p.add(quit) ;
        quit.addActionListener(this) ;
        add("South", p) ;  
        } // end of constructor method
  
    /**
     * handles button depression events, etc.
     */
    public void actionPerformed(ActionEvent event)
        {
        dispose() ;
        System.exit(0) ;
        } // end of method actionPerformed
      
    /**
     * set the type of shape that you want to draw
     * @param shape e.g. BasicGraphics.RECTANGLE
     */
    public void setShape(int shape)
        {
        if ((shape != RECTANGLE) && (shape != FILLED_RECTANGLE) &&
	    (shape != OVAL) && (shape != FILLED_OVAL) &&
	    (shape != TRIANGLE) && (shape != FILLED_TRIANGLE))
            {  
            System.err.println("This is not a valid shape");
            System.exit(1);
            }
        newShape.shape = shape ;
        } // end of method setShape

    /**
     * set the dimensions of the shape that you want to draw
     * @param x x-coordinate of the top left hand corner of the bounding
     *           rectangle
     * @param y y-coordinate of the top left hand corner of the bounding
     *           rectangle
     * @param w width of the bounding rectangle
     * @param h height of the bounding rectangle
     */
    public void setDimensions(int x, int y, int w, int h)
        {
        if (newShape.shape == -1)
            {  
            System.err.println("You need to set the shape first");
            System.exit(1);
            }
        if ((x < 5) || (y < 5) || (w < 5) || (h < 5) ||
            (x + w > MAX_WIDTH - 5) || (y + h > MAX_HEIGHT - 5))
            {
            System.err.println("Invalid dimensions supplied") ;
            System.exit(1);
	    }
        newShape.x = x ;
        newShape.y = y ;
        newShape.w = w ;
        newShape.h = h ;
        } // end of method setDimensions

    /**
     * set the colour of the shape that you want to draw
     * @param colour the Color type (Color.red, Color.blue, etc.)
     */
    public void setColour(Color colour)
        {
        if (newShape.x == -1)
            {
            System.err.println("You need to set the dimensions first");
            System.exit(1);
            }
        newShape.colour = colour ;
        shapeList[noOfShapes] = new BasicShape(newShape.shape,
					       newShape.x, newShape.y,
					       newShape.w, newShape.h,
					       newShape.colour) ;
        noOfShapes++ ;
        newShape = new BasicShape() ;
        } // end of method setColour


    /**
     * draws the window on the screen with the specified shapes
     */
    public void draw()
        {
        setVisible(true) ;
        } // end of method draw

    } // end of class BasicGraphics
