package org.bluej.extensions.submitter;

/**
 * This is the main dialog of the submitter, the starting point of all.
 * It should provide an interface for doing things and this interface
 * should just popup when the user push the submit button.
 * 
 * therefore:
 * 1) The dialog should be constructed on extension loading but NOT shown
 * 2) The dialog should have a reload methods that reloads the values on request
 * 3) It should be able to cancel whatever activity is going on....
 * 
 * The defaultSubmissionScheme value IS ONLY here, the reasonnign is that
 * - It is not a property of the TreeData, that one holds data only
 * - It is not a property of the dialog either....
 */

import org.bluej.extensions.submitter.transport.*;
import org.bluej.utility.*;

import bluej.extensions.*;

import java.awt.event.*;
import java.awt.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import javax.swing.tree.*;

import java.util.Collection;
import java.util.Properties;

import java.io.*;

 
public class SubmitDialog implements ActionListener
  {
  private final static String SELECTED_NODE_PROPERTY = "selectedNode";
  private final static String PROPERTIES_FILENAME = "submitter.properties";
  
  private Stat         stat;
  private JFrame       mainFrame;
  private JTextField   schemeSelected;  // The chosen submission scheme is here.
  private JButton      browseButton;    // When you press this the browsing window opens
  private JTextArea    statusArea;      // Display what is happening here
  private JTextArea    logArea;         // Display what is happening here
  private JButton      submitButton;
  private JButton      cancelButton;
  private JProgressBar progressBar;
  private ResultDialog resultDialog;
  private BPackage     curPkg;
  private Thread       backgroundThread;
  
  /**
   * You have to create this class at the beginning, do not show it but create it.
   */
  public SubmitDialog(Stat i_stat)
    {
    stat = i_stat;

    // Let me put all of it in a nice frame, with the right layout !
    JPanel aPanel = new JPanel();
    aPanel.setLayout(new BoxLayout(aPanel,BoxLayout.Y_AXIS));
    aPanel.setBorder(new EmptyBorder(5,5,5,5));

    aPanel.add(getWorkPanel());
    aPanel.add(getLoggingPanel());

    // -------- TIme to create the real window...
    mainFrame = new JFrame("Submitter");
    mainFrame.addWindowListener(new onWindowClosing());

    ImageIcon icon = new ImageIcon (this.getClass().getResource("World.gif"));
    mainFrame.setIconImage(icon.getImage());
    mainFrame.getContentPane().add(aPanel);
    mainFrame.setLocation(50,50);  // You need this to place the windows on Unix

    // Now that I have the parent frame I can make a dialog
    resultDialog = new ResultDialog(stat, mainFrame);
    }


  /**
   * This gets called when someone whants to submit something
   */
  public void submitThis (BPackage i_curPkg )
    {
    mainFrame.pack();             // You need this, othervise it does not resize right, Damiano
    mainFrame.setVisible(true);   // In ANY case I MUST show something to the user

    if ( backgroundThread != null && backgroundThread.isAlive() )
      {
      statusWriteln("ERROR: Background Task still running");
      return;
      }

    // Only now I can start mesing up with the current status.
    curPkg = i_curPkg;

    // Need to clean up things from possible old run
    statusArea.setText("");
    logArea.setText("");
    
    backgroundThread = new TreeLoadThread ();
    backgroundThread.start();
    }

  /**
   * WHen you want to add somethinf to the status line/text you can call this one.
   */
  public void statusWriteln ( String aMessage )
    {
    statusArea.append(aMessage);
    statusArea.setCaretPosition(statusArea.getText().length());
    // By doing the setCaretBefore this I show the content and not the newline
    statusArea.append("\n");
    }

 /**
   * When you want to add something to the log line/text you can call this one.
   */
  public void logWriteln ( String aMessage )
    {
    logArea.append(aMessage);
    logArea.setCaretPosition(logArea.getText().length());
    // By doing the setCaretBefore this I show the content and not the newline
    logArea.append("\n");
    }

  /**
   * Use this to set the current submissoin scheme
   */
  public void schemeSelectedSet (String curScheme)
    {
    // We store it in the global props
    stat.globalProp.setProperty(GlobalProp.TITLE_VAR,curScheme);

    // We also need it as a short version
    String simpleScheme = curScheme;
    int index = curScheme.lastIndexOf('/');
    if (index >= 0 && (index+1) < curScheme.length() ) simpleScheme = curScheme.substring(index + 1);

    stat.globalProp.setProperty(GlobalProp.SIMPLETITLE_VAR,simpleScheme);

    schemeSelected.setText(curScheme);
    }

  /**
   * Use this to set the current submissoin scheme
   */
  public String schemeSelectedGet ()
    {
    return schemeSelected.getText();
    }
    
// =========================== VARIOUS UTILITYES HERE ==========================


  /**
   * For dialog you need to know this frame.
   */
  JFrame getFrame()
    {
    return mainFrame;
    }


  /**
   * This will manage to return a nice workPanel, where you shoose what to do
   */
  private JPanel getWorkPanel ()
    {
      
    // --------- This is used to have afeeling of something running
    progressBar = new JProgressBar();
    progressBar.setPreferredSize(new Dimension (400,10));
    progressBar.setBorderPainted(false);
    
    // Let me put all of it in a nice frame, with the right layout !
    JPanel aPanel = new JPanel();
    aPanel.setLayout(new BoxLayout(aPanel,BoxLayout.Y_AXIS));
    aPanel.setBorder(new EtchedBorder(EtchedBorder.RAISED));

    aPanel.add(getSubmitPanel());
    aPanel.add(progressBar);
    aPanel.add(getButtonPanel());

    return aPanel;
    }

  /**
   * Returns the panel that allows you to choose the submit scheme
   */
  private JPanel getSubmitPanel()
    {
    // This is a complex layout manager, if you want it :-)
    JPanel submitPanel = new JPanel(new FlowLayout());
    
    // ------- This is the label sayng to choose a dialog
    JLabel schemeLabel = new JLabel(stat.bluej.getLabel("dialog.scheme"));
    submitPanel.add(schemeLabel);

    // ------- This is the text where to choose a submission
    schemeSelected = new JTextField();
    schemeSelected.setPreferredSize(new Dimension(350,26));
    schemeSelected.addActionListener(this); 
    // so that pressing return in the field does the submit
    schemeSelected.getDocument().addDocumentListener(new onFieldChange());
    submitPanel.add(schemeSelected);

    // ------- This is the actual button to browse
    browseButton = new JButton(stat.bluej.getLabel("button.browse"));
    browseButton.addActionListener(this);
    submitPanel.add(browseButton);

    return submitPanel;
    }

  /**
   * Utility, gets the loggingPane.
   * The logging pane is quite complicated :-)
   */
  private JTabbedPane getLoggingPanel ()
    {
    statusArea = new JTextArea();
    statusArea.setLineWrap(true);
    statusArea.setEditable(false);
    JScrollPane statusPane = new JScrollPane(statusArea);
    statusPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

    logArea = new JTextArea();  
    logArea.setEditable(false);
    logArea.setLineWrap(true);
    JScrollPane logPane = new JScrollPane(logArea);
    logPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

    // Now I need to put all of the two into a tab pane
    JTabbedPane tabbed = new JTabbedPane();
    tabbed.setPreferredSize(new Dimension(400,60));
    tabbed.add("Status",statusPane);
    tabbed.add("Log",logPane);

    return tabbed;
    }


  /**
   * Utility, returns the panel containit the lower remaining buttons.
   */
  private JPanel getButtonPanel ()
    {
    JPanel risulPanel  = new JPanel();

    submitButton = new JButton(stat.bluej.getLabel("button.submit"));
    submitButton.addActionListener(this);
    risulPanel.add(submitButton);

    cancelButton = new JButton(stat.bluej.getLabel("cancel"));
    cancelButton.addActionListener(this);
    risulPanel.add(cancelButton);

    return risulPanel;
    }

  /**
   * When somethig gets pressed we come here
   */
  public void actionPerformed(ActionEvent event)
    {
    if ( event == null ) return;
    
    Object eventObject = event.getSource();
    if ( eventObject == null ) return;

    if ( eventObject == browseButton )
      {
      // When the browse button is pressed do a browse tree.
      stat.treeDialog.setVisible(true);
      }
    
    if ( eventObject == cancelButton )
      {
      /* While there is a background thread cancel means just interrupting it
       * Othervise cancel means removing this frame from the view
       */
      if ( backgroundThread != null && backgroundThread.isAlive() ) 
        backgroundThread.interrupt();
      else      
        mainFrame.setVisible(false);
      }

    if ( eventObject == submitButton )
      {
      if ( backgroundThread != null && backgroundThread.isAlive() ) 
        {
        // Cannot do anything if there is a thread running, press CANCEL
        statusWriteln("ERROR: Background task running");
        }
      else
        {
        backgroundThread = new SubmitThread();
        backgroundThread.start();
        }
      }

    }


  /**
   * TIme to try to send some files, we know that they are files...
   * The issue here is to behave reasonably, and manage exceptions.
   *
   * @param  urlProps Properties that are part of this task
   * @return null if there is nothing to display else what is being said by the remote side
   */
  private String sendFiles(UrlRewrite urlProps)
    {
    TransportSession ts = null;
    File[] files = new File[0];
    
    try 
      {
      FileHandler fh = new FileHandler(stat.bluej, curPkg, stat.treeData);
      if ((files=fh.getFiles()) == null) 
        {
        statusWriteln ("sendFiles: NOTICE: no files to send");
        return null;
        }
        
      Collection jarNames = stat.treeData.getProps(".file.jar");
      String jarName=null;

      if (jarNames.isEmpty()) 
          ts = TransportSession.createTransportSession(urlProps.getURL(), stat.globalProp);
      else
        {
        jarName = (String) jarNames.iterator().next();
        ts = TransportSession.createJarTransportSession(urlProps.getURL(), stat.globalProp, jarName);
        }
  
      ts.setTransportReport(new transportReport());
      ts.connect();

      // ere we are sending files, the issue here is to put the prefix
      // as wanted by Michael. Damiano.
      String projNamePrefix = null;
      String tsProtocol = ts.getProtocol();
      if (tsProtocol.equals("ftp") || tsProtocol.equals("file") || jarName != null)
          projNamePrefix = curPkg.getProject().getName();

      for (int index=0; index < files.length; index++) 
        {
        boolean binary = FileHandler.isBinary(files[index]);
        FileInputStream fis = new FileInputStream(files[index]);

        String name = fh.getSubName(files[index]);

        if (projNamePrefix != null)
            name = projNamePrefix + File.separator + name;

        name = name.replace(File.separatorChar, '/');
        statusWriteln(stat.bluej.getLabel("message.sending") + " " + name);
        ts.send(fis, name, binary);
        Utility.inputStreamClose(fis);
        }

      ts.disconnect();
      } 
    catch (Exception ex) 
      {
      // WARNING: There is still open all the issue of CLOSING streams !!!!
      logWriteln ("sendFiles Exception="+ex.toString());
      statusWriteln (translateException(ex));
      return null;
      }

    statusWriteln (files.length + " " + stat.bluej.getLabel("message.filessent"));

    // Lets try to get back possible results....
    return ts.getResult();
    }



  /**
   *  Translates a given exception into a more user friendly form
   *
   * @param  ex  The exception being translated
   * @return     A friendly return string
   */
  private String translateException(Throwable ex)
    {
    if (ex instanceof AbortOperationException)
        ex = ((AbortOperationException) ex).getException();
          
    if (ex instanceof java.net.UnknownHostException)
        return stat.bluej.getLabelInsert("exception.unknownhost", ex.getMessage());
          
    if (ex instanceof java.net.NoRouteToHostException)
        return stat.bluej.getLabel("exception.notroutetohost");
          
    if (ex instanceof java.net.ProtocolException)
        return ex.getMessage();
          
    if (ex instanceof java.io.FileNotFoundException)
        return stat.bluej.getLabelInsert("exception.filenotfound", ex.getMessage());
          
    if (ex instanceof IllegalArgumentException && ex.getMessage().equals("SMTP Host has not been set"))
        return stat.bluej.getLabel("exception.hostnotset");
          
    if (ex instanceof IllegalArgumentException && ex.getMessage().equals("User Email address invalid"))
        return stat.bluej.getLabel("exception.addrnotset");

    // I want to have a message in any case...        
    return ex.getMessage();
    }







  /**
   * This sends a plain mesage
   */
  private String sendMessage (UrlRewrite urlProps)
    {
    try
      {
      TransportSession ts = TransportSession.createTransportSession(urlProps.getURL(), stat.globalProp);
      ts.connect();
      // WARNING: nothing to do ?
      ts.disconnect();
      return ts.getResult();
      }
    catch ( Exception exc )
      {
      statusWriteln ("sendMessage: Exception="+exc.toString());
      }

    return null;  
    }


  /**
   * This does the real work of submitting the stuff, well kind of...
   * This is called from within a Thread, the GUI is alive and well.
   */
  private void submitWork ()
    {
    String result = null;

    UrlRewrite urlProps = new UrlRewrite(stat);

    // This tryes to get whaever is requested from the user. By a dialog box.
    if (!urlProps.process(mainFrame)) return;

    if (urlProps.isMessage()) 
      result = sendMessage (urlProps);
    else                      
      result = sendFiles(urlProps);

    // Now I should display the result somewhere....
    resultDialog.showResult(result);    
    }



  /**
   * This will get the default submission scheme.
   * It should load it from a property file athat is in the current project directory, if any...
   * When called it WILL load the currently saved default scheme
   */
  private void loadDefaultScheme(BPackage curPkg)
    {
    // Ok, time to retrieve the selected scheme, but first let's set a nice default
    schemeSelectedSet ("");

    BProject curProj = curPkg.getProject();
    // FOr some misterious reason there is no project open, let's return the default
    if (curProj == null) return;

    File projectDefsFile = new File(curProj.getProjectDir(), PROPERTIES_FILENAME);
    // For some reason (maybe the file is not there, I cannot read it...
    if (!projectDefsFile.canRead()) return;

    Properties projProps = new Properties();
    FileInputStream iStream = null;
    try 
      {
      // ACK ! Need to do this way to be shure to close the dammed file...
      iStream = new FileInputStream(projectDefsFile);
      projProps.load(iStream);
      } 
    catch (Exception exc) 
      {
      statusWriteln("loadDefaultScheme: No defaultScheme on project "+projectDefsFile);
      return;
      } 
    finally 
      {
      Utility.inputStreamClose(iStream);
      }

    // Ah, finally I can set a nice value !
    schemeSelectedSet ( projProps.getProperty(SELECTED_NODE_PROPERTY, ""));
    }



  /**
   * This will write down what is the default scheme.
   * Of course the default is the one that is currently selected...
   */
  private void saveDefaultScheme(BPackage curPkg)
    {
    String curScheme = schemeSelectedGet();
    TreePath path = stat.treeData.getPathFromString(curScheme);

    // Don't save invalid paths
    if (path == null) return;

    BProject curProj = curPkg.getProject();
    if (curProj == null) 
      {
      // For some misterious reason there is no project open
      stat.aDbg.error(Stat.SVC_PROP, "setDefaultScheme: ERROR: No current project");
      return;
      }

    // Let me put what I need into the properties.
    Properties projProps = new Properties();
    projProps.setProperty(SELECTED_NODE_PROPERTY, curScheme);

    // Now let me try to open the file to write the properties on
    File projectDefsFile = new File(curProj.getProjectDir(), PROPERTIES_FILENAME);

    FileOutputStream oStream = null;
    try 
      {
      // ACK ! Need to do this way to be shure to close the dammed file...
      oStream = new FileOutputStream(projectDefsFile);
      projProps.store(oStream, "Submitter per project properties");
      } 
    catch (Exception exc) 
      {
      logWriteln("setDefaultScheme: Cannot write properties to file=" + projectDefsFile.toString());
      return;
      } 
    finally 
      {
      Utility.outputStreamClose(oStream);
      }
  }


  /**
   * This decides if the submit button should be anabled or not depending
   * on the value of the current default scheme.
   */
  private void checkSubmitButton ( )
    {
    TreePath possiblePath = stat.treeData.getPathFromString(schemeSelectedGet());

    if ( possiblePath == null ) 
      submitButton.setEnabled(false);
    else
      submitButton.setEnabled(true);
    }


// ===================== UTILITY CLASSES HERE === ALIGNED LEFT ====================



/**
 * Used to watch for change of status and report accordingly
 */
class transportReport implements TransportReport
  {
  public void reportEvent(String message)
    {
    statusWriteln(message);
    }

  public void reportLog(String message)
    {
    logWriteln(message);
    }
  }

/**
 * This is used to load in backgrund the properties
 */
class TreeLoadThread extends Thread
  {
  /**
   * starts loading the submitter definitions in a thread
   */
  public void run ()
    {
    progressBar.setIndeterminate(true);
    stat.treeData.loadTree(curPkg);
    loadDefaultScheme(curPkg);
    statusWriteln("Loading Done");
    progressBar.setIndeterminate(false);    
    }
  }

/**
 * This is instead used to send the real stuff
 */
class SubmitThread extends Thread
  {
  /**
   * Do the real submitting task
   */
  public void run ()
    {
    progressBar.setIndeterminate(true);
    saveDefaultScheme(curPkg);
    submitWork();
    progressBar.setIndeterminate(false);    
    }
  }



/**
 * Utility class, when the user wants to close a window it falls here.
 */
class onWindowClosing extends WindowAdapter
  {
  public void windowClosing(WindowEvent e)
    {
//    System.out.println ("onWindowClosing: CALLED");
    mainFrame.setVisible(false);
    }
  }

/**
 * Utility class, used to trap when the text field changes
 * So you can then decide if to change the submit button
 */
class onFieldChange implements DocumentListener
  {
  public void changedUpdate(DocumentEvent de)
    {
    checkSubmitButton();
    }

  public void insertUpdate(DocumentEvent de)
    {
    checkSubmitButton();
    }

  public void removeUpdate(DocumentEvent de)
    {
    checkSubmitButton();
    }
  }



  // ======================== END OF UTILITY CLASSES =====================
  }









/*

    
    // This is a complex layout manager, if you want it :-)
    JPanel gridPanel = new JPanel(new GridBagLayout());
    GridBagConstraints where = new GridBagConstraints();
    
    // ------- This is the label sayng to choose a dialog
    JLabel schemeLabel = new JLabel(stat.bluej.getLabel("dialog.scheme"));
    where.gridx=0; where.gridy=0; 
    gridPanel.add(schemeLabel,where);

    // ------- This is the text where to choose a submission
    schemeSelected = new JTextField();
    schemeSelected.setMinimumSize(new Dimension(100,12));
    schemeSelected.setPreferredSize(new Dimension(300,12));
    schemeSelected.addActionListener(this); 
    // so that pressing return in the field does the submit
    schemeSelected.getDocument().addDocumentListener(new onFieldChange());
    where.gridx=1; where.gridy=0; where.fill=GridBagConstraints.BOTH;
    gridPanel.add(schemeSelected,where);

    // ------- This is the actual button to browse
    browseButton = new JButton(stat.bluej.getLabel("button.browse"));
    browseButton.addActionListener(this);
    where.gridx=2; where.gridy=0; where.anchor=GridBagConstraints.EAST;
    gridPanel.add(browseButton,where);
    
    // --------- This is where I will display the status
    where.gridx=0; where.gridy=1; where.gridwidth=3;
    gridPanel.add(getLoggingPanel(),where);

    // --------- This is used to have afeeling of something running
    progressBar = new JProgressBar();
    progressBar.setPreferredSize(new Dimension (300,10));
    progressBar.setBorderPainted(false);
    where.gridx=0; where.gridy=2; where.gridwidth=3; where.insets=new Insets(2,0,0,0);
    gridPanel.add(progressBar,where);

    // ------------ Now we put the above and the buttons in another pane
    JPanel    mainPanel = new JPanel(new BorderLayout());
    mainPanel.setBorder(new EmptyBorder(5,5,5,5));
    
    mainPanel.add(gridPanel,BorderLayout.CENTER);
    mainPanel.add(getButtonPanel(),BorderLayout.SOUTH);
    
    // -------- TIme to create the real window...
    mainFrame = new JFrame("Submitter");
    mainFrame.addWindowListener(new onWindowClosing());

    ImageIcon icon = new ImageIcon (this.getClass().getResource("World.gif"));
    mainFrame.setIconImage(icon.getImage());

    Container main = mainFrame.getContentPane();
    main.add(mainPanel);
    mainFrame.setLocation(50,50);  // You need this to place the windows on Unix

    // Now that I have the parent frame I can make a dialog
    resultDialog = new ResultDialog(stat, mainFrame);



 
 */