package org.bluej.extensions.submitter;

import bluej.extensions.BlueJ;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;

/**
 * Processes the transport URL
 * Parses it and changes it to substitute vars that are in it.
 * I really need to get around to clean it up a bit, Damiano
 * 
 * @author Clive Miller
 * @version $Id: UrlRewrite.java 1620 2003-02-04 10:15:22Z damiano $
 */

class UrlRewrite
{
    private Stat stat;
    private URL  url;
    private boolean isMessage;
    
    UrlRewrite (Stat i_stat)
    {
        stat = i_stat;
    }
    
    boolean process (Window parent) throws AbortOperationException
    {
        Collection urlStrings = stat.submiProp.getProps (".transport");
        if (urlStrings.isEmpty()) throw new AbortOperationException (stat.bluej.getLabel("message.notransport"));
        String urlString = (String)urlStrings.iterator().next();
        isMessage = urlString.startsWith ("message:");
        ArrayList userInfo = new ArrayList(); // of JTextComponent
        JPanel body = new JPanel (new GridBagLayout());
        GridBagConstraints left = new GridBagConstraints();
        {
            left.gridx = 0;
            left.ipadx = 10;
        }
        GridBagConstraints right = new GridBagConstraints();
        {
            right.gridx = 1;
            right.fill = GridBagConstraints.HORIZONTAL;
        }
        body.setBorder(stat.bluej.getDialogBorder());
        NumberFormat digits = new DecimalFormat ("00");
        if (!isMessage) while (true) {
            final int start = urlString.indexOf ('<');
            final int end = urlString.indexOf ('>');
            if (start == -1 && end == -1) break;
            if (start == -1 || start>end) throw new AbortOperationException (stat.bluej.getLabel("message.badurlrequest")+" "+urlString);

    // param
    // action:message<=param>
            String sub = urlString.substring (start+1, end);
            int equal = sub.indexOf ('=');
            int colon = sub.indexOf (':');
            if (equal != -1 && colon > equal) throw new AbortOperationException (stat.bluej.getLabel ("message.invalidparm")+": "+sub);
            String param = sub.substring (equal+1);
            String replace = null;
            if (param.length() > 0) {
                if (param.equals ("username")) replace = URLEncoder.encode(stat.globalProp.getProperty(GlobalProp.USERNAME_VAR,""));
                else if (param.equals ("title")) replace = URLEncoder.encode(stat.globalProp.getProperty(GlobalProp.TITLE_VAR,""));
                else if (param.equals ("simpletitle")) replace = URLEncoder.encode(stat.globalProp.getProperty(GlobalProp.SIMPLETITLE_VAR,""));
                else if (param.equals ("address")) replace = URLEncoder.encode(stat.globalProp.getProperty(GlobalProp.USERADDR_VAR,""));
                else if (param.equals ("date")) replace = URLEncoder.encode(stat.globalProp.getProperty(GlobalProp.DATE_VAR,""));
                else if (param.length() == 2 
                         && Character.isDigit (param.charAt(0))
                         && Character.isDigit (param.charAt(1))) {
                    replace = "{"+param+"}";
                }
                else if (colon == -1 || equal != -1) {
                    throw new AbortOperationException (stat.bluej.getLabel ("message.unknownparm")+": "+param);
                }
            }

            if (colon != -1) {
                String action = sub.substring (0,colon);
                String message = (equal == -1) ? sub.substring (colon+1) : sub.substring (colon+1, equal);
                JTextComponent tc;
                if (action.equals ("field")) tc = new JTextField (20);
                else if (action.equals ("area")) tc = new JTextArea (4,20);
                else if (action.equals ("password")) tc = new JPasswordField (20);
                else if (action.equals ("show")) {
                    tc = new JTextField (20);
                    tc.setEnabled (false);
                }
                else throw new AbortOperationException (stat.bluej.getLabel ("message.unknownaction")+": "+action);
                if (replace != null) tc.setText (replace);
                body.add (new JLabel (message), left);
                body.add (new JScrollPane (tc), right);
                replace = "{"+digits.format (userInfo.size()) +"}";
                userInfo.add (tc);
            }
            if (replace == null) throw new AbortOperationException (stat.bluej.getLabel ("message.unknwonparm")+": "+sub);
            urlString = urlString.substring (0,start) + replace + urlString.substring (end+1);
        }
        
        if (!userInfo.isEmpty()) {
            int id = JOptionPane.showConfirmDialog (parent, body, stat.bluej.getLabel ("dialog.parameters.title"), JOptionPane.OK_CANCEL_OPTION,
                                                                                JOptionPane.PLAIN_MESSAGE);
            if (id == JOptionPane. CANCEL_OPTION) return false;
            int i=0;
            for (Iterator it=userInfo.iterator(); it.hasNext(); i++) {
                JTextComponent tc = (JTextComponent)it.next();
                int loc;
                while ((loc = urlString.indexOf ("{"+digits.format(i)+"}")) != -1) {
                    String value = URLEncoder.encode (tc.getText());
                    urlString = urlString.substring (0,loc)+value+urlString.substring (loc+4);
                }
            }
        }
        try {
            if (isMessage) {
                url = new URL (null, urlString, new MessageHandler (urlString.substring (8)));
            } else {
                url = new URL (urlString);
            }
        } catch (MalformedURLException ex) {
            throw new AbortOperationException (ex.toString());
        }
        return true;
    }
    
    URL getURL()
    {
        return url;
    }
    
    public boolean isMessage()
    {
        return isMessage;
    }
}       