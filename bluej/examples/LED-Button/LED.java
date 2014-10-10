import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.event.*;
/**
 * Write a description of class LED here.
 * 
 * @author Ian Utting
 * @author Fabio Heday
 * @version 02 Oct 2014
 */
public class LED implements GpioPinListenerDigital
{
    //The LED gpio
    private GpioPinDigitalOutput ledPin;
    

    /**
     * Constructor for objects of class LED
     */
    public LED()
    {
        GpioController gpio = GpioFactory.getInstance();
        ledPin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_06,"LED", PinState.LOW);
    }

    /**
     * Flash the LED for a given amount of time
     * @param  ms   the time to flash the LED in milliseconds
     */
    public void flash (int ms)
    {
        ledPin.high();
        try{
            Thread.sleep(ms);
        }
        catch (InterruptedException e)
        {
        }
        ledPin.low();
        
    }
    /**
     * Turns on the LED
     */
    public void on() {
        ledPin.high();
    }
    
    /**
     * Turns off the LED
     */
    public void off() {
        ledPin.low();
    }
    
    /**
     * toggle LED state
     */
    @Override
    public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event){        
        ledPin.toggle();
    }
}