package bluej;

import javax.swing.filechooser.FileSystemView;
import javax.swing.text.JTextComponent;
import javax.swing.JTextField;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.metal.DefaultMetalTheme;
import java.util.Properties;
import java.awt.*;

import bluej.prefmgr.PrefMgr;

/**
 * Class to provide simple UI customisations such as colours and fonts.
 * Specifically created to allow access to default Fonts for user interface
 * components for i18n purposes.
 *
 * @author Bruce Quig
 * 
 * @version $Id: OldBluejTheme.java 879 2001-05-04 07:08:22Z bquig $
 */

public class BluejTheme extends DefaultMetalTheme
{

    private final FontUIResource controlFont = 
        new FontUIResource(PrefMgr.getStandardFont());
    private final FontUIResource systemFont = 
        new FontUIResource(controlFont);
    private final FontUIResource userFont = 
        new FontUIResource(controlFont);  
    private final FontUIResource menuFont = 
        new FontUIResource(PrefMgr.getStandardMenuFont());


    /**
     * name of theme
     */
    public String getName() 
    {  
        return "BluejTheme"; 
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
    
}

