package bluej.editor;

import bluej.Config;
import bluej.utility.Utility;

import bluej.BlueFrame;
import java.awt.*;
import java.awt.event.*;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 ** @version $Id: SimpleFrame.java 36 1999-04-27 04:04:54Z mik $
 ** @author Michael Cahill
 ** Frame to edit a single file.
 **/

public class SimpleFrame extends BlueFrame implements ActionListener
{
	private static String noTitle = Config.getString("editor.noTitle");
	private static String CVSVersion = "$Revision: 36 $";
	private static String AppVersion = CVSVersion.substring(11, CVSVersion.length() - 2);
	static String AppTitle = Config.getString("editor.title") + AppVersion;

	static Font PkgMgrFont = new Font("SansSerif", Font.PLAIN, 10);
	static Color bgColor = new Color(208, 212, 208);

	SimpleText editor;
	Label statusbar;
	String filename;
	boolean modified;

	public SimpleFrame(String filename)
	{
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent E)
			{
				if(canClose())
					doClose();
			}
		});
		
		setFont(PkgMgrFont);
		setBackground(bgColor);

		setupMenus();

		String initial_text = "";
		if(filename == null)
			this.filename = noTitle;
		else
		{
			this.filename = filename;
		
			try {
				FileInputStream input = new FileInputStream(filename);
				int size = input.available();
				byte[] buffer = new byte[size];
				int bufsize = input.read(buffer);
				initial_text = new String(buffer, 0, 0, bufsize);
				input.close();
			} catch(Exception e) {
				Utility.reportError(openError + filename + ": " + e);
			}
		}

		editor = new SimpleText(initial_text);
		add("Center", editor);

		statusbar = new Label();
		add("South", statusbar);

		pack();

		setModified(false);
		setStatus(AppTitle);
	}

	public SimpleFrame()
	{
		this(null);
	}

	void setupMenus()
	{
		MenuBar menubar = new MenuBar();
		Menu menu = null;

		for(int menuType = 0; menuType < Main.CmdTypes.length; menuType++)
		{
			int sep = 0;

			menu = new Menu(Main.CmdTypeNames[menuType]);
			for(int i = 0; i < Main.CmdStrings[menuType].length; i++)
			{
				MenuItem item = new MenuItem(Main.CmdStrings[menuType][i]);
				item.addActionListener(this);
				menu.add(item);
				if(sep < Main.CmdSeparators[menuType].length
				  && Main.CmdSeparators[menuType][sep] == Main.CmdTypes[menuType] + i)
				{
					menu.addSeparator();
					++sep;
				}
			}
			menubar.add(menu);
		}

		if(menu != null)
		{
			// Always put help menu last
			menubar.setHelpMenu(menu);
		}

		setMenuBar(menubar);
	}

	public void actionPerformed(ActionEvent evt)
	{
		String cmd = evt.getActionCommand();
		Integer evtIdObj = (cmd != null) ? (Integer)Main.commands.get(cmd) : null;
		int evtId = (evtIdObj != null) ? evtIdObj.intValue() : 0;
		String name;

		switch(evtId)
		{
		// File commands
		case Main.FILE_NEW:
			SimpleFrame editor = new SimpleFrame();
			editor.setVisible(true);
			return;

		case Main.FILE_OPEN:
			Unimplemented();
			return;
		
		case Main.FILE_SAVE:
			doSave(false);
			return;

		case Main.FILE_SAVEAS:
			doSave(true);
			return;

		case Main.FILE_SAVEALL:
			Unimplemented();
			return;

		case Main.FILE_PRINT:
			Unimplemented();
			return;
		
		case Main.FILE_CLOSE:
			if(canClose())
				doClose();
			return;

		case Main.FILE_QUIT:
			bluej.Main.exit();
			return;

		// Edit commands
		case Main.EDIT_CUT:
		case Main.EDIT_COPY:
		case Main.EDIT_PASTE:
		case Main.EDIT_FIND:
			Unimplemented();
			return;

		// Help commands
		case Main.HELP_CONTENTS:
		case Main.HELP_ABOUT:
			Unimplemented();
			return;
		}
	}

	void Unimplemented()
	{
		Utility.reportError(unimplemented);
	}

	/** Is a close allowed? **/
	public boolean canClose()
	{
		if(modified)
			return false;

		return true;
	}

	public void doSave(boolean askFilename)
	{
		if(askFilename || filename == noTitle)
		{
			return;
		}
		
		if(filename == noTitle)	// still no filename
			return;

		try {
			FileOutputStream output = new FileOutputStream(filename);
			String text = editor.getText();
			byte[] buffer = text.getBytes();
			output.write(buffer);
			output.close();
			setModified(false);
		} catch(Exception e) {
			Utility.reportError(saveError + filename + ": " + e);
		}
	}

	void setStatus(String status)
	{
		statusbar.setText(status);
	}

	void setModified(boolean modified)
	{
		this.modified = modified;

		setTitle(title + " - " + filename + (modified ? " (modified)" : ""));
	}
	
	public boolean isModified()
	{
		return modified;
	}
	
	public void showMessage(String message)
	{
		Utility.showAlertDialog(this, "Editor message", message);
	}
	
	// String for internationalisation
	static String title = Config.getString("editor.title");
	static String saveError = Config.getString("editor.saveError");
	static String openError = Config.getString("editor.openError");
	static String unimplemented = Config.getString("editor.unimplemented");
}
