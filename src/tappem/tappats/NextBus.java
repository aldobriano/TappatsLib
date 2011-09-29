package tappem.tappats;




import java.io.IOException;
import java.util.Random;


import android.widget.Toast;


import tappem.tappats.BusLine;
import tappem.tappats.BusStop;
import tappem.tappats.MargueriteTransportation;
import tappem.tappats.listeners.FavoriteButtonListener;
import tappem.tappats.server.NextBusServerTask;
import tappem.tappats.server.NfcTagServerTask;
import tappem.tappats.threads.DownloadTask;
import tappem.tappats.threads.DownloadThread;
import tappem.tappats.threads.DownloadThreadListener;
import android.app.Activity;
import android.app.PendingIntent;
import android.net.Uri;

import android.os.Bundle;
import android.os.Handler;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.telephony.TelephonyManager;
import android.webkit.*;
import android.database.Cursor;


public class NextBus extends Activity implements DownloadThreadListener, OnClickListener{

	private MargueriteTransportation serverData;
	
	private BusStop currentBusStop;
	//public String stopId;
	public static String uniqueId;
	private FavoritesDbAdapter dbHelper;
	private StopsAssistant stopHelper;
	String stop_lon;
	String stop_lat;
	boolean isfavorite;
	GestureDetector gestureDetector;
	View.OnTouchListener gestureListener;
	
	
	private DownloadThread downloadThread;
	private String method = "";
	private Handler handler;
	
	private int updateListCounter = 0;
	
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.tappats_current);

		PackageManager pm = getPackageManager();
		
		Button homeBtn = (Button) findViewById(R.id.navbar_home);
		homeBtn.setOnClickListener(this);

		Button favoritesBtn = (Button) findViewById(R.id.navbar_favorites);
		favoritesBtn.setOnClickListener(this);

		Button currentBtn = (Button) findViewById(R.id.navbar_current);
		currentBtn.setEnabled(false);
		String uri = "drawable/" + "navbar_current_over";
		int imageResource = getResources().getIdentifier(uri, null, getPackageName());
		currentBtn.setBackgroundResource(imageResource);
		currentBtn.setOnClickListener(this);

		Button settingsBtn = (Button) findViewById(R.id.navbar_settings);
		settingsBtn.setOnClickListener(this);

		if(savedInstanceState == null){
			
			resolveIntent(getIntent());

		}


		stopHelper = new StopsAssistant(this);
		
		//populate the webview with map


		showEmptyList();

		showMap();



		dbHelper = new FavoritesDbAdapter(this);
		


		//stopHelper.open();

		final Button button = (Button) findViewById(R.id.refresh);
		button.setOnClickListener(this);

		// Create and launch the download thread
        downloadThread = new DownloadThread(this);
        downloadThread.start();
        
        // Create the Handler. It will implicitly bind to the Looper
        // that is internally created for this thread (since it is the UI thread)
        handler = new Handler();


	}
	void resolveIntent(Intent intent) {
		Bundle extras = intent.getExtras();

		String stopId = extras != null ? extras.getString(BusAdapter.STOP_ID) : null;
		String stopName = extras != null ? extras.getString(BusAdapter.STOP_NAME) : "Bus Stop #" + stopId;
		method = extras != null ? extras.getString(BusAdapter.INTERFACE) : "";
		try{
			currentBusStop = new BusStop(Integer.parseInt(stopId), stopName);
			
		}catch(Exception e)
		{

			System.err.println("  WRONG PARAMETERS FOR NEXT BUS");
			finish();
		}
		uniqueId = extras != null ? extras.getString(BusAdapter.PHONE_ID) : null;


		if(uniqueId == null)

			uniqueId = SystemTools.getUniqueId(this);

		try{
			System.out.println(currentBusStop.getStopId() + "    " + currentBusStop.getStopLabel() + "    " + uniqueId);
		}catch(Exception e)
		{
			System.out.println("null intent");
		}

	}
	private void showMap()
	{
		WebView webview = (WebView) findViewById(R.id.map);
		webview.setWebViewClient(new WebViewClient());
		webview.getSettings().setJavaScriptEnabled(true);
		webview.setVerticalScrollBarEnabled(false);
		if(stop_lon != null || stop_lon == null){
			Cursor cursor = null;
			//KEY_STOPLON,KEY_STOPLAT
			stopHelper.openDB();
			cursor = stopHelper.fetchStop(currentBusStop.getStopId());
			stopHelper.closeDB();
			stop_lon= cursor.getString(2);
			stop_lat= cursor.getString(3);
		}
		String url ="http://maps.google.com/maps/api/staticmap?center="+ stop_lon+","+ stop_lat + "&zoom=15&size=440x80&maptype=roadmap&markers=color:red%7Ccolor:red%7Clabel:C%7C"+ stop_lon +"," +stop_lat + "&sensor=true";
		System.out.println(url);
		webview.loadUrl(url);
		webview.setOnTouchListener(new View.OnTouchListener() {

			public boolean onTouch(View arg0, MotionEvent arg1) {
				
				try {
			

					Uri newuri = Uri.parse("geo:" + stop_lon  + "," + stop_lat +"");
					System.out.println(newuri.toString());
					Intent intent = new Intent(Intent.ACTION_VIEW, newuri);
					startActivity(intent);
					
				} catch (Exception e) { }
				
				return true;
			}
		});    

	}
	@Override
	public void onNewIntent(Intent intent) {
		setIntent(intent);

	}
	
	protected void showEmptyList()
	{
		TextView stopName = (TextView) findViewById(R.id.stopname);
		stopName.setText(currentBusStop.getStopLabel());
		ListView l1 = (ListView) findViewById(R.id.buses);
		l1.setAdapter(new LinesEfficientAdapter(this, currentBusStop, R.layout.buses));


	}
	public void handleBusStop(String castopId)
 	{
		if(castopId.equals("ERROR"))
		{
			String text = "Sorry, this tag hasn't been activated yet, use GPS instead! Our team is working on it! Thanks for letting us know.";

			int duration = Toast.LENGTH_SHORT;

			Toast toast = Toast.makeText(this, text, duration);
			toast.setGravity(Gravity.CENTER, 0, 0);
			toast.show();
		}else{
			currentBusStop = new BusStop(castopId,castopId);

			stopHelper = new StopsAssistant(this);

			//populate the webview with map

			showMap();

			dbHelper = new FavoritesDbAdapter(this);

			dbHelper.open();
			Cursor cursor_favorite = dbHelper.fetchFavorites(currentBusStop.getStopId());
			dbHelper.close();
			isfavorite =cursor_favorite.moveToFirst(); 
			final Button favorite_button = (Button) findViewById(R.id.favorites);


			if(isfavorite)
			{
				favorite_button.setBackgroundDrawable(getResources().getDrawable(R.drawable.star_favorite));

			}
			favorite_button.setOnClickListener(new FavoriteButtonListener(this,currentBusStop.getStopId(),currentBusStop.getStopLabel(),isfavorite));
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			downloadThread.enqueueDownload(new NextBusServerTask(currentBusStop.getStopId(),uniqueId,getMethod()), this);
		}
 	}
	
	private String getMethod()
	{
		if(method == null || method.equals(""))
			return "gps";
		else
			return method;
	}
	private void setTitleStopText(BusStop stop)
	{
		if(stop != null){
			TextView stopName = (TextView) findViewById(R.id.stopname);
			stopName.setText(stop.getStopId() + ": " + stop.getStopLabel());
		}
	}
	
	
	
	protected void updateList(BusStop b)
	{
		
		
		if(b != null)
		{
		currentBusStop = b;
		setTitleStopText(currentBusStop);
		// Get all of the rows from the database and create the item list
		ListView l1 = (ListView) findViewById(R.id.buses);
		l1.setAdapter(new LinesEfficientAdapter(this, currentBusStop, R.layout.buses));
		
		}else
		{
			//try one more time before sending a message
			//downloadThread.enqueueDownload(new NextBusServerTask(currentBusStop.getStopId(),uniqueId,"test"), this);  TODO
			
		}
	}
	protected void onSaveInstanceState(Bundle savedInstanceState)
	{
			savedInstanceState.putString("stopId", currentBusStop.getStopId());
		  savedInstanceState.putString("stopName", currentBusStop.getStopLabel());
		  savedInstanceState.putString("uniqueId", uniqueId);
		  savedInstanceState.putString("lat", stop_lat);
		  savedInstanceState.putString("lon", stop_lon);
	System.out.println("IN SAVE");
		  super.onSaveInstanceState(savedInstanceState);
		

	}
	
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
	  super.onRestoreInstanceState(savedInstanceState);
	  // Restore UI state from the savedInstanceState.
	  // This bundle has also been passed to onCreate.
	  System.out.println("IN RESTORE");
	 
	  String stopId = savedInstanceState.getString("stopId");
	  String stopName = savedInstanceState.getString("stopName");
	  uniqueId = savedInstanceState.getString("uniqueId");
	   stop_lat = savedInstanceState.getString("stop_lat");
	   stop_lon = savedInstanceState.getString("stop_lon");
	   currentBusStop = new BusStop(Integer.parseInt(stopId), stopName);
	}

	protected void onPause()
	{
		
		//mAdapter.disableForegroundDispatch(this);
		super.onPause();
		stopHelper.closeDB();
		saveState();
		

	}
	@Override
	protected void onDestroy() {
		super.onDestroy();
				
		// request the thread to stop
		downloadThread.requestStop();	
    }
	protected void onResume()
	{
		updateListCounter = 0;

		super.onResume();
		dbHelper.open();
		Cursor cursor_favorite = dbHelper.fetchFavorites(currentBusStop.getStopId());
		dbHelper.close();
		isfavorite =cursor_favorite.moveToFirst(); 
		final Button favorite_button = (Button) findViewById(R.id.favorites);
		
		
		if(isfavorite)
    	{
			favorite_button.setBackgroundDrawable(getResources().getDrawable(R.drawable.star_favorite));
			
    	}
		favorite_button.setOnClickListener(new FavoriteButtonListener(this,currentBusStop.getStopId(),currentBusStop.getStopLabel(),isfavorite));
		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		downloadThread.enqueueDownload(new NextBusServerTask(currentBusStop.getStopId(),uniqueId,getMethod()), this);
	}

	private void saveState()
	{
		
	}



	public void displayText(String text)
	{
		
		
		int duration = Toast.LENGTH_SHORT;

		Toast toast = Toast.makeText(this, text, duration);
		toast.setGravity(Gravity.CENTER, 0, 0);
		toast.show();
	}
	public void handleUpdateUI(final String text, final int situation)
	{
		handler.post(new Runnable() {
			
			public void run() {
				
				
				switch(situation){
				
				
					
				case TappatsErrorCodes.ERROR_INTERNET:
					displayText(text);
					
					setProgressBarVisible(false);
					break;
				case TappatsErrorCodes.CLEAR_LIST:
					
				}

			}
		});
		
	}
	// note! this might be called from another thread
	public void handleDownloadThreadUpdate(final DownloadTask dt) {
		// we want to modify the progress bar so we need to do it from the UI thread 
		// how can we make sure the code runs in the UI thread? use the handler!
		handler.post(new Runnable() {
			public void run() {
				int total = downloadThread.getTotalQueued();
				int completed = downloadThread.getTotalCompleted();
								
				System.out.println("progress so far: " + completed + " / " + total);
				
				if(dt instanceof NextBusServerTask)
				{
					setProgressBarVisible(false);
					
					updateList((BusStop) dt.getResult());
					
				}else if(dt instanceof NfcTagServerTask)
				{
					
					
					handleBusStop((String) dt.getResult());
					setProgressBarVisible(false);
				}
				
				
				
				
			}
		});
	}

	public void setProgressBarVisible(boolean visible)
	{
		ProgressBar pb = (ProgressBar) findViewById(R.id.pb);
		if(!visible)
			pb.setVisibility(View.GONE);
		else
			pb.setVisibility(View.VISIBLE);
	}



	public void onClick(View v) {

		switch (v.getId()) {
		case R.id.navbar_home:
			Intent i = new Intent(this, Home.class);
			i.putExtra(BusAdapter.PHONE_ID, uniqueId);
			if(currentBusStop != null){
				i.putExtra(BusAdapter.STOP_ID, currentBusStop.getStopId());
				i.putExtra(BusAdapter.STOP_NAME, currentBusStop.getStopLabel());
			}
			
			startActivity(i);
			finish();

			break;
		case R.id.navbar_current:





			break;
		case R.id.navbar_favorites:
			//Toast.makeText(this, "You clicked the button favorites", Toast.LENGTH_SHORT).show();
			Intent i1 = new Intent(this, FavoritesScreen.class);
			i1.putExtra(BusAdapter.PHONE_ID, uniqueId);
			i1.putExtra(BusAdapter.STOP_ID, currentBusStop.getStopId());
			i1.putExtra(BusAdapter.STOP_NAME, currentBusStop.getStopLabel());
			startActivity(i1);
			finish();

			break;
		case R.id.navbar_settings:
			Intent i11 = new Intent(this, Settings.class);
			
			i11.putExtra(BusAdapter.PHONE_ID, uniqueId);
			i11.putExtra(BusAdapter.STOP_ID, currentBusStop.getStopId());
			i11.putExtra(BusAdapter.STOP_NAME, currentBusStop.getStopLabel());
			startActivity(i11);

			break;
		case R.id.refresh:
			if(currentBusStop != null && !currentBusStop.isEmpty()){
				setProgressBarVisible(true);
				currentBusStop.setBusLines(null);
				showEmptyList();
				handleBusStop(currentBusStop.getStopId());
			}
			else
			{
				Intent i2 = new Intent(this, Home.class);
				startActivity(i2);
				finish();
			}
			break;
		default:
			Toast.makeText(this, "You clicked", Toast.LENGTH_SHORT).show();
			break;
		}


	}






}