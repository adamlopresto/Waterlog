package org.pubcrawler.adam.waterlog;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.MediaStore.Audio;
import android.text.format.DateUtils;

public class WaterlogReceiver extends BroadcastReceiver {

	final private long dit = 70L;
	final private long da = dit * 3;
	final private long space = dit;
	final private long letter = da;
	

	@Override
	public void onReceive(Context context, Intent intent) {
		Waterlog.setAlarmRel(context, 15*DateUtils.MINUTE_IN_MILLIS);
		
		String ns = Context.NOTIFICATION_SERVICE;
    	NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(ns);
    	
    	int icon = R.drawable.icon;
    	CharSequence tickerText = "Get a drink!";
    	//long when = System.currentTimeMillis();

    	Notification.Builder builder = new Notification.Builder(context);
    	//Notification notification = new Notification(icon, tickerText, when);
    	builder.setSmallIcon(icon).setTicker(tickerText);

    	
    	SharedPreferences prefs = context.getSharedPreferences("Waterlog", Context.MODE_PRIVATE);
    	int drinksToday = prefs.getInt("DRINKS_TODAY", 0);
    	int ozToday = prefs.getInt("OZ_TODAY", 0);
    	long lastDrink = prefs.getLong("LAST_DRINK_TIME", 0L);
    	builder.setWhen(lastDrink).setUsesChronometer(true);
    	
    	CharSequence contentTitle = "You could use a drink";
    	CharSequence contentText = drinksToday+" drinks today, "+ozToday+" ounces";
    	Intent notificationIntent = new Intent(context, Waterlog.class);
    	PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);

    	//notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
    	builder.setContentIntent(contentIntent);
    	builder.setContentTitle(contentTitle);
    	builder.setContentText(contentText);
    	//notification.defaults |= Notification.DEFAULT_SOUND;
    	long[] vibrate = {0, 
    			da, space, dit, space, dit, //D 
    			letter, 					
    			dit, space, da, space, dit,	//R
    			letter,
    			dit, space, dit,			//I
    			letter,
    			da, space, dit,				//N
    			letter,
    			da, space, dit, space, da	//K
    			};
    	builder.setVibrate(vibrate);
    	/*
    	 notification.vibrate = vibrate;
    	notification.ledARGB=0xFFFF0000;
    	notification.ledOnMS=1000;
    	notification.ledOffMS=250;
    	*/
    	//notification.flags |= Notification.FLAG_SHOW_LIGHTS;
    	//notification.sound = Uri.withAppendedPath(Audio.Media.INTERNAL_CONTENT_URI, "14");    	
    	builder.setSound(Uri.withAppendedPath(Audio.Media.INTERNAL_CONTENT_URI, "14"));    	
    	
    	/*private static*/ final int HELLO_ID = 1;

//    	mNotificationManager.notify(HELLO_ID, notification);
    	mNotificationManager.notify(HELLO_ID, builder.build());
	}
	
}