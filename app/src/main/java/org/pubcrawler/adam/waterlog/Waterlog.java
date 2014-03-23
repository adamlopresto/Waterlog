package org.pubcrawler.adam.waterlog;

import java.util.Calendar;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.TextView;
import android.widget.Toast;

public class Waterlog extends Activity implements OnClickListener, OnLongClickListener{
	private int drinksToday;
	private int ozToday;
	private long lastDrink;
	private Runnable updater;
	private Handler handler;
	
	private View coffee;
	private View home;
	private View work;
	
	public enum DrinkType {
		Home(12), Work(16), Coffee(8);
		
		private final int oz;
		DrinkType(int oz){
			this.oz = oz;
		}
		
		public int oz(){
			return oz;
		}
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
    	
        setContentView(R.layout.main);

        coffee = findViewById(R.id.coffee);
        home = findViewById(R.id.home);
        work = findViewById(R.id.work);

        coffee.setOnClickListener(this);
        home.setOnClickListener(this);
        work.setOnClickListener(this);
        findViewById(R.id.snooze).setOnClickListener(this);
        findViewById(R.id.drink).setOnClickListener(this);

        coffee.setOnLongClickListener(this);
        home.setOnLongClickListener(this);
        work.setOnLongClickListener(this);
        findViewById(R.id.snooze).setOnLongClickListener(this);
        findViewById(R.id.drink).setOnLongClickListener(this);
        
        updateText();
    	
    	updater = new Runnable(){
    		public void run(){
    			updateText();
    			long delay;
    			long now = System.currentTimeMillis();
    			if (lastDrink == 0){
    				return;	
    			} else if (lastDrink >= now-60*1000){ //Within one minute, update every second
    				delay = 1000-(now - lastDrink)%1000;
    				
    			} else {
    				delay = 60*1000-(now - lastDrink) % (60*1000);
    			}
    			//Log.d("Waterlog", delay+","+lastDrink+","+now);
    			handler.postDelayed(this,delay);
    		}
    	};
    	handler = new Handler();
    
        updateText();
        
    }
    
    @Override
    public void onResume(){
    	super.onResume();
    	handler.postDelayed(updater, 200);
    }
    
    @Override
    public void onPause(){
    	super.onPause();
    	handler.removeCallbacks(updater);
    }
    
    @Override
	public boolean onCreateOptionsMenu(Menu menu){
		getMenuInflater().inflate(R.menu.main_menu, menu);
		/*
		Cursor cur = managedQuery(Audio.Media.INTERNAL_CONTENT_URI, null, null, null, null);
		cur.moveToFirst();
		do {
			Log.d("Waterlog", "_id:"+cur.getLong(0)+"\t"+cur.getString(2)+"\t"+cur.getInt(18));
		} while (cur.moveToNext());
		*/
		return true;
	}

	@Override
	/* Handles item selections */
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.reset:
                drinksToday = ozToday = 0;
                lastDrink = 0L;
                updatePrefs();
                updateText();
                return true;
            case R.id.notify:
                sendBroadcast(new Intent(this, WaterlogReceiver.class).setAction(WaterlogReceiver.ALARM_ACTION));
                return true;
        }
        return false;
    }

	public void onClick(View v){

		onLongClick(v);
    	
    	finish();
    }

	@Override
	public boolean onLongClick(View v) {

		switch(v.getId()){
    	case R.id.coffee:
    		drink(this, 8, "Coffee");
    		break;
    	case R.id.home:
    		drink(this, 12, "Home");
    		break;
    	case R.id.work:
    		drink(this, 16, "Work");
    		break;
    	case R.id.snooze:
	    	setAlarmRel(this, DateUtils.HOUR_IN_MILLIS);
    		break;
    	case R.id.drink:
    		
    		try {
	    		@SuppressWarnings("ConstantConditions") int oz = Integer.parseInt(((TextView)findViewById(R.id.oz)).getText().toString());
	    		drink(this, oz, oz + " ounces");
    		} catch (NumberFormatException e){
    			findViewById(R.id.oz).requestFocus();
    			return true;
    		}
    	}

		updateText();
		return true;
	}


	public static void drink(Context context, int oz, String msg) {
        SharedPreferences prefs = context.getSharedPreferences("Waterlog", MODE_PRIVATE);
    	int drinksToday = prefs.getInt("DRINKS_TODAY", 0);
    	int ozToday = prefs.getInt("OZ_TODAY", 0);
    	long lastDrink = prefs.getLong("LAST_DRINK_TIME", 0L);

		if (!DateUtils.isToday(lastDrink)){
			drinksToday = 0;
			ozToday = 0;
		}
	
		ozToday += oz;
    	drinksToday++;
    	lastDrink = System.currentTimeMillis();

    	SharedPreferences.Editor editor = prefs.edit();
    	editor.putInt("DRINKS_TODAY", drinksToday);
    	editor.putInt("OZ_TODAY", ozToday);
    	editor.putLong("LAST_DRINK_TIME", lastDrink);
    	editor.commit();
    	
    	if (ozToday < 100){
	    	Toast.makeText(context, TextUtils.concat(context.getText(R.string.drink_recorded),msg), Toast.LENGTH_LONG).show();
	    	setAlarmRel(context, DateUtils.HOUR_IN_MILLIS);
    	} else {
    		Toast.makeText(context, R.string.end_of_day, Toast.LENGTH_LONG).show();
    		
    		TaskerIntent i = new TaskerIntent("Finished drinking");
        	context.sendBroadcast(i);
        	
    		Calendar cal = Calendar.getInstance();
    		cal.set(Calendar.HOUR_OF_DAY, 9);
    		cal.set(Calendar.MINUTE, 0);
    		cal.set(Calendar.SECOND, 0);
    		cal.set(Calendar.MILLISECOND, 0);
    		if (DateUtils.isToday(cal.getTimeInMillis())){
    			cal.add(Calendar.DATE, 1);
    		}
    		setAlarmAbs(context, cal.getTimeInMillis());
    	}
	}
	
	
	
	/**
	 * Sets an alarm to an absolute time, as specified in ms from epic
	 * @param wakeupTime Time at which to alarm
	 */
	public static void setAlarmAbs(Context context, long wakeupTime){
		NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
    	notificationManager.cancel(1);
    
    	
    	AlarmManager manager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
    	
    	Intent alarmIntent = new Intent(context, WaterlogReceiver.class).setAction(WaterlogReceiver.ALARM_ACTION);
    	PendingIntent contentIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, 0);
    	
    	manager.set(AlarmManager.RTC_WAKEUP, wakeupTime, contentIntent);
    	//Log.d("Waterlog", new Date(wakeupTime).toString());
	}
	
	/**
	 * Sets an alarm for a given number of milliseconds into the future 
	 * @param wakeupMS Number of milliseconds before wakeup
	 */
	public static void setAlarmRel(Context context, long wakeupMS){
		setAlarmAbs(context, System.currentTimeMillis()+wakeupMS);
	}

	private void updatePrefs() {
		SharedPreferences prefs = getSharedPreferences("Waterlog", MODE_PRIVATE);
    	SharedPreferences.Editor editor = prefs.edit();
    	editor.putInt("DRINKS_TODAY", drinksToday);
    	editor.putInt("OZ_TODAY", ozToday);
    	editor.putLong("LAST_DRINK_TIME", lastDrink);
    	editor.commit();
	}
    
    private void updateText(){
        SharedPreferences prefs = getSharedPreferences("Waterlog", MODE_PRIVATE);
    	drinksToday = prefs.getInt("DRINKS_TODAY", 0);
    	ozToday = prefs.getInt("OZ_TODAY", 0);
    	lastDrink = prefs.getLong("LAST_DRINK_TIME", 0L);

		((TextView)findViewById(R.id.drinks_today)).setText(TextUtils.concat(getText(R.string.drinks_today)," ",
				String.valueOf(drinksToday)));
		((TextView)findViewById(R.id.oz_today)).setText(TextUtils.concat(getText(R.string.ounces_today)," ",
				String.valueOf(ozToday)));
			
		((TextView)findViewById(R.id.last_drink)).setText(TextUtils.concat(getText(R.string.last_drink_at)," ",
				lastDrink == 0 ? "" :
				DateUtils.getRelativeDateTimeString(this, lastDrink, 0, DateUtils.WEEK_IN_MILLIS, 0)));
		
		DrinkType next = whatsNext(this);

		if (next == DrinkType.Home)
			home.getBackground().setColorFilter(Color.CYAN,PorterDuff.Mode.MULTIPLY);
		else 
			home.setBackgroundResource(android.R.drawable.btn_default);

		if (next == DrinkType.Coffee)
			coffee.getBackground().setColorFilter(Color.CYAN,PorterDuff.Mode.MULTIPLY);
		else 
			coffee.setBackgroundResource(android.R.drawable.btn_default);

		if (next == DrinkType.Work)
			work.getBackground().setColorFilter(Color.CYAN,PorterDuff.Mode.MULTIPLY);
		else 
			work.setBackgroundResource(android.R.drawable.btn_default);


	}
    
    
    public static DrinkType whatsNext(Context context){
        SharedPreferences prefs = context.getSharedPreferences("Waterlog", MODE_PRIVATE);
    	int drinksToday = prefs.getInt("DRINKS_TODAY", 0);
    	int ozToday = prefs.getInt("OZ_TODAY", 0);
    	long lastDrink = prefs.getLong("LAST_DRINK_TIME", 0L);

		if (!DateUtils.isToday(lastDrink)){
			drinksToday = 0;
			ozToday = 0;
		}

		if (ozToday >= 100){
			return null;
		}

    	Calendar cal = Calendar.getInstance();
    	switch (cal.get(Calendar.DAY_OF_WEEK)){
    	case Calendar.SATURDAY: 
    	case Calendar.SUNDAY:
	    	if (drinksToday < 2)
	    		return DrinkType.Coffee;
	    	return DrinkType.Home;
    	default:
    		return DrinkType.Work;
    	}
    }

}