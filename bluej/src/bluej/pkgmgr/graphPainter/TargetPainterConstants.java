package bluej.pkgmgr.graphPainter;

import java.awt.*;

public class TargetPainterConstants
{
	private TargetPainterConstants(){}// prevent instantiation
	
	/**
	 * Colors used for shadows. 
	 * On white background Color(0,0,0,a) displayes as Color(255-a,255-a,255-a)
	 */
    public static final Color[] colours = {
            new Color(0,0,0,13),//on white background this is (242,242,242)
            new Color(0,0,0,44),//on white background this is (211,211,211)     
            new Color(0,0,0,66),//on white background this is (189,189,189)
            new Color(0,0,0,172)//on white background this is (83,83,83)
    };
    
    static final int TEXT_HEIGHT = 16;
    static final int TEXT_BORDER = 4;
    static final float alpha = (float)0.5;
     
}
