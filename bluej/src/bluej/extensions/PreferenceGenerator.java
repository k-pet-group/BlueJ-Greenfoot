package bluej.extensions;

import javax.swing.JPanel;

/**
 *  Provides a means to add preference items to the Tools-Preferences Extensions panel.
 *
 *  What you do is to implement getPanel(), loadValues() and saveValues() and
 *  put all your swing Components into a JPanel that will be taken for drawing.
 *  After having allocated this class you can simply call 
 *  bluej.setPreferenceGenerator (myPrefGen );  
 *  This is a simple example of how a preference panel can be implemented.
 *  
 * <PRE>
 * public class JsPreferences implements PreferenceGenerator
 * {
 * private JPanel myPanel;
 * private JTextField profileCmd;
 * private BlueJ bluej;
 * public static final String PROFILE_LABEL="profile.cmd";
 *
 * public JsPreferences(BlueJ bluej) {
 *   this.bluej = bluej;
 *   myPanel = new JPanel();
 *   myPanel.add (new JLabel ("Profile Command"));
 *   profileCmd = new JTextField (40);
 *   myPanel.add (profileCmd);
 *   // So the user sees the previous values
 *   loadValues();
 *   }
 *
 * public JPanel getPanel ()  { return myPanel; }
 *
 * public void saveValues () {
 *   bluej.setExtPropString(PROFILE_LABEL,profileCmd.getText());
 *   }
 *
 * public void loadValues () {
 *   profileCmd.setText(bluej.getExtPropString(PROFILE_LABEL,""));
 *   }
 * }
 * </pre>
 *
 * @version $Id: PreferenceGenerator.java 1780 2003-04-10 08:10:42Z damiano $
 */

/*
 * AUthor Damiano Bolla, University of Kent at Canterbuty, January 2003
 */
 
public interface PreferenceGenerator
{
    /**
     * Bluej will call this method to get the panel where preferences for this
     * extension are. Preferences can be layout aswished.
     *
     * @return    The JPanel where preferences are.
     */
    public JPanel getPanel();


    /**
     * When this method is called the Extension should load its current values into
     * its preference panel.
     * This is called from a swing thread, so ALWAYS be quick in doing your job.
     */
    public void loadValues();


    /**
     * When this method is called the Extension should save values from the preference panel to 
     * a longer term of storage. At this stage some sort of control on what is being
     * saved can be done.
     * This is called from a swing thread, so ALWAYS be quick in doing your job
     */
    public void saveValues();
}

