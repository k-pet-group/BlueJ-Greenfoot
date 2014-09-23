package greenfoot.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
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

/**
 * Encapsulates a panel that pops up, overlaid at the bottom of the world, to ask the user
 * for an input string, given a prompt string.  Used in the IDE and standalone versions.
 *
 */
public class AskPanel implements ActionListener
{
    private static final Color BACKGROUND = new Color(222, 166, 41);
    private JLabel promptDisplay;
    private JPanel panel;
    private JTextField answer;
    private JButton ok;
    private AnswerListener answerListener;
    
    /**
     * Constructs the AskPanel but sets it to hidden.  Must be called on EDT.
     */
    public AskPanel()
    {
        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.BLACK));
        
        promptDisplay = new JLabel("");
        promptDisplay.setOpaque(true);
        promptDisplay.setBackground(BACKGROUND);
        promptDisplay.setAlignmentX(0.0f);
        promptDisplay.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(2, 0, 0, 0, Color.DARK_GRAY), BorderFactory.createEmptyBorder(4, 20, 4, 20)));
        JPanel promptPanel = new JPanel();
        promptPanel.setLayout(new BorderLayout());
        promptPanel.setOpaque(false);
        
        promptPanel.add(promptDisplay, BorderLayout.SOUTH);
        
        panel.add(promptPanel);
        
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
        panel.add(answerPanel);
        
        hidePanel();
    }
    
    /**
     * Simple listener to listen for the answer when it is given.  Will be called
     * on the EDT.
     */
    public static interface AnswerListener
    {
        public void answered(String answer);
    }
    
    /**
     * Show the panel, with the given width and user prompt.  Returns immediately,
     * and will later call the AnswerListener with the answer, once it is given.
     * 
     * Must be called on EDT.
     */
    public void showPanel(int width, String prompt, AnswerListener listener)
    {
        answerListener = listener;
        panel.setMaximumSize(new Dimension(width, Integer.MAX_VALUE));
        panel.setPreferredSize(new Dimension(width, 0));
        panel.setVisible(true);
        
        answer.setText("");    
        promptDisplay.setText("<html>" + prompt + "</html>");
        promptDisplay.setMaximumSize(new Dimension(width, Integer.MAX_VALUE));
        answer.requestFocus();
    }
    
    /**
     * Hides the answer panel.  Must be called on EDT.
     */
    public void hidePanel()
    {
        panel.setVisible(false);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        hidePanel();
        if (answerListener != null)
            answerListener.answered(answer.getText());    
    }

    /**
     * Checks if panel is currently showing.
     */
    public boolean isPanelShowing()
    {
        return panel.isVisible();
    }

    /**
     * Get the actual JPanel (since we encapsulate rather inherit)
     */
    public JPanel getComponent()
    {
        return panel;
    }
}
