package me.lachlanpage.b525helper;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.util.Log;
import android.widget.Toast;

import java.util.Map;

public class SettingsFragment extends PreferenceFragmentCompat{

    private SharedPreferences sharedPreferences;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);


        // Listener for feedback click
        final Preference feedbackPref = (Preference) findPreference("feedback");
        feedbackPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                /* Create the Intent */
                final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);

                /* Fill it with Data */
                emailIntent.setType("plain/text");
                emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{"lachlanpage97@gmail.com"});
                emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Feedback");
                emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, "");

                /* Send it off to the Activity-Chooser */
                getActivity().startActivity(Intent.createChooser(emailIntent, "Send mail..."));
                return false;
            }
        });
    }



}
