package bluej.debugmgr.inspector;

import java.awt.Color;
import java.awt.event.*;

import javax.swing.*;

import bluej.*;
import bluej.Config;

/**
 * A panel that can record assertion statements.
 * 
 * @author  Andrew Patterson  
 * @version $Id: AssertPanel.java 2032 2003-06-12 05:04:28Z ajp $
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

	/**
	 * The panels and UI elements of this panel.
	 */
    private JPanel standardPanel;
	private JPanel freeformPanel;

    private JLabel assertLabel;
    private JTextField assertData;
	private JComboBox assertCombo;
	protected JCheckBox assertCheckbox;
	
	/**
	 * The data that is displayed in the combo box to the user.
	 * Must be in the same order as the labelsFieldNeeded and 
	 * labelAssertStatement arrays.
	 */
    private String[] labels = new String[]
    	 { equalToLabel, sameAsLabel, notSameAsLabel, notNullLabel, nullLabel };
    	 
    private boolean[] labelsFieldNeeded = new boolean[] 
    	{ true, true, true, false, false };

	private String[] labelsAssertStatement = new String[]
		 { "assertEquals", "assertSame", "assertNotSame", "assertNotNull", "assertNull"  };

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
						// if the selected assertion does not require an extra parameter,
						// disable the text field.
						if (ie.getStateChange() == ItemEvent.SELECTED) {
							int index = findItemIndex((String)ie.getItem());
			
							if(index >= 0) {
								assertData.setEnabled(labelsFieldNeeded[index]);
								assertData.setBackground(labelsFieldNeeded[index] ? Color.WHITE : Color.LIGHT_GRAY);
							}
						}
					}
				});
			}                       
            standardPanel.add(assertCombo);
			standardPanel.add(Box.createHorizontalStrut(BlueJTheme.componentSpacingSmall));
            
            standardPanel.add(assertData = new JTextField(14));
        }

/*        freeformPanel = new JPanel();
        { 
            freeformPanel.setLayout(new BoxLayout(freeformPanel, BoxLayout.Y_AXIS));
            freeformPanel.add(new JLabel("Free form assertions use the identifier 'result' to"));
            freeformPanel.add(new JLabel("refer to rhe method result"));
            freeformPanel.add(new JLabel("assert that"));
            freeformPanel.add(new JTextField(20));
            freeformPanel.add(new JLabel("is true"));
        } */

//		JTabbedPane assertionTabs;
//        assertionTabs = new JTabbedPane();
//        assertionTabs.addTab("Standard Assertions", null, standardPanel);
//        assertionTabs.addTab("Free Form Assertions", null, freeformPanel);

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
	
	public boolean isAssertEnabled()
	{
		return assertCheckbox != null ? assertCheckbox.isSelected() : false;
	}
	
	/**
	 * Returns a statement representing this assertion.
	 * Does not include a trailing variable or bracket! 
	 * 
	 * @return a String of the assertion statement.
	 */
    public String getAssertStatementStart()
    {
        StringBuffer sb = new StringBuffer();
        
        int index = assertCombo.getSelectedIndex();
        
        sb.append(labelsAssertStatement[index]);
        sb.append("(");

		if (labelsFieldNeeded[index]) {
			sb.append(assertData.getText());
			sb.append(", ");
		}
					
        return sb.toString();    
    }
}
