// NO PACKAGE.

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;

/**
  * A Simple installer.
  * <p>
  * Usage: 
  * <pre><blockquote>
  * <code>java install build</code> <i>properties_file</i>
  * </pre></blockquote>
  * <p>
  * This modifes the install.class file, which can then be run as
  * <pre><blockquote>
  * <code>java install</code>
  * </pre></blockquote>
  *
  * IMPLEMENTATION NOTE:  This is not an object oriented program; in
  * fact it cannot be since we want to jam everything into a single
  * .class file.  This means we can't use some of the normal tricks
  * we'd like to, and have to resort to less elegant methods.  Que sara.
  *
  * @version $Id: install.orig.java 174 1999-07-08 23:00:28Z mik $
  *
  * @author  Andrew Hunt, Toolshed Technologies Inc.
  * <a href="mailto:andy@toolshed.com">Andy@Toolshed.com</a><p>
  * Copyright &#169 1998 Toolshed Technologies, Inc.
  * @author  modified my Michael Kolling to use user specified java path
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation; either version 2 of the License, or
  * (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program; if not, write to the Free Software
  * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
  */

public class install extends Panel 
	implements ActionListener, LayoutManager2, ImageObserver
{
    static final int BUFFER_SIZE=8192;

    Frame myFrame;
    Dialog myDialog;
    Panel myPanel;
    Vector myComponentList;
    String myTitle;
    Image myImage;
    int myImageHeight;
    int myImageWidth;
    TextField myDirField;
    String myDir;
    TextField myJavaField;
    String myJava;
    String myTagLine;
    Label myStatusField;
    Dialog myStatusDlg;
    Hashtable myUserFields;

    Font myFont;
    FontMetrics myFm;
    Font myBigFont;
    FontMetrics myBigFm;

    Color myBkgColor;
    Color myGradient;
    Color myFancyColor;
    Color myFancyShadow;

    Dimension mySize;
    Hashtable myCapsule;
    Hashtable myChildren;
    Vector myFancyText;
    boolean is8Bit;

    Image offScreen;
    Dimension offScreenSize;
    int myPanelNum;
    long myTotalBytes;

    ///////////////////////////////////////////////////////////////////////
    // Build Time
    ///////////////////////////////////////////////////////////////////////

    /**
     ** Helper function to read a GIF off the disk.
     **/
    public static byte[] readGIF(String name)
    {
	int len = (int)(new File((String)name)).length();
	byte[] buffer = new byte[len];
	System.err.println ("Loading gif '" + name + "' (" + len + " bytes)");
	try {
	    FileInputStream fs = new FileInputStream((String)name);
	    fs.read(buffer,0,len);
	    fs.close();
	} catch (IOException ge) {
	    System.err.println ("Couldn't load gif: " + ge.getMessage());
	}
		
	return buffer;
    }

    /**
     ** Load the properties file and create a capsule to be used at install
     ** time.
     **/
    public static Hashtable loadSpec(String fileName) {

	Hashtable capsule = new Hashtable();
	Properties props = new Properties();
	try {
	    FileInputStream rdr = new FileInputStream(fileName);
	    props.load(rdr);
	} catch (Exception e) {
	    System.err.println ("Exception " + e.getMessage());
	}

	String cspec = "color.";
	String gspec = "gif.";
	Enumeration list = props.keys();
	while (list.hasMoreElements()) {
	    String key = (String)list.nextElement();
	    Object value = props.getProperty(key);
	    // take out the install. prefix
	    key = key.substring(key.indexOf('.')+1);

	    // If value is something we need to translate (like a GIF, or a
	    // color) do it here and now.
	    if (key.regionMatches(0,cspec,0,cspec.length())) {
				// It's a color
		StringTokenizer tok = new StringTokenizer((String)value," \t,",false);
		int r = (Integer.valueOf(tok.nextToken())).intValue();
		int g = (Integer.valueOf(tok.nextToken())).intValue();
		int b = (Integer.valueOf(tok.nextToken())).intValue();
		value = new Color(r,g,b);
	    } else if (key.regionMatches(0,gspec,0,gspec.length())) {
				// It's a GIF89A
		byte[] buffer = readGIF((String)value);
		value = buffer;
	    }
	    capsule.put(key,value);
	}

	return capsule;
    }

    public static void main(String[] args) {

	// Build time
	if (args.length > 0) {
	    if (args[0].equals("build")) {
		try {
		    Hashtable capsule = loadSpec(args[1]);
		    RandomAccessFile out = new RandomAccessFile("install.class","rw");
		    BufferedInputStream in = new BufferedInputStream(
				     new FileInputStream((String)capsule.get("pkgJar")));
		    long totalBytes = 0;
		    int bytesRead;
		    byte[] cbuf = new byte[BUFFER_SIZE];
		    out.seek(out.length());
		    System.err.println ("Original file is " + out.length());
		    long orig = out.length();

		    // First, write out the capsule values
		    ObjectOutputStream ostr = 
			new ObjectOutputStream(new FileOutputStream(out.getFD()));
		    if (ostr == null)
			System.err.println ("Could not open output file");
		    ostr.writeObject(capsule);
		    ostr.flush();

		    // Then copy the jar.
		    while ((bytesRead = in.read(cbuf,0,BUFFER_SIZE)) != -1) {
			out.write(cbuf,0,bytesRead);
			totalBytes += bytesRead;
		    }

		    System.err.println ("Jar is " + totalBytes + " bytes.");
		    totalBytes = out.length() - orig;
		    System.err.println ("Added a total of " + totalBytes + " bytes.");
		    System.err.println ("New size is " + out.length());
		    out.writeLong(totalBytes);
		    out.close();
		    in.close();
		    System.err.println ("Done.");
		    System.exit(0);
		} catch (IOException e) {
		    System.err.println ("Couldn't build installer: " + e.getMessage());
		    System.exit(1);
		}
	    } 
	}

	// Install time
	new install();
    }

    ///////////////////////////////////////////////////////////////////////
    // Install time
    ///////////////////////////////////////////////////////////////////////

    public install()
    {
	// Set up the capsule
	unpackTo(false);

	// Run the show
	showGUI();
    }


    /**
     ** Check for any required classes, show a help message if not found.
     **/
    public void checkDepends() {
	String dep = (String)getCapsule("requires");
	int err = 0;
	if (dep != null) {
	    StringTokenizer tok = new StringTokenizer(dep," \t,:;",false);
	    while (tok.hasMoreTokens()) {
		String name = tok.nextToken();
		try {
		    Class.forName(name);
		} catch (ClassNotFoundException e) {
		    String help = (String)getCapsule("requires.help." + name);
		    if (help == null) help = "";
		    notify ("Required class " + name + " not found.\n" + help);
		    err++;
		}
	    }
	}
	if (err > 0) {
	    System.exit(1);
	}
    }

    /**
     ** Internal GUI event dispatch.	We can't use listeners.
     **/
    public void actionPerformed(ActionEvent e) {
	String w = e.getActionCommand();
	if (w.equals("Install")) installBtn();
	else if (w.equals("Cancel")) cancelBtn();
	else if (w.equals("Next")) nextBtn();
	else if (w.equals("Previous")) prevBtn();
	else if (w.equals("OK")) myDialog.dispose();
    }

    /**
     ** Next button action
     **/
    public void nextBtn() {
	myPanelNum++;
	if (myPanelNum > myComponentList.size() -1)
	    myPanelNum = myComponentList.size() -1;
	setPanel(myPanelNum);
    }

    /**
     ** Previous button action
     **/
    public void prevBtn() {
	myPanelNum--;
	if (myPanelNum < 0) myPanelNum = 0;
	setPanel(myPanelNum);
    }

    /**
     ** Cancel button action
     **/
    public void cancelBtn() {
	System.exit(1);
    }

    /**
     ** Install button action
     **/
    public void installBtn() {

	int last = setPanel(myComponentList.size() - 1); // Last one is "blank"

	statusBar(true);

	try {
	    setInstallDir(myDirField.getText());
	    if (installDir().length() == 0)
		throw new RuntimeException("No directory specified");

	    setJavaPath(myJavaField.getText());
	    if (javaPath().length() == 0)
		throw new RuntimeException("No java executable specified");

			// Make sure installDir is accessible (make it if need be)

	    unpackTo(true);

				// Write the scripts, if this is an application
	    if (getCapsule("exeName") != null) {
		writeWindows();
		writeUnix();
	    }

				// and the user fields (if any)
	    writeProps();

	} catch (Exception e) {
	    e.printStackTrace();
	    notify("Installation FAILED:	" + e.getMessage());
	    setPanel(last);
	    statusBar(false);
	    return;
	}

	if (getCapsule("exeName") != null) {
	    notify("The application has been installed to " + installDir() +
		   ".\nTo run the application, execute\n\"" + 
		   (String)getCapsule("exeName") + "\"");
	} else {
	    notify("The package has been installed to " + installDir());
	}

	System.exit(0);
    }

    ///////////////////////////////////////////////////////////////////////
    // Pop-up dialogs
    ///////////////////////////////////////////////////////////////////////

    /**
     ** display running progress
     **/
    public void statusBar(boolean doLaunch)
    {
	myFrame.setVisible(false);
	if (doLaunch)
	    {
		myStatusDlg = new Dialog(myFrame, "Unpacking files...",false);
		Panel panel = new Panel();
		panel.setBackground(myBkgColor);
		panel.setLayout(this);

		myStatusField = new Label("Preparing to unpack all files to installation directory");
		myStatusField.setFont(myFont);
		myStatusField.setBackground(myBkgColor);
		panel.add(myStatusField, new Point(0,0));
			
		myStatusDlg.add(panel);
		myStatusDlg.setSize(400,100);
		myStatusDlg.setLocation(100,100);
		myStatusDlg.show();
	    } else {
		myStatusDlg.dispose();
		myFrame.setVisible(true);
	    }
    }

    /**
     ** Update the status dialog
     **/
    public void setStatus(String text)
    {
	myStatusField.setText(text);
	myStatusField.repaint();
    }

    /**
     ** Pop up a dialog box with the message.
     **/
    public void notify(String msg) {
	if (myFrame == null) {
	    System.err.println(msg);
	    return;
	}
	myDialog = new Dialog(myFrame, "Note", true);
	myDialog.setBackground(myBkgColor);
	Panel panel = new Panel();
	panel.setBackground(myBkgColor);
	panel.setLayout(this);
	StringTokenizer tok = new StringTokenizer(msg,"\n",false);
	int yloc = 10;
	int widest = 500;

	while (tok.hasMoreTokens()) {
	    String m = tok.nextToken();
	    Label lbl = new Label(m);
	    lbl.setFont(myFont);
	    lbl.setBackground(myBkgColor);
	    panel.add(lbl, new Point(10,yloc));
	    yloc += myFm.getHeight()+5;
	    int w = myFm.stringWidth(m);
	    if (w > widest)
		widest = w;
	}
	Button ok = new Button("OK");
	ok.setFont(myFont);
	ok.setActionCommand("OK");
	ok.addActionListener(this);
	panel.add(ok, new Point(widest/2,yloc+10));
	myDialog.add(panel);
	myDialog.setSize(widest,yloc+100);
	myDialog.setLocation(100,100);
	myDialog.show();
    }


    ///////////////////////////////////////////////////////////////////////
    // Dynamic user interface stuff
    ///////////////////////////////////////////////////////////////////////

    /**
     ** Make one of the panels (each one gets its own buttons)
     **/
    public void makePanel(boolean lastOne)
    {

	int q = mySize.width/4;

	if (myPanelNum > 0)
	    makeButton(myPanel,"Previous", q, 350);
	if (lastOne) {
	    makeButton(myPanel,"Install", 2 * q, 350);
	} else {
	    makeButton(myPanel,"Next", 2 * q, 350);
	}
	makeButton(myPanel,"Cancel",3 * q,350);

	makeFancyText(myPanelNum,(String)getCapsule("title"),-1,120,1);
	makeFancyText(myPanelNum,myTagLine,-1,410,0);
	makeFancyText(myPanelNum,
		      "Installer " + version() + 
		      " \u00a9 1998 Toolshed Technologies, Inc.	www.toolshed.com",
		      -1,430,0);
	myComponentList.addElement(myPanel.getComponents());
	layoutContainer(this);
	myPanel.removeAll();
	myPanelNum++;
    }

    /**
     ** Get the user defined data from the hash and build some screens.
     ** @return false if no more user panels in the hash.
     **/
    public boolean buildUserPanel(int screen) {
	int field = 0;
	int inity = 200;
	int w;
	int widest = 0;
	int count = 0;

	// One pass through to get a count and sizes.
	while (true) {
	    String p =(String)getCapsule("screen"+screen+".field"+field+".prompt");
	    if (p != null) {
		count++;
		w = myFm.stringWidth(p);
		if (w > widest) widest = w;
		field++;
	    } else {
		if (field == 0) {
		    return false;
		} else {
		    break;
		}
	    }
	}

	// Now go through and layout the stuff
	field = 0;
	while (true) {
	    String prompt =(String)
		getCapsule("screen"+screen+".field"+field+".prompt");
	    String prop =(String)
		getCapsule("screen"+screen+".field"+field+".property");
	    if (prompt != null && prop != null) {
		makeFancyText(myPanelNum,prompt,10,inity,0);
		TextField t = new TextField(30);
		myUserFields.put(prop, t);
		myPanel.add(t, new Point(widest+25,inity-myFm.getHeight()));
		inity += myFm.getHeight() + 25;
		field++;
	    } else {
		if (field == 0) {
		    return false;
		} else {
		    makePanel(false);
		    return true;
		}
	    }
	}
    }

    /**
     ** Make panel n the current panel.
     ** Again, this is crude, but this is a limited environment.
     **/
    public int setPanel(int n) {
	int ret = myPanelNum;
	myPanelNum = n;
	myPanel.removeAll();
	Component[] arr = (Component[])myComponentList.elementAt(n);
	for (int i=0; i < arr.length; i++) {
	    myPanel.add(arr[i],arr[i].getLocation());
	}
	layoutContainer(this);
	repaint();
	return ret;
    }

    ///////////////////////////////////////////////////////////////////////
    // Build and run the GUI
    ///////////////////////////////////////////////////////////////////////

    /**
     ** Build and fire up the gui.
     **/
    public void showGUI() {

	offScreenSize = new Dimension(0,0);

	myDir = new String();

	myBkgColor = (Color)getCapsule("color.background");
	myGradient = (Color)getCapsule("color.gradient");
	myFancyColor	= (Color)getCapsule("color.text");
	myFancyShadow = (Color)getCapsule("color.shadow");

	myTagLine	= (String)getCapsule("tagline");
	mySize = new Dimension(
			       (Integer.valueOf((String)getCapsule("width"))).intValue(),
			       (Integer.valueOf((String)getCapsule("height"))).intValue()
			       );

	if (getToolkit().getColorModel().getPixelSize() <= 8) {
	    is8Bit = true;
	} else {
	    is8Bit = false;
	}
	myTitle = (String)getCapsule("title");
	myChildren = new Hashtable();
	myFancyText = new Vector();
	myUserFields = new Hashtable();
	myFrame = new Frame("Installer");
	myFont = new Font ("Helvetica",Font.PLAIN,14);
	myFm = getToolkit().getFontMetrics(myFont);
	myBigFont = new Font ("Helvetica",Font.PLAIN,24);
	myBigFm = getToolkit().getFontMetrics(myBigFont);

	myImage = getToolkit().createImage((byte[])getCapsule("gif.logo"));
		
	myComponentList = new Vector();
	myPanel = this;
	setLayout(this);


	myPanel.setBackground(myBkgColor);

	myPanelNum = 0;

	int screen = 0;
	//
	// Build all user-defined panels
	//
	while (buildUserPanel(screen))
	    screen++;

	//
	// Last panel that kicks off the install...
	//
	makeFancyText(myPanelNum,"Directory to install to: ",10,220,0);
	myDirField = new TextField(30);
	myDirField.setText(System.getProperty("user.dir"));
	myPanel.add(myDirField, new Point(250,200));

	if (getCapsule("exeName") != null) {
	    makeFancyText(myPanelNum,"Name of Java executable: ",10,320,0);
	    myJavaField = new TextField(30);
	    myJavaField.setText("java");
	    myPanel.add(myJavaField, new Point(250,300));
	}

	makePanel(true);

	//
	// "Blank" last page
	//
	myComponentList.addElement(new Component[0]);

	setPanel(0);

	myFrame.add(myPanel);
	myFrame.pack();
	myFrame.setVisible(true);
	myFrame.setSize(mySize.width,mySize.height);
	myFrame.setLocation(100,100);

	checkDepends();
    }

    /**
     ** Convenience function to make and wire up a button.
     **/
    public Button makeButton(Panel panel, String label,int x, int y) {
	Button ret = new Button(label);
	ret.setFont(myFont);
	ret.setActionCommand(label);
	ret.addActionListener(this);
	panel.add(ret, new Point(x,y));
	return ret;
    }

    ///////////////////////////////////////////////////////////////////////
    // Layout manager 
    ///////////////////////////////////////////////////////////////////////

    /**
     ** This set of functions implements LayoutManager
     **/
    public void addLayoutComponent(String name, Component comp) {
    }
    public void removeLayoutComponent(Component comp) {
    }
    public Dimension preferredLayoutSize(Container parent) {
	return new Dimension(mySize.width,mySize.height);
    }
    public Dimension minimumLayoutSize(Container parent) {
	return new Dimension(mySize.width,mySize.height);
    }
    public void layoutContainer(Container parent) {
	Insets insets = parent.getInsets();
	Component[] children = parent.getComponents();

	for (int i=0; i < children.length; i++) {
	    Point p = (Point)myChildren.get(children[i]);
	    Dimension d = children[i].getPreferredSize();
	    if (children[i] instanceof TextField) {
				// Expand text fields to right hand edge
		d.width = mySize.width - p.x - insets.left - 25;
	    }
	    Rectangle r = new Rectangle(insets.left + p.x, insets.top + p.y,
					d.width,d.height);
	    children[i].setBounds(r);
	}
    }
    public void addLayoutComponent(Component comp, Object constraints) {
	myChildren.put(comp, constraints);
    }
    public Dimension maximumLayoutSize(Container target) {
	return new Dimension(mySize.width,mySize.height);
    }
    public float getLayoutAlignmentX(Container target) {
	return (float)0.0;
    }
    public float getLayoutAlignmentY(Container target) {
	return (float)0.0;
    }
    public void invalidateLayout(Container target) {
    }

    ///////////////////////////////////////////////////////////////////////
    // Shadow-ed text
    ///////////////////////////////////////////////////////////////////////

    /**
     ** Compute the gradient.
     **/
    public Color fadeToBlue(Color src) {
	int inc = 1;
	int r = src.getRed();
	int g = src.getGreen();
	int b = src.getBlue();
	r -= inc; 
	g -= inc;
	b -= inc;
	if (r < myGradient.getRed()) r = myGradient.getRed();
	if (g < myGradient.getGreen()) g = myGradient.getGreen();
	if (b < myGradient.getBlue()) b = myGradient.getBlue();
	return new Color(r,g,b);
    }

    /**
     ** Set up fancy (shadowed) text to be drawn
     **/
    public void makeFancyText(int which, String label, int x,int y,int isBig) {
	int[] triple = new int[3];
	triple[0] = x;
	triple[1] = y;
	triple[2] = isBig;
	Hashtable ht;
	if (myFancyText.size() <= which) {
	    ht = new Hashtable();
	    myFancyText.addElement(ht);
	} else {
	    ht = (Hashtable)myFancyText.elementAt(which);
	}
	ht.put(label,triple);
    }

    /**
     ** Draw the shadowed text on this screen.
     **/
    public int drawFancy(int which, Graphics g, Color c, int offset)
    {
	Enumeration e;
	int x;
	int y;
	int w;
	int widest = 0;
	// Draw the shadowed text
	Insets insets = getInsets();

	if (g != null)
	    g.setColor(c);

	if (which >= myFancyText.size())
	    return 0; // no text on this panel
	Hashtable ht = (Hashtable)myFancyText.elementAt(which);
	e = ht.keys();

	while (e.hasMoreElements()) {
	    String label = (String)e.nextElement();
	    int[] triple = (int[])ht.get(label);
	    if (triple[2] == 1) {
		if (g != null)
		    g.setFont(myBigFont);
		w = myBigFm.stringWidth(label);
	    } else {
		if (g != null)
		    g.setFont(myFont);
		w = myFm.stringWidth(label);
	    }
	    if (w > widest) widest = w;
	    if (triple[0] == -1) {
		x = insets.left+((mySize.width -w)/2)+offset;
	    } else {
		x = insets.left+triple[0]+offset;
	    }
	    y = insets.top+triple[1]+offset;

	    if (g != null)
		g.drawString(label,x,y);
	}

	return widest;
    }

    ///////////////////////////////////////////////////////////////////////
    // Panel 
    ///////////////////////////////////////////////////////////////////////

    /**
     ** Called to clear the screen and paint
     **/
    public void update(Graphics g) {
	paint(g);
    }

    /**
     ** Paint the application panel.
     **/
    public void paint(Graphics g) {

	mySize = getSize();

	Graphics	off;

	if ((offScreen == null) ||
	    (mySize.width	!= offScreenSize.width) ||
	    (mySize.height != offScreenSize.height)) {

	    offScreen = createImage(mySize.width, mySize.height);
	    offScreenSize = getSize();
	}

	off = offScreen.getGraphics();


	// Draw the logo
	off.drawImage(myImage,(mySize.width-myImageWidth)/2,10,this);

	// And the gradiant
	Color c = myBkgColor;
	if (myImageHeight != 0) {
	    for (int i=myImageHeight+10; i < mySize.height; i+=1) {
		if (is8Bit) {
		    c = myGradient;
		    off.setColor(c);
		} else {
		    c = fadeToBlue(c);
		    off.setColor(c);
		}
		off.fillRect(0,i,mySize.width,1);
	    }
	}

	// Draw drop shadow
	drawFancy(myPanelNum, off,myFancyShadow,2);
	// And then the text
	drawFancy(myPanelNum, off,myFancyColor,0);

	// Call superclass paint for the widgets
	super.paint(off);

	g.drawImage(offScreen, 0, 0, this);
    }

    /**
     ** ImageObserver: we get this callback when the image is ready.
     **/
    public boolean imageUpdate(Image img, int infoflags, int x, int y,
			       int width, int height) {

	if ((infoflags & ImageObserver.ALLBITS) != 0) {
	    if (img == myImage) {
		myImageHeight = height;
		myImageWidth = width;
	    }
	    repaint();
	    return false; // all done
	}
	return true; // more needed
    }

    ///////////////////////////////////////////////////////////////////////
    // File I/O (JAR extraction and scripts)
    ///////////////////////////////////////////////////////////////////////

    /**
     ** Find a classfile in the classpath
     **/
    InputStream getFileFromClasspath(String filename) throws IOException
    {
	String[] cpath = getClassPath(System.getProperty("java.class.path"));
		
	for(int i = 0; i < cpath.length; i++)
	    {
		if(cpath[i].endsWith(".zip") || cpath[i].endsWith(".jar"))
		    {
			InputStream ret = readZip(cpath[i], filename);
			if(ret !=null)
			    return ret;
		    }
		
			else
		    {
			if(File.separatorChar != '/')
			    filename = filename.replace('/', File.separatorChar);
			String fullpath = cpath[i] + File.separator + filename;
			File fd = new File(fullpath);
	
			if(fd.exists())
			    return new FileInputStream(fd);
		    }
	    }
		
	throw new FileNotFoundException(filename);
    }

    InputStream readZip(String classzip, String arg) throws IOException
    {
	ZipFile zipf = new ZipFile(classzip);
	ZipEntry entry = zipf.getEntry(arg);

	if(entry == null)
	    return null;

	long size = entry.getCompressedSize();

	InputStream is = zipf.getInputStream(entry);
	return is;
    }

    public static String[] getClassPath(String cp)
    {
	String[] entries = {};
		
	try {
	    Vector v = new Vector();
			
	    StringTokenizer st = new StringTokenizer(cp, File.pathSeparator);
	    while(st.hasMoreTokens())
		{
		    String entry = st.nextToken();
		    File entryFile = new File(entry);
		    if(entryFile.exists())
			v.addElement(entry);
		}
			
	    entries = new String[v.size()];
	    v.copyInto(entries);
	} catch(Exception e) {
	    e.printStackTrace();
	}
	
	return entries;
    }

    /**
     ** Grab the jar data from the class file and unjar it into the 
     ** install directory.
     **/
    public void unpackTo(boolean doJar) {
	try {
	    InputStream cpin = getFileFromClasspath("install.class");
	    FileOutputStream cpout = new FileOutputStream("install.class.tmp");
	    byte[] buffer = new byte[8192];
	    int len;
	    while((len = cpin.read(buffer)) != -1)
		cpout.write(buffer, 0, len);
	    cpin.close();
	    cpout.close();
			
	    RandomAccessFile in = new RandomAccessFile("install.class.tmp","r");
	    in.seek(in.length() - 8);
	    long size = in.readLong();
	    in.seek(in.length() - 8 - size);
	    myTotalBytes = in.length() - size; // this is wrong!!
	    ObjectInputStream istr = new ObjectInputStream(new FileInputStream(in.getFD()));
	    try {
		myCapsule = (Hashtable)istr.readObject();
	    } catch (ClassNotFoundException nf) {
		System.err.println (nf.getMessage());
	    }
	    if (doJar) {
		dumpJar(installDir(), new FileInputStream(in.getFD()));
	    }
	    in.close();
			
	    (new File("install.class.tmp")).delete();
	} catch (Exception e) {
	    notify("Installer failed to open: " + e.getMessage());
	    if (!doJar)
		System.exit(1);
	}
    }


    /**
     ** Recusrively make directories needed for a file.
     **/
    public void makeDirsFor(String start, String path) {
	String sofar = null;
	StringTokenizer tok = new StringTokenizer(path,"/",false);
	sofar = start;
	while (tok.hasMoreTokens()) {
	    String part = tok.nextToken();
	    if (tok.hasMoreTokens()) {
		File d = new File(sofar + File.separatorChar + part);
		sofar = d.toString();
		d.mkdirs();
	    }
	}
	return;
    }

    /**
     ** Extract a JAR from a file stream to the given directory on disk.
     **/
    public void dumpJar(String dir, FileInputStream in) 
	throws IOException, ZipException
    {
	makeDirsFor(dir,"");
	int percent=0;
	long bytesRead = 0;

	// Make a zip stream
	ZipInputStream zip = new ZipInputStream(in);

	byte[] buffer = new byte[BUFFER_SIZE];
	while (true) {
	    // Get a zip entry
	    ZipEntry z = zip.getNextEntry();
	    if (z == null)
		break;

	    String name = dir + "/" + z.getName();

	    if (z.isDirectory()) {
		File d = new File(name);
		d.mkdirs();
		continue;
	    }

	    // Make any necessary directories
	    if (z.getName().indexOf('/') != -1) {
		makeDirsFor(dir,z.getName());
	    }

	    // Create the file
	    FileOutputStream out;
	    try {
		out = new FileOutputStream(name);
	    } catch (FileNotFoundException e) {
		throw new IOException("Couldn't write to specified file/directory");
	    }
	    int len;
	    while ((len = zip.read(buffer,0, BUFFER_SIZE)) != -1) {
		bytesRead += len;
		out.write(buffer,0,len);
		//percent = (int)(((double)bytesRead/(double)myTotalBytes)*100.00);
		setStatus("extracting: " + name);
	    }
	    zip.closeEntry();
	    out.close();
	}

	zip.close();
	setStatus("Done.");
	return;
    }

    /**
     ** Write out a Unix, Bourne shell script to start the application
     **/
    public void writeUnix() throws IOException {
	File outputFile = new File(installDir(), (String)getCapsule("exeName"));
	FileWriter out = new FileWriter(outputFile.toString());
	out.write("#!/bin/sh\n");
	out.write("APPBASE=" + installDir() + "\n");
	String classpath = getCapsule("classpath").toString();
	classpath = classpath.replace(';', ':');
	classpath = replace(classpath, "~", "$APPBASE");
	out.write("CLASSPATH=$CLASSPATH:" + classpath + "\n");
	out.write("export CLASSPATH\n");
	out.write(javaPath() + " " + getCapsule("javaOpts") + " -D" + 
		  getCapsule("installDirProp") + "=$APPBASE "+ 
		  getCapsule("mainClass") + " $*\n");
	out.close();
		
	try {
	    Runtime.getRuntime().exec("chmod 755 " + outputFile);
	} catch(Exception e) {
	    // ignore it - might not be Unix
	}
    }

    /**
     ** Write out an MSDOS style batch file to start the application.
     **/
    public void writeWindows() throws IOException {
	File outputFile = new File(installDir(),
				   (String)getCapsule("exeName") + ".bat");
			
	FileWriter out = new FileWriter(outputFile.toString());
	out.write("@echo off\r\n");
	out.write("set OLDPATH=%CLASSPATH%\r\n");
	out.write("set APPBASE=" + installDir() + "\r\n");
	String classpath = getCapsule("classpath").toString();
	classpath = classpath.replace('/', '\\');
	classpath = replace(classpath, "~", "%APPBASE%");
	out.write("set CLASSPATH=%CLASSPATH%;" +
		  classpath + "\r\n");
	out.write(javaPath() + " " + getCapsule("javaOpts") + " -D" + 
		  getCapsule("installDirProp") + "=%APPBASE% "+ 
		  getCapsule("mainClass") + " %1 %2 %3 %4 %5 %6 %7 %8 %9\r\n");
	out.write("set CLASSPATH=%OLDPATH%\r\n");

	out.close();
    }

    /**
     ** write out properties file with user-defined fields
     **/
    public void writeProps() throws IOException {

	String propFile = (String)getCapsule("writePropertiesTo");
	if (propFile == null)
	    return;
	File outputFile = new File(installDir(),propFile);
	FileWriter out = new FileWriter(outputFile.toString(),true);
	Enumeration e = myUserFields.keys();
	while (e.hasMoreElements()) {
	    String prop = (String)e.nextElement();
	    String value = ((TextField)(myUserFields.get(prop))).getText();
	    out.write(prop+"="+value+"\n");
	}
	out.close();
    }

    /**
     ** Constructs a new string by replacing any of the characters in
     ** pattern found in src by the string subst
     **/
    String replace(String src, String pattern, String subst)
    {
	StringTokenizer tokenizer = new StringTokenizer(src, pattern);
	StringBuffer ret = new StringBuffer();
		
	while(tokenizer.hasMoreElements())
	    {
		ret.append(subst);
		ret.append(tokenizer.nextToken());
	    }
		
	return ret.toString();
    }
	
    ///////////////////////////////////////////////////////////////////////
    // Property type stuff
    ///////////////////////////////////////////////////////////////////////

    public Object getCapsule(String key) {
	return myCapsule.get(key);
    }

    public String installDir() {
	return myDir;
    }

    public void setInstallDir(String d) {
	myDir = d;
    }

    public String javaPath() {
	return myJava;
    }

    public void setJavaPath(String p) {
	myJava = p;
    }

    public String version() {
	String v = "$Revision: 174 $";
	int colon = v.indexOf(':');
	return v.substring(colon+1,v.length()-1);
    }

} // End class install

