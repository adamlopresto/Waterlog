package org.pubcrawler.adam.waterlog;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.TextView;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Calendar;

public class Waterlog extends Activity implements OnClickListener, OnLongClickListener{
    private int drinksToday;
    private int ozToday;
    private long lastDrink;
	private Runnable updater;
	private Handler handler;

    private TextView home;
	private TextView coffee;
	private TextView work;

    private int hilightedColor = 0xFF00bcd4;

    public static void drink(@NotNull Context context, int oz, String msg) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int drinksToday = prefs.getInt(SettingsActivity.DRINKS_TODAY, 0);
        int ozToday = prefs.getInt(SettingsActivity.OZ_TODAY, 0);
        long lastDrink = prefs.getLong(SettingsActivity.LAST_DRINK_TIME, 0L);

        if (!DateUtils.isToday(lastDrink)) {
            drinksToday = 0;
            ozToday = 0;
        }

        ozToday += oz;
        drinksToday++;
        lastDrink = System.currentTimeMillis();

        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(SettingsActivity.DRINKS_TODAY, drinksToday);
        editor.putInt(SettingsActivity.OZ_TODAY, ozToday);
        editor.putLong(SettingsActivity.LAST_DRINK_TIME, lastDrink);
        editor.apply();

        if (ozToday < SettingsActivity.getIntPref(prefs, SettingsActivity.KEY_GOAL)) {
            Toast.makeText(context, TextUtils.concat(context.getText(R.string.drink_recorded), msg), Toast.LENGTH_LONG).show();
            setAlarmRel(context, DateUtils.MINUTE_IN_MILLIS *
                    SettingsActivity.getIntPref(prefs, SettingsActivity.KEY_DRINK_INTERVAL));
        } else {
            Toast.makeText(context, R.string.end_of_day, Toast.LENGTH_LONG).show();

            TaskerIntent i = new TaskerIntent("Finished drinking");
            context.sendBroadcast(i);

            setAlarmMorning(context, prefs, false);
        }
    }

    /**
     * Sets an alarm for the first drink of the morning
     * @param context context for setting alarm
     * @param prefs SharedPreferences to determine the time
     * @param today True for this morning, false for tomorrow
     */
    public static void setAlarmMorning(@NotNull Context context, @NotNull SharedPreferences prefs, boolean today) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, SettingsActivity.getIntPref(prefs, SettingsActivity.KEY_START_HOUR));
        cal.set(Calendar.MINUTE, SettingsActivity.getIntPref(prefs, SettingsActivity.KEY_START_MINUTE));
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        if (!today) {
            cal.add(Calendar.DATE, 1);
        }
        setAlarmAbs(context, cal.getTimeInMillis());
    }

    /**
     * Sets an alarm to an absolute time, as specified in ms from epic
     *
     * @param wakeUpTime Time at which to alarm
     */
    public static void setAlarmAbs(Context context, long wakeUpTime) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(1);


        AlarmManager manager = (AlarmManager) context.getSystemService(ALARM_SERVICE);

        Intent alarmIntent = new Intent(context, WaterlogReceiver.class).setAction(WaterlogReceiver.ALARM_ACTION);
        PendingIntent contentIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, 0);

        manager.set(AlarmManager.RTC_WAKEUP, wakeUpTime, contentIntent);
        //Log.d("Waterlog", new Date(wakeUpTime).toString());
    }

    /**
     * Sets an alarm for a given number of milliseconds into the future
     *
     * @param wakeUpMS Number of milliseconds before wake up
     */
    public static void setAlarmRel(Context context, long wakeUpMS) {
        setAlarmAbs(context, System.currentTimeMillis() + wakeUpMS);
    }

    @Nullable
    public static DrinkType whatsNext(@NotNull Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        if (!prefs.getBoolean(SettingsActivity.KEY_GUESS_NEXT, false))
            return null;

        int drinksToday = prefs.getInt(SettingsActivity.DRINKS_TODAY, 0);
        int ozToday = prefs.getInt(SettingsActivity.OZ_TODAY, 0);
        long lastDrink = prefs.getLong(SettingsActivity.LAST_DRINK_TIME, 0L);

        if (!DateUtils.isToday(lastDrink)) {
            drinksToday = 0;
            ozToday = 0;
        }

        if (ozToday >= SettingsActivity.getIntPref(prefs, SettingsActivity.KEY_GOAL)) {
            return null;
        }

        Calendar cal = Calendar.getInstance();
        switch (cal.get(Calendar.DAY_OF_WEEK)) {
            case Calendar.SATURDAY:
            case Calendar.SUNDAY:
                if (drinksToday < 2)
                    return DrinkType.Coffee;
                return DrinkType.Home;
            default:
                return DrinkType.Work;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        home = (TextView) findViewById(R.id.home);
        coffee = (TextView) findViewById(R.id.coffee);
        work = (TextView) findViewById(R.id.work);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        home.setText(prefs.getString(SettingsActivity.KEY_BUTTON1_LABEL, ""));
        coffee.setText(prefs.getString(SettingsActivity.KEY_BUTTON2_LABEL, ""));
        work.setText(prefs.getString(SettingsActivity.KEY_BUTTON3_LABEL, ""));

        home.setOnClickListener(this);
        coffee.setOnClickListener(this);
        work.setOnClickListener(this);
        findViewById(R.id.snooze).setOnClickListener(this);
        findViewById(R.id.drink).setOnClickListener(this);

        coffee.setOnLongClickListener(this);
        home.setOnLongClickListener(this);
        work.setOnLongClickListener(this);
        findViewById(R.id.snooze).setOnLongClickListener(this);
        findViewById(R.id.drink).setOnLongClickListener(this);

        updateText();

        updater = new Runnable() {
            public void run() {
                updateText();
                long delay;
                long now = System.currentTimeMillis();
                if (lastDrink == 0) {
                    return;
                } else if (lastDrink >= now - 60 * 1000) { //Within one minute, update every second
                    delay = 1000 - (now - lastDrink) % 1000;

                } else {
                    delay = 60 * 1000 - (now - lastDrink) % (60 * 1000);
                }
                //Log.d("Waterlog", delay+","+lastDrink+","+now);
                handler.postDelayed(this, delay);
            }
        };
        handler = new Handler();

        updateText();

    }

    @Override
    public void onResume() {
        super.onResume();
        handler.postDelayed(updater, 200);
    }

    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacks(updater);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
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

    public void onClick(View v) {

        onLongClick(v);

        finish();
    }

    @Override
    public boolean onLongClick(View v) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        switch (v.getId()) {
            case R.id.home:
                drink(this, SettingsActivity.getIntPref(prefs, SettingsActivity.KEY_BUTTON1_OZ),
                        prefs.getString(SettingsActivity.KEY_BUTTON1_LABEL, ""));
                break;
            case R.id.coffee:
                drink(this, SettingsActivity.getIntPref(prefs, SettingsActivity.KEY_BUTTON2_OZ),
                        prefs.getString(SettingsActivity.KEY_BUTTON2_LABEL, ""));
                break;
            case R.id.work:
                drink(this, SettingsActivity.getIntPref(prefs, SettingsActivity.KEY_BUTTON3_OZ),
                        prefs.getString(SettingsActivity.KEY_BUTTON3_LABEL, ""));
                break;
            case R.id.snooze:
                setAlarmRel(this, DateUtils.HOUR_IN_MILLIS);
                break;
            case R.id.drink:

                try {
                    @SuppressWarnings("ConstantConditions") int oz = Integer.parseInt(((TextView) findViewById(R.id.oz)).getText().toString());
                    drink(this, oz, oz + " ounces");
                } catch (NumberFormatException e) {
                    findViewById(R.id.oz).requestFocus();
                    return true;
                }
        }

        updateText();
        return true;
    }

	private void updatePrefs() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    	SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(SettingsActivity.DRINKS_TODAY, drinksToday);
        editor.putInt(SettingsActivity.OZ_TODAY, ozToday);
        editor.putLong(SettingsActivity.LAST_DRINK_TIME, lastDrink);
        editor.apply();
    }
    
    private void updateText(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        drinksToday = prefs.getInt(SettingsActivity.DRINKS_TODAY, 0);
        ozToday = prefs.getInt(SettingsActivity.OZ_TODAY, 0);
        lastDrink = prefs.getLong(SettingsActivity.LAST_DRINK_TIME, 0L);

		((TextView)findViewById(R.id.drinks_today)).setText(TextUtils.concat(getText(R.string.drinks_today)," ",
				String.valueOf(drinksToday)));
		((TextView)findViewById(R.id.oz_today)).setText(TextUtils.concat(getText(R.string.ounces_today)," ",
				String.valueOf(ozToday)));
			
		((TextView)findViewById(R.id.last_drink)).setText(TextUtils.concat(getText(R.string.last_drink_at)," ",
				lastDrink == 0 ? "" :
				DateUtils.getRelativeDateTimeString(this, lastDrink, 0, DateUtils.WEEK_IN_MILLIS, 0)));
		
		DrinkType next = whatsNext(this);

        if (next == DrinkType.Home)
            highlight(home);
        else
            resetBackground(home);

        if (next == DrinkType.Coffee)
            highlight(coffee);
        else
            resetBackground(coffee);

        if (next == DrinkType.Work)
            highlight(work);
        else
            resetBackground(work);
    }

    private void resetBackground(View v) {
        if (v != null)
            v.setBackgroundResource(android.R.drawable.btn_default);
    }

    private void highlight(View v) {
        if (v != null) {
            Drawable d = v.getBackground();
            if (d != null)
                d.setColorFilter(hilightedColor, PorterDuff.Mode.MULTIPLY);
        }
    }

    public enum DrinkType {
        Home(12), Work(16), Coffee(8);

        private final int oz;

        DrinkType(int oz) {
            this.oz = oz;
        }

        public int oz() {
            return oz;
        }
    }

}