package bluej.utility;

import bluej.Config;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.StringTokenizer;

/**
 * A generic dialog box with an okay button at the bottom, title
 * and user configurable text. 
 * The text is assumed to be in HTML format and is presented
 * using a scrolling, HTML-aware JEditorPane.
 * 
 * @see JEditorPane
 * @see JDialog
 * @version $Id: OkayDialog.java 36 1999-04-27 04:04:54Z mik $
 * @author $Author: mik $
 */
public class OkayDialog extends JDialog implements ActionListener {
	private static final Dimension SIZE = new Dimension(400, 200);
	private static final Color BGCOLOR = new Color(0, 0, 156);

	private JButton button = new JButton("Ok");
		
	/**
	 * @param a JFrame object (or subclass) to act as the parent of the dialog
	 * @param title a plaing text string title
	 * @param text a string of HTML formatted text for the body of the dialog
	 * @param model true if the dialog blocks input to the rest of the application, false otherwise
	 */
	public OkayDialog(JFrame parent, String text, String title, boolean modal) {
		super(parent, title, modal);
		
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent E) {
				setVisible(false);
			}
		});
		
		this.setResizable(false);
		this.setBackground(this.BGCOLOR);
		
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
		buttonPanel.add(button);
		buttonPanel.setBackground(this.BGCOLOR);
		button.addActionListener(this);
		getContentPane().add(buttonPanel, BorderLayout.SOUTH);
		JEditorPane message = new JEditorPane("text/html", text);
		JScrollPane scroller = new JScrollPane(message);
		message.setBackground(this.BGCOLOR);
		message.setEditable(false);
		getContentPane().add(scroller);

		if (parent != null)
			this.setLocation(parent.getPreferredSize().width / 2 - (SIZE.width / 2), parent.getPreferredSize().height / 2 - (SIZE.height / 2));
		
		this.setSize(SIZE);
		this.pack();
	}

	public void actionPerformed(ActionEvent evt) {
		String cmd = evt.getActionCommand();
		if (button.getText().equals(cmd))
			setVisible(false);
	}
	
	public void display() {
		setVisible(true);
	}

	public Dimension getMinumumSize() {
		return SIZE;
	}
	
	public Dimension getPreferredSize() {
		return SIZE;
	}
}

