package bluej.guibuilder.graphics; 
import java.awt.*;

/**
 * A Panel containing a single component; an etched rectangle is 
 * drawn around the component, and a Label is centered at the top
 * of the rectangle. Of course, the single component may be
 * a container, and therefore a Box may surround many components.
 * <p>
 * 
 * Both the Component around which the box is drawn, and the 
 * String drawn at the top of the box are specified at 
 * construction time.<p>
 *
 * Etching of the box is controlled by etchedIn() and 
 * etchedOut().  Default etching is etched in.<p>
 *
 * @version 1.0, Apr 1 1996
 * @author  David Geary
 * @see     EtchedRectangle
 * @see     gjt.test.BoxTest
 */
public class Box extends Panel {
	static private Orientation _defaultOrientation =
					Orientation.CENTER;
    private EtchedRectangle box = new EtchedRectangle(this);
    private Label           titleLabel;
	private Orientation     orient;

    public Box(Component surrounded, String title) {
        this(surrounded, 
		     new Label(title, Label.CENTER),
			 _defaultOrientation);
    }
	public Box(Component   surrounded, 
	           String      title, 
			   Orientation orient) {
		this(surrounded, new Label(title, Label.CENTER), orient);
	}
    public Box(Component surrounded, Label label) {
		this(surrounded, label, _defaultOrientation);
	}
	public Box(Component   surrounded, 
	           Label       label, 
			   Orientation orient) {
        Assert.notNull(surrounded);
        Assert.notNull(label);

        titleLabel  = label;
		this.orient = orient;

        GridBagLayout      gbl = new GridBagLayout();
        GridBagConstraints gbc = new GridBagConstraints();

        setLayout(gbl);
        gbc.gridwidth = GridBagConstraints.REMAINDER;

		if(orient == Orientation.CENTER)
        	gbc.anchor = GridBagConstraints.NORTH;
		else if(orient == Orientation.RIGHT) {
			gbc.anchor = GridBagConstraints.NORTHEAST;
			gbc.insets = new Insets(0,0,0,5);
		}
		else if(orient == Orientation.LEFT) {
			gbc.anchor = GridBagConstraints.NORTHWEST;
			gbc.insets = new Insets(0,5,0,0);
		}

        gbl.setConstraints(titleLabel, gbc);
        add(titleLabel);

        gbc.insets  = new Insets(0,5,5,5);
        gbc.anchor  = GridBagConstraints.NORTHWEST;
        gbc.weighty = 1.0;
        gbc.weightx = 1.0;
        gbc.fill    = GridBagConstraints.BOTH;
        gbl.setConstraints(surrounded,gbc); 
        add(surrounded);
    }
    public void etchedIn ()           { box.etchedIn (); }
    public void etchedOut()           { box.etchedOut(); }

    public void paint(Graphics g) { 
		box.paint();     
		super.paint(g);  // ensure that lightweight components 
		                 // are painted
	}
	/**
	 * @deprecated for JDK1.1
	 */
	public void resize(int w, int h) {
        setBounds(getLocation().x, getLocation().y, w, h);
	}
    public void setSize(int w, int h) {
		resize(w,h);
    }
	/**
	 * @deprecated for JDK1.1
	 */
	public void reshape(int x, int y, int w, int h) {
        super.reshape(x,y,w,h);

        FontMetrics fm   = titleLabel.getFontMetrics(
                                titleLabel.getFont());
        int         top  = getInsets().top + fm.getAscent();
        Dimension   size = getSize();

        box.setBounds(0, top, size.width-1, size.height-top-1);
    }
    public void setBounds(int x, int y, int w, int h) {
		reshape(x,y,w,h);
	}
    protected String paramString() {
        return super.paramString() + ",etching=" +
        (box.isEtchedIn() ? Etching.IN : Etching.OUT) +
        ",title=" + titleLabel;
    }
}
