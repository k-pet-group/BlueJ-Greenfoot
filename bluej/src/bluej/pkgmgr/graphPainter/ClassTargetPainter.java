package bluej.pkgmgr.graphPainter;

import java.awt.*;
import java.awt.Color;

import bluej.Config;
import bluej.pkgmgr.target.*;
import bluej.prefmgr.PrefMgr;
import bluej.utility.Utility;
/**
 * Paints a ClassTarget
 * @author fisker
 * @version $Id: ClassTargetPainter.java 2475 2004-02-10 09:53:59Z fisker $
 */

public class ClassTargetPainter
{
    private static final int HANDLE_SIZE = 20;
    private static final String STEREOTYPE_OPEN = "<<";
    private static final String STEREOTYPE_CLOSE = ">>";
    private static final Color textfg = Config.getItemColour("colour.text.fg");
    private static final Color colBorder = Config.getItemColour("colour.target.border");
    private static final Color compbg = Config.getItemColour("colour.target.bg.compiling");
    private static final Color stripeCol = Config.getItemColour("colour.target.stripes");
    private static final Image brokenImage = Config.getImageAsIcon("image.broken").getImage();
    
    private static final int TEXT_HEIGHT = GraphPainterStdImpl.TEXT_HEIGHT;
    private static final int TEXT_BORDER = GraphPainterStdImpl.TEXT_BORDER;
    private static final Color[] colours = GraphPainterStdImpl.colours;
    private static final AlphaComposite alphaComposite = GraphPainterStdImpl.alphaComposite;
    private static Composite oldComposite;
    
    /**
     * Construct the ClassTargetPainter 
     *
     */
    public ClassTargetPainter(){
    }
    
    public void paint(Graphics2D g, Target target) {
        ClassTarget classTarget = (ClassTarget) target;
        g.translate(classTarget.getX(), classTarget.getY());
        
        // draw the stationary class
        drawSkeleton(g, classTarget);
        drawUMLStyle(g, classTarget);
        drawWarnings(g, classTarget);
        drawRole(g, classTarget);
        g.translate(-classTarget.getX(), -classTarget.getY());
    }
    
    
    public void paintGhost(Graphics2D g, Target target){
        ClassTarget classTarget = (ClassTarget) target;
        oldComposite = g.getComposite();
        g.translate(classTarget.getGhostX(), classTarget.getGhostY());
        g.setComposite(alphaComposite);
        drawSkeleton(g, classTarget);
        drawUMLStyle(g, classTarget);
        drawWarnings(g, classTarget);
        drawRole(g, classTarget);
        g.setComposite(oldComposite); 
        g.translate(-classTarget.getGhostX(), -classTarget.getGhostY());
    }
    
    
    /**
     * Draw the Coloured rectangle, the shadow and the borders.
     *
     */
    private void drawSkeleton(Graphics2D g, ClassTarget classTarget)
    {
        g.setColor(getBackgroundColour(classTarget));
        g.fillRect(0, 0, classTarget.getWidth(), classTarget.getHeight());

        drawShadow(g, classTarget);
    }
    
    
    /**
     * Draw the stereotype, identifier name and the line beneath the identifier 
     * name. 
     */
    private void drawUMLStyle(Graphics2D g, ClassTarget classTarget)
    {
        // get the Stereotype
        String stereotype = classTarget.getRole().getStereotypeLabel();
        
        Font originalFont = getFont();
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

        // draw line beneath the stereotype and indentifiername. The UML-style
        g.setColor(getBorderColour());
        g.drawLine(0, currentTextPosY, classTarget.getWidth(), currentTextPosY);
    }
    
    
    /**
     * If the state of the class insn't normal, make it stripped.
     * Write warning if the sourcecode is missing. Display the "broken" image
     * if the sourcecode couldn't be parsed.
     */
    private void drawWarnings(Graphics2D g, ClassTarget classTarget){
        
        // If the state isn't normal, draw stripes in the rectangle
        String stereotype = classTarget.getRole().getStereotypeLabel();
        if(classTarget.getState() != ClassTarget.S_NORMAL) {
            g.setColor(stripeCol);
            int divider = (stereotype == null) ? 19 : 33;
            Utility.stripeRect(g, 0, divider  , classTarget.getWidth(), classTarget.getHeight() - divider, 8, 3);
        }
        
        g.setColor(getBorderColour());
        drawBorders(g, classTarget);

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
    private void drawRole(Graphics2D g, ClassTarget classTarget) {
        // If a role ever needs to add to the diagram, put the drawing code here
        // delegate extra functionality to role object
        //ClassRole role =  classTarget.getRole();
    }

    
    Color getBorderColour() {
        return colBorder;
    }

   
    Color getBackgroundColour(ClassTarget classTarget) {
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
    protected void drawBorders(Graphics2D g, ClassTarget classTarget)
    {
        int height = classTarget.getHeight();
        int width = classTarget.getWidth();
        int thickness = 1; // default thickness
        
        if (classTarget.isSelected()){
            thickness = 2; // thickness of borders when class is selected
            // Draw lines showing resize tag
            g.drawLine(width - HANDLE_SIZE - 2, height,	
                       width, height - HANDLE_SIZE - 2);
            g.drawLine(width - HANDLE_SIZE + 2, height, 
                       width, height - HANDLE_SIZE + 2);  
        }
        Utility.drawThickRect(g, 0, 0, width, height, thickness);
    }
    
    
    /**
     * Draw a 'shadow' appearance under and to the right of the target.
     */
    private void drawShadow(Graphics2D g, Target target)
    {// colorchange is expensive on mac, so draworder is by color, not position
        int height = target.getHeight();
        int width = target.getWidth();

        g.setColor(colours[3]);
        g.drawLine(3, height + 1, width , height + 1);//bottom
        
        g.setColor(colours[2]);
        g.drawLine(4, height + 2, width , height + 2);//bottom
        g.drawLine(width + 1, height + 2, width + 1, 3);//left
        
        g.setColor(colours[1]);
        g.drawLine(5, height + 3, width + 1, height + 3);//bottom
        g.drawLine(width + 2, height + 3, width + 2, 4);//left
        
        g.setColor(colours[0]);
        g.drawLine(6, height + 4, width + 2, height + 4 ); //bottom
        g.drawLine(width + 3, height + 3, width + 3, 5); // left 
    }
}
