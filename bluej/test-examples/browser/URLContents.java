 import java.io.*;
import java.net.*;
import java.util.*;

/** This is a simple little class that will hold the contents of a Web page, it
    has a couple of convienence methods for returning the contents of the page,
    as text, or HTML, and a simple way of returning the title of the document....

    <BR><UL>
        <LI>Version One
        <LI>Steve Kemp
        </UL>
 */
public class URLContents
{
  private String textHTML;
  private String originalHTML;
  private String raw;
  private String title;
 

  /** Construct a simple little information holder for the contents of a page.
      @param orig The HTML that is contained in the page
      @param text The text representation of that content
   */
  public URLContents( String orig, String text )
  { this.originalHTML = orig;
    this.textHTML = text;
    this.raw = orig;

    // Now get the title, and save it away
    String html = this.originalHTML.toUpperCase();

    int titlestart = html.indexOf( "<TITLE>" );
    if ( titlestart < 0 )
    { this.title = "Untitled";
      
    }
    else
    {
      // remove the <TITLE> part, seven letters.
      titlestart += 7;

      int titleend = html.indexOf( "</TITLE>" );
      if ( titleend < 0 )
      { this.title = "Untitled";
      }
      else
      { this.title = this.originalHTML.substring( titlestart, titleend );
        this.originalHTML = raw.substring( 0, titlestart - 7 );
        this.originalHTML += raw.substring( titleend + 8 );
      }
    }
  }



  /** Return the contents of this object as text
     @return The HTML as text
   */
  public String getText()
  { return this.textHTML;
  }


  /** Returns the contents of this object as HTML, complete with all the tags...
  */
  public String getHtml()
  {// return this.originalHTML;
   return( this.originalHTML );
  }


  /** Returns the title of the document, as text.. */
  public String getTitle()
  {    return ( this.title );
  }



//--------------------------------------------------------------------------
// Do the work of reading from the input stream, in, and stripping the text
// between the symbols '<', and '>'.  This result is then written to the
// output stream out.
//
  public static URLContents convert( DataInputStream in )
  {
// Hold the output we are going to return
   String out = new String();
// Hold the raw HTML from the server
   String original = new String();

// Hold one line of input
    String one_line;
// hold one token
    String token;
// The last token we outputted
    String lastToken = new String( "" );
// Do we have more lines of input to read?  Assumes true initially
    boolean more = true;
// Are we inside a '<' '>' pair?  Assumes false initialy
    boolean insideTag = false;
// An object to break up the input, splitting at '<' and '>'
    StringTokenizer st;

    try 
    { while( true )
      { one_line = in.readLine();
        if ( one_line == null )
          break;
        if ( one_line.trim().equals( "") )
          break;
      }
      while( more )
      { one_line = in.readLine();

// If we couldn't read input we've finished
        if ( one_line == null )
          more = false;
        else 

// Otherwise construct a new input tokenizer.  This will split the line
// at the characters '<' and '>'.  The string will consist of the line that
// was read, and a system specific new line character, (This was removed by
// the readLine call earlier.
        { st = new StringTokenizer( one_line + System.getProperty(
            "line.separator" ), "<>\n", true);
          original += one_line + System.getProperty( "line.separator" );

// While there are more characters '<' '>' get the text
          while( st.hasMoreTokens() )
          { token = st.nextToken();

// If the character was a '<' we are inside a tag.
            if ( token.equals( "<" ) )
              insideTag = true;

// If the character was a '>' the tag is closed, mark us as being outside it
            else if ( token.equals( ">" ) )
              insideTag = false;


// If we have a newline only print it if the last token wasn't a newline
// Otherwise it is either plain text, or the text of the tag, i.e. H3
// if we are currently inside a tag do nothing, otherwise we have the text
// of the document, so send it to the output steam
            else if ( insideTag == false ) 
            { if ( token.indexOf( "\n" ) != -1 )
              { if ( lastToken.indexOf( "\n" ) == -1 )
                  out += ( token );
              
                lastToken = token;
              }
              else
              { out += ( token );
                lastToken = token;
              }
            }
          }
        }       
      }
    }

// If error reading/writing print error message, and exit
    catch ( IOException e )
    { System.err.println( "Error processing HTML " + e );
      //System.exit( 1 );
    }
    finally
    { return ( new URLContents( original, out ) );
    }
  }

}

