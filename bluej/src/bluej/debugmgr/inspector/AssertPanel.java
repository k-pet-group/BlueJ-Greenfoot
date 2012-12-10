/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2011,2012  Michael Kolling and John Rosenberg 
 
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

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.collect.DataCollector;
import bluej.debugger.gentype.JavaType;
import bluej.pkgmgr.Package;
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

    /**
     * The panels and UI elements of this panel.
     */
    private JPanel standardPanel;
    private JLabel assertLabel;
    private JLabel deltaLabel;
    private JTextField assertData;
    // used for delta in float and double comparison
    private JTextField deltaData; 
    private JComboBox assertCombo;
    protected JCheckBox assertCheckbox;
    private int[] comboIndexes;
    
    private boolean[] firstLabelFieldNeeded = new boolean[] {
            true, true, true, false, false, true
        };

    private boolean[] secondFieldNeeded = new boolean[] {
            false, false, false, false, false, true
        };

    /**
     * Assert labels together with the corresponding assertion method to be called
     */
    private String[][] labelStatements = new String[][] {
            {equalToLabel, "assertEquals"},
            {sameAsLabel, "assertSame"},
            {notSameAsLabel, "assertNotSame"},
            {notNullLabel, "assertNotNull"},
            {assertNullLabel, "assertNull"},
            {equalToFloatingPointLabel, "assertEquals"}
    };
    
    private int[] fpIndexes = {5};
    private int[] primitiveIndexes = {0};
    private int[] objectIndexes = {0, 1, 2, 3, 4};
        
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

            assertCombo = new JComboBox();
            {
                assertCombo.addItemListener(new ItemListener() {
                    public void itemStateChanged(ItemEvent ie)
                    {
                        if (ie.getStateChange() == ItemEvent.SELECTED) {
                            itemSelected(assertCombo.getSelectedIndex());
                        }
                    }
                });
            }                       

            assertData = new JTextField(14);

            deltaData = new JTextField(6);

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
     * An assertion type was selected from the combo box.
     */
    private void itemSelected(int index)
    {
        index = comboIndexes[index];

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
    
    /**
     * Set the result type, used to determine which assertion methods may be applicable.
     */
    public void setResultType(JavaType type)
    {
        if (type.typeIs(JavaType.JT_FLOAT) || type.typeIs(JavaType.JT_DOUBLE)) {
            comboIndexes = fpIndexes;
            deltaData.setText("0.1");
            itemSelected(0); // force display of delta box
        }
        else if (type.isPrimitive()) {
            comboIndexes = primitiveIndexes;
        }
        else {
            comboIndexes = objectIndexes;
        }
        
        String [] comboLabels = new String[comboIndexes.length];
        for (int i = 0; i < comboLabels.length; i++) {
            comboLabels[i] = labelStatements[comboIndexes[i]][0];
        }
        
        assertCombo.setModel(new DefaultComboBoxModel(comboLabels));
    }
    
    /**
     * Check whether the user has asked for an assertion to be recorded.
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
        int index = comboIndexes[assertCombo.getSelectedIndex()];
        
        if (secondFieldNeeded[index]) {
            if (deltaData.getText().trim().length() == 0) {
                return false;
            }
        }
        
        if (firstLabelFieldNeeded[index]) {
            if (assertData.getText().trim().length() == 0) {
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
        int index = comboIndexes[assertCombo.getSelectedIndex()];
        
        // for double/float assertEquals() assertions, we need a delta value
        if (secondFieldNeeded[index]) {
            return InvokerRecord.makeAssertionStatement(labelStatements[index][1],
                                                        assertData.getText(),
                                                        deltaData.getText());
        }
        else if (firstLabelFieldNeeded[index]) {
            return InvokerRecord.makeAssertionStatement(labelStatements[index][1],
                                                        assertData.getText());
        }
        else {
            return InvokerRecord.makeAssertionStatement(labelStatements[index][1]);
        }
    }
    
    public void recordAssertion(Package pkg, int testIdentifier, int invocationIdentifier)
    {
        int index = comboIndexes[assertCombo.getSelectedIndex()];
        
        DataCollector.assertTestMethod(pkg,
          testIdentifier,
          invocationIdentifier,
          labelStatements[index][1],
          firstLabelFieldNeeded[index] ? assertData.getText() : null,
          secondFieldNeeded[index] ? deltaData.getText() : null);
    }
    
}
