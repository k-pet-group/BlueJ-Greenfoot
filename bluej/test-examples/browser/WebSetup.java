import VasTabs;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class WebSetup extends Frame
  implements WebConstants
{
  // The tabbed panel container
  private VasTabs tabs;

  // The panels that are displayed in the tabbed panel
  private Panel homepage;
    private TextField homepageEntry;
    private Checkbox homepageStartup;
    private boolean showHomeAtStartup;


  private Panel color;
    private Choice textColor;
    private Choice linkColor;
    private Choice backgroundColor;

  private Panel general;
  private Panel about;

  // The buttons that are used to accept, or cancel changes
  private Button ok;
  private Button cancel;


  private ShowMessage browserParent;

  /** The default constructor.  This creates the nice
     tabbed panel, and sets up event handlers for the
     two buttons.
     */
  public WebSetup( ShowMessage parent )
  { browserParent = parent;
    this.setLayout( new BorderLayout() );
    tabs = new VasTabs( 25, 25, 350, 400 );
    homepage = setupHomepage();
    general = new Panel();
    color = setupColor();
    about = setupAbout();

    tabs.addTab( "Homepage", homepage );
    tabs.addTab( "Colour Scheme", color );
    tabs.addTab( "Identity", general );
    tabs.addTab( "Statistics", about );
    
    this.setSize( 350, 475 );
    this.add( "Center", tabs );

    Color c = tabs.getBackground();
   
    Panel buttons = new Panel();
    buttons.setBackground( c );

    buttons.setLayout( new FlowLayout( FlowLayout.RIGHT ) );
    
    ok = new Button( "OK" );
    ok.addActionListener( new ActionListener() {
       public void actionPerformed( ActionEvent e )
       { Ok( );
       }
    } );
    buttons.add( ok );

    cancel = new Button( "Cancel" );
    cancel.addActionListener( new ActionListener() {
       public void actionPerformed( ActionEvent e )
       { Cancel();
       }
    } );
    buttons.add( cancel );

    this.add( "South", buttons );

   this.enableEvents( AWTEvent.WINDOW_EVENT_MASK ); 

   setTitle( "View, or alter your browser settings" );
  }


  public Panel setupHomepage()
  { Panel home = new Panel();
    home.setLayout( new BorderLayout() );

    String homepage = System.getProperty( HOMEPAGE_PROPERTY );
    if ( homepage == null )
      homepage = new String( " " );

    showHomeAtStartup = false;

    String starthomepage = System.getProperty( LOAD_HOMEPAGE_PROPERTY );
    if ( starthomepage == null )
      starthomepage = "no";
    
    if ( starthomepage.equalsIgnoreCase( "yes" ) )
      showHomeAtStartup = true;
      
    Panel entry = new Panel();
    entry.setLayout( new GridLayout( 7, 1 ) );
    entry.add( new Label( " " ) );
    entry.add( new Label( " " ) );
    entry.add( new Label( "Your homepage is" ) );
    homepageEntry = new TextField( homepage );
    entry.add( homepageEntry );
    entry.add( new Label( " " ) );
    entry.add( new Label( " " ) );

    Panel choice = new Panel();
    choice.setLayout( new FlowLayout( FlowLayout.CENTER ) );
    homepageStartup = new Checkbox( "Show homepage at Startup", showHomeAtStartup );
    choice.add( homepageStartup );
    entry.add( choice );
       
    home.add( "North", entry );

    return( home );
  }

  public Panel setupAbout()
  { Panel about = new Panel();
    about.setLayout( new BorderLayout() );

    Font boldFont = new Font( "Helvetica", Font.BOLD, 12 );
    Font normalFont = new Font( "Helvetica", Font.PLAIN, 12 );    
    Panel text = new Panel();
    text.setFont( boldFont );

    text.setLayout( new GridLayout( 6, 1 ) );
    text.add( new Label( " " ) );
    text.add( new Label( " " ) );

    Panel name = new Panel();
    name.setLayout( new FlowLayout( FlowLayout.CENTER ) );
    name.add( new Label( "A portable Web Browser v" + 
      BROWSER_VERSION ) );
    text.add( name );

    Panel author = new Panel();
    author.setLayout( new FlowLayout( FlowLayout.CENTER ) );
    author.add( new Label( " by Steve Kemp - skx@tardis.ed.ac.uk" ) );
    text.add( author );

// Show the memory usage...   
    Panel memoryString = new Panel();
    memoryString.setLayout( new FlowLayout( FlowLayout.LEFT ) );
    memoryString.add( new Label( "Memory usage:-" ) );
    text.add( memoryString );

    Panel memoryPic = new MemoryInfo();
    memoryPic.setFont( normalFont );
    memoryPic.setSize( 350, 60 );
    text.add( memoryPic );

    about.add( "North", text );
    return( about );
  }


  public Panel setupColor()
  { Panel color = new Panel();

    textColor = new Choice();

    textColor.addItem( "Black" );
    textColor.addItem( "Blue" );
    textColor.addItem( "Cyan" );
    textColor.addItem( "Gray" );
    textColor.addItem( "Green" );
    textColor.addItem( "Magenta" );
    textColor.addItem( "Orange" );
    textColor.addItem( "Pink" );
    textColor.addItem( "Red" );
    textColor.addItem( "White" );
    textColor.addItem( "Yellow" );
    textColor.select( System.getProperty( TEXT_COLOR_PROPERTY ) );
    color.setLayout( new GridLayout( 8, 1 ) );
    color.add( new Label( "" ) );
    
    Panel normalText = new Panel();
    normalText.setLayout( new FlowLayout( FlowLayout.LEFT ) );
    normalText.add( new Label ( "The color of the normal text" ) );
    normalText.add( textColor);
    color.add( normalText );
    
    
    linkColor = new Choice();

    linkColor.addItem( "Black" );
    linkColor.addItem( "Blue" );
    linkColor.addItem( "Cyan" );
    linkColor.addItem( "Gray" );
    linkColor.addItem( "Green" );
    linkColor.addItem( "Magenta" );
    linkColor.addItem( "Orange" );
    linkColor.addItem( "Pink" );
    linkColor.addItem( "Red" );
    linkColor.addItem( "White" );
    linkColor.addItem( "Yellow" );
    linkColor.select( System.getProperty( LINK_COLOR_PROPERTY ) );

    color.add( new Label( "" ) );
    
    Panel linkText = new Panel();
    linkText.setLayout( new FlowLayout( FlowLayout.LEFT ) );
    linkText.add( new Label ( "The color of the hypertext links" ) );
    linkText.add( linkColor);
    color.add( linkText );

    backgroundColor = new Choice();

    backgroundColor.addItem( "Black" );
    backgroundColor.addItem( "Blue" );
    backgroundColor.addItem( "Cyan" );
    backgroundColor.addItem( "Gray" );
    backgroundColor.addItem( "Green" );
    backgroundColor.addItem( "Magenta" );
    backgroundColor.addItem( "Orange" );
    backgroundColor.addItem( "Pink" );
    backgroundColor.addItem( "Red" );
    backgroundColor.addItem( "White" );
    backgroundColor.addItem( "Yellow" );
    backgroundColor.select( System.getProperty( BACKGROUND_COLOR_PROPERTY ) );

    color.add( new Label( "" ) );
    
    Panel backText = new Panel();
    backText.setLayout( new FlowLayout( FlowLayout.LEFT ) );
    backText.add( new Label ( "The color of the browser" ) );
    backText.add( backgroundColor);
    color.add( backText );

    color.add( new Label ( "" ) );
    color.add( new Label ( "" ) );

    return( color );
  }


  public void processWindowEvent( WindowEvent e )
  { if ( e.getID() == WindowEvent.WINDOW_CLOSING )
    { Cancel();
    }
    else
      super.processWindowEvent( e );
  }


  public void Cancel()
  { this.setVisible( false ); 
    this.dispose();
  }


  /** This method is called when the user clicks on the okay button,
     its purpose is to hide this frame, set the options, and return
     control to the Browser.
     */
  public void Ok()
  {
    this.setVisible( false );
    browserParent.showStatus( "Saving settings ..." );
    
    // Save the homepage settings
    addProperty( HOMEPAGE_PROPERTY, homepageEntry.getText() );
    if ( homepageStartup.getState() )
      addProperty( LOAD_HOMEPAGE_PROPERTY, "yes" );
    else
      addProperty( LOAD_HOMEPAGE_PROPERTY, "no" ) ;

    // Save the color settings
    addProperty( TEXT_COLOR_PROPERTY, textColor.getSelectedItem() );
    addProperty( LINK_COLOR_PROPERTY, linkColor.getSelectedItem() );
    addProperty( BACKGROUND_COLOR_PROPERTY,
      backgroundColor.getSelectedItem() );

    // Save misc. settings

    browserParent.saveSettings();
    this.dispose();
  }


  /** This method will add a new property to the system
     property list.  If the property already exists the value 
     is changed.
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


  /** An overridden method to centre the frame on the parent frame. */
  public void setVisible( boolean state )
  {
    Dimension frameSize = browserParent.getBrowser().getSize();
    Point frameLoc = browserParent.getBrowser().getLocation();
    Dimension mySize = getSize();
    int x, y;

    x = frameLoc.x + ( frameSize.width / 2 ) - (mySize.width / 2 );
    y = frameLoc.y + ( frameSize.height / 2 ) - ( mySize.height / 2 ); 

    setBounds( x, y, getSize().width, getSize().height );

    super.setVisible( state );
  }

}




