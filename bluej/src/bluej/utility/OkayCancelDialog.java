package bluej.utility;

import bluej.Config;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 ** @version $Id: OkayCancelDialog.java 36 1999-04-27 04:04:54Z mik $
 ** @author Justin Tan
 ** A dialog box with okay and cancel buttons at the bottom
 **
 ** NOTE: deprecated! Should be removed. Currently still used as base class
 ** of NewClassDialog
 **/
public class OkayCancelDialog extends JDialog 

	implements ActionListener
{
	static FlowLayout flowLayout = new FlowLayout();
	boolean ok;

	public OkayCancelDialog(JFrame parent, String title, boolean modal)
	{
		super(parent, title, modal);

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent E)
			{
				ok = false;
				setVisible(false);
			}
		});
		
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(flowLayout);
		JButton button;
		buttonPanel.add(button = new JButton(okay));
		button.addActionListener(this);
		buttonPanel.add(button = new JButton(cancel));
		button.addActionListener(this);
		getContentPane().add("South", buttonPanel);
	}

	// Provide function to allow setting of focuses before showing
	public void beforeShow() {}

	public void actionPerformed(ActionEvent evt)
	{
		String cmd = evt.getActionCommand();
		if(okay.equals(cmd))
			doOK();
		else if(cancel.equals(cmd))
			doCancel();
	}
	
	public void doOK()
	{
		ok = true;
		setVisible(false);
	}
	
	public void doCancel()
	{
		ok = false;
		setVisible(false);
	}
	
	public boolean doShow()
	{
		ok = false;
		beforeShow();
		setVisible(true);
		return ok;
	}

	public boolean okayPressed()
	{
		return ok;
	}
	
	// Internationalisation
	static String okay = Config.getString("okay");
	static String cancel = Config.getString("cancel");
}

