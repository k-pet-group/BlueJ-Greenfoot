package bluej.graph;

import java.awt.*;
import java.awt.Graphics2D;

import bluej.Config;
import bluej.pkgmgr.target.PackageTarget;
import bluej.prefmgr.PrefMgr;
import bluej.utility.Utility;


public class PackageTargetPainter
{
    PackageTarget packageTarget;
    private int tabWidth;
    static final int TAB_HEIGHT = 12;
    static final Color defaultbg = Config.getItemColour("colour.package.bg.default");
    static final Color shadowCol = Config.getItemColour("colour.target.shadow");
    static final Color bordercolour = Config.getItemColour("colour.target.border");
    static final int HANDLE_SIZE = 20;
    static final int TEXT_HEIGHT = 16;
    static final int TEXT_BORDER = 4;
    static final BasicStroke normalStroke = new BasicStroke(1);
    static final BasicStroke selectedStroke = new BasicStroke(2);
    static final int SHAD_SIZE = 4;
    static final float alpha = (float)0.5;
    private AlphaComposite alphaComposite;
    private Composite oldComposite;
    /**
     * 
     */
    public PackageTargetPainter() {
        this.alphaComposite = 
            AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha);
    }
    
    public void paint(Graphics2D g, PackageTarget packageTarget){
        this.packageTarget = packageTarget;
        g.translate(packageTarget.getX(), packageTarget.getY());
        drawUMLStyle(g);
        g.translate(-packageTarget.getX(), -packageTarget.getY());
    }
    
    public void paintGhost(Graphics2D g, PackageTarget packageTarget){
        this.packageTarget = packageTarget;
        oldComposite = g.getComposite();
        g.translate(packageTarget.getGhostX(), packageTarget.getGhostY());
        g.setComposite(alphaComposite);
        drawUMLStyle(g);
        g.setComposite(oldComposite); 
        g.translate(-packageTarget.getGhostX(), -packageTarget.getGhostY());
    }

    private void drawUMLStyle(Graphics2D g)
    {
        tabWidth = packageTarget.getWidth() / 3;

        g.setColor(getBackgroundColour());
        //g.fillRect(0, 0, width, height);
        g.fillRect(0, 0, tabWidth, TAB_HEIGHT);
        g.fillRect(0, TAB_HEIGHT, packageTarget.getWidth(), 
                	packageTarget.getHeight() - TAB_HEIGHT);

        g.setColor(shadowCol);
        drawShadow(g);

        g.setColor(getBorderColour());
        g.setFont(getFont());
        Utility.drawCentredText(g, packageTarget.getDisplayName(),
                TEXT_BORDER, TEXT_BORDER + TAB_HEIGHT,
                packageTarget.getWidth() - 2*TEXT_BORDER, TEXT_HEIGHT);
        drawUMLBorders(g);
    }

    void drawUMLBorders(Graphics2D g)
    {
        if(packageTarget.isSelected())
            g.setStroke(selectedStroke);

        g.drawRect(0, 0, tabWidth, TAB_HEIGHT);
        g.drawRect(0, TAB_HEIGHT, packageTarget.getWidth(), packageTarget.getHeight() - TAB_HEIGHT);

        if(!packageTarget.isSelected())
            return;

        g.setStroke(normalStroke);
        // Draw lines showing resize tag
        g.drawLine(packageTarget.getWidth() - HANDLE_SIZE - 2, packageTarget.getHeight(),
                packageTarget.getWidth(), packageTarget.getHeight() - HANDLE_SIZE - 2);
        g.drawLine(packageTarget.getWidth() - HANDLE_SIZE + 2, packageTarget.getHeight(),
                packageTarget.getWidth(), packageTarget.getHeight() - HANDLE_SIZE + 2);
    }

    public void drawShadow(Graphics2D g)
    {
        g.fillRect(SHAD_SIZE, packageTarget.getHeight(),
                  packageTarget.getWidth(), SHAD_SIZE);
        g.fillRect(packageTarget.getWidth(), SHAD_SIZE + TAB_HEIGHT, SHAD_SIZE, 
                   packageTarget.getHeight() - TAB_HEIGHT);
    }
    
    Color getBackgroundColour()
    {
        return defaultbg;
    }
    
    Color getBorderColour()
    {
        return bordercolour;
    }
    
    Font getFont()
    {
        return (packageTarget.getState() == PackageTarget.S_INVALID) ? PrefMgr.getStandoutFont() : PrefMgr.getStandardFont();
    }
    
    
}
