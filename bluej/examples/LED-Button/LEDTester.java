/**
 * This class testa led by flashing some morse code
 * @author Ian Utting
 * @author Fabio Heday
 * @version 02 Oct 2014
 */
public class LEDTester
{
    public LED pin1;
    
    //unit of time, in milliseconds
    private final static int UNIT = 250;
    
    public static void main(String [] args) {
        LEDTester lt = new LEDTester();
        
        String s = "SOS SOS SOS";
        if(args.length > 0 && args[0] != null && !args[0].equals("")) s = args[0];
        
        lt.flashMorse(s);
    }
    
    /*
     * create a new LED instance
     */
    public LEDTester() {
        this(new LED());
    }
    
    /*
     * Creates a LED instance based on a LED object
     */
    public LEDTester(LED p) {
        pin1 = p;
    }
    
    /*
     * Flashes the LED 10 times
     */
    public void flash10Times() {

        pin1 = new LED();
        
        for(int i = 0; i < 10; i++) {
            pin1.flash(200);
            try { Thread.sleep(200); } catch (InterruptedException e) {}
        }
    }
    
    
    /*
     * Sends the morse code using the LED.
     */
    public void flashMorse(String msg) {
        pin1 = new LED();
        
        char [] morse = StringToMorse.translate(msg).toCharArray();
        
        // . = 1, - = 3, intra-char = 1, inter-char = 3, space = 7.",
        
        for (char atom : morse) {
                if (atom == '.') {
                    pin1.flash(UNIT);
                } else if (atom == '-') {
                    pin1.flash(3*UNIT);
                } else if (atom == '/') {   // inter-symbol gap
                    sleep(1);   // plus one leading and one trailing == 3
                } else {
                    // must be a space
                    sleep(5);   // plus one leading and one trailing == 7
                }
                sleep(1);   // common gap
        }
    }
    /*
     * Wait units times the defined unit of time
     */
    private void sleep(int units) {
        try { 
            Thread.sleep(units*UNIT); 
        } catch (InterruptedException e) 
        {
        }
    }
            
}
