package bluej;

import java.awt.*;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.metal.DefaultMetalTheme;

import bluej.prefmgr.PrefMgr;

/**
 * Class to provide simple UI customisations such as colours and fonts.
 * Specifically created to allow access to default Fonts for user interface
 * components for i18n purposes.
 *
 * @author  Bruce Quig
 * @version $Id: BlueJTheme.java 1954 2003-05-15 06:06:01Z ajp $
 */
public class BlueJTheme extends DefaultMetalTheme
{
    private final FontUIResource controlFont = 
        new FontUIResource(PrefMgr.getStandardFont());
    private final FontUIResource systemFont = 
        new FontUIResource(controlFont);
    private final FontUIResource userFont = 
        new FontUIResource(controlFont);  
    private final FontUIResource menuFont = 
        new FontUIResource(PrefMgr.getStandardMenuFont());

	// icon to be used for BlueJ windows
	private static Image iconImage = null;

	// common strings - must be accessed through getOkLabel()
	private static String okayLabel;
	private static String cancelLabel;
	private static String continueLabel;

	// a dimension for ok and cancel buttons that is as large as
	// needed to display either
	private static Dimension okCancelDimension;
	
	// JSplitPane divider width constant
	public static final int splitPaneDividerWidth = 3;

	// Other general spacing constants. We should try to use these for consistency
	public static final int generalSpacingWidth = 5;

	public static final Border generalBorder =
	    BorderFactory.createEmptyBorder(10,10,10,10);

	public static final Border generalBorderWithStatusBar =
	    BorderFactory.createEmptyBorder(10,10,0,10);

	public static final Border dialogBorder =
	    BorderFactory.createEmptyBorder(12,12,11,11);

	public static final int commandButtonSpacing = 5;
	public static final int commandButtonPadding = 12;

	public static final int componentSpacingSmall = 5;
	public static final int componentSpacingLarge = 11;

	public static final int dialogCommandButtonsVertical = 17;

    /**
     * Name of theme
     */
    public String getName() 
    {  
        return "BlueJTheme"; 
    } 

    public FontUIResource getControlTextFont() 
    {  
        return controlFont;
    }
 
    public FontUIResource getSystemTextFont() 
    {  
        return systemFont;
    }
 
    public FontUIResource getUserTextFont() 
    {  
        return userFont;
    }
 
    public FontUIResource getMenuTextFont() 
    {  
        return menuFont;
    }
      
	/**
	 * Get the icon for most BlueJ frames.
	 * 
	 * @return	an icon to be used as the frame icon for most BlueJ windows
	 */
	public static Image getIconImage()
	{
		if (iconImage == null)
			iconImage = Config.getImageAsIcon("image.icon").getImage();

		return iconImage;
	}

	public static String getOkLabel()
	{
		if (okayLabel == null) {
			okayLabel = Config.getString("okay");
		}
		return okayLabel;
	}

	public static String getCancelLabel()
	{
		if (cancelLabel == null) {
			cancelLabel = Config.getString("cancel");
		}
		return cancelLabel;
	}
	
	public static String getContinueLabel()
	{
		if (continueLabel == null) {
			continueLabel = Config.getString("continue");
		}
		return continueLabel;
	}

	/**
	 * Get a standard BlueJ "ok" button.
	 * 
	 * @return	A JButton that says "ok"
	 */
	public static JButton getOkButton()
	{
		computeButtonWidths();
		
		JButton okButton = new JButton(getOkLabel());
		// try to make the OK, cancel and continue buttons have equal size
		okButton.setPreferredSize(okCancelDimension);
		return okButton;
	}
	
	/**
	 * Get a standard BlueJ "cancel" button.
	 * 
	 * @return	A JButton that says "cancel"
	 */
	public static JButton getCancelButton()
	{
		computeButtonWidths();

		JButton cancelButton = new JButton(getCancelLabel());
		// try to make the OK, cancel and continue  buttons have equal size
		cancelButton.setPreferredSize(okCancelDimension);
		return cancelButton;	
	}

	/**
	 * Get a standard BlueJ "continue" button.
	 * 
	 * @return	A JButton that says "Continue"
	 */
	public static JButton getContinueButton()
	{
		computeButtonWidths();

		JButton continueButton = new JButton(getContinueLabel());
		// try to make the OK, cancel and continue  buttons have equal size
		continueButton.setPreferredSize(okCancelDimension);
		return continueButton;	
	}

	/**
	 * Computer the maximum width of the ok, cancel and continue buttons
	 * and set the okCancelDimension to be representative of that size.
	 */
	private static void computeButtonWidths()
	{
		if (okCancelDimension != null)
			return;
		
		JButton okButton = new JButton(getOkLabel());
		JButton cancelButton = new JButton(getCancelLabel());
		JButton continueButton = new JButton(getContinueLabel());
		
		int maxWidth = Math.max(cancelButton.getPreferredSize().width,
									okButton.getPreferredSize().width);
		maxWidth = Math.max(maxWidth,
								continueButton.getPreferredSize().width);
									
		okCancelDimension = new Dimension(maxWidth, okButton.getPreferredSize().height);
	}
}
