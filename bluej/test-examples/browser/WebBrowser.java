import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;



/** An extremely simple little web browser, this can only
    cope with little text and links, no graphics, or tables
    or anything like that.
  */
public class WebBrowser extends Frame
  implements ShowMessage,
   WebConstants
{
  // Where the browser settings are saved...
  public static final String INIT_FILE = ".BROWSER.ini" ;

  // Things concerned with browsing in general
  private Vector history = new Vector();
  private int historyPosition = 0;
  private Properties settings;
  private String homepage = new String( "" );
  private String loadhomepage = new String( "" );

  // Default homepage
  private static final String DEFAULT_HOMEPAGE =
    new String( "http://www.tardis.ed.ac.uk/~skx/java/Browser/" );
  

  private static final String DEFAULT_TEXT_COLOR = "Black";
  private static final String DEFAULT_LINK_COLOR = "Red";
  private static final String DEFAULT_BACKGROUND_COLOR = "Gray";

  // Graphics Components
  private Button backButton;
  private Button forwardButton;
  private Button refreshButton;
  private Button homeButton;

  // Two fields, one for getting a URL from the user, the other for
  // displaying brief status messages...  
  private TextField status;
  private TextField url_entry;
  
  // Where the HTML is actually displayed to the user.
  private HTMLPanel url_display;
  private ScrollPane url_view;

  // These two objects are concerned with displaying the source code of
  // a page to the user....
  private URLContents content;
  private SourceViewer sv;
  private WebSetup options;
  private ColorImage animation;

  /** Creates a new Web Browser object, this is quite a complicated
      business - setting up the AWT, and reading in the properties.
      etc
   */
  public WebBrowser()
  { boolean doneProps = false;
    Properties defaultSettings = new Properties();
    defaultSettings.put( "HOMEPAGE", DEFAULT_HOMEPAGE );
    defaultSettings.put( "LOAD_HOMEPAGE", "yes" );
    defaultSettings.put( "TEXT_COLOR", DEFAULT_TEXT_COLOR );
    defaultSettings.put( "LINK_COLOR", DEFAULT_LINK_COLOR );
    defaultSettings.put( "BACKGROUND_COLOR", DEFAULT_BACKGROUND_COLOR );

    this.settings = new Properties( defaultSettings );
    // First of all try to get the starting/home page
    try
    { 
      String pfilename = new String( System.getProperty( "user.home" ) + 
       System.getProperty( "file.separator" ) + INIT_FILE );

      FileInputStream sf = new FileInputStream( pfilename );
     
      settings.load( sf );
      this.homepage = new String( settings.getProperty( "HOMEPAGE" ) );
      this.loadhomepage = new String( settings.getProperty( "LOAD_HOMEPAGE" ) );

      doneProps = true; 
    }
    catch( FileNotFoundException e )
    { doneProps = false;
    }
    catch( Exception e )
    {
    }

// Set up a property for the homepage
    addProperty( HOMEPAGE_PROPERTY, this.homepage );
    addProperty( LOAD_HOMEPAGE_PROPERTY, this.loadhomepage );
    addProperty( TEXT_COLOR_PROPERTY, settings.getProperty( "TEXT_COLOR" ) );
    addProperty( LINK_COLOR_PROPERTY, settings.getProperty( "LINK_COLOR" ) );
    addProperty( BACKGROUND_COLOR_PROPERTY, settings.getProperty( "BACKGROUND_COLOR" ) );


// If there was an error in processing the properties, create, and
// save new ones
    if ( !doneProps )
    { saveSettings();
    }

    this.setLayout( new BorderLayout() );

    url_entry = new TextField( "", 40 );
    url_entry.addActionListener( new ActionListener() {
       public void actionPerformed( ActionEvent e )
       { getURL( url_entry.getText(), true );
       }
    } );
    
    Panel top = new Panel();
    Panel entry = new Panel();
    Panel buttons = new Panel();
    
    buttons.setLayout( new FlowLayout( FlowLayout.LEFT ) );

    entry.setLayout( new FlowLayout( FlowLayout.LEFT ) );

    entry.add( new Label( "URL:" ) );
    entry.add( url_entry );

    backButton = new Button( "Back" );
    backButton.addActionListener( new ActionListener() {
       public void actionPerformed( ActionEvent e )
       { back();
       }
    } );
    buttons.add( backButton);

    forwardButton = new Button( "Forward" );
    forwardButton.addActionListener( new ActionListener() {
       public void actionPerformed( ActionEvent e )
       { forward();
       }
    } );
    buttons.add( forwardButton);

    refreshButton = new Button( "Refresh" );
    refreshButton.addActionListener( new ActionListener() {
       public void actionPerformed( ActionEvent e )
       { refresh();
       }
    } );
    buttons.add( refreshButton );

    homeButton = new Button( "Home" );
    homeButton.addActionListener( new ActionListener() {
       public void actionPerformed( ActionEvent e )
       { goHome();
       }
    } );
    buttons.add( homeButton);


    Panel anim = new Panel();
    anim.setLayout( new FlowLayout( FlowLayout.RIGHT ) );

    animation = new ColorImage( "Logo.gif", 75 );
    animation.setSize( 58, 40 );
    anim.add( animation );
    buttons.add( anim );
    animation.start();

    backButton.disable();
    forwardButton.disable();

    top.setLayout( new GridLayout( 2, 1 ) );
    top.add( entry );
    top.add( buttons );

    add( "North", top );
    
    url_view = new ScrollPane( ScrollPane.SCROLLBARS_AS_NEEDED );
    
    url_display = new HTMLPanel( this, url_view, 
     new HTMLString( url_entry.getText(), "<html>" +
       "<TITLE>About this program</TITLE> <P>" +
       "<P><H1>Welcome</H1><HR><P>Thank you for trying out this browser...<P><P> You are seeing this page because you do not have" + 
       "any preferences set up.<P>" + 
       "To set up preferences select 'View -> Settings' from the menu" +
       "bar.  <P><P><HR><B><I>A small web Browser by Steve Kemp</I></B><P>" +
       "<P><B>skx@tardis.ed.ac.uk</B><P><HR><P><HTML>" ) );
    
    url_view.add( "Center", url_display );
    add( "Center", url_view );

    status = new TextField( "" );
    status.setEditable( false );
    add( "South", status );

    setupMenus();
    setTitle( "A Small Web Browser" );

    if ( this.loadhomepage.equalsIgnoreCase( "yes" ) )
       getURL( this.homepage, true );

    showStatus( "A small Web Browser by Steve Kemp - skx@tardis.ed.ac.ul" );


   this.enableEvents( AWTEvent.WINDOW_EVENT_MASK ); 
   }


 /** This is a simple method which opens a connection to the host specified
      in the URL text field.  It sends a request for the relevent page, and
      displays it in the text area 
   */
  public void getURL( String url, boolean addToHistory )
  { try
    { 
      url_entry.setText( url );
      if ( addToHistory )
      { history.addElement( new String ( url ) );
        historyPosition++;
      }

      if ( ( historyPosition ) > 0 )
        backButton.enable();
      else
        backButton.disable();

      if ( historyPosition < history.size() )
        forwardButton.enable();
      else
        forwardButton.disable(); 

      URL Url = new URL( url );
      String host = Url.getHost();
      showStatus( "Contacting " + host + " ..." );
      String file = Url.getFile();
      
      int port = Url.getPort();
      if ( port < 1 )
        port = 80;

      Socket connection = new Socket( host, port );
      DataInputStream datain = new DataInputStream( 
       connection.getInputStream() );

      DataOutputStream dataout = new DataOutputStream(
       connection.getOutputStream() );
      PrintStream pout = new PrintStream( dataout );

      showStatus( "Establishing connection ..." );
      pout.print( "GET " + file + " HTTP//1.0\n\r\n\r" );
   
      String contents = new String( "" );

      showStatus( "Getting file " + file + " ..." );

      content = URLContents.convert ( datain );
      contents = content.getText();

      url_display.setContents( new HTMLString( url_entry.getText(),
        content.getHtml() ) );

      url_view.invalidate();

      // Update the title of the page...
      setTitle( content.getTitle() );
 
      showStatus( "Done (" + contents.length() + " bytes)." );

    }
    catch( IOException e )
    { showStatus( "Error " + e );
    }
    finally
    { 
    }
  }



  /** This method is called when the user clicks on the
      back button, when the user clicks on the back button.
      Or selects it from the 'Go' menu.  Its purpose is
      to take the user to their previous page.
   */
  public void back()
  { 
    if ( ( historyPosition - 2) >= 0 )
    { String previousURL = (String)history.elementAt( historyPosition - 2 );
      historyPosition -= 1;
      getURL( previousURL, true ); 
    }
    else
    { showStatus( "No previous document to return to..." );
    }
  }



  /** This is a very similar method to the back one, it moves the user to
      the next page in their history.
   */
  public void forward()
  { 
    if ( ( historyPosition + 2  ) < history.size() )
    { String previousURL = (String)history.elementAt( historyPosition + 2 );
      historyPosition += 1;
      getURL( previousURL, true ); 
    }
    else
    { showStatus( "No next document to jump to..." );
    }
  }


  /** Takes the user home */
  public void goHome( )
  { String home = System.getProperty( HOMEPAGE_PROPERTY );
    getURL( home, true );
  }

  /** Redraws the current page, if desired by the user */
  public void refresh()
  { url_display.repaint();
  }



  public void setOptions()
  { options = new WebSetup( this );
    options.setVisible( true );
  }

  /** This method will show the user the HTML source of the current page, it is
      displayed in a seperate window, inside a text area, so the user is quite
      capable of copying it, or whatever.  The source code will soon have
      the option for printing it....
   */
  public void viewSource()
  { if ( content.getHtml() != null )
    { sv = new SourceViewer( content.getHtml() );
      sv.setSize( 400,300 );
      sv.setVisible( true );  
    }
    else
    { Toolkit.getDefaultToolkit().beep();
    }
  }


  /** This method sets up the menus, this is called
    from the constructor of this class. 
   */
  public void setupMenus()
  { MenuBar menubar = new MenuBar();
    setMenuBar( menubar );
    Menu file = new Menu( "File" );
    menubar.add( file );

    MenuItem m;
    file.add( m = new MenuItem( "E-mail message", new MenuShortcut(
      KeyEvent.VK_M ) ) );

    m.addActionListener( new ActionListener() {
      public void actionPerformed( ActionEvent e )
      { email();
      }
    });

    MenuItem p;
    file.add( p = new MenuItem( "E-mail Page", new MenuShortcut(
      KeyEvent.VK_P ) ) );

    p.addActionListener( new ActionListener() {
      public void actionPerformed( ActionEvent e )
      { emailPage();
      }
    });

    MenuItem q;
    file.add( q = new MenuItem( "Quit", new MenuShortcut(
      KeyEvent.VK_Q ) ) );

    q.addActionListener( new ActionListener() {
      public void actionPerformed( ActionEvent e )
      { CloseBrowser();
      }
    });


    Menu view = new Menu( "View" );
    menubar.add( view );

    MenuItem refresh;
    view.add( refresh = new MenuItem( "Refresh", new MenuShortcut(
      KeyEvent.VK_R ) ) );

    refresh.addActionListener( new ActionListener() {
      public void actionPerformed( ActionEvent e )
      { refresh();
      }
    });

    MenuItem source;
    view.add( source = new MenuItem( "Source", new MenuShortcut(
      KeyEvent.VK_U ) ) );

    source.addActionListener( new ActionListener() {
      public void actionPerformed( ActionEvent e )
      { viewSource();
      }
    });
    view.addSeparator();
    MenuItem settings;
    view.add( settings = new MenuItem( "Settings", new MenuShortcut(
      KeyEvent.VK_E ) ) );

    settings.addActionListener( new ActionListener() {
      public void actionPerformed( ActionEvent e )
      { setOptions();
      }
    });

    Menu go = new Menu( "Go" );
    menubar.add( go );

    MenuItem back;
    go.add( back = new MenuItem( "Back", new MenuShortcut(
      KeyEvent.VK_LEFT ) ) );

    back.addActionListener( new ActionListener() {
      public void actionPerformed( ActionEvent e )
      { back();
      }
    });
    MenuItem forward;
    go.add( forward = new MenuItem( "Forward", new MenuShortcut(
      KeyEvent.VK_RIGHT ) ) );

    forward.addActionListener( new ActionListener() {
      public void actionPerformed( ActionEvent e )
      { forward();
      }
    });
  }


  public void processWindowEvent( WindowEvent e )
  { if ( e.getID() == WindowEvent.WINDOW_CLOSING )
    { CloseBrowser();
    }
    else
      super.processWindowEvent( e );
  }



  /** Called when the user selects 'Quit', from the
      main file menu */
  public void CloseBrowser()
  { System.exit( 0 );
  }




  /** This method updates the text field at the bottom
   of the browser.
    @param statusMessage The string to display
   */
  public void showStatus( String statusMessage )
  { status.setText( statusMessage );
  }




  public void email()
  { EmailSender es = new EmailSender();
    es.resize( 350, 400 );
    es.setVisible( true );

  }

  public void emailPage()
  { EmailSender es = new EmailSender();
    es.resize( 350, 400 );
    es.setVisible( true );

  }

  /** Create, resize, and show a browser.
       If any command line arguments are given
       treat the first one as an URL to show*/
  public static void main( String args[] )
  { WebBrowser wb = new WebBrowser();

    wb.setSize( 400, 500 );

    if ( args.length > 0 )
      wb.getURL( args[ 0 ] , true );
  
    wb.setVisible( true );
  }


  public void start()
  {
    setSize(400, 500);
    setVisible(true);
  }


  /** This method will add a new property to the system
     property list
     */
   private void addProperty( String key, String value )
  { // Get the system wide properties
     Properties system = System.getProperties();

     // If the property is not already present
     // add it
     if ( system.getProperty( key ) == null )
     { system.put( key, value );
       System.setProperties( system );
     }
     // Otherwise remove it, and add it - 
     // efectively updating it.
     else
     { system.remove( key );
       system.put( key, value );
       System.setProperties( system );
      
     }
   } 

  /** Save the settings - At present this is only called when the 
      user has no .ini file on startup - But I would like to make some
      of the settings a little more special... */
   public void saveSettings()
   { String pfilename = new String( System.getProperty( "user.home" ) + 
        System.getProperty( "file.separator" ) + INIT_FILE );
     try
     { FileOutputStream sf = new FileOutputStream( pfilename );

       Properties exitProperties = new Properties();
       exitProperties.put( "HOMEPAGE", System.getProperty( 
        HOMEPAGE_PROPERTY ) );
       exitProperties.put( "LOAD_HOMEPAGE", System.getProperty( 
        LOAD_HOMEPAGE_PROPERTY ) );
       exitProperties.put( "TEXT_COLOR", System.getProperty( 
        TEXT_COLOR_PROPERTY ) );
       exitProperties.put( "LINK_COLOR", System.getProperty( 
        LINK_COLOR_PROPERTY ) );
       exitProperties.put( "BACKGROUND_COLOR", System.getProperty( 
        BACKGROUND_COLOR_PROPERTY ) );


       // Save the properties
       exitProperties.save( sf, "Saved settings for the Java WebBrowser" );
     }
     catch( Exception e )
     { showStatus(  "Error Saving Settings " + pfilename );
     }
    
   }

   public WebBrowser getBrowser()
   { return ( this );
   } 
}









