package bluej.pkgmgr.graphPainter;

import java.awt.*;
import java.awt.Graphics2D;

import bluej.Config;
import bluej.pkgmgr.target.*;
import bluej.pkgmgr.target.PackageTarget;
import bluej.prefmgr.PrefMgr;
import bluej.utility.Utility;

/**
 * Paints a packageTarget
 * @author fisker
 * @version $Id: PackageTargetPainter.java 2590 2004-06-11 11:29:14Z fisker $
 */
public class PackageTargetPainter
{
    private static final int TAB_HEIGHT = 12;
    private static final int HANDLE_SIZE = 20;
    private static final BasicStroke normalStroke = new BasicStroke(1);
    private static final BasicStroke selectedStroke = new BasicStroke(2);
    
    private static final Color defaultbg = Config.getItemColour("colour.package.bg.default");
    private static final Color shadowCol = Config.getItemColour("colour.target.shadow");
    private static final Color bordercolour = Config.getItemColour("colour.target.border");

    private static final int TEXT_HEIGHT = GraphPainterStdImpl.TEXT_HEIGHT;
    private static final int TEXT_BORDER = GraphPainterStdImpl.TEXT_BORDER;
    private static final Color[] colours = GraphPainterStdImpl.colours;
    private static final AlphaComposite alphaComposite = GraphPainterStdImpl.alphaComposite;
    private static Composite oldComposite;
    private int tabWidth;
    
    private GraphPainterStdImpl graphPainterStdImpl;
    
    /**
     * 
     */
    public PackageTargetPainter(GraphPainterStdImpl graphPainterStdImpl){
    	this.graphPainterStdImpl = graphPainterStdImpl;
    }
    
    public void paint(Graphics2D g, Target target){
        PackageTarget packageTarget = (PackageTarget) target;
        g.translate(packageTarget.getX(), packageTarget.getY());
        drawUMLStyle(g, packageTarget);
        g.translate(-packageTarget.getX(), -packageTarget.getY());
    }
    
    public void paintGhost(Graphics2D g, Target target){
        PackageTarget packageTarget = (PackageTarget) target;
        oldComposite = g.getComposite();
        g.translate(packageTarget.getGhostX(), packageTarget.getGhostY());
        g.setComposite(alphaComposite);
        drawUMLStyle(g, packageTarget);
        g.setComposite(oldComposite); 
        g.translate(-packageTarget.getGhostX(), -packageTarget.getGhostY());
    }

    private void drawUMLStyle(Graphics2D g, PackageTarget packageTarget)
    {
        tabWidth = packageTarget.getWidth() / 3;

        g.setColor(getBackgroundColour());
        g.fillRect(0, 0, tabWidth, TAB_HEIGHT);
        g.fillRect(0, TAB_HEIGHT, packageTarget.getWidth(), 
                	packageTarget.getHeight() - TAB_HEIGHT);

        g.setColor(shadowCol);
        drawShadow(g, packageTarget);

        g.setColor(getBorderColour());
        g.setFont(getFont(packageTarget));
        Utility.drawCentredText(g, packageTarget.getDisplayName(),
                TEXT_BORDER, TEXT_BORDER + TAB_HEIGHT,
                packageTarget.getWidth() - 2*TEXT_BORDER, TEXT_HEIGHT);
        drawUMLBorders(g, packageTarget);
    }

    private void drawUMLBorders(Graphics2D g, PackageTarget packageTarget)
    {
        boolean isSelected = packageTarget.isSelected() && graphPainterStdImpl.isGraphEditorInFocus();
        g.setStroke((isSelected ? selectedStroke : normalStroke));

        g.drawRect(0, 0, tabWidth, TAB_HEIGHT); //draw the tab
        g.drawRect(0, TAB_HEIGHT, packageTarget.getWidth(), 
                   packageTarget.getHeight() - TAB_HEIGHT); // draw Package

        if(!isSelected)
            return;

        g.setStroke(normalStroke);
        // Draw lines showing resize tag
        g.drawLine(packageTarget.getWidth() - HANDLE_SIZE - 2, packageTarget.getHeight(),
                packageTarget.getWidth(), packageTarget.getHeight() - HANDLE_SIZE - 2);
        g.drawLine(packageTarget.getWidth() - HANDLE_SIZE + 2, packageTarget.getHeight(),
                packageTarget.getWidth(), packageTarget.getHeight() - HANDLE_SIZE + 2);
    }

    
    private void drawShadow(Graphics2D g, PackageTarget packageTarget)
    {
        int height = packageTarget.getHeight();
        int width = packageTarget.getWidth();

        g.setColor(colours[3]);
        g.drawLine(3, height + 1, width , height + 1);//bottom
        
        g.setColor(colours[2]);
        g.drawLine(4, height + 2, width , height + 2);//bottom
        g.drawLine(width + 1, height + 2, width + 1, 3 + TAB_HEIGHT);//left
        g.drawLine(tabWidth + 1, 3, tabWidth + 1, TAB_HEIGHT);//tab
        
        g.setColor(colours[1]);
        g.drawLine(5, height + 3, width + 1, height + 3);//bottom
        g.drawLine(width + 2, height + 3, width + 2, 4 + TAB_HEIGHT);//left
        g.drawLine(tabWidth + 2, 4, tabWidth + 2, TAB_HEIGHT);//tab
        
        g.setColor(colours[0]);
        g.drawLine(6, height + 4, width + 2, height + 4 ); //bottom
        g.drawLine(width + 3, height + 3, width + 3, 5 + TAB_HEIGHT); // left
        g.drawLine(tabWidth + 3, 5, tabWidth + 3, TAB_HEIGHT); // tab
    } 
    
    Color getBackgroundColour()
    {
        return defaultbg;
    }
    
    Color getBorderColour()
    {
        return bordercolour;
    }
    
    Font getFont(PackageTarget packageTarget)
    {
        return (packageTarget.getState() == PackageTarget.S_INVALID) ? PrefMgr.getStandoutFont() : PrefMgr.getStandardFont();
    }
    
    
}
