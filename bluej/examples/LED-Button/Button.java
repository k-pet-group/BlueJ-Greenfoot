import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.event.*;

/**
 * This class represents a Button.
 * @author Fabio Heday
 * @version 02 Oct 2014
 */
public class Button
{
    // the button instance
    private GpioPinDigitalInput button;

    /**
     * Button constructor at GPIO 7
     */
    public Button()
    {
        //get a gpio controller
        GpioController gpio = GpioFactory.getInstance();
        //provision the Button.
        button = gpio.provisionDigitalInputPin(RaspiPin.GPIO_07, PinPullResistance.PULL_DOWN);
    }

    /**
     * Print the state of the button
     *  
     */
    public void checkButton()
    { 
        System.out.println("State = "+button.getState());
    }
    
    /*
     * Add a listener to this button
     */
    public void addListener(GpioPinListenerDigital listener)
    {
        //add the listener.
        button.addListener(listener);
    }
}