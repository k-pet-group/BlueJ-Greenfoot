 import java.awt.*;
import java.awt.event.*;

public class SourceViewer
  extends Frame
{

  public SourceViewer( String source )
  {
     this.setLayout( new BorderLayout() );
     TextArea ta = new TextArea();
     this.add( "Center", ta );
     ta.setText( source );
     ta.setEditable( false );
     setupMenus();
  }

  public void setupMenus()
  { MenuBar menubar = new MenuBar();
    setMenuBar( menubar );
    Menu file = new Menu( "File" );
    menubar.add( file );

    MenuItem q;
    file.add( q = new MenuItem( "Close", new MenuShortcut(
      KeyEvent.VK_Q ) ) );

    q.addActionListener( new ActionListener() {
      public void actionPerformed( ActionEvent e )
      { CloseSourceViewer();
      }
    });
  }


  public void CloseSourceViewer()
  { this.setVisible( false );
    this.dispose();
  }

}
