/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010  Michael Kolling and John Rosenberg 
 
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
package bluej.debugmgr.inspector;

import java.awt.event.*;
import java.util.StringTokenizer;

import javax.swing.*;

import bluej.*;
import bluej.testmgr.record.InvokerRecord;

/**
 * A panel that can record assertion statements.
 * 
 * @author  Andrew Patterson  
 */
public class AssertPanel extends JPanel
{
    private static final String equalToLabel =
        Config.getString("debugger.assert.equalTo");
    private static final String sameAsLabel =
        Config.getString("debugger.assert.sameAs");
    private static final String notSameAsLabel =
        Config.getString("debugger.assert.notSameAs");
    private static final String notNullLabel =
        Config.getString("debugger.assert.notNull");
    private static final String assertNullLabel =
        Config.getString("debugger.assert.null");
    private static final String equalToFloatingPointLabel =
        Config.getString("debugger.assert.equalToFloatingPoint");

    private static final String nullLabel = "null";

    /**
     * The panels and UI elements of this panel.
     */
    private JPanel standardPanel;
    private JLabel assertLabel;
    private JLabel deltaLabel;
    private JTextField assertData;
    //	used for delta in float and double comparison
    private JTextField deltaData; 
    private JComboBox assertCombo;
    protected JCheckBox assertCheckbox;
    
    // a boolean indicating if the user has interacted in
    // any way with the assertion panel (ie changed the
    // combo box, typed in the edit control etc.)
    // if they have not interacted at all, we feel free
    // to modify the interface
    private boolean userInput = false;
	
    /**
     * The data that is displayed in the combo box to the user.
     * Must be in the same order as the labelsFieldNeeded and 
     * labelAssertStatement arrays.
     */
    private String[] labels = new String[]
                                         { equalToLabel, sameAsLabel, notSameAsLabel, notNullLabel, assertNullLabel , equalToFloatingPointLabel };

    private boolean[] firstLabelFieldNeeded = new boolean[] 
                                                          { true, true, true, false, false, true};

    private boolean[] secondFieldNeeded = new boolean[] 
                                                      { false, false, false, false, false, true };

    private String[] labelsAssertStatement = new String[]
                                                        { "assertEquals", "assertSame", "assertNotSame", "assertNotNull", "assertNull", "assertEquals"};

    /**
     * A panel which presents an interface for making a single
     * assertion about a result. 
     */    	
    public AssertPanel()
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // a checkbox which enables/disables all the assertion UI

        assertCheckbox = new JCheckBox(Config.getString("debugger.assert.assertThat"), true);
        {
            assertCheckbox.setAlignmentX(LEFT_ALIGNMENT);
            assertCheckbox.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent ie)
                {
                    signalUserInput();
                    boolean isSelected = ie.getStateChange() == ItemEvent.SELECTED;
                    assertCombo.setEnabled(isSelected);
                    assertData.setEnabled(isSelected);
                    assertLabel.setEnabled(isSelected);
                    deltaData.setEnabled(isSelected);
                    deltaLabel.setEnabled(isSelected);
                }				
            });
        }

        standardPanel = new JPanel();
        {
            standardPanel.setBorder(BlueJTheme.generalBorder);
            standardPanel.setLayout(new BoxLayout(standardPanel, BoxLayout.X_AXIS)); 
            standardPanel.setAlignmentX(LEFT_ALIGNMENT);

            standardPanel.add(assertLabel = new JLabel(Config.getString("debugger.assert.resultIs")));
            standardPanel.add(Box.createHorizontalStrut(BlueJTheme.componentSpacingSmall));

            assertCombo = new JComboBox(labels);
            {
                assertCombo.addItemListener(new ItemListener() {
                    public void itemStateChanged(ItemEvent ie)
                    {
                        signalUserInput();

                        // if the selected assertion does not require an extra parameter,
                        // disable the text field.
                        if (ie.getStateChange() == ItemEvent.SELECTED) {
                            int index = findItemIndex((String)ie.getItem());

                            // we have to also take into account the assertion check box status.
                            // if it is not enabled, then we shouldn't enable the data controls either
                            if(index >= 0) {
                                boolean firstNeeded = firstLabelFieldNeeded[index] && assertCheckbox.isSelected();
                                boolean secondNeeded = secondFieldNeeded[index] && assertCheckbox.isSelected();

                                assertData.setEnabled(firstNeeded);
                                //assertData.setBackground(firstNeeded ? Color.white : Color.lightGray);

                                // if the second field is needed, we _always_ make it visible
                                // (but perhaps not enabled)
                                deltaLabel.setVisible(secondFieldNeeded[index]);
                                deltaData.setVisible(secondFieldNeeded[index]);

                                deltaLabel.setEnabled(secondNeeded);
                                deltaData.setEnabled(secondNeeded);
                            }
                        }
                    }
                });
            }                       

            assertData = new JTextField(14);
            {
                assertData.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ae)
                    {
                        signalUserInput();
                    }
                });
            }

            deltaData = new JTextField(6);
            {
                deltaData.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ae)
                    {
                        signalUserInput();
                    }
                });
            }

            standardPanel.add(assertCombo);
            standardPanel.add(Box.createHorizontalStrut(BlueJTheme.componentSpacingSmall));

            deltaLabel = new JLabel(Config.getString("debugger.assert.delta"));
            standardPanel.add(assertData);
            standardPanel.add(Box.createHorizontalStrut(BlueJTheme.componentSpacingSmall));
            standardPanel.add(deltaLabel);
            standardPanel.add(Box.createHorizontalStrut(BlueJTheme.componentSpacingSmall));
            standardPanel.add(deltaData);

            deltaData.setVisible(false);
            deltaLabel.setVisible(false);

            assertCombo.setEnabled(true);
            assertData.setEnabled(true);
            assertLabel.setEnabled(true);
            deltaData.setEnabled(true);
            deltaLabel.setEnabled(true);
        }

        add(assertCheckbox);
        add(standardPanel);
    }

    /**
     * This code should move to somewhere else once we sort out the whole
     * inspector/debugger object mess.
     * 
     * @param resultString
     */
    public void updateWithResultData(String resultString)
    {
        // if the user has put in any data themselves, who are
        // we to change it
        if (userInput)
            return;
        
        // parse the result string to try and make a reasonable guess
        // at the initial assertion values.
        // we should really be dealing with the actual objects here (rather
        // than their string representations) but the DebuggerObject interface
        // forces us to do it this way
        String tokens[] = new String[3];
        
        StringTokenizer st = new StringTokenizer(resultString);
        int i = 0;
        while (st.hasMoreTokens() && i<3) {
            tokens[i++] = st.nextToken();
        }
         
        //For a String with spaces, we want the rest to be added as well.
        if(st.hasMoreTokens()) {
            //find the index of the last token, and add all from there.
            int startIndex = resultString.indexOf(tokens[2]);
            tokens[2] = resultString.substring(startIndex);
        }
        
        // floats and doubles, we calculate a delta
        if (tokens[0].equals("float") || tokens[0].equals("double")) {
            assertCombo.setSelectedIndex(findItemIndex(equalToFloatingPointLabel));
            assertData.setText(tokens[2]);
            double delta = Double.parseDouble(tokens[2]);
            deltaData.setText(Double.toString(Math.abs(delta * 0.01)));
        }
        else if (tokens[2].equals(nullLabel)) {
            // an object reference that is null
            assertCombo.setSelectedIndex(findItemIndex(assertNullLabel));
        }
        else if (tokens[2].equals("<object")) {
            // an object reference that is not null
            assertCombo.setSelectedIndex(findItemIndex(notNullLabel));
        } else {
            // anything else, which means it is one of the primitive types
            // or String
            assertData.setText(tokens[2]);
            assertCombo.setSelectedIndex(findItemIndex(equalToLabel));
        }
    }
    
    private int findItemIndex(String item)
    {
        for(int i=0; i<labels.length; i++) {
            if (labels[i].equals(item))
                return i;
        }

        return -1;
    }
	
    /**
     * Record that the user has made a change to the
     * assertion UI.
     * 
     * If this is the first time that this has been done,
     * we enable the assertion check box.
     */
    private void signalUserInput()
    {
        if (!userInput) {
            //assertCheckbox.setSelected(true);    
        }
        userInput = true;
    }
    
    /**
     * 
     * @return a boolean indicating if the user wanted assertions for this
     *         result
     */
    public boolean isAssertEnabled()
    {
        return assertCheckbox != null ? assertCheckbox.isSelected() : false;
    }
	
    /**
     * Check whether the necessary fields have been filled in to make a compilable
     * assert statement.
     */
    public boolean isAssertComplete()
    {
        int index = assertCombo.getSelectedIndex();
        
        if (secondFieldNeeded[index]) {
            if (deltaData.getText().length() == 0) {
                return false;
            }
        }
        
        if (firstLabelFieldNeeded[index]) {
            if (assertData.getText().length() == 0) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Return an assertion statement out of the data in the UI.
     * 
     * @return a String representing the assertion specified in this
     *         assertion panel.
     */
    public String getAssertStatement()
    {
        // which type of assertion is selected
        int index = assertCombo.getSelectedIndex();
        
        // for double/float assertEquals() assertions, we need a delta value
        if (secondFieldNeeded[index]) {
            return InvokerRecord.makeAssertionStatement(labelsAssertStatement[index],
                                                        assertData.getText(),
                                                        deltaData.getText());
        }
        else if (firstLabelFieldNeeded[index]) {
            return InvokerRecord.makeAssertionStatement(labelsAssertStatement[index],
                                                        assertData.getText());
        }
        else
            return InvokerRecord.makeAssertionStatement(labelsAssertStatement[index]);
    }
    
}
