import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

/** This is a simple application that will allow a user to send email
 */
public class EmailSender extends Frame
{
  private TextField tofield;
  private TextField fromfield;
  private TextField subject;
  private TextArea message;
  private Button send;

   public EmailSender()
   { tofield = new TextField( 30 );
     fromfield = new TextField( 30 );
     subject = new TextField( 30 );
     message = new TextArea();
     send = new Button( "Send E-mail" );

     setLayout( new BorderLayout() );
     
     Panel details = new Panel();
     details.setLayout( new GridLayout( 3,1 ) );

     Panel to = new Panel();
     to.setLayout( new FlowLayout( FlowLayout.LEFT) );
     to.add( new Label( "To:" ) );
     to.add( tofield );

     Panel from = new Panel();
     from.setLayout( new FlowLayout( FlowLayout.LEFT ) );
     from.add( new Label( "From:" ) );
     from.add( fromfield );

     Panel sub = new Panel();
     sub.setLayout( new FlowLayout( FlowLayout.LEFT ) );
     sub.add( new Label( "Subject:" ) );
     sub.add( subject );
    
     details.add( to );
     details.add( from );
     details.add( sub );


     add( "North", details );
     add( "Center", message );
     add( "South", send );     


     setupMenus();
  }


  public void setFrom( String sender )
  { this.fromfield.setText( sender );
  }

  public void setTo( String recipient )
  { this.tofield.setText( recipient );
  }

  public void setFromEditable( boolean b )
  { fromfield.setEditable( b );
  }

  public void setupMenus()
  { MenuBar menubar = new MenuBar();
    setMenuBar( menubar );
    Menu file = new Menu( "File" );
    menubar.add( file );

    MenuItem q;
    file.add( q = new MenuItem( "Quit", new MenuShortcut(
      KeyEvent.VK_Q ) ) );

    q.addActionListener( new ActionListener() {
      public void actionPerformed( ActionEvent e )
      { CloseMessage();
      }
    });


    Menu view = new Menu( "Message" );
    menubar.add( view );

    MenuItem refresh;
    view.add( refresh = new MenuItem( "Send", new MenuShortcut(
      KeyEvent.VK_S ) ) );

    refresh.addActionListener( new ActionListener() {
      public void actionPerformed( ActionEvent e )
      { sendMessage();
      }
    });

    MenuItem source;
    view.add( source = new MenuItem( "Reset", new MenuShortcut(
      KeyEvent.VK_U ) ) );

    source.addActionListener( new ActionListener() {
      public void actionPerformed( ActionEvent e )
      { reset();
      }
    });


    menubar.add( file );
    menubar.add( view );
  }

  public boolean action( Event e, Object arg )
  { if ( e.target == send )
    { sendMessage();
    }

    return super.action( e , arg );
  }


  public void reset()
  { tofield.setText( "" );
    fromfield.setText( "" );
    subject.setText( "" );
    message.setText( "" );
  }



  public void sendMessage()
  { try
    { setCursor( WAIT_CURSOR );
      showStatus( "Connecting to host" );
      Qsmtp mail = new Qsmtp( "holyrood.ed.ac.uk" );
  
      showStatus( "Creating message" );
      mail.sendmsg( fromfield.getText().trim(), tofield.getText().trim(),
                    subject.getText().trim(), message.getText().trim() );
     
     showStatus( "Message Sent" );
    }
    catch( ProtocolException e )
    { showStatus ( "Error sending mail " );
    }
    catch( IOException e )
    { showStatus ( "Error sending mail " );
    }
    finally
    { reset();
      setCursor( DEFAULT_CURSOR );
    }
  }

  public void CloseMessage()
  { this.setVisible( false );
    this.dispose();
  }

  public void showStatus( String msg )
  {
  }

  public static void main( String args[] )
  { EmailSender es = new EmailSender();
    es.resize( 350, 400 );
    es.setTo( "stevek@epc.co.uk" );
    es.setFrom( "skx@tardis.ed.ac.uk" );
    es.setVisible( true );
  }

}

