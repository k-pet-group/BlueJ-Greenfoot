package bluej.editor;

import java.util.Hashtable;

/**
 ** @version $Id: Main.java 36 1999-04-27 04:04:54Z mik $
 ** @author Michael Cahill
 ** Simple class to start the bluej.editor.
 **/

public class Main
{
	/**
	 ** Start everything off
	 **/
	public static void main(String[] args)
	{
		if(args.length == 0)
		{
			// No arguments, so start an empty editor window
			SimpleFrame editor = new SimpleFrame();
			editor.setVisible(true);
		}
		else
		{
			for(int i = 0; i < args.length; i++)
			{
				SimpleFrame editor = new SimpleFrame(args[i]);
				editor.setVisible(true);
			}
		}
	}

	/**
	 ** Commands - for lookup from events
	 **/
	public static Hashtable commands = new Hashtable();

	static final int FILE_COMMAND = 1000;
	static final int FILE_NEW = FILE_COMMAND;
	static final int FILE_OPEN = FILE_NEW + 1;
	static final int FILE_SAVE = FILE_OPEN + 1;
	static final int FILE_SAVEAS = FILE_SAVE + 1;
	static final int FILE_SAVEALL = FILE_SAVEAS + 1;
	static final int FILE_PRINT = FILE_SAVEALL + 1;
	static final int FILE_CLOSE = FILE_PRINT + 1;
	static final int FILE_QUIT = FILE_CLOSE + 1;

	static final String[] FileCmds = {
		"New", "Open", "Save", "Save As", "Save All", "Print", "Close", "Quit",
	};

	static final int[] FileSeparators = {
		FILE_OPEN, FILE_SAVEALL, FILE_PRINT,
	};

	static final int EDIT_COMMAND = FILE_COMMAND + 100;
	static final int EDIT_CUT = EDIT_COMMAND;
	static final int EDIT_COPY = EDIT_CUT + 1;
	static final int EDIT_PASTE = EDIT_COPY + 1;
	static final int EDIT_FIND = EDIT_PASTE + 1;

	static final String[] EditCmds = {
		"Cut", "Copy", "Paste", "Find",
	};

	static final int[] EditSeparators = {
		EDIT_PASTE,
	};

	static final int HELP_COMMAND = EDIT_COMMAND + 100;
	static final int HELP_CONTENTS = HELP_COMMAND;
	static final int HELP_ABOUT = HELP_CONTENTS + 1;

	static final String[] HelpCmds = {
		"Contents", "About",
	};

	static final int[] HelpSeparators = {
		HELP_CONTENTS,
	};

	static final int[] CmdTypes = {
		FILE_COMMAND, EDIT_COMMAND, HELP_COMMAND,
	};

	static final String[] CmdTypeNames = {
		"File", "Edit", "Help",
	};

	static final String[][] CmdStrings = {
		FileCmds, EditCmds, HelpCmds,
	};

	static final int[][] CmdSeparators = {
		FileSeparators, EditSeparators, HelpSeparators,
	};

	private static void addCmd(String cmd, int value, String desc)
	{
		commands.put(cmd, new Integer(value));
	}

	private static void addCmd(String cmd, int value)
	{
		addCmd(cmd, value, null);
	}

	static		// Set up the commands
	{
		for(int type = 0; type < CmdTypes.length; type++)
			for(int i = 0; i < CmdStrings[type].length; i++)
				addCmd(CmdStrings[type][i], CmdTypes[type] + i);
	}
}
