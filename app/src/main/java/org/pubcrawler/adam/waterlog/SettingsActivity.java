package org.pubcrawler.adam.waterlog;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceFragment;

public class SettingsActivity extends Activity {

    public static final String KEY_BUTTON1_LABEL = "button1_label";
    public static final String KEY_BUTTON2_LABEL = "button2_label";
    public static final String KEY_BUTTON3_LABEL = "button3_label";
    public static final String KEY_BUTTON1_OZ = "button1_oz";
    public static final String KEY_BUTTON2_OZ = "button2_oz";
    public static final String KEY_BUTTON3_OZ = "button3_oz";
    public static final String KEY_GOAL = "goal";
    public static final String KEY_DRINK_INTERVAL = "drink_interval";
    public static final String KEY_REPEAT_INTERVAL = "repeat_interval";
    public static final String KEY_START_TIME = "start_time";
    public static final String KEY_FANCY_VIBRATION = "fancy_vibration";
    public static final String KEY_RINGTONE = "ringtone";
    public static final String KEY_GUESS_NEXT = "guess_next";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);
        }
    }
}
