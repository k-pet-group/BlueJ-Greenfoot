/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 

 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 

 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 

 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 

 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.editor.moe;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.prefmgr.PrefMgr;
import bluej.utility.DBox;
import bluej.utility.DBoxLayout;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 * ReplacePanel display and functionality for replace
 * 
 * @author Marion Zalk
 *
 */
public class ReplacePanel extends JPanel implements ActionListener, KeyListener {

    private MoeEditor editor;
    private Font font;
    private JTextField replaceText;
    private String replaceString;
    private JButton replaceButton;
    private JButton replaceAllButton;

    private final static String REPLACE_BUTTON_NAME ="replaceBtn";
    private final static String REPLACE_ALL_BUTTON_NAME ="replaceAllBtn";
    private final static String REPLACE_TEXTFIELD ="replaceTextField";

    public ReplacePanel(MoeEditor ed) {
        super();
        font=new Font(PrefMgr.getStandardFont().getFontName(), PrefMgr.getStandardFont().getSize(), PrefMgr.getStandardFont().getSize());
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createLineBorder(Color.black));
        addReplaceBody();
        editor=ed;
    }

    public void actionPerformed(ActionEvent e) {
        JComponent src = (JComponent) e.getSource();
        setReplaceString(replaceText.getText());
        if (getReplaceString()==null){
            editor.writeMessage(Config.getString("editor.replaceAll.string") + 
            "is Empty");
            return;
        }         
        if (src.getName()==REPLACE_BUTTON_NAME){
            if (getReplaceString()!=null)
                editor.replace(getReplaceString());
        }
        if (src.getName()==REPLACE_ALL_BUTTON_NAME){
            if (getReplaceString()!=null)
                editor.replaceAll(getReplaceString());

        }

    }

    public void keyPressed(KeyEvent e) {

    }

    public void keyReleased(KeyEvent e) {
        JComponent src = (JComponent) e.getSource();
        if (src.getName()== REPLACE_TEXTFIELD){
            setReplaceString(replaceText.getText());
            //only enable the once and all buttons if both find and replace are populated
            if (editor.getFindSearchString()!=null && editor.getFindSearchString().length()!=0){
                enableButtons(true);
            }
            else
                enableButtons(false);

        }
    }

    public void keyTyped(KeyEvent e) {

    }

    private void addReplaceBody()
    {
        JLabel replaceLabel;
        DBox replaceBody;
        DBox optionsBody;
        JPanel rBody;
        JPanel body;

        body = new JPanel(new GridLayout(1, 2)); // one row, many columns
        rBody=new JPanel(new GridLayout(1, 2));
        //rBody=new DBox(DBoxLayout.X_AXIS, 0, BlueJTheme.commandButtonSpacing, 0.0f);
        replaceBody=new DBox(DBoxLayout.X_AXIS, 0, BlueJTheme.commandButtonSpacing, 0.0f);
        optionsBody=new DBox(DBoxLayout.X_AXIS, 0, BlueJTheme.commandButtonSpacing, 0.0f);
        body.setBorder(BorderFactory.createEmptyBorder(3,0,3,0));
        
        JPanel closeBody=new JPanel(new GridLayout(1,2));
        closeBody.setBackground(MoeEditor.infoColor); 
        body.setBackground(MoeEditor.infoColor);

        replaceLabel=new JLabel("Replace: ");
        replaceLabel.setFont(font);

        replaceText=new JTextField(11);
        replaceText.setMaximumSize(replaceText.getPreferredSize());
        replaceText.setFont(font);
        replaceText.addKeyListener(this);
        replaceText.setName(REPLACE_TEXTFIELD);

        replaceButton=new JButton();
        replaceButton.setName(REPLACE_BUTTON_NAME);
        replaceButton.setText("Once");
        replaceButton.setFont(font);
        replaceButton.addActionListener(this);
        replaceButton.setEnabled(true);

        replaceAllButton=new JButton();
        replaceAllButton.setName(REPLACE_ALL_BUTTON_NAME);
        replaceAllButton.setText(" All  ");
        replaceAllButton.setFont(font);
        replaceAllButton.addActionListener(this);
        replaceAllButton.setEnabled(true);

        JPanel rTemp=new JPanel(new GridLayout(1, 1));
        rTemp.add(replaceLabel);
        JPanel rtTemp=new JPanel(new GridLayout(1, 1));
        rtTemp.add(replaceText);
        replaceBody.add(rTemp);
        replaceBody.add(rtTemp);
        //replaceBody.add(replaceLabel);
        //replaceBody.add(replaceText, BorderLayout.EAST);
        optionsBody.add(replaceButton);
        optionsBody.add(replaceAllButton);
        //optionsBody.add(new JLabel(" "));
        rBody.add(replaceBody);
        rBody.add(optionsBody);


        body.add(rBody);  
        body.add(closeBody);
        add(body);
    }

    public void requestReplaceTextFocus(){
        replaceText.requestFocus();
        replaceText.setText(getReplaceString());
    }

    protected String getReplaceString() {
        return replaceString;
    }

    protected void setReplaceString(String replaceString) {
        this.replaceString = replaceString;
    }

    /**
     * enableButtons enable the once and all buttons
     * @param enable
     */
    protected void enableButtons(boolean enable)
    {
        replaceAllButton.setEnabled(enable);
        replaceButton.setEnabled(enable);
    }

}
