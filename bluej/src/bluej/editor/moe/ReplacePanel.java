package bluej.editor.moe;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.prefmgr.PrefMgr;
import bluej.utility.DBox;
import bluej.utility.DBoxLayout;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;


public class ReplacePanel extends JPanel implements ActionListener, KeyListener {

    private MoeEditor editor;
    private Font font;
    private JTextField replaceText;
    private String replaceString;
    private JButton replaceButton;
    private JButton replaceAllButton;

    private final static String REPLACE_BUTTON_NAME ="replaceBtn";
    private final static String REPLACE_ALL_BUTTON_NAME ="replaceAllBtn";
    private final static String REPLACE_TEXTFIELD ="replaceTextField";

    public ReplacePanel() {
        super();
        font=new Font(PrefMgr.getStandardFont().getFontName(), PrefMgr.getStandardFont().getSize(), PrefMgr.getStandardFont().getSize());
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createLineBorder(Color.black));
        addReplaceBody();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // TODO Auto-generated method stub
        JComponent src = (JComponent) e.getSource();
        String rText=replaceText.getText();
        setReplaceString(rText);
        if (rText==null){
            editor.writeMessage(Config.getString("editor.replaceAll.string") + 
            "is Empty");
            return;
        }         
        if (src.getName()==REPLACE_BUTTON_NAME){
            if (rText!=null)
                editor.replace(rText);
        }
        if (src.getName()==REPLACE_ALL_BUTTON_NAME){
            if (rText!=null)
                editor.replaceAll(rText);
                
        }

    }

    @Override
    public void keyPressed(KeyEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void keyReleased(KeyEvent e) {
        JComponent src = (JComponent) e.getSource();
        if (src.getName()== REPLACE_TEXTFIELD){
            if (((JTextField)src).getText()!=null &&((JTextField)src).getText()!="" ){
                replaceButton.setEnabled(true);
                replaceAllButton.setEnabled(true);
            }    
            else{
                replaceButton.setEnabled(false);
                replaceAllButton.setEnabled(false);
            }
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // TODO Auto-generated method stub

    }

    private void addReplaceBody()
    {
        JLabel replaceLabel;
        DBox replaceBody;
        DBox optionsBody;
        JPanel rBody;
        JPanel body;

        body = new JPanel(new GridLayout(1, 2)); // one row, many columns
        rBody=new JPanel(new GridLayout(1, 2));
        replaceBody=new DBox(DBoxLayout.X_AXIS, 0, BlueJTheme.commandButtonSpacing, 0.0f);
        optionsBody=new DBox(DBoxLayout.X_AXIS, 0, BlueJTheme.commandButtonSpacing, 0.0f);
        
        body.setBackground(MoeEditor.infoColor);
        
        replaceLabel=new JLabel("Replace: ");
        replaceLabel.setFont(font);

        replaceText=new JTextField(10);
        replaceText.setFont(font);
        replaceText.addKeyListener(this);
        replaceText.setName(REPLACE_TEXTFIELD);
        
        replaceButton=new JButton();
        replaceButton.setName(REPLACE_BUTTON_NAME);
        replaceButton.setText("Once");
        replaceButton.setFont(font);
        replaceButton.addActionListener(this);
        replaceButton.setEnabled(false);
        
        replaceAllButton=new JButton();
        replaceAllButton.setName(REPLACE_ALL_BUTTON_NAME);
        replaceAllButton.setText(" All ");
        replaceAllButton.setFont(font);
        replaceAllButton.addActionListener(this);
        replaceAllButton.setEnabled(false);
        
        JLabel closeBody=new JLabel(" ");
        
        replaceBody.add(replaceLabel);
        replaceBody.add(replaceText);
        optionsBody.add(replaceButton);
        optionsBody.add(replaceAllButton);
        optionsBody.add(new JLabel(" "));
        rBody.add(replaceBody);
        rBody.add(optionsBody);
        
        body.add(rBody);  
        body.add(closeBody);
        add(body);
    }

    public MoeEditor getEditor() {
        return editor;
    }

    public void setEditor(MoeEditor editor) {
        this.editor = editor;
    }
    
    public void requestReplaceTextFocus(){
        replaceText.requestFocus();
        replaceText.setText(getReplaceString());
    }
    
    public String getReplaceString() {
        return replaceString;
    }

    public void setReplaceString(String replaceString) {
        this.replaceString = replaceString;
    }

}
