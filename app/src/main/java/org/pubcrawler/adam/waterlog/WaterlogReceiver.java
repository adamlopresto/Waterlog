package org.pubcrawler.adam.waterlog;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.RemoteInput;
import android.text.format.DateUtils;
import android.util.Log;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

public class WaterlogReceiver extends BroadcastReceiver {

    public static final String ALARM_ACTION = "ALARM";
	public static final String DRINK_ACTION = "DRINK";
    public static final String DRINK_OTHER_ACTION = "DRINK_OTHER";
    public static final String DRINK_DEFAULT_ACTION = "DRINK_DEFAULT";
	public static final String SNOOZE_ACTION = "SNOOZE";
    private static final String EXTRA_VOICE_REPLY = "extra_voice_reply";


    @Override
	public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        dumpIntent(intent);

		if (ALARM_ACTION.equals(intent.getAction())){
            boolean background = intent.getBooleanExtra("background", false);
            if (!background)
                Waterlog.setAlarmRel(context, SettingsActivity.getIntPref(prefs, SettingsActivity.KEY_REPEAT_INTERVAL)*DateUtils.MINUTE_IN_MILLIS);

            NotificationManagerCompat mNotificationManager = NotificationManagerCompat.from(context);

            int icon = R.drawable.icon;
            CharSequence tickerText = "Get a drink!";
	    	//long when = System.currentTimeMillis();
	
	    	NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
	    	//Notification notification = new Notification(icon, tickerText, when);
	    	builder.setSmallIcon(icon).setTicker(tickerText);

	    	int drinksToday = prefs.getInt("DRINKS_TODAY", 0);
	    	int ozToday = prefs.getInt("OZ_TODAY", 0);
	    	long lastDrink = prefs.getLong("LAST_DRINK_TIME", 0L);
            int goal = SettingsActivity.getIntPref(prefs, SettingsActivity.KEY_GOAL);
	    	builder.setWhen(lastDrink).setUsesChronometer(true);
	    	
	    	CharSequence contentTitle = drinksToday+" drinks, "+ozToday+" ounces";
            CharSequence contentText=null;
            if (ozToday == 0){
                contentText = "Time to get started for the day";
            } else if (ozToday >= goal){
                contentText = "Good job, you're done for the day.";
            } else if (ozToday >= 0.75*goal){
                contentText = "Almost there!";
            } else if (ozToday >= .5*goal){
                contentText = "Halfway there. Keep going!";
            } else {
                contentText = "Good start! Don't stop now.";
            }

	    	Intent notificationIntent = new Intent(context, Waterlog.class);
	    	PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
	
	    	//notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
	    	builder.setContentIntent(contentIntent);
	    	builder.setContentTitle(contentTitle);
	    	builder.setContentText(contentText);
            builder.setCategory(NotificationCompat.CATEGORY_EVENT);
	    	//notification.defaults |= Notification.DEFAULT_SOUND;


            if (background){
                builder.setPriority(NotificationCompat.PRIORITY_MIN);
            } else {
                builder.setLights(0xFF00FFFF, 1000, 1000);
                if (prefs.getBoolean(SettingsActivity.KEY_FANCY_VIBRATION, false)) {
                    long dit = 70L;
                    long da = dit * 3;
                    //noinspection UnnecessaryLocalVariable
                    long space = dit;
                    //noinspection UnnecessaryLocalVariable
                    long letter = da;
                    long[] vibrate = {0,
                            da, space, dit, space, dit, //D
                            letter,
                            dit, space, da, space, dit,    //R
                            letter,
                            dit, space, dit,            //I
                            letter,
                            da, space, dit,                //N
                            letter,
                            da, space, dit, space, da    //K
                    };
                    builder.setVibrate(vibrate);
                } else {
                    builder.setVibrate(new long[]{0, 300});
                }

                builder.setSound(Uri.parse(prefs.getString(SettingsActivity.KEY_RINGTONE, "")));
            }

	    	DrinkType next = Waterlog.whatsNext(context);
            NotificationCompat.Action drinkAction = null;
	    	if (next != null){
		    	Intent drinkIntent = new Intent(context, WaterlogReceiver.class).setAction(DRINK_ACTION);
		    	drinkIntent.putExtra("oz", next.oz());
		    	drinkIntent.putExtra("msg", next.name());
		    	PendingIntent drinkPI = PendingIntent.getBroadcast(context, 0, drinkIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                drinkAction = new NotificationCompat.Action(R.drawable.ic_action_accept, next.name(), drinkPI);
                builder.addAction(drinkAction);
	    	}


	    	Intent snoozeIntent = new Intent(context, WaterlogReceiver.class).setAction(SNOOZE_ACTION);
	    	PendingIntent snoozePI = PendingIntent.getBroadcast(context, 0, snoozeIntent, 0);

            NotificationCompat.Action snoozeAction =
                    new NotificationCompat.Action(R.drawable.ic_action_alarms, "Snooze", snoozePI);
	    	builder.addAction(snoozeAction);

            try {
                AssetManager assets = context.getAssets();
                Bitmap bitmap = BitmapFactory.decodeStream(assets.open("Drinking_fountain.jpg"));
                NotificationCompat.WearableExtender extender = new NotificationCompat.WearableExtender();
                extender.setBackground(bitmap);

                String[] choices = new String[]{
                        "Coffee", "Home", "Work", "2", "4", "6", "8", "10", "12", "14", "16", "18", "20"
                };
                RemoteInput remoteInput = new RemoteInput.Builder(EXTRA_VOICE_REPLY)
                        .setChoices(choices)
                        .build();

                Intent drinkIntent = new Intent(context, WaterlogReceiver.class).setAction(DRINK_OTHER_ACTION);
                PendingIntent drinkPI = PendingIntent.getBroadcast(context, 0, drinkIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                NotificationCompat.Action otherAction =
                        new NotificationCompat.Action.Builder(R.drawable.ic_action_accept, "Other", drinkPI)
                                .addRemoteInput(remoteInput)
                                .build();
                if (drinkAction != null) {
                    extender.addAction(drinkAction);
                    //extender.setContentAction(0);
                }
                extender.addAction(otherAction);
                extender.addAction(snoozeAction);
                builder.extend(extender);
                Log.e(Waterlog.TAG, "Successfully set background (allegedly)");
            } catch (IOException ignored) {
                Log.e(Waterlog.TAG, "Error setting background bitmap, " + ignored);
            }

	    	/*private static*/ final int HELLO_ID = 1;
	
	//    	mNotificationManager.notify(HELLO_ID, notification);
	    	mNotificationManager.notify(HELLO_ID, builder.build());

		} else if (SNOOZE_ACTION.equals(intent.getAction())) {
            Waterlog.setAlarmRel(context, SettingsActivity.getIntPref(prefs,
                    SettingsActivity.KEY_DRINK_INTERVAL)*DateUtils.MINUTE_IN_MILLIS);
		} else if (DRINK_ACTION.equals(intent.getAction())) {
            Waterlog.drink(context, intent.getIntExtra("oz", 12), intent.getStringExtra("msg"));
        } else if (DRINK_DEFAULT_ACTION.equals(intent.getAction())){
		    DrinkType next = Waterlog.whatsNext(context);
		    if (next != null){
                Waterlog.drink(context, next.oz(), next.name());
            }
        } else if (DRINK_OTHER_ACTION.equals(intent.getAction())){
            Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
            if (remoteInput != null){
                String drink = remoteInput.getCharSequence(EXTRA_VOICE_REPLY).toString();
                try {
                    drink = Character.toUpperCase(drink.charAt(0))+drink.substring(1);
                    DrinkType drinkType = DrinkType.valueOf(drink);
                    Waterlog.drink(context, drinkType.oz(), drink);
                } catch (IllegalArgumentException|IndexOutOfBoundsException e){
                    try {
                        int oz = Integer.parseInt(drink);
                        Waterlog.drink(context, oz, drink);
                    } catch (NumberFormatException nfe){
                        Log.e("Waterlog", "Don't know what to do with other drink of "+drink);
                    }
                }
            }

        } else if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.e(Waterlog.TAG, "System rebooted");
            long lastDrink = prefs.getLong(SettingsActivity.LAST_DRINK_TIME, 0L);
            int ozToday = prefs.getInt(SettingsActivity.OZ_TODAY, 0);
            int goal = SettingsActivity.getIntPref(prefs, SettingsActivity.KEY_GOAL);
            if (ozToday >= goal) {
                Log.e(Waterlog.TAG, "Following reboot, we've already finished. Setting alarm for the morning.");
                //We've had enough, so set the alarm for the first thing in the morning.
                //But we need isToday to tell us whether it's this morning or tomorrow morning.
                boolean isToday = DateUtils.isToday(lastDrink);
                Waterlog.setAlarmMorning(context, prefs, !isToday);
            } else {
                Log.e(Waterlog.TAG, "Rebooted, still drinking, need to alarm at the next appropriate time.");
                Waterlog.setAlarmAbs(context, lastDrink +
                        SettingsActivity.getIntPref(prefs, SettingsActivity.KEY_DRINK_INTERVAL)
                                * DateUtils.MINUTE_IN_MILLIS);
            }

        }
    }

    private static void dumpIntent(@Nullable Intent i){
        if (i != null){
            Log.e(Waterlog.TAG, "Got intent "+i);
            Bundle bundle = i.getExtras();
            if (bundle != null) {
                Set<String> keys = bundle.keySet();
                Iterator<String> it = keys.iterator();
                Log.e(Waterlog.TAG,"Dumping Intent start");
                while (it.hasNext()) {
                    String key = it.next();
                    Log.e(Waterlog.TAG,"[" + key + "=" + bundle.get(key)+"]");
                }
                Log.e(Waterlog.TAG,"Dumping Intent end");
            }
        } else {
            Log.e(Waterlog.TAG, "Got a null intent!");
        }

    }
}