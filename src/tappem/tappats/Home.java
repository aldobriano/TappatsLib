package tappem.tappats;



import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import tappem.tappats.MyLocation.LocationResult;
import tappem.tappats.server.NearbyStopsServerTask;
import tappem.tappats.threads.DownloadTask;
import tappem.tappats.threads.DownloadThread;
import tappem.tappats.threads.DownloadThreadListener;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;


public class Home extends Activity implements DownloadThreadListener, OnClickListener, OnItemClickListener {
	public static int HOME_SCREEN = 0;
	public static int FAVORITES_SCREEN = 1;
	public static int CURRENT_SCREEN = 2;
	private int state;   // 1 - visible,  0 - anything else
	private MargueriteTransportation serverData;
	private String currentStopId;
	private String currentStopName;
	private Location currentLocation;
private Context context; 
	public static String uniqueId;

	private DownloadThread downloadThread;
	
	PendingIntent mPendingIntent;
	private Handler handler;
	ArrayList<BusStop> stops;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		context = getApplicationContext();
		state = 1;
		setContentView(R.layout.tappats_home);
		setProgressBarVisible(true);
		
		//showProgressDialog();
		//SystemTools.checkInternet(this);

		
		//checkVersion();
		
		Button homeBtn = (Button) findViewById(R.id.navbar_home);
		homeBtn.setEnabled(false);
		String uri = "drawable/" + "navbar_home_over";
		int imageResource = getResources().getIdentifier(uri, null, getPackageName());
		homeBtn.setBackgroundResource(imageResource);
		homeBtn.setOnClickListener(this);

		Button favoritesBtn = (Button) findViewById(R.id.navbar_favorites);
		favoritesBtn.setOnClickListener(this);

		Button currentBtn = (Button) findViewById(R.id.navbar_current);
		currentBtn.setOnClickListener(this);

		Button settingsBtn = (Button) findViewById(R.id.navbar_settings);
		settingsBtn.setOnClickListener(this);
		
		showEmptyList();


		

	}

	public void onStart()
	{
		super.onStart();
		uniqueId = SystemTools.getUniqueId(this);

		serverData = new MargueriteTransportation(SystemTools.getUniqueId(this));
		checkVersion();

		


		MyLocation myLocation = new MyLocation();
		LocationResult locationResult = new LocationResult(){
			public void gotLocation(final Location location){
				//Got the location!
				if((currentLocation != null) && (currentLocation.getLatitude() == location.getLatitude()) && (currentLocation.getLongitude() == location.getLongitude())){
					return;
				}
				currentLocation = location;
				
				if(currentLocation != null){
					if(state == 1){
						Log.d("TAPPATS", "GOT GPS, lets download");
						downloadThread.enqueueDownload(new NearbyStopsServerTask(location.getLatitude(), location.getLongitude()), context);
					}else
					{
					}
				}else
				{
					String text = "Your current GPS location couldn't be found.  Please verify your GPS is turned on and try again. \n" +
					"If you know the desired stop name, use the search button to search for the stop.";



					downloadThread.updateUI(text, TappatsErrorCodes.ERROR_GPS, context);

					
					
				}
				
			}

		};
		myLocation.getLocation(this, locationResult);
		if(currentLocation != null){
			Log.d("TAPPATS", currentLocation.getLatitude() + " that was latitude " + currentLocation.getLongitude() +" that was longitude ");
		
		}
		else{
			Log.d("TAPPATS", "currentLocation is null");
			
			showStatusTextInList("...acquiring your gps location...");
		}

		







		
	}
	
	
	@Override
	public void onNewIntent(Intent intent) {
		setIntent(intent);
		
	}
	
	
	
	
	protected void showEmptyList()
	{

		updateList(null);

	}
	
	
	protected void showStatusTextInList(String statusText)
	{
		
			
			ListView l1 = (ListView) findViewById(R.id.bus_stop_list);
			
			l1.setClickable(false);
			l1.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
			l1.setAdapter(new AppStatusEfficientAdapter(this, statusText, R.layout.status_text_list));
			
			
	}
	protected void updateList(ArrayList<BusStop> b)
	{
		stops = b;
		//System.out.println("updatelist");

		// Get all of the rows from the database and create the item list
		ListView l1 = (ListView) findViewById(R.id.bus_stop_list);
		if(l1 == null)
		{
			Log.d("AAH", "l1 esta nullo");
		}
		l1.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		l1.setClickable(true);
		l1.setOnItemClickListener(this);
		l1.setAdapter(new StopsEfficientAdapter(this, stops, R.layout.nearby_stop_list));
	}
	
	public void displayText(String text)
	{
		
		
		int duration = Toast.LENGTH_LONG;

		Toast toast = Toast.makeText(this, text, duration);
		toast.setGravity(Gravity.CENTER, 0, 0);
		toast.show();
	}
	public void handleUpdateUI(final String text, final int situation)
	{
		handler.post(new Runnable() {
			
			public void run() {
				
				
				switch(situation){
				
				case TappatsErrorCodes.ERROR_GPS:
					displayText(text);
					setProgressBarVisible(false);
					showStatusTextInList("...Error while getting GPS...");
					break;
					
				case TappatsErrorCodes.ERROR_INTERNET:
					displayText(text);
					
					setProgressBarVisible(false);
					break;
				case TappatsErrorCodes.CLEAR_LIST:
					showStatusTextInList("...Error with internet connection...");
					setProgressBarVisible(false);
					break;
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

				//System.out.println("progress so far: " + completed + " / " + total);

				if(dt instanceof NearbyStopsServerTask)
				{
					//System.out.println("here ");
					
					
					updateList((ArrayList<BusStop>) dt.getResult());
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
	

	protected void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		saveState();
		outState.putSerializable(BusAdapter.STOP_ID, currentStopId);

	}

	void resolveIntent(Intent intent) {
		Bundle extras = intent.getExtras();
		currentStopId = extras != null ? extras.getString(BusAdapter.STOP_ID) : null;
		currentStopName = extras != null ? extras.getString(BusAdapter.STOP_NAME) : null;
		uniqueId = extras != null ? extras.getString(BusAdapter.PHONE_ID) : null;
		
	}


	protected void onPause()
	{
		
		super.onPause();
		state = 0;
		saveState();
		
	}

	protected void onResume()
	{
		
		resolveIntent(getIntent());
		super.onResume();
		
		// Create and launch the download thread
		
		downloadThread = new DownloadThread(this);
		downloadThread.start();

		// Create the Handler. It will implicitly bind to the Looper
		// that is internally created for this thread (since it is the UI thread)
		handler = new Handler();
		state = 1;
		if(currentLocation != null)
		{
			try{
				downloadThread.enqueueDownload(new NearbyStopsServerTask(currentLocation.getLatitude(), currentLocation.getLongitude()), context);
			}catch(Exception e){}
		
			
		}
		//fillData();
	}
	protected void onDestroy()
	{
		finish();
		super.onDestroy();
	}

	private void saveState()
	{
		//TODO
	}

	public void checkVersion()
	{
		URL sourceUrl;
		try {
			sourceUrl = new URL("http://margueritenfc.heroku.com/version.xml");


			BufferedReader in = new BufferedReader(
					new InputStreamReader(
							sourceUrl.openStream()));

			String version = in.readLine();
			in.close();

			//System.out.println(version);

			if(!BusAdapter.VERSION.equals(version))
			{
				CharSequence text = "A new version of TapPATS has been released. Please update your app!";
				int duration = Toast.LENGTH_LONG;

				Toast toast = Toast.makeText(this, text, duration);
				toast.setGravity(Gravity.CENTER, 0, 0);
				toast.show();



				//app is outdated
				String url = "http://www.tappem.com/stanfordtappats.htm";
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setData(Uri.parse(url));
				startActivity(i);

				finish();
			}


		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e)
		{}
	}




	public void onClick(View v) {

		switch (v.getId()) {
		case R.id.navbar_home:


			break;
		case R.id.navbar_current:

			if(currentStopId != null && currentStopName != null){

				Intent i = new Intent(this, NextBus.class);
				i.putExtra(BusAdapter.PHONE_ID, uniqueId);
				i.putExtra(BusAdapter.STOP_ID, currentStopId);
				i.putExtra(BusAdapter.STOP_NAME, currentStopName);
				i.putExtra(BusAdapter.INTERFACE, "gps");
				startActivity(i);
			//	finish();
			}else
			{
				String text = "Whoa! You haven't selected your current stop. Please pick one from the list!";
				
				int duration = Toast.LENGTH_SHORT;

				Toast toast = Toast.makeText(context, text, duration);
				toast.setGravity(Gravity.CENTER, 0, 0);
				toast.show();
			}

			break;
		case R.id.navbar_favorites:
			//Toast.makeText(this, "You clicked the button favorites", Toast.LENGTH_SHORT).show();
			Intent i = new Intent(this, FavoritesScreen.class);
			i.putExtra(BusAdapter.PHONE_ID, uniqueId);
			if(currentStopId != null && currentStopName != null)
			{
				i.putExtra(BusAdapter.STOP_ID, currentStopId);
				i.putExtra(BusAdapter.STOP_NAME, currentStopName);
			}
			startActivity(i);
			//finish();

			break;
		case R.id.navbar_settings:
			Intent i1 = new Intent(this, Settings.class);
			i1.putExtra(BusAdapter.PHONE_ID, uniqueId);
			if(currentStopId != null && currentStopName != null)
			{
				i1.putExtra(BusAdapter.STOP_ID, currentStopId);
				i1.putExtra(BusAdapter.STOP_NAME, currentStopName);
			}
			startActivity(i1);

			break;
		default:
			//Toast.makeText(this, "You clicked", Toast.LENGTH_SHORT).show();
			break;
		}


	}

	public void onItemClick(AdapterView<?> arg0, View arg1, int listIndex,
			long arg3) {
		
		try{
			if(stops != null || !stops.isEmpty() || stops.size() >= listIndex || !stops.get(listIndex).getStopLabel().equals("")){
				//arg2 is the list index cool.
				currentStopId = stops.get(listIndex).getStopId();
				currentStopName = stops.get(listIndex).getStopLabel();
				Intent i = new Intent(this, NextBus.class);
				i.putExtra(BusAdapter.STOP_ID, currentStopId);
				i.putExtra(BusAdapter.STOP_NAME, currentStopName);
				i.putExtra(BusAdapter.PHONE_ID, uniqueId);
				i.putExtra(BusAdapter.INTERFACE, "gps");
				startActivity(i);
				downloadThread.requestStop();
				//finish();
			}
		}catch(Exception e)
		{}
	}

	









}
