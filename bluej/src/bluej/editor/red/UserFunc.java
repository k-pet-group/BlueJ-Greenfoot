package bluej.editor.red;                	// This file forms part of the red package

import bluej.utility.Debug;
   
import java.io.*;		// Object input, ouput streams
import java.awt.*;		// Dialog and components
import java.awt.event.*;	// ActionListener, ItemListener
 
/**
 ** @version $Id: UserFunc.java 36 1999-04-27 04:04:54Z mik $
 ** @author Michael Klling
 ** @author Giuseppe Speranza
 **
 ** Class specification of class UserFunc.  This class manages the functions
 ** that are accessible to the user via key sequences or menus.  It holds
 ** internally a linked list of function objects, each describing one user
 ** function.  Functions are grouped into groups (sometimes called
 ** categories).  A table (groups) exists with one entry for each group.  The
 ** entries in that table are pointers into the function list pointing to the
 ** first functin in that group.  From there on, all functions belong to the
 ** same group up to the function pointed to by the next group pointer.  The
 ** last group are hidden functions.  Hidden functions are not to be shown in
 ** the key definition dialog, since redefinition by the user is not allowed.
 **/
public final class UserFunc
	extends UserFuncID
	implements ActionListener, ItemListener
{
	//public variables
	public static final String red_prefs_file = ".jred-key-bindings";

	// Menu IDs
	public static final int FileMenu = 1;
	public static final int EditMenu = 2;
	public static final int ViewMenu = 3;
	public static final int ToolsMenu = 4;
	public static final int OptionsMenu = 5;
	public static final int HelpMenu = 6;

	// Function Group IDs
	public static final int FGEdit = 1;
	public static final int FGMove = 2;
	public static final int FGFile = 3;
	public static final int FGCustom = 4;
	public static final int FGHelp = 5;
	public static final int FGMisc = 6;
	public static final int FGHidden = 7;
	public static final int NR_OF_GROUPS = 8;
	
	// IDs for buttons in key binding dialog
	public static final int AddKeyButton = 1;
	public static final int DeleteKeyButton = 2;
	public static final int CloseButton = 3;
	public static final int DefaultsButton = 4;
	public static final int HelpButton = 5;
	
	// maximum keys for one function
	public static final int MAX_KEYS = 20;

//////const int NR_OF_BUTTONS=9;	// number of buttons in the toolbar

	// The seven keymaps used by the editors. Each keymap is
	// used for a certain modifier combination to determine the
	// meaning of a keypress.
	public Keymap plain_keymap;
	public Keymap	shift_keymap;
	public Keymap	ctrl_keymap;
	public Keymap	alt_keymap;
	public Keymap	shift_ctrl_keymap;
	public Keymap	shift_alt_keymap;
	public Keymap	ctrl_alt_keymap;

	// private variables
	private Function[] func_table;				// Array of user functions
//	private int menu_func[NR_OF_ITEMS];		// Array of user functions in menu
//	private String menu_accel[NR_OF_ITEMS];	// Array of accelarator strings for menu
//	private int tool_func[NR_OF_BUTTONS];		// Array of indices to tool but funcs
//	private String tool_name[NR_OF_BUTTONS];	// Array of names of tool buttons
	private int[] groups;						// Array of indices to function groups
	private String[] group_names;				// Array with names of groups

	private int current_group;			// The function group currently shown
	private int current_func;			// The function currently shown
	private int current_key;			// The key item currently selected
	private int no_of_keys;			// current number of keys displayed
	private Keymap Keymapy_maps[];	// The keymaps for the items in the key list
	private int key_syms[];			// The keysyms for the items in the key list
	private boolean keys_modified;	// True, if key bindings have been modified
	private boolean menu_changed;	// True, if menu item accelerators changed
	private Keymap add_kmap;		// Keymap used in adding a new key
	private int add_keysym;			// Keysym used in adding a new key

	// private variables for Dialog
	private Dialog dialog;			// the dialog
	private Frame frame;				// the dialog parent frame
	private Choice menu;	 			// dropdown list to display categories
	private List function_name;		// function name list
	private List key_bindings;		 	// function key bindings list
	private Button close;				// close dialog button
	private Button defaults;			// set defaults button
	private Button help;				// help button
	private Button add_key;	 		// add key binding button
	private Button delete_key;		 // delete key binding button
	private TextArea function_desc;	// shows function 

	/**
	 ** Create and bind all user functions.
	 **/
	public UserFunc()
	{
		int i;
	
		func_table = new Function[NR_OF_USERFUNC];
		groups = new int[NR_OF_GROUPS];
		group_names = new String[NR_OF_GROUPS];
	
		Keymapy_maps = new Keymap[MAX_KEYS];
		key_syms = new int[MAX_KEYS];
	
	//////
	//////	for (i=0; i<NR_OF_ITEMS; i++) {
	//////	menu_func[i] = NOT_BOUND;
	//////	menu_accel[i] = null;
	//////	}
	//////
	//////	for (i=0; i<NR_OF_BUTTONS; i++)
	//////	tool_func[i] = NOT_BOUND;
	//////
		plain_keymap = new Keymap ("");
		shift_keymap = new Keymap ("Shift-");
		ctrl_keymap = new Keymap ("Ctrl-");
		alt_keymap = new Keymap ("Alt-");
		shift_ctrl_keymap = new Keymap ("Shift-Ctrl-");
		shift_alt_keymap = new Keymap ("Shift-Alt-");
		ctrl_alt_keymap = new Keymap ("Ctrl-Alt-");
	
		init_functions ();
		init_function_bindings ();
	}
	
	/**
	 ** Given a key and modifiers, translate that key to a function.
	 ** Return null if the key was undefined.
	 **/
	int translate_key (int key, int modifiers)
	{
		Keymap kmap;
	
		kmap = find_keymap (modifiers);	// find out which keymap to use
		if (kmap != null)
		return (kmap.map (key));
		else
		return NOT_BOUND;
	}
	
	//	translate_item: Given a menu item, translate that item to a function code.
	/*
	UserFuncId USERFUNC::translate_item (MenuItemId item)
	{
		return menu_func[item];
	}
	*/
	//	item_accelerator: Return the accelerator string of a menu item
	/*
	BLString USERFUNC::item_accelerator (MenuItemId item)
	{
		return menu_accel[item];
	}
	*/
	//	translate_toolbutton: Given a tool button number, translate that button to
	//	a function code.
	/*
	UserFuncId USERFUNC::translate_toolbutton (int button)
	{
		return tool_func[button];
	}
	*/
	//	toolbutton_name: Return the button name of the toolbar button with the
	//	ID button_no
	/*
	char* USERFUNC::toolbutton_name (int button_no)
	{
		return tool_name[button_no];
	}
	*/
	
	/**
	 ** Return the name of the user function with the ID 'func'
	 **/
	String funcname (int func)
	{
		return func_table[func].functionName;
	}
	
	/**
	 ** Return the code pointer of the user function with the
	 ** ID 'func'
	 **/
	int code_pointer (int func)
	{
		return func_table[func].code;
	}
	
	/**
	 ** Return the help string of the user function with the ID 'func'
	 **/
	String help (int func)
	{
		return func_table[func].helpString;
	}
	
	/**
	 ** show the function reference dialog
	 **/
	public void show_dialog(Frame frame)
	{
		this.frame = frame;
		dialog = new Dialog(frame, "Key-Bindings", true);
	
		// initialisation of state variables
		current_group = 0;
		current_func = UserFunc.UNKNOWN_KEY;
		keys_modified = false;
		menu_changed = false;
	
		// set up menu
		Label categories_label = new Label("Categories:",Label.LEFT);
		menu = new Choice();
		menu.add("Edit Functions");
		menu.add("Move & Scroll Functions");
		menu.add("File Functions");
		menu.add("Customisation Functions");
		menu.add("Help Functions");
		menu.add("Misc. Functions");
		menu.addItemListener(this);
	
		// set up function_name List
		function_name = new List(12, false);
		function_name.addActionListener(this);
	//////
		make_list ();
	//////
	
		// set up key_bindings List
		Label key_bindings_label = new Label("Key Bindings:",Label.LEFT);
		key_bindings = new List(4, false);
	
		// set up close Button
		close = new Button("Close");
		close. addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				close_dialog();
			}
		});
	
		// set up defaults Button
		defaults = new Button("Set Defaults");
		defaults.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				set_defaults1();
			}
		});
	
		// set up help Button
		help = new Button("Help");
		help.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				show_help();
			}
		});
	
		// set up add_key Button
		add_key = new Button("Add Key");
		add_key.setEnabled(false);
		add_key.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
							key_add();
			}
		});
	
		// set up delete_key Button
		delete_key = new Button("Delete Key");
		delete_key.setEnabled(false);
		delete_key.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				key_delete();
			}
		});
	
		// set up function_desc TextArea
		function_desc = new TextArea("",4,80,TextArea.SCROLLBARS_NONE);
		function_desc.setEditable(false);
	
	//////	
		clear_description ();
	//////
	
		// use the GridBag Layout manager to add the components to the Dialog
		GridBagLayout layout = new GridBagLayout();
		dialog.setLayout(layout);
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.weightx = 100;
		constraints.weighty = 100;
		constraints.fill = GridBagConstraints.NONE;
		constraints.anchor = GridBagConstraints.EAST;
		add(categories_label, layout, constraints, 0, 0, 1, 1);
		constraints.anchor = GridBagConstraints.WEST;
		add(menu, layout, constraints, 1, 0, 1, 1);
		constraints.anchor = GridBagConstraints.CENTER;
		constraints.fill = GridBagConstraints.BOTH;
		add(function_name, layout, constraints, 0, 1, 2, 7);
		constraints.anchor = GridBagConstraints.EAST;
		constraints.fill = GridBagConstraints.NONE;
		add(close, layout, constraints, 2, 0, 2, 1);
		add(defaults, layout, constraints, 2, 1, 2, 1);
		add(help, layout, constraints, 2, 2, 2, 1);
		constraints.anchor = GridBagConstraints.WEST;
		add(key_bindings_label, layout, constraints, 2, 3, 2, 1);
		constraints.anchor = GridBagConstraints.CENTER;
		constraints.fill = GridBagConstraints.BOTH;
		add(key_bindings, layout, constraints, 2, 4, 2, 3);
		constraints.fill = GridBagConstraints.NONE;
		add(add_key, layout, constraints, 2, 7, 1, 1);
		add(delete_key, layout, constraints, 3, 7, 1, 1);
		constraints.fill = GridBagConstraints.HORIZONTAL;
		add(function_desc, layout, constraints, 0, 8, 4, 4);
	
		dialog.pack();
		Dimension myDim = dialog.getSize();
		Dimension frameDim = frame.getSize();
		Dimension screenSize = dialog.getToolkit().getScreenSize();
		Point loc = frame.getLocation();
			
		// Center the dialog w.r.t. the frame.
		loc.translate((frameDim.width-myDim.width)/2,
				(frameDim.height-myDim.height)/2);
	
		// Ensure that slave is withing screen bounds.
		loc.x = Math.max(0, Math.min(loc.x,screenSize.width-dialog.getSize().width));
		loc.y = Math.max(0, Math.min(loc.y,screenSize.width-dialog.getSize().height));
		dialog.setLocation(loc.x, loc.y);
	
		dialog.show();
	}
	
	/**
	 ** This function is used to add the components to the dialog
	 ** using the GridBagLayout manager
	 **/
	public void add(Component c, GridBagLayout gbl, GridBagConstraints gbc,
					int x, int y, int w, int h)
	{
		gbc.gridx = x;
		gbc.gridy = y;
		gbc.gridwidth = w;
		gbc.gridheight = h;
		gbl.setConstraints(c, gbc);
		dialog.add(c);
	}
	
	/**
	 ** implement the ActionListener interface to
	 ** get the selected item in the function_name list
	 **/
	public void actionPerformed(ActionEvent event)
	{
		String command = event.getActionCommand();
		int select = function_name.getSelectedIndex();
		list_select(select-1);
	}
	
	/**
	 ** implement the ItemListener interface to
	 ** get when the Choice menu has changed
	 **/
	public void itemStateChanged(ItemEvent event)
	{
		int command = event.getStateChange();
	
		int group = 0;
		String select = menu.getSelectedItem();
		if(select.equals("Edit Functions"))
		{ Debug.message("EDIT"); group = FGEdit; }
		else if(select.equals("Move & Scroll Functions"))
		{ Debug.message("MOVE"); group = FGMove; }
		else if(select.equals("File Functions"))
		{ Debug.message("FILE"); group = FGFile; }
		else if(select.equals("Customisation Functions"))
		{ Debug.message("CUSTOMISATION"); group = FGCustom; }
		else if(select.equals("Help Functions"))
		{ Debug.message("HELP"); group = FGHelp; }
		else if(select.equals("Misc. Functions"))
		{ Debug.message("MISC"); group = FGMisc; }
		else { Debug.message("ERROR"); }
	
		menu_select(group);
	}
	
	/**
	 ** A new item in the categories menu was selected. Update the
	 ** function list to show the functions for the selected group.	The group
	 ** parameter holds the number of the item that was selected.
	 **/
	public void menu_select (int group)
	{
		current_group = group;
		current_func = UNKNOWN_KEY;
		clear_keylist ();
		add_key.setEnabled(false);
		clear_description ();
		make_list ();
	}
	
	/**
	 ** A new item in the function list was selected. Show the
	 ** description and the key bindings for this function.
	 **/
	public void list_select (int item)
	{
		current_func = get_func (current_group, item);
		display_description (current_func);
		clear_keylist ();
		search_keys (current_func);
		add_key.setEnabled(true);
	}
	
	/**
	 ** A new item in the key list was selected.	Enable the 
	 ** Delete button.
	 **/
	public void key_list_select (int item)
	{
		current_key = item;
		delete_key.setEnabled(true); 
	}
	
	/**
	 ** A new key to be added as a function key has been pressed. Now
	 ** define that key binding.	This function is in two parts, because we
	 ** need a question dialog in the middle.
	 **/
	public void add_new_key1 (KeyEvent event)
	{
	/////////NOT YET
	/*
		char str[2];
		unsigned int modifierMask, modifiers;
		String name;
		char message[128];
		int func;
		int okay;
	
		modifierMask = ShiftMask | ControlMask | Mod1Mask;
		modifiers = event.state & modifierMask;
	
		okay = utility->lookup_string (event, str, 1, &add_keysym, NULL) <= 1;
		assertion (okay);
		if (((str[0] == 'q') || (str[0] == 'Q')) && (modifiers == 0))
		{
			add_new_key2 (False);		// abort this operation
			return;
		}
	
		add_kmap = find_keymap (modifiers);	// find out which keymap to use
		if (!add_kmap)			// modifiers that we don't support
		return;
	
		func = add_kmap->map (add_keysym);
	
		if (func == UNKNOWN_KEY)		// do nothing. probably modifier key, 
			return;				// or something we don't want to handle
	
		if (func == UFSelfInsert) {
			messages->show_help (func_dlg, HlpCantRedefine);
			add_new_key2 (False);		// complete this operation
			return;
		}
	
		if (func == NOT_BOUND)
			add_new_key2 (True);		// complete this operation
		else {
			utility->get_string (func_table[func].functionName, 
				&name);
			strcpy (message, "This key currently calls ");
			strcat (message, name);
			strcat (message, ".	Do you want\nto replace this binding?");
			new QUESTION_DLG (func_dlg, message,
						"Replace", "Cancel", false, replace_question_cb, this);
	
	//get yes or no answer and call add_new_key2 function
		}
	*/
	}
	
	/**
	 ** The questions have all been answered - really add a new key
	 ** definition now.	The keymap, key, and function are specified in
	 ** add_kmap, add_keysym, and current_func.
	 ** If 'cont' is false, the add operation should be aborted without adding
	 ** a new key.
	 **/
	public void add_new_key2 (boolean cont)
	{
		if (cont == true)
		{
			defkey (add_kmap, add_keysym, current_func);	// add the key
			clear_keylist ();
			search_keys (current_func);
			keys_modified = true;
			check_menus (current_func);				// test whether menus
		}							//	have to be updated
		display_description (current_func);
	
		function_name.setEnabled(true);
		key_bindings.setEnabled(true);
		menu.setEnabled(true);
		close.setEnabled(true);	
		defaults.setEnabled(true);	
		help.setEnabled(true);	
		add_key.setEnabled(true);	
		delete_key.setEnabled(true);	
	}
	
	/**
	 ** The Add-key button was pressed. Add a key definition to the
	 ** current function.
	 **/
	public void key_add ()
	{
		function_desc.setText("ADDING KEY BINDING\n\n"+
		"Press the key or key combination you want to bind to this function.\n"+
		"(Press 'q' to cancel)");
	
		function_name.setEnabled(false);
		key_bindings.setEnabled(false);
		menu.setEnabled(false);
		close.setEnabled(false);	
		defaults.setEnabled(false);	
		help.setEnabled(false);	
		add_key.setEnabled(false);	
		delete_key.setEnabled(false);	
	
	/*
	//////	utility->add_event_handler (func_dlg, KeyPressMask, False, (BLEventHandler) func_dlg_defkey_cb, this);
	//////	utility->grab_keyboard (func_dlg);
	//////	add_new_key1();
	*/
	}
	
	/**
	 ** The Delete-key button was pressed. Delete the selected key
	 ** definition.
	 **/
	public void key_delete ()
	{
	////	defkey (key_maps[current_key], key_syms[current_key], NOT_BOUND);
		clear_keylist ();
		search_keys (current_func);
		keys_modified = true;
		check_menus (current_func);
	}
	
	/**
	 ** The Set-defaults button was pressed. After confirming, reset
	 ** all key bindings to their default values.
	 **/
	public void set_defaults1 ()
	{
	////	RedEditorManager.red.messages.show_question(frame, Messages.QuKeyDefaults);
		String response = RedEditorManager.red.messages.questiondialog.getCommand();
		if(response.equals("Set Defaults"))
			{ set_defaults2(); }
	}
	
	/**
	 ** Confirmation was recieved, reset key bindings to defaults
	 **/
	public void set_defaults2 ()
	{
		default_key_bindings ();
		clear_keylist ();
		search_keys (current_func);
		keys_modified = true;
	}
	
	/**
	 ** A keybinding has been changed. Check whether menus need to
	 ** be updated.
	 **/
	public void check_menus (int func)
	{
	/*
		int item;
	
		for (item=0; item < NR_OF_ITEMS; item++)
			if (menu_func[item] == func)
			{
				menu_changed = True;
				break;
			}
	*/
	}
	
	/**
	 ** show the help dialog
	 **/
	public void show_help()
	{
		RedEditorManager.red.messages.show_help(frame, Messages.HlpUserFunc);
	}
	
	/**
	 ** Close the function reference dialog
	 **/
	public void close_dialog ()
	{
		dialog.dispose();
		if (keys_modified == true)
			save_key_bindings ();
		if (menu_changed == true)
	;////		RedEditorManager.red.messages.show_help (frame, Messages.HlpMenuUpdate);
	}
	
	/**
	 ** Clear the function list.
	 **/
	public void clear_list ()
	{
		function_name.removeAll();
	}
	
	/**
	 ** Build the function list for the current function group 
	 ** (category)
	 **/
	public void make_list ()
	{
		int func;
	
		clear_list ();
		func = groups[current_group];
		while (func != groups[current_group+1]) {
			add_to_list (func);
			func++;
		}
	}
	
	/**
	 ** Add one function to the function list (remember that some
	 ** indices in the function list are unused - don't add those).
	 **/
	public void add_to_list (int func)
	{
		if (func_table[func].functionName != null)	// take care of unused indices
			function_name.add(func_table[func].functionName);
	}
	
	/**
	 ** Clear the key list
	 **/
	public void clear_keylist ()
	{
		key_bindings.removeAll();
		delete_key.setEnabled(false);
		no_of_keys = 0;
	}
	
	/**
	 ** Add an item to the key list
	 **/
	public void add_to_keylist (Keymap kmap, int key)
	{
		String item;
	
	////	key_maps[no_of_keys] = kmap;		// store key map and symbol
		key_syms[no_of_keys] = key;
		no_of_keys++;
		item = kmap.key_string (key);
		key_bindings.add(item);
	}
	
	/**
	 ** Clear the description field
	 **/
	public void clear_description ()
	{
		function_desc.setText("");
	}
	
	/**
	 ** Display the help text of func in the description
	 ** field
	 **/
	public void display_description (int func)
	{
		function_desc.setText(func_table[func].helpString);
	}
	
	/**
	 ** Search for all keys calling func and add them to key-list
	 **/
	public void search_keys (int func)
	{
		search_keymap (plain_keymap, func);
		search_keymap (shift_keymap, func);
		search_keymap (ctrl_keymap, func);
		search_keymap (alt_keymap, func);
		search_keymap (shift_ctrl_keymap, func);
		search_keymap (shift_alt_keymap, func);
		search_keymap (ctrl_alt_keymap, func);
	}
	
	/**
	 ** Search one keymap for keys calling func and add them to
	 ** key-list
	 **/
	public void search_keymap (Keymap km, int func)
	{
		int key;
	
		km.init_search ();
		while ((key = km.search_key (func)) != KeyEvent.VK_UNDEFINED)
		add_to_keylist (km, key);
	}
	
	/**
	 ** Given a group and an item number for that group, find the
	 ** function.	(The first item in the list is number 0.)
	 **/
	public int get_func (int group, int item)
	{
		int func = groups[group];
	
		while (func_table[func] == null)		// skip unused indices
		func++;
	
		for (; item>0; item--)
		do
			func++;
		while (func_table[func].functionName == null);	// skip unused indices
	
		return func;
	}
	
	/**
	 ** Convert modifier code into pointer to corrosponding keymap.
	 **/
	public Keymap find_keymap (int modifiers)
	{
	
	// map META to ALT
	
		if ((modifiers & InputEvent.META_MASK) != 0) {
			modifiers &= (InputEvent.SHIFT_MASK | InputEvent.CTRL_MASK);
			modifiers |= InputEvent.ALT_MASK;
		}
	
		switch (modifiers) {
	
		case (0):			 // no	modifiers
			return plain_keymap;
	
		case (InputEvent.SHIFT_MASK):
			return shift_keymap;
	
		case (InputEvent.CTRL_MASK):
			return ctrl_keymap;
	
		case (InputEvent.ALT_MASK):
			return alt_keymap;
	
		case (InputEvent.SHIFT_MASK | InputEvent.CTRL_MASK):
			return shift_ctrl_keymap;
	
		case (InputEvent.SHIFT_MASK | InputEvent.ALT_MASK):
			return shift_alt_keymap;
	
		case (InputEvent.CTRL_MASK | InputEvent.ALT_MASK):
			return ctrl_alt_keymap;
	
		default:
			return null;		// modifier combination that we don't handle
		}
	}
	
	/**
	 ** Save the current key bindings to disk.
	 **/
	public void save_key_bindings ()
	{
		String filename = red_prefs_file;
	
		try {
			FileOutputStream fos = new FileOutputStream(filename);
			ObjectOutputStream file = new ObjectOutputStream(fos);
			file.writeObject(RedVersion.versionString());
			file.writeObject(plain_keymap);
			file.writeObject(shift_keymap);
			file.writeObject(ctrl_keymap);
			file.writeObject(alt_keymap);
			file.writeObject(shift_ctrl_keymap);
			file.writeObject(shift_alt_keymap);
			file.writeObject(ctrl_alt_keymap);
			file.close ();
		}
		catch(Exception e) {
			RedEditorManager.red.messages.show_error (frame, Messages.ErrSaveBindings);
		}
	}
	
	/**
	 ** Load the saved key bindings from disk.
	 **/
	public boolean load_key_bindings ()
	{
		String filename = red_prefs_file;
		String version;
	
		try {
			FileInputStream fos = new FileInputStream(filename);
			ObjectInputStream file = new ObjectInputStream(fos);
	
			version = (String)file.readObject();
			if(!version.equals(RedVersion.versionString()))
			{
		RedEditorManager.red.messages.show_error (frame, Messages.ErrReadKeys);
		file.close ();
		return false;
			}
			plain_keymap = (Keymap)file.readObject();
			shift_keymap = (Keymap)file.readObject();
			ctrl_keymap = (Keymap)file.readObject();
			alt_keymap = (Keymap)file.readObject();
			shift_ctrl_keymap = (Keymap)file.readObject();
			ctrl_alt_keymap = (Keymap)file.readObject();
	
			file.close ();
		}
		catch(Exception e) {
			return false;			// bindings file does not exist
		}
	
		if (!version.equals(RedVersion.versionString()))	// if not in current format...
		save_key_bindings ();			//	 ...save
	
		return true;
	}
	
	/**
	 ** define a new user function
	 **/
	private void defun (int index, String name, String help)
	{
		func_table[index] = new Function (name, index, help);
	}
	
	/**
	 ** Add a key to envoke the function last defined.
	 **/
	private void defkey (Keymap kmap, int key, int func)
	{
		kmap.bind (key, func);
	}
	
	
	//	defmenu: Define a menu item to call a function.
	/////////
	/*
	inline void USERFUNC::defmenu (MenuItemId item, UserFuncId func)
	{
		KeySym key;
		char* str = NULL;
	
		menu_func[item] = func;
	
		// search for key binding for this function
	
		plain_keymap->init_search ();
		shift_keymap->init_search ();
		alt_keymap->init_search ();
		ctrl_keymap->init_search ();
		shift_alt_keymap->init_search ();
		ctrl_alt_keymap->init_search ();
		shift_ctrl_keymap->init_search ();
	
		if ((key = alt_keymap->search_key (func)) != XK_VoidSymbol)
		str = alt_keymap->key_string (key);
		else if ((key = plain_keymap->search_key (func)) != XK_VoidSymbol)
		str = plain_keymap->key_string (key);
		else if ((key = shift_keymap->search_key (func)) != XK_VoidSymbol)
		str = shift_keymap->key_string (key);
		else if ((key = ctrl_keymap->search_key (func)) != XK_VoidSymbol)
		str = ctrl_keymap->key_string (key);
		else if ((key = shift_alt_keymap->search_key (func)) != XK_VoidSymbol)
		str = shift_alt_keymap->key_string (key);
		else if ((key = ctrl_alt_keymap->search_key (func)) != XK_VoidSymbol)
		str = ctrl_alt_keymap->key_string (key);
		else if ((key = shift_ctrl_keymap->search_key (func)) != XK_VoidSymbol)
		str = shift_ctrl_keymap->key_string (key);
	
		if (str) {
		menu_accel[item] = utility->create_string_ltor (str);
		delete [] str;
		}
	}
	*/
	
	//	deftool: Define a tool button to call the function last defined.
	/*
	inline void USERFUNC::deftool (int button, char *name, UserFuncId func)
	{
		tool_func[button] = func;
		tool_name[button] = name;
	}
	*/
	
	/**
	 ** create a function object for each user function and
	 ** link then in the list 'function_list'. Link entries in keymaps to
	 ** these functions, so they can be called when a key is pressed.
	 **/
	private void init_functions ()
	{
		groups [FGEdit] = UFNewLine;					// GROUP 0
		group_names [FGEdit] = "Edit Functions";
	
		defun (UFNewLine, "new-line",
			 "Insert a new line at cursor position.");
	
		defun (UFOpenLine, "open-line",
			"Open a new line and leave the cursor in the current line.");
	
		defun (UFDeleteChar, "delete-char",
			"Delete the character to the right of the cursor.");
	
		defun (UFBackDeleteChar, "backward-delete-char",
			"Delete the character to the left of the cursor.");
	
		defun (UFBackDeleteUntab, "backward-delete-untab",
			"Like \"backward-delete-char\", but if the character to be deleted\n"+
			"is a TAB, then convert that TAB into spaces first.");
	
		defun (UFTabToTabStop, "tab-to-tab-stop",
			"Insert a TAB into the text.	This moves the cursor to the next tab\n"+
		"stop.	Tab stops are every eight characters.");
	
		defun (UFHalfTab, "half-tab",
			"Move cursor to next half-tab stop (every four characters), using TABs\n"+
			"and spaces as appropriate.");
	
		defun (UFIndent, "indent",
			"Move cursor to same indentation as next word on the line above.");
	
		defun (UFNewLineIndent, "new-line-and-indent",
			"Insert a new line and indent as line above.");
	
		defun (UFCutWord, "cut-word",
			"Cut the whole word the cursor is currently in and place\n"+
			"it into the paste buffer.");
	
		defun (UFCutToEOWord, "cut-to-end-of-word",
			"Cut from the cursor position to the end of the word and place\n"+
			"it into the paste buffer.");
	
		defun (UFCutLine, "cut-line",
			"If no selection is on, cut the line in which the cursor is to the\n"+
			"paste buffer.	If a selection is on, cut the selection instead.");
	
		defun (UFCutToEOLine, "cut-to-end-of-line",
			"Cut the text from the cursor position to the end of the line and place\n"+
			"it into the paste buffer.");
	
		defun (UFCutRegion, "cut-region",
			"Cuts the current region (area between the cursor and mark).	The text is\n"+
			"placed into the paste buffer.	It can then be pasted by using the 'paste'\n"+
			"command.");
	
		defun (UFCut, "cut",
			"Cuts the current selection. That is: Removes the current selection\n"+
			"from the text and places it into the paste buffer. It can then be\n"+
			"pasted by using the 'paste' command.");
	
		defun (UFPaste, "paste",
			"Pastes the contents of the paste buffer at the cursor position into\n"+
			"the text.	The paste buffer contains the taxt last selected or cut.");
	
		defun (UFSelectWord, "select-word",
			"Select the word in which the cursor is.");
						 
		defun (UFSelectLine, "select-line",
			"Select the complete line in which the cursor is.");
	
		defun (UFSelectRegion, "select-region",
			"Select the text area between the mark and the cursor.\n"+
			"The mark must be set before calling this function.");
	
		defun (UFSelectAll, "select-all",
			"Select the whole text.");
	
		defun (UFShiftLeft, "shift-left",
			"Shift the content of the current line (or, if the selection is on, every\n"+
			"line in the current selection) one character to the left.	If the leftmost\n"+
			"character in the line is not a whitespace character, the function has no\n"+
			"effect.");
	
		defun (UFShiftRight, "shift-right",
			"Shift the content of the current line (or, if the selection is on, every\n"+
			"line in the current selection) one character to the right.	This is done\n"+
			"by inserting spaces or TABs at the beginning of the line.");
	
		defun (UFInsertComment, "comment",
			"Comment out the current line (or, if the selection is on, every line in\n"+
			"the selection).	The strings used for start and end of comments are defined\n"+
			"in the \"Preferences\" dialog box.	Use \"uncomment\" to remove the\n"+
			"comment symbols.");
	
		defun (UFRemoveComment, "uncomment",
			"Remove comment symbols at beginning and end of the current line (or, if\n"+
			"the selection is on, every line in the selection).	The strings used for\n"+
			"start and end of comments are defined in the \"Preferences\" dialog box.");
	
		defun (UFInsertFile, "insert-file",
			"Insert the contents of another file at the cursor position\n"+
			"into the current text.");
	
	
		groups [FGMove] = UFForwardChar;				// GROUP 1
		group_names [FGMove] = "Move & Scroll Functions";
	
		defun (UFForwardChar, "forward-char",
			"Move the cursor one character forward.");
	
		defun (UFBackwardChar, "backward-char",
			"Move the curser one character back.");
	
		defun (UFForwardWord, "forward-word",
			"Move the cursor one word forward.");
	
		defun (UFBackwardWord, "backward-word",
			"Move the cursor one word back.");
	
		defun (UFEndOfLine, "end-of-line",
			"Move the cursor to the end of the current line.");
	
		defun (UFBegOfLine, "beginning-of-line",
			"Move the cursor to the beginning of the current line.");
	
		defun (UFNextLine, "next-line",
			"Move cursor down into next text line");
	
		defun (UFPrevLine, "previous-line",
			"Move cursor up into previous text line");
	
		defun (UFScrollLineDown, "scroll-line-down",
			"Scroll text window one line down (text scrolls up)");
	
		defun (UFScrollLineUp, "scroll-line-up",
			"Scroll text window one line up (text scrolls down)");
	
		defun (UFScrollHPDown, "scroll-half-page-down",
			"Scroll text window half a page down (text scrolls up)");
	
		defun (UFScrollHPUp, "scroll-half-page-up",
			"Scroll text window half a page up (text scrolls down)");
	
		defun (UFPrevPage, "previous-page",
			"Move display window to previous page of text. The original two lines at\n"+
			"the top of the screen are shown at the bottom of the screen.");
	
		defun (UFNextPage, "next-page",
			"Move display window to next page of text. The original two lines at the\n"+
			"bottom of the screen are shown at the top of the screen.");
	
		defun (UFBegOfText, "beginning-of-text",
			"Move cursor to the beginning of the text. Redisplay if out of screen.");
	
		defun (UFEndOfText, "end-of-text",
			"Move cursor to the end of the text. Redisplay if out of screen.");
	
		defun (UFSwapCursorMark, "swap-cursor-mark",
			"Swap the cursor and the mark. Redisplay the screen if\n"+
			"cursor is not on screen any more.");
	
	
		group_names [FGFile] = "File Functions";
		groups [FGFile] = UFNew;					// GROUP 2
	
		defun (UFNew, "new",
			"Open a new, empty window.");
	
		defun (UFOpen, "open",
			"Open a file for editing in another window. A file selection\n"+
			"dialog is shown to select the file.\n");
	
		defun (UFOpenSel, "open-selection",
			"Try to open a file with the file name defined by the current selection.\n"+
			"The selection can be inside the Red window or anywhere else on the screen.\n"+
			"An error is reported if the current selection does not corrospond the the\n"+
			"name of an existing file.");
	
		defun (UFSave, "save",
			"Save the buffer to file if modified.\n"+
			"Makes the previous version into a backup file.");
	
		defun (UFSaveAs, "save-as",
			"Save the buffer under a new file name.	A file selection box is\n"+
			"opened to specify a name.\n"+
			"Makes the previous version of a file with that name, if it existed,\n"+
			"into a backup file.");
	
		defun (UFRevert, "reload",
			"Reload buffer from file.\n"+
			"All changes since the last save will be lost.");
	
		defun (UFClose, "close",
			"Close the editor window. If it was the only Red window\n"+
			"open, leave Red.");
	
		defun (UFPrint, "print",
			"Print the buffer. A dialog is opened in which the\n"+
			"printer can be specified.");
	
		groups [FGCustom] = UFPreferences;				// GROUP 3
		group_names [FGCustom] = "Customisation Functions";
	
		defun (UFPreferences, "preferences",
			"Shows a dialog to set some preferences for behaviour of Red.");
	
		defun (UFKeyBindings, "key-bindings",
			"Opens this dialog that you are watching now.	Shows a list of all\n"+
			"user functions, their key bindings and a short description.	It\n"+
			"also shows the keys which call this function and allows to add or\n"+
			"remove key bindings.");
	/*
		defun (UFEditToolb, "edit-toolbar",
			"NOT YET IMPLEMENTED!!");
	
		defun (UFSetFonts, "set-fonts",
			"NOT YET IMPLEMENTED!!");
	
		defun (UFSetColours, "set-colours",
			"NOT YET IMPLEMENTED!!");
	*/
	
		groups [FGHelp] = UFDescribeKey;				// GROUP 4
		group_names [FGHelp] = "Help Functions";
	
		defun (UFDescribeKey, "describe-key",
			"Prints out the name of the function called by a key.	After calling\n"+
			"describe-key, press the key you are interested in and the associated\n"+
			"function will be shown in the information area.");
	
		defun (UFShowManual, "show-manual",
			"Show the online manual for Red.	The manual is an HTML document and a\n"+
			"WWW browser is started to display it.	The default browser is Netscape,\n"+
			"but this can be changed using Red's X resources (see the \"Customisation\"\n"+
			"section in the manual).");
	
	
		groups [FGMisc] = UFUndo;					// GROUP 5
		group_names [FGMisc] = "Misc. Functions";
	
		defun (UFUndo, "undo",
			"Undo the last editing command.	Undo can be called repeatedly to undo\n"+
			"several previous changes.	The number of possible undos currently is 40.\n"+
			"Undos themselves are recorded on the undo-stack and can be undone (redo).\n"+
			"To redo, use a non-undo function (e.g. move cursor), then undo again.");
	
		defun (UFFind, "find",
			"Find a word or string in the buffer. Starts a dialog to specify\n"+
			"some options.	Find forward is the default option.");
	
		defun (UFFindBackward, "find-backward",
			"Same as \"find\", but the default button is \"Find Backward\"");
	
		defun (UFFindNext, "find-next",
			"Finds the string currently selected or, if nothing is selected, the same pattern\n"+
			"as in the last find operation.	Uses the same parameters (direction, case\n"+
			"sensitivity, whole word option) as the last find operation. Stops at the end\n"+
			"of the buffer. If called again, wraps around to the beginning (end) of the buffer.");
	
		defun (UFFindNextRev, "find-next-reverse",
			"Like \"find-next\", but reverse the direction of the search.");
	
		defun (UFReplace, "replace",
			"Replace string in the buffer with another one. Starts a dialog to specify\n"+
			"some options.");
	
		defun (UFSetMark, "set-mark",
			"Set the mark at the current cursor position.");
	
		defun (UFGotoLine, "goto-line",
			"Move cursor to line by line number.	The line number is prompted for\n"+
			"after starting this command.\n");
	
		defun (UFShowLine, "show-line-number",
			"Display the current line number in the information area.");

// 		defun (UFDefMacro, "define-macro",
// 			"Record the following keq sequence until the next call to\n"+
// 			"\"end-macro\" and store it as a macro.\n\n"+
// 			"(NOT YET IMPLEMENTED!)");
// 	
// 		defun (UFEndMacro, "end-macro",
// 			"End the current macro recording, ask for a key, and bind execution\n"+
// 			"of the macro to that key. (Must be currently defining a macro.\n\n"+
// 			"(NOT YET IMPLEMENTED!)");
// 	
// 		defun (UFRunMacro, "run-macro",
// 			"...\n"+
// 			"...\n\n"+
// 			"(NOT YET IMPLEMENTED!)");
// 	
// 	
// 		defun (UFInterface, "interface",
// 			"NOT YET IMPLEMENTED!!");
// 	
// 			defun (UFInterface, null, null);
	
	
		defun (UFRedisplay, "redisplay",
			"Center cursor in the window and redisplay the screen.");

// 		defun (UFBlueNewRout, "blue-new-routine",
// 			"Insert a skeleton for a Blue routine into the text.");
// 	
// 		defun (UFBlueExpand, "blue-expand",
// 			"Expand a Blue control structure.	If the cursor is in or behind one of\n"+
// 			"the words \"if\", \"loop\" or \"case\", a skeleton for the respective\n"+
// 			"control structure is inserted.	The function next-flag may be used to\n"+
// 			"move the cursor to flagged positions in the structure.");
// 	
// 		defun (UFStatus, "edit-status",
// 			"Print out some debugging information.");
	
		defun (UFCompile, "compile",
			"Compile this class.	If an error is detected, show the error.");

// 			defun (UFCompile, null, null);
	
	

// 		groups [FGDebug] = UFSetBreak;					// GROUP 5
// 		group_names [FGDebug] = "Debug Functions";
// 	
// 		defun (UFSetBreak, "set-breakpoint",
// 			"Set a breakpoint at the current line.\n"+
// 			"A breakpoint can only be set after the class was compiled.");
// 	
// 		defun (UFClearBreak, "clear-breakpoint",
// 			"Clear the breakpoint in the current line.\n"+
// 			"If there is no breakpoint in the current line, this function has no effect.");
// 	
// 		defun (UFStep, "step",
// 			"Execute the next line of the program text.\n"+
// 			"This is possible only if the machine has been started and stopped.");
// 	
// 		defun (UFStepInto, "step-into",
// 			"\"step-into\" is similar to \"step\", but if the statement to be executed is a\n"+
// 			"routine call, then \"step-into\" goes to the next line inside the routine (while \n"+
// 			"\"step\" executes the whole routine as one statement).");
// 	
// 		defun (UFContinue, "continue",
// 			"Continue execution of the project after it has been interrupted.");
// 	
// 		defun (UFTerminate, "terminate",
// 			"Terminate execution of a project after it has been interrupted.");

	////		defun (UFSetBreak, null, null);
	////		defun (UFClearBreak, null, null);
	////		defun (UFStep, null, null);
	////		defun (UFStepInto, null, null);
	////		defun (UFContinue, null, null);
	////		defun (UFTerminate, null, null);
	
	
		groups [FGHidden] = UFSelfInsert;		// last group (grp 6) is hidden
		group_names [FGHidden] = "Hidden Functions";
	
		defun (UFSelfInsert, "self-insert", "");
	}
	
	/**
	 ** Initialise all menu, tool bar and key bindings.
	 **/
	private void init_function_bindings ()
	{
		if (!load_key_bindings ())		// try to load from file
			default_key_bindings ();
	
	//////	init_menu_bindings ();
	}
	
	//	init_menu_bindings: Initialise all menu and tool bar bindings.
	/*
	void USERFUNC::init_menu_bindings ()
	{
	#ifdef RED_ONLY
		defmenu (NewItem, UFNew);
		defmenu (OpenItem, UFOpen);
		defmenu (OpenSelItem, UFOpenSel);
	#endif
		defmenu (SaveItem, UFSave);
	#ifdef RED_ONLY
		defmenu (SaveAsItem, UFSaveAs);
	#endif
		defmenu (RevertItem, UFRevert);
		defmenu (PrintItem, UFPrint);
		defmenu (CloseItem, UFClose);
	
		defmenu (UndoItem, UFUndo);
		defmenu (CutItem, UFCut);
		defmenu (PasteItem, UFPaste);
		defmenu (SelectAllItem, UFSelectAll);
		defmenu (CommentItem, UFInsertComment);
		defmenu (UncommentItem, UFRemoveComment);
	#ifdef RED_ONLY
		defmenu (InsertFileItem, UFInsertFile);
	#endif
	
		defmenu (FindItem, UFFind);
		defmenu (FindNextItem, UFFindNext);
		defmenu (ReplaceItem, UFReplace);
		defmenu (GotoLineItem, UFGotoLine);
		defmenu (ShowLineItem, UFShowLine);
	#ifndef RED_ONLY
		defmenu (CompileItem, UFCompile);
		defmenu (InterfaceItem, UFInterface);
	#endif
	
		defmenu (SetBreakItem, UFSetBreak);
		defmenu (ClearBreakItem, UFClearBreak);
		defmenu (StepItem, UFStep);
		defmenu (StepIntoItem, UFStepInto);
		defmenu (ContinueItem, UFContinue);
		defmenu (TerminateItem, UFTerminate);
	
		defmenu (PrefItem, UFPreferences);
		defmenu (KeyBindItem, UFKeyBindings);
		defmenu (EditToolbarItem, UFEditToolb);
		defmenu (FontsItem, UFSetFonts);
		defmenu (ColoursItem, UFSetColours);
	
		defmenu (FunctionsItem, UFKeyBindings);
		defmenu (DescribeItem, UFDescribeKey);
		defmenu (ManualItem, UFShowManual);
	
	#ifdef RED_ONLY
		deftool (0, "Save", UFSave);
		deftool (1, "Open...", UFOpen);
		deftool (2, "Undo", UFUndo);
		deftool (3, "Cut", UFCut);
		deftool (4, "Paste", UFPaste);
		deftool (5, "Find...", UFFind);
		deftool (6, "Find Next", UFFindNext);
		deftool (7, "Replace...", UFReplace);
		deftool (8, "Close", UFClose);
	#else
		deftool (0, "Interface", UFInterface);
		deftool (1, "Compile", UFCompile);
		deftool (2, "Undo", UFUndo);
		deftool (3, "Cut", UFCut);
		deftool (4, "Paste", UFPaste);
		deftool (5, "Find...", UFFind);
		deftool (6, "Find Next", UFFindNext);
		deftool (7, "Replace...", UFReplace);
		deftool (8, "Close", UFClose);
	#endif
	}
	*/
	
	/**
	 ** Set the key bindings to the default.
	 **/
	private void default_key_bindings ()
	{
		plain_keymap.init ();
		shift_keymap.init ();
		ctrl_keymap.init ();
		alt_keymap.init ();
		shift_ctrl_keymap.init ();
		shift_alt_keymap.init ();
		ctrl_alt_keymap.init ();
		plain_keymap.bind_printables (UFSelfInsert);
		shift_keymap.bind_printables (UFSelfInsert);
	
		defkey (plain_keymap, KeyEvent.VK_ENTER, UFNewLine);
	
		defkey (ctrl_keymap,	KeyEvent.VK_O, UFOpenLine);
	
		defkey (ctrl_keymap,	KeyEvent.VK_D, UFDeleteChar);
		defkey (ctrl_keymap,	KeyEvent.VK_RIGHT, UFDeleteChar);
		defkey (plain_keymap,	KeyEvent.VK_DELETE, UFDeleteChar);
	
		defkey (plain_keymap,	KeyEvent.VK_BACK_SPACE, UFBackDeleteChar);
		defkey (ctrl_keymap,	KeyEvent.VK_LEFT, UFBackDeleteChar);
		
		defkey (plain_keymap,	KeyEvent.VK_TAB, UFTabToTabStop);
	
		defkey (shift_keymap,	KeyEvent.VK_TAB, UFIndent);
	
		defkey (shift_keymap,	KeyEvent.VK_ENTER, UFNewLineIndent);		defkey (ctrl_alt_keymap,	KeyEvent.VK_LEFT, UFCutWord);
	
		defkey (ctrl_alt_keymap,	KeyEvent.VK_RIGHT, UFCutToEOWord);

		defkey (shift_ctrl_keymap,	KeyEvent.VK_LEFT, UFCutLine);
		defkey (plain_keymap,	KeyEvent.VK_F4, UFCutLine);
	
		defkey (shift_ctrl_keymap,	KeyEvent.VK_RIGHT, UFCutToEOLine);
		defkey (ctrl_keymap,	KeyEvent.VK_K, UFCutToEOLine);
	
	//	defkey (, , UFCutRegion);
	
		defkey (alt_keymap,	KeyEvent.VK_X, UFCut);
		defkey (plain_keymap,	KeyEvent.VK_F1, UFCut);
	
		defkey (plain_keymap,	KeyEvent.VK_F3, UFPaste);
		defkey (alt_keymap,	KeyEvent.VK_V, UFPaste);
	////////		defkey (plain_keymap,	KeyEvent.VK_INSERT, UFPaste);
	
		defkey (ctrl_keymap,	KeyEvent.VK_W, UFSelectWord);
						 
		defkey (plain_keymap,	KeyEvent.VK_F2, UFSelectLine);
		defkey (ctrl_keymap,	KeyEvent.VK_L, UFSelectLine);
	
		defkey (shift_ctrl_keymap,	KeyEvent.VK_SPACE, UFSelectRegion);
		defkey (plain_keymap,	KeyEvent.VK_F8, UFSelectRegion);
		defkey (alt_keymap,	KeyEvent.VK_A, UFSelectAll);
	
		defkey (alt_keymap,	KeyEvent.VK_COMMA, UFShiftLeft);
		defkey (alt_keymap,	KeyEvent.VK_PERIOD, UFShiftRight);
		defkey (ctrl_keymap,	KeyEvent.VK_C, UFInsertComment);
		defkey (shift_ctrl_keymap,	KeyEvent.VK_C, UFRemoveComment);
	
		defkey (alt_keymap,	KeyEvent.VK_Q, UFInsertFile);
	
		defkey (plain_keymap,	KeyEvent.VK_RIGHT, UFForwardChar);
		defkey (plain_keymap,	KeyEvent.VK_LEFT, UFBackwardChar);
		defkey (alt_keymap,	KeyEvent.VK_RIGHT, UFForwardWord);
		defkey (alt_keymap,	KeyEvent.VK_LEFT, UFBackwardWord);
		defkey (shift_keymap,	KeyEvent.VK_RIGHT, UFEndOfLine);
		defkey (shift_keymap,	KeyEvent.VK_LEFT, UFBegOfLine);
		defkey (plain_keymap,	KeyEvent.VK_DOWN, UFNextLine);
		defkey (plain_keymap,	KeyEvent.VK_UP, UFPrevLine);
		defkey (shift_keymap,	KeyEvent.VK_DOWN, UFScrollLineDown);
		defkey (shift_keymap,	KeyEvent.VK_UP, UFScrollLineUp);
		defkey (shift_alt_keymap,	KeyEvent.VK_DOWN, UFScrollHPDown);
		defkey (shift_alt_keymap,	KeyEvent.VK_UP, UFScrollHPUp);
		defkey (plain_keymap,	KeyEvent.VK_PAGE_UP, UFPrevPage);
		defkey (alt_keymap,	KeyEvent.VK_UP, UFPrevPage);
		defkey (plain_keymap,	KeyEvent.VK_PAGE_DOWN, UFNextPage);
		defkey (alt_keymap,	KeyEvent.VK_DOWN, UFNextPage);
		defkey (plain_keymap,	KeyEvent.VK_HOME, UFBegOfText);
		defkey (shift_ctrl_keymap,	KeyEvent.VK_UP, UFBegOfText);
		defkey (plain_keymap,	KeyEvent.VK_END, UFEndOfText);
		defkey (shift_ctrl_keymap,	KeyEvent.VK_DOWN, UFEndOfText);
		defkey (ctrl_keymap,	KeyEvent.VK_X, UFSwapCursorMark);
		defkey (ctrl_keymap,	KeyEvent.VK_TAB, UFNextFlag);
	
	
		defkey (alt_keymap,	KeyEvent.VK_N, UFNew);
		defkey (alt_keymap,	KeyEvent.VK_O, UFOpen);
		defkey (shift_alt_keymap,	KeyEvent.VK_O, UFOpenSel);
		defkey (alt_keymap,	KeyEvent.VK_S, UFSave);
		defkey (alt_keymap,	KeyEvent.VK_W, UFClose);
		defkey (alt_keymap,	KeyEvent.VK_P, UFPrint);
	
		defkey (alt_keymap,	KeyEvent.VK_K, UFKeyBindings);
	
		defkey (alt_keymap,	KeyEvent.VK_D, UFDescribeKey);
	
		defkey (alt_keymap,	KeyEvent.VK_Z, UFUndo);
		defkey (plain_keymap,	KeyEvent.VK_F12, UFUndo);
	
		defkey (alt_keymap,	KeyEvent.VK_F, UFFind);
	
		defkey (shift_alt_keymap,	KeyEvent.VK_F, UFFindBackward);
	
		defkey (alt_keymap,	KeyEvent.VK_G, UFFindNext);
	
		defkey (shift_alt_keymap,	KeyEvent.VK_G, UFFindNextRev);
	
		defkey (alt_keymap,	KeyEvent.VK_R, UFReplace);
	
		defkey (ctrl_keymap,	KeyEvent.VK_SPACE, UFSetMark);
		defkey (plain_keymap,	KeyEvent.VK_F7, UFSetMark);
	
		defkey (plain_keymap,	KeyEvent.VK_F5, UFGotoLine);
	
		defkey (plain_keymap,	KeyEvent.VK_F6, UFShowLine);
	
		defkey (ctrl_keymap,	KeyEvent.VK_OPEN_BRACKET, UFDefMacro);
	
		defkey (ctrl_keymap,	KeyEvent.VK_CLOSE_BRACKET, UFEndMacro);
		defkey (ctrl_keymap,	KeyEvent.VK_M, UFRunMacro);
	
		defkey (alt_keymap,	KeyEvent.VK_L, UFRedisplay);

		defkey (ctrl_keymap,	KeyEvent.VK_R, UFBlueNewRout);
		defkey (ctrl_keymap,	KeyEvent.VK_E, UFBlueExpand);
	
		defkey (shift_ctrl_keymap,	KeyEvent.VK_D, UFStatus);

		defkey (alt_keymap,	KeyEvent.VK_C, UFCompile);

	/*
		defkey (ctrl_keymap, XK_B, UFSetBreak);
		defkey (shift_ctrl_keymap, XK_B, UFClearBreak);
	
		defkey (ctrl_keymap, XK_S, UFStep);
		defkey (shift_ctrl_keymap, XK_S, UFStepInto);
		defkey (ctrl_keymap, XK_U, UFContinue);
		defkey (ctrl_keymap, XK_T, UFTerminate);
	*/
	}
} // end class UserFunc
