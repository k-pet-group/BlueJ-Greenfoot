package bluej.debugmgr.inspector;

import java.awt.Color;
import java.awt.event.*;

import javax.swing.*;

import bluej.*;
import bluej.testmgr.record.InvokerRecord;

/**
 * A panel that can record assertion statements.
 * 
 * @author  Andrew Patterson  
 * @version $Id: AssertPanel.java 2229 2003-10-28 02:09:36Z ajp $
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
	private static final String nullLabel =
		Config.getString("debugger.assert.null");
    private static final String equalToFloatingPointLabel =
            Config.getString("debugger.assert.equalToFloatingPoint");

	/**
	 * The panels and UI elements of this panel.
	 */
    private JPanel standardPanel;
	private JPanel freeformPanel;

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
    	 { equalToLabel, sameAsLabel, notSameAsLabel, notNullLabel, nullLabel , equalToFloatingPointLabel };
    	 
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
		
		assertCheckbox = new JCheckBox(Config.getString("debugger.assert.assertThat"), false);
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
			
							if(index >= 0) {
								assertData.setEnabled(firstLabelFieldNeeded[index]);
								assertData.setBackground(firstLabelFieldNeeded[index] ? Color.white : Color.lightGray);
								deltaData.setVisible(secondFieldNeeded[index]);
                                deltaLabel.setVisible(secondFieldNeeded[index]);
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
        }

		add(assertCheckbox);
        add(standardPanel);
	}

	private int findItemIndex(String item)
	{
		for(int i=0; i<labels.length; i++) {
			if (labels[i].equals(item))
				return i;
		}

		return -1;
	}
	
    private void signalUserInput()
    {
        if (!userInput) {
            assertCheckbox.setSelected(true);    
        }
        userInput = true;
    }
    
	public boolean isAssertEnabled()
	{
		return assertCheckbox != null ? assertCheckbox.isSelected() : false;
	}
	
    public String getAssertStatement()
    {
        int index = assertCombo.getSelectedIndex();
        
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
