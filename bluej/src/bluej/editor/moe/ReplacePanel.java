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

    public ReplacePanel(MoeEditor ed) {
        super();
        font=new Font(PrefMgr.getStandardFont().getFontName(), PrefMgr.getStandardFont().getSize(), PrefMgr.getStandardFont().getSize());
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createLineBorder(Color.black));
        addReplaceBody();
        editor=ed;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JComponent src = (JComponent) e.getSource();
        setReplaceString(replaceText.getText());
        if (getReplaceString()==null){
            editor.writeMessage(Config.getString("editor.replaceAll.string") + 
            "is Empty");
            return;
        }         
        if (src.getName()==REPLACE_BUTTON_NAME){
            if (getReplaceString()!=null)
                editor.replace(getReplaceString());
        }
        if (src.getName()==REPLACE_ALL_BUTTON_NAME){
            if (getReplaceString()!=null)
                editor.replaceAll(getReplaceString());

        }

    }

    @Override
    public void keyPressed(KeyEvent e) {

    }

    @Override
    public void keyReleased(KeyEvent e) {
        JComponent src = (JComponent) e.getSource();
        if (src.getName()== REPLACE_TEXTFIELD){
            setReplaceString(replaceText.getText());
            //only enable the once and all buttons if both find and replace are populated
            if ((getReplaceString()!=null && getReplaceString().length()!=0) 
                    && (editor.getFindSearchString()!=null && editor.getFindSearchString().length()!=0) ){
                enableButtons(true);
            }
            else
                enableButtons(false);

        }
    }

    @Override
    public void keyTyped(KeyEvent e) {

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

    public void requestReplaceTextFocus(){
        replaceText.requestFocus();
        replaceText.setText(getReplaceString());
    }

    protected String getReplaceString() {
        return replaceString;
    }

    protected void setReplaceString(String replaceString) {
        this.replaceString = replaceString;
    }

    /**
     * enableButtons enable the once and all buttons
     * @param enable
     */
    protected void enableButtons(boolean enable)
    {
        replaceAllButton.setEnabled(enable);
        replaceButton.setEnabled(enable);
    }

}
