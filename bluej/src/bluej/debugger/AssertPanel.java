package bluej.debugger;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

/**
 * @author  Andrew Patterson  
 * @version $Id: AssertPanel.java 1828 2003-04-11 08:40:48Z mik $
 */
public class AssertPanel extends JPanel
	implements ItemListener
{
    JTabbedPane assertionTabs;
    JPanel standardPanel;
    JPanel freeformPanel;
    JTextField assertData;
	JComboBox assertCombo;
	
	/**
	 * The assertion statement that this panel represents
	 */
	private String panelAssertStatement;
    
    private String[] labels = new String[]
    	 { "equal to", "same as", "not the same as", "not null", "null"  };
    	 
    private boolean[] labelsFieldNeeded = new boolean[] 
    	{ true, true, true, false, false };

	private String[] labelsAssertStatement = new String[]
		 { "assertEquals", "assertSame", "assertNotSame", "assertNotNull", "assertNull"  };
    	
    public AssertPanel()
    {
        standardPanel = new JPanel();
        { 
            standardPanel.add(new JLabel("result is"));

            assertCombo = new JComboBox(labels);
            {
				assertCombo.addItemListener(this);
			}                       
            standardPanel.add(assertCombo);
            
            standardPanel.add(assertData = new JTextField(14));
        }

        freeformPanel = new JPanel();
        { 
            freeformPanel.setLayout(new BoxLayout(freeformPanel, BoxLayout.Y_AXIS));
            freeformPanel.add(new JLabel("Free form assertions use the identifier 'result' to"));
            freeformPanel.add(new JLabel("refer to rhe method result"));
            freeformPanel.add(new JLabel("assert that"));
            freeformPanel.add(new JTextField(20));
            freeformPanel.add(new JLabel("is true"));
        }

        assertionTabs = new JTabbedPane();
        assertionTabs.addTab("Standard Assertions", null, standardPanel);
//        assertionTabs.addTab("Free Form Assertions", null, freeformPanel);

        add(assertionTabs, BorderLayout.CENTER);
	}

	public void itemStateChanged(ItemEvent e)
	{
		if (e.getStateChange() == ItemEvent.SELECTED) {
			int index = findItemIndex((String)e.getItem());
			
			if(index >= 0) {
				assertData.setEnabled(labelsFieldNeeded[index]);
				assertData.setBackground(labelsFieldNeeded[index] ? Color.WHITE : Color.LIGHT_GRAY);
			}
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
	
    public String getAssertStatement()
    {
        StringBuffer sb = new StringBuffer();
        
        int index = assertCombo.getSelectedIndex();
        
        sb.append(labelsAssertStatement[index]);
        sb.append("(");

		if (labelsFieldNeeded[index]) {
			sb.append(assertData.getText());
			sb.append(", ");
		}
					
		sb.append("result);");
    	
        return sb.toString();    
    }
}
