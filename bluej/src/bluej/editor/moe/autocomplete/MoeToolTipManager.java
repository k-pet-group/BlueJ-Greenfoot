package bluej.editor.moe.autocomplete;

import javax.swing.*;
import java.awt.*;


/**
 * This class is used to control the showing hiding and updating
 * of a MoeToolTip.  We couldn't use the standard ToolTipManager
 * because that works by detecting mouse events and hides tool
 * tips after a certain amount of time.  We specifically need
 * to show and hide a tool tip at a given moment of time. The
 * class also controls the arguments of the current tool tip
 * and the bold argument can be specified.
 *
 * @version 1.2 (10/03/2003)
 *
 * @author Chris Daly (<A href="mailto:cdd1@ukc.ac.uk">cdd1@ukc.ac.uk</A>),
 *         Darren Link (<A href="mailto:drl1@ukc.ac.uk">drl1@ukc.ac.uk</A>),
 *         Mike Stewart (<A href="mailto:mjs7@ukc.ac.uk">mjs7@ukc.ac.uk</A>)
 */
public class MoeToolTipManager{

    private MoeToolTip tip;
    transient Popup tipWindow;

    private JComponent comp;

    private Rectangle popupRect = null;
    private Rectangle popupFrameRect = null;

    private boolean tipShowing = false;

    private Point screenLocation;
    private String[] arguments;
    private String returnType;
    private int boldArgument = -1;


    private int fontSize = -1;
    private double toolTipHeight = -1;

    /**
     * This constructs a MoeToolTipManager that attempts
     * to show tool tips within the given component.  The
     * font size of the tool tip can be specified using an
     * HTML font size.
     *
     * @param comp the component the tool tip should be displayed in
     * @param htmlFontSize the HTML font size of the text in the tool tip.
     */
    public MoeToolTipManager(JComponent comp, int htmlFontSize){
        this.comp = comp;
        setFontSize(htmlFontSize);
    }

    /**
     * This sets the font size and makes
     * sure that it is at least 1.
     *
     * @param htmlFontSize the HTML font size of the text in the tool tip.
     */
    private void setFontSize(int htmlFontSize){
        if(htmlFontSize<1){
            this.fontSize = 1;
        }
        else{
            this.fontSize = htmlFontSize;
        }
    }

    /**
     * This method determines the height of the tool tips
     * that will be displayed.
     *
     * @return the height of a single line tool tip.
     */
    public int getToolTipHeight(){
        if(toolTipHeight < 0){
            MoeToolTip tempTip = new MoeToolTip();
            tempTip.setTipText("<FONT size=" + fontSize +
                               ">Test Height Text</FONT>");
            Dimension size = tempTip.getPreferredSize();
            toolTipHeight = size.getHeight();
        }

        int h = (int) toolTipHeight;
        return h;

    }

    /**
     * This method can be used to determine
     * whether a MoeToolTip is visible
     */
    public boolean toolTipVisible(){
        return (tipWindow != null);
    }


    /**
     * This method shows a MoeToolTip at a given screenLocation.
     * The arguments, return type and bold argument can be specified.
     *
     * @param screenLocation the screen location to show the tool tip
     * @param arguments an array of Strings for the arguments section
     *                  of the tool tip.
     * @param returnType the return type of the method
     * @param boldArg the index of the bold argument. 0 is the first argument
     */
    public void showToolTip(Point screenLocation,
                            String[] arguments,
                            String returnType,
                            int boldArg){

        setScreenLocation(screenLocation);
        setArguments(arguments);
        setReturnType(returnType);

        int oldBoldArgument = boldArgument;
        boldArgument = boldArg;

        boolean boldArgumentChanged =  (boldArgument != oldBoldArgument);

        if(tipWindow==null || boldArgumentChanged){
            String text = createTipText(boldArgument);
            showTip(screenLocation, text);
        }
    }

    /**
     * This hides the currently visible tool tip
     */
    public void hideToolTip() {
        if (tipWindow != null) {

            tipWindow.hide();
            tipWindow = null;
            tipShowing = false;

            tip.getUI().uninstallUI(tip);
            tip = null;
        }
    }


    /**
     * This sets the bold argument of the tool tip.
     *
     * @param boldArg the index of the bold argument to be highlighted.
     */
    public void setBoldArgument(int boldArg){
        showToolTip(screenLocation, arguments, returnType, boldArg);
    }



    private void setScreenLocation(Point screenLocation){
        this.screenLocation = screenLocation;
    }

    private void setArguments(String[] arguments){
        this.arguments = arguments;
    }

    private void setReturnType(String returnType){
        this.returnType = returnType;
    }



    private String createTipText(int boldArg){
        StringBuffer tipText = new StringBuffer();
        tipText.append("<FONT size=" + fontSize + ">");
        tipText.append("(");
        if(arguments.length > 0){

            StringBuffer args = new StringBuffer();

            for(int i=0; i<arguments.length; i++){

                if(i==boldArg){
                    args.append("<B>");
                    args.append(arguments[i]);
                    args.append("</B>");
                }
                else{
                    args.append(arguments[i]);
                }

                if(i < arguments.length-1){
                    args.append(", ");
                }
            }

            tipText.append(args);
        }
        else{
            tipText.append(" No arguments required ");
        }

        tipText.append(") ");
        tipText.append(returnType);
        tipText.append("</FONT>");

        return tipText.toString();
    }









// ----------------------------------------------------------------------------
// The remaining methods have been taken from the source of the standard tool
// tip manager.  It was not practical to extend the standard tool tip.


    private void showTip(Point screenLocation, String tipText) {

        setArguments(arguments);
        Dimension size;
        Point location = new Point();
        Rectangle sBounds = comp.getGraphicsConfiguration().getBounds();

        // Hide the existing tip if one is already shown
        hideToolTip();

        tip = new MoeToolTip ();
        tip.setTipText(tipText);
        size = tip.getPreferredSize();

        location.x = screenLocation.x;
        location.y = screenLocation.y;

        if (location.x + size.width > sBounds.x + sBounds.width) {
            location.x -= size.width;
        }

        if (location.y + size.height > sBounds.y + sBounds.height) {
            location.y -= (size.height + 20);
        }

        if (popupRect == null) popupRect = new Rectangle();

        popupRect.setBounds(location.x,
                            location.y,
                            size.width,
                            size.height);

        int y = getPopupFitHeight(popupRect, comp);
        int x = getPopupFitWidth(popupRect, comp);

        if (y > 0){
            location.y -= y;
        }
        if (x > 0){
              location.x -= x;
        }

        PopupFactory popupFactory = PopupFactory.getSharedInstance();

            tipWindow = popupFactory.getPopup(comp, tip,
                          location.x,
                          location.y);

        tipWindow.show();
        tipShowing = true;

    }



    // Returns: 0 no adjust
    //         -1 can't fit
    //         >0 adjust value by amount returned

    private int getPopupFitWidth(Rectangle popupRectInScreen, Component invoker){
        if (invoker != null){
            Container parent;
            for (parent = invoker.getParent(); parent != null; parent = parent.getParent()){
                // fix internal frame size bug: 4139087 - 4159012
                if( parent instanceof JFrame ||
                    parent instanceof JDialog ||
                    parent instanceof JWindow) { // no check for awt.Frame since we use Heavy tips

                    return getWidthAdjust(parent.getBounds(),popupRectInScreen);
                }
                else if (parent instanceof JApplet || parent instanceof JInternalFrame) {
                    if (popupFrameRect == null){
                        popupFrameRect = new Rectangle();
                    }
                    Point p = parent.getLocationOnScreen();
                    popupFrameRect.setBounds(p.x,
                                             p.y,
                                             parent.getBounds().width,
                                             parent.getBounds().height);
                    return getWidthAdjust(popupFrameRect,popupRectInScreen);
                }
            }
        }
        return 0;
    }




    // Returns:  0 no adjust
    //          >0 adjust by value return
    private int getPopupFitHeight(Rectangle popupRectInScreen, Component invoker){
        if (invoker != null){
            Container parent;
            for (parent = invoker.getParent(); parent != null; parent = parent.getParent()){
                if( parent instanceof JFrame ||
                    parent instanceof JDialog ||
                    parent instanceof JWindow) {

                    return getHeightAdjust(parent.getBounds(),popupRectInScreen);
                }
                else if (parent instanceof JApplet || parent instanceof JInternalFrame) {
                    if (popupFrameRect == null){
                        popupFrameRect = new Rectangle();
                    }
                    Point p = parent.getLocationOnScreen();
                    popupFrameRect.setBounds(p.x,
                                             p.y,
                                             parent.getBounds().width,
                                             parent.getBounds().height);
                    return getHeightAdjust(popupFrameRect,popupRectInScreen);
                }
            }
        }
        return 0;
    }


    private int getHeightAdjust(Rectangle a, Rectangle b){
        if (b.y >= a.y && (b.y + b.height) <= (a.y + a.height)){
            return 0;
        }
        else{
            return (((b.y + b.height) - (a.y + a.height)) + 5);
        }
    }


    // Return the number of pixels over the edge we are extending.
    // If we are over the edge the ToolTipManager can adjust.
    // REMIND: what if the Tool tip is just too big to fit at all - we currently will just clip
    private int getWidthAdjust(Rectangle a, Rectangle b){
        if (b.x >= a.x && (b.x + b.width) <= (a.x + a.width)){
            return 0;
        }
        else{
            return (((b.x + b.width) - (a.x +a.width)) + 5);
        }
    }
}
