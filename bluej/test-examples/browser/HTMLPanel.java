import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;

/** This is a small panel that is cabable of displaying HTML Content.
    This content takes the form of a HTML String, which can be an
   arbitrarily long string consisting of HTML tags, and text.  The
   string can be changed, and the panel will redisplay itself.
 
    <UL>
    <LI>Steve Kemp
    <LI>Version 0
    </UL>
 */

public class HTMLPanel extends Panel
  implements WebConstants
{
  /** The html that the panel will display */
  private HTMLString html;

  private Font normalFont;
  private FontMetrics normalFontMetrics;

  private ScrollPane parentContainer;
  private ShowMessage parentBrowser;

  private FontManager fontManager;
  
  private static final Font STANDARD_FONT
    = new Font( "Helvetica", Font.PLAIN, 12 );

  private int lengthOfPage;
  private int widthOfPage;

  private Vector links;

  /** Create a HTML displaying panel that will show HTML text
      @param s The HTML to display
   */
  public HTMLPanel( ShowMessage webBrowser, ScrollPane parent, HTMLString s )
  { normalFont = new Font( "Helvetica", Font.PLAIN, 12 );
    fontManager = new FontManager( STANDARD_FONT );
    fontManager.push( normalFont );

    this.parentContainer = parent;
    this.parentBrowser = webBrowser;

    this.html = s;
    this.links = new Vector();   
    this.enableEvents( AWTEvent.MOUSE_EVENT_MASK );
    this.enableEvents( AWTEvent.MOUSE_MOTION_EVENT_MASK );
  }



  public void blank()
  { setBackground( convertColor( System.getProperty( BACKGROUND_COLOR_PROPERTY ) ) ); 
    Dimension d = this.getSize();
    Graphics g = this.getGraphics();
    g.setColor( convertColor( System.getProperty( BACKGROUND_COLOR_PROPERTY ) ) );
    g.fillRect( 0, 0, d.width, d.height );
  }



  public void setContents( HTMLString newContents )
  { this.html = newContents;
    normalFont = new Font( "Helvetica", Font.PLAIN, 12 );
    links = new Vector();
    html.reset();
    repaint();
  }



  public void update( Graphics g )
  { blank();
    paint( g );
  }



  
  public void paint( Graphics g )
  { blank();
    Dimension size = this.getSize();
    int maxX = size.width;
    int maxY = size.height;
    widthOfPage = maxX;

    fontManager = new FontManager( STANDARD_FONT );
    // Only used for the comments...
    boolean insideComment = false;

    // Only used for fonts....
    Font currentFont = fontManager.getFont();
   
    normalFont = fontManager.getFont();
   
    html.reset();
    g.setColor( convertColor( System.getProperty( TEXT_COLOR_PROPERTY ) ) );
     
    normalFontMetrics = g.getFontMetrics( normalFont );
    
    int x = 0;
    int y = normalFontMetrics.getHeight();


    // ////////////////////////////////////////////////////////////////////
    //  The main loop of drawing the HTML panel starts here
    // //////////////////////////////////////////////////////////////////// 
    while( html.hasMoreTokens() )
    { String nextString = html.getNext();

    // Make sure that we are using the current font
      normalFont = fontManager.getFont();
      g.setFont( normalFont );
      // And alter the measurements of the string lengths, accordingly
      normalFontMetrics = g.getFontMetrics( normalFont );

      if ( html.insideTag() )
      { 
        String upperCaseTag = nextString.toUpperCase();
        
        if ( upperCaseTag.indexOf( "HREF=\"" ) != -1 )
        {  g.setColor( convertColor( System.getProperty( LINK_COLOR_PROPERTY ) ) );
           links.addElement( new HTMLLink( x, y, nextString, ( g.getFontMetrics( g.getFont() ).stringWidth( "This is a test" ) ), ( g.getFontMetrics( g.getFont() ).getHeight() ) ) );
        }
        else if ( upperCaseTag.indexOf( "/A" )  != -1 )
          g.setColor( convertColor( System.getProperty( TEXT_COLOR_PROPERTY ) )  );
        else if ( upperCaseTag.trim().equals( "P" ) )
        { y += ( normalFontMetrics.getHeight() );
          x = 0;
        }
        else if ( upperCaseTag.trim().equals( "BR" ) )
        { y += ( normalFontMetrics.getHeight() );
          x = 0;
        }
        else if ( upperCaseTag.trim().equals( "LI" ) )
	  { y += ( normalFontMetrics.getHeight() / 2 );
          x = 5;
          g.fillOval( x, y, (normalFontMetrics.getHeight() / 3),
             (normalFontMetrics.getHeight() / 3 ) );
          x += ( normalFontMetrics.getHeight() );
          y += ( normalFontMetrics.getHeight() / 2 );
        }
        else if ( ( upperCaseTag.startsWith( "!--" )  ) &&
        ( upperCaseTag.endsWith( "--" ) ) )
	{ System.out.println( "Start of comment" );
          insideComment = true;
          System.out.println( upperCaseTag );
        }
        else if ( ( upperCaseTag.endsWith( "-->" ) )  && ( !upperCaseTag.endsWith( "!--" ) ) )
          insideComment = false;

// BOLD
        else if ( ( upperCaseTag.trim().equals( "B" ) ) 
          || ( upperCaseTag.trim().equals( "STRONG" ) ) )
	{ Font old = fontManager.getFont();
          Font newfont = new Font( old.getName(), old.getStyle() | Font.BOLD, old.getSize() );
          
          fontManager.push( newfont );
          normalFont = newfont;
       	}

// ITALIC
        else if ( upperCaseTag.trim().equals( "I" ) ) 
	{ Font old = fontManager.getFont();
          Font newfont = new Font( old.getName(), old.getStyle() | Font.ITALIC, old.getSize() );
          
          fontManager.push( newfont );
          normalFont = newfont;
     	}
// Headings
       else if ( upperCaseTag.trim().equals( "H1" ) )
       { Font old = fontManager.getFont();
         Font newfont = new Font( old.getName(), Font.BOLD, 24 );
         fontManager.push( newfont );
         normalFont = newfont;
       }
       else if ( upperCaseTag.trim().equals( "H2" ) )
       { Font old = fontManager.getFont();
         Font newfont = new Font( old.getName(), Font.BOLD, 20 );
         fontManager.push( newfont );
         normalFont = newfont;
       }
       else if ( upperCaseTag.trim().equals( "H3" ) )
       { Font old = fontManager.getFont();
         Font newfont = new Font( old.getName(), Font.BOLD, 16 );
         fontManager.push( newfont );
         normalFont = newfont;
       }
       else if ( upperCaseTag.trim().equals( "H4" ) )
       { Font old = fontManager.getFont();
         Font newfont = new Font( old.getName(), Font.BOLD, 14 );
         fontManager.push( newfont );
         normalFont = newfont;
       }
	 
        else if ( ( upperCaseTag.trim().equals( "/B" ) ) ||
                  ( upperCaseTag.trim().equals( "/STRONG" ) ) ||
                  ( upperCaseTag.trim().equals( "/I" ) ) ||
                  ( upperCaseTag.trim().equals( "/H1" ) ) ||
                  ( upperCaseTag.trim().equals( "/H2" ) ) ||
                  ( upperCaseTag.trim().equals( "/H3" ) ) ||
                  ( upperCaseTag.trim().equals( "/H4" ) )
                )
	{ Font newfont = fontManager.pop();
          normalFont = newfont;
          currentFont = newfont;

          // Reset the line if there is a change from H[1-4]
             if ( ( upperCaseTag.trim().equals( "/H1" ) ) ||
                  ( upperCaseTag.trim().equals( "/H2" ) ) ||
                  ( upperCaseTag.trim().equals( "/H3" ) ) ||
                  ( upperCaseTag.trim().equals( "/H4" ) )
                )
             { y += ( normalFontMetrics.getHeight() );
               x = 0;
             }
      	}   
        else if ( ( upperCaseTag.trim().equals( "HR" ) ) || ( upperCaseTag.startsWith( "HR" ) ) )
        {  y += ( normalFontMetrics.getHeight() ) / 2;
           x = 0;
           g.drawLine( x, y, maxX, y );
           y += ( normalFontMetrics.getHeight() );
        }   
      }  

      if ( ( !insideComment ) &&  ( !html.insideTag() ) && ( !nextString.equals( "" ) ) )
      { if ( ( normalFontMetrics.stringWidth( nextString ) + x ) > maxX )
        { x = 0;
          y += normalFontMetrics.getHeight();     
        }

        g.drawString( nextString, x, y );
        x += normalFontMetrics.stringWidth( nextString );
      }
      
    }

    // Make sure that there is some room below the bottom line of text.
    y  += normalFontMetrics.getHeight();

// If all of the text will not fit onto the screen, or the panel is too large for the
// text then resize the panel
    if ( ( y > maxY ) || ( y < maxY ) )
    { 
      this.lengthOfPage = y;
      
      parentContainer.doLayout();
    }

// Reset the html for the next time it is displayed...
//    html.reset();
  }


  public void processMouseEvent( MouseEvent e )
  { if ( e.getID() == MouseEvent.MOUSE_PRESSED )
    { int x = e.getX();
      int y = e.getY();
      for ( int i = 0; i < links.size(); i++ )
      { HTMLLink l = (HTMLLink)links.elementAt( i );
        if ( l.isInside( x, y ) )
        { StringTokenizer st = new StringTokenizer( l.getTarget(), "\"", false );
          st.nextToken();   // Discard 'A HREF=' bit
//          System.out.println(" Link selected " + st.nextToken() );
          if ( st.hasMoreTokens() )
          { String currentURL = html.getURLBase();
            String target = st.nextToken();

            if ( target.startsWith( "/" ) && ( target.length() > 1 ) )
              target = target.substring( 1, ( target.length() - 1 ) );
            
            if ( !( (target.startsWith( "http" ) ) || ( target.startsWith( "HTTP" ) ) ) )
              if ( currentURL.endsWith( "/" ) )
                parentBrowser.getURL( currentURL + target, true );
              else
                parentBrowser.getURL( ( currentURL.substring( 0, currentURL.lastIndexOf( "/" ) + 1 ) )
                 + target, true );
            else           
              parentBrowser.getURL( target + "", true );
          }
        }
      }
    }
    else
      super.processMouseEvent( e );
  }


  /** This method is called every time that the mouse is moved.<BR>
      It tests to see whether the current mouse is over a link, if so
      the status box on the Web Browser is updated...
      @param e The mouse event object */
  public void processMouseMotionEvent( MouseEvent e )
  { boolean inLink = false;
    if ( e.getID() == MouseEvent.MOUSE_MOVED )
    { int x = e.getX();
      int y = e.getY();
      for ( int i = 0; i < links.size(); i++ )
      { HTMLLink l = (HTMLLink)links.elementAt( i );
        if ( l.isInside( x, y ) )
        { StringTokenizer st = new StringTokenizer( l.getTarget(), "\"", false );
          st.nextToken();   // Discard 'A HREF=' bit
          if ( st.hasMoreTokens() )
            parentBrowser.showStatus( st.nextToken() );

          inLink = true;
        }
      }
      if ( !inLink )
        parentBrowser.showStatus( "" );
    }
    else
     super.processMouseMotionEvent( e );
  }



  public Dimension getPreferredSize()
  { return( new Dimension( widthOfPage, lengthOfPage ) );
  }

  public void reset()
  { html.reset();
  }



  public Color convertColor( String col )
  { if ( col.equalsIgnoreCase( "black" ) )
      return ( Color.black );
    if ( col.equalsIgnoreCase( "blue" ) )
      return ( Color.blue );
    if ( col.equalsIgnoreCase( "cyan" ) )
      return ( Color.cyan );
    if ( col.equalsIgnoreCase( "gray" ) )
      return ( Color.gray );
    if ( col.equalsIgnoreCase( "green" ) )
      return ( Color.green );
    if ( col.equalsIgnoreCase( "magenta" ) )
      return ( Color.magenta );
    if ( col.equalsIgnoreCase( "orange" ) )
      return ( Color.orange );
    if ( col.equalsIgnoreCase( "pink" ) )
      return ( Color.pink );
    if ( col.equalsIgnoreCase( "red" ) )
      return ( Color.red );
    if ( col.equalsIgnoreCase( "white" ) )
      return ( Color.white );
    if ( col.equalsIgnoreCase( "yellow" ) )
      return ( Color.yellow );


    return( null );
  }

}