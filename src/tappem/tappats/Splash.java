package tappem.tappats;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;

public class Splash extends Activity{
	protected boolean _active = true;
	protected int _splashTime = 5000; // time to display the splash screen in ms
	private Thread mSplashThread;
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.intro_screen);
		
		
		final Splash sPlashScreen = this;   
        
        // The thread to wait for splash screen events
        mSplashThread =  new Thread(){
            @Override
            public void run(){
            	
            	try {
                    int waited = 0;
                    synchronized(this){
                    	while (waited < _splashTime) {
                    		sleep(100);
                    		waited += 100;
                    	}
                    }
                 } catch (InterruptedException e) {
                    // do nothing
                 } finally {
                // Run next activity
                	 finish(); 
                Intent intent = new Intent();
                intent.setClass(sPlashScreen, Home.class);
                startActivity(intent);
                //finish(); 
                
                 }
            }
        };
        
        mSplashThread.start();        
    }
        
    /**
     * Processes splash screen touch events
     */
    @Override
    public boolean onTouchEvent(MotionEvent evt)
    {
        if(evt.getAction() == MotionEvent.ACTION_DOWN)
        {
            synchronized(mSplashThread){
                mSplashThread.notifyAll();
            }
        }
        return true;
    }   
}
