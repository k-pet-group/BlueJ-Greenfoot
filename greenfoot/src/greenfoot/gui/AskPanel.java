package greenfoot.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class AskPanel extends JPanel implements ActionListener
{
    private static final Color BACKGROUND = new Color(222, 166, 41);
    private JLabel promptDisplay;
    private JTextField answer;
    private JButton ok;
    private AnswerListener answerListener;
    
    public AskPanel()
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.BLACK));
        
        promptDisplay = new JLabel("");
        promptDisplay.setOpaque(true);
        promptDisplay.setBackground(BACKGROUND);
        promptDisplay.setAlignmentX(0.0f);
        promptDisplay.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(2, 0, 0, 0, Color.DARK_GRAY), BorderFactory.createEmptyBorder(4, 20, 4, 20)));
        JPanel promptPanel = new JPanel();
        promptPanel.setLayout(new BorderLayout());
        promptPanel.setOpaque(false);
        
        promptPanel.add(promptDisplay, BorderLayout.SOUTH);
        
        add(promptPanel);
        
        JPanel answerPanel = new JPanel();
        answerPanel.setLayout(new BoxLayout(answerPanel, BoxLayout.X_AXIS));
        answerPanel.setBackground(BACKGROUND);
        
        answer = new JTextField();
        answer.setMaximumSize( 
                new Dimension(Integer.MAX_VALUE, answer.getPreferredSize().height) );
        answer.addActionListener(this);
        answerPanel.add(answer);
        
        ok = new JButton("OK");
        ok.addActionListener(this);
        ok.setMaximumSize(new Dimension(50, answer.getMaximumSize().height));
        answerPanel.add(ok);
        
        answerPanel.setBorder(BorderFactory.createEmptyBorder(3, 20, 8, 20));
        add(answerPanel);
        
        hidePanel();
    }
    
    public static interface AnswerListener
    {
        public void answered(String answer);
    }
    
    public void showPanel(int width, String prompt, AnswerListener listener)
    {
        answerListener = listener;
        setMaximumSize(new Dimension(width, Integer.MAX_VALUE));
        setPreferredSize(new Dimension(width, 0));
        setVisible(true);
        
        answer.setText("");    
        promptDisplay.setText("<html>" + prompt + "</html>");
        promptDisplay.setMaximumSize(new Dimension(width, Integer.MAX_VALUE));
        answer.requestFocus();
    }
    
    public void hidePanel()
    {
        setVisible(false);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        hidePanel();
        if (answerListener != null)
            answerListener.answered(answer.getText());    
    }
}
