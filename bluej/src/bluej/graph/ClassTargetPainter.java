package bluej.graph;

import java.awt.*;
import java.awt.Color;

import bluej.Config;
import bluej.pkgmgr.target.*;
import bluej.prefmgr.PrefMgr;
import bluej.utility.Utility;


public class ClassTargetPainter 
{
    Graphics2D g;
    static final int TEXT_HEIGHT = 16;
    static final int TEXT_BORDER = 4;
    static final int SHAD_SIZE = 4;
    static final int HANDLE_SIZE = 20;
    private static final Color textfg = Config.getItemColour("colour.text.fg");
    private static final Color colBorder = Config.getItemColour("colour.target.border");
    private static final Color compbg = Config.getItemColour("colour.target.bg.compiling");
    private static final Color stripeCol = Config.getItemColour("colour.target.stripes");
    static final Color shadowCol = Config.getItemColour("colour.target.shadow");
    private static final String STEREOTYPE_OPEN = "<<";
    private static final String STEREOTYPE_CLOSE = ">>";
    private static final Image brokenImage =
        Config.getImageAsIcon("image.broken").getImage();
    ClassTarget classTarget;
    private AlphaComposite alphaComposite;
    private Composite oldComposite;
    static final float alpha = (float)0.5;
    
    public ClassTargetPainter(){
        this.alphaComposite = 
            AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha);
    }
    
    public void paint(Graphics2D g, ClassTarget classTarget){
        this.classTarget = classTarget;
        this.g = g;
        g.translate(classTarget.getX(), classTarget.getY());
        
        // draw the stationary class
        drawSkeleton();
        drawUMLStyle();
        drawWarnings();
        drawRole();
        // if the class is being dragged, draw the moving class as a ghost
        g.translate(-classTarget.getX(), -classTarget.getY());
    }
    
    public void paintGhost(Graphics2D g, ClassTarget classTarget){
        this.classTarget = classTarget;
        this.g = g;
        oldComposite = g.getComposite();
        g.translate(classTarget.getGhostX(), classTarget.getGhostY());
        g.setComposite(alphaComposite);
        drawSkeleton();
        drawUMLStyle();
        drawWarnings();
        drawRole();
        g.setComposite(oldComposite); 
        g.translate(-classTarget.getGhostX(), -classTarget.getGhostY());
    }
    
    /**
     * Draw the Coloured rectangle, the shadow and the borders.
     *
     */
    private void drawSkeleton()
    {
        g.setColor(getBackgroundColour());
        g.fillRect(0, 0, classTarget.getWidth(), classTarget.getHeight());

        g.setColor(shadowCol);
        drawShadow();

        g.setColor(getBorderColour());
        //drawBorders();
    }
    
    
    /**
     * Draw the stereotype, identifier name and the line beneath the identifier 
     * name. 
     */
    private void drawUMLStyle()
    {
        // get the Stereotype
        String stereotype = classTarget.getRole().getStereotypeLabel();
        
        Font originalFont = getFont();
        Color originalColor = g.getColor();
        g.setColor(getTextColour());
        int currentTextPosY = 2;

        // draw stereotype if applicable
        if(stereotype != null) {

            String stereotypeLabel = STEREOTYPE_OPEN + stereotype + STEREOTYPE_CLOSE;
            Font stereotypeFont = originalFont.deriveFont((float)(originalFont.getSize() - 2));
            g.setFont(stereotypeFont);
            Utility.drawCentredText(g, stereotypeLabel, TEXT_BORDER, currentTextPosY,
                    classTarget.getWidth() - 2 * TEXT_BORDER, TEXT_HEIGHT);
            currentTextPosY += TEXT_HEIGHT - 2;
        }
        
        g.setFont(originalFont);

        
        // draw the identifiername of the class
        Utility.drawCentredText(g, classTarget.getIdentifierName(), TEXT_BORDER,
                				currentTextPosY, 
                				classTarget.getWidth() - 2 * TEXT_BORDER,
                				TEXT_HEIGHT);
        currentTextPosY += TEXT_HEIGHT;
        g.setColor(originalColor);
        // draw line beneath the stereotype and indentifiername. The UML-style
        g.drawLine(0, currentTextPosY, classTarget.getWidth(), currentTextPosY);
    }
    
    
    /**
     * If the state of the class insn't normal, make it stripped.
     * Write warning if the sourcecode is missing. Display the "broken" image
     * if the sourcecode couldn't be parsed.
     */
    private void drawWarnings(){
        
        // If the state isn't normal, draw stripes in the rectangle
        String stereotype = classTarget.getRole().getStereotypeLabel();
        if(classTarget.getState() != ClassTarget.S_NORMAL) {
            g.setColor(stripeCol);
            int divider = (stereotype == null) ? 18 : 32;
            Utility.stripeRect(g, 0, divider, classTarget.getWidth(), classTarget.getHeight() - divider, 8, 3);
        }
        
        g.setColor(getBorderColour());
        drawBorders();

        // if sourcecode is missing. Write "(no source)" in the diagram
        if(!classTarget.hasSourceCode()) {
            g.setColor(getTextColour());
            g.setFont(getFont().deriveFont((float)(getFont().getSize() - 2)));
            Utility.drawCentredText(g, "(no source)", TEXT_BORDER, 
                    			    classTarget.getHeight() - 18,
                    			    classTarget.getWidth() - 2 * TEXT_BORDER,
                    			    TEXT_HEIGHT);
        }
        // if the sourcecode is invalid, display the "broken" image in diagram
        else if(!classTarget.getSourceInfo().isValid()) {
            g.drawImage(brokenImage, TEXT_BORDER, classTarget.getHeight() - 22, null);
        }
    }
 
    
    /**
     * Let the role object of the class draw.
     */
    private void drawRole() {
        // If a role ever needs to add to the diagram, put the drawing code here
        // delegate extra functionality to role object
        //ClassRole role =  classTarget.getRole();
    }

    
    Color getBorderColour() {
        return colBorder;
    }

   
    Color getBackgroundColour() {
        Color bg;
        if(classTarget.getState() == ClassTarget.S_COMPILING) {
            return compbg;
        } else {
            return classTarget.getRole().getBackgroundColour();
        }
    }
    
    Color getTextColour()
    {
        return textfg;
    }

    Font getFont()
    {
        return PrefMgr.getTargetFont();
    }
    
    /**
     * Draw the borders of this target.
     */
    protected void drawBorders()
    {
        int thickness = 1; // default thickness
        if (classTarget.isSelected()){
            thickness = 2; // thickness of borders when class is selected
            // Draw lines showing resize tag
            g.drawLine(classTarget.getWidth() - HANDLE_SIZE - 2,
                    classTarget.getHeight(),	classTarget.getWidth(), 
                    classTarget.getHeight() - HANDLE_SIZE - 2);
            g.drawLine(classTarget.getWidth() - HANDLE_SIZE + 2, 
                       classTarget.getHeight(), classTarget.getWidth(),
                       classTarget.getHeight() - HANDLE_SIZE + 2);  
        }
        Utility.drawThickRect(g, 0, 0, classTarget.getWidth(),
                			  classTarget.getHeight(), thickness);
    }
    
    /**
     * Draw a 'shadow' appearance under and to the right of the target.
     */
    protected void drawShadow()
    {
        int shadSize = SHAD_SIZE + (classTarget.isSelected() ? 1 : 0);
        //bottom rectangle
        g.fillRect(shadSize, classTarget.getHeight(), 
                   classTarget.getWidth(), shadSize  );
        //right rectangle
        g.fillRect(classTarget.getWidth(), shadSize, 
                shadSize, classTarget.getHeight());
    }
}
