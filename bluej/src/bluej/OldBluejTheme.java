package bluej;

import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.metal.DefaultMetalTheme;

import bluej.prefmgr.PrefMgr;

/**
 * Class to provide simple UI customisations such as colours and fonts.
 * Specifically created to allow access to default Fonts for user interface
 * components for i18n purposes.
 *
 * @author Bruce Quig
 * 
 * @version $Id: OldBluejTheme.java 1700 2003-03-13 03:34:20Z ajp $
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

