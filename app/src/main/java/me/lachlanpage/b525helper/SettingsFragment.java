package me.lachlanpage.b525helper;

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


        // LISTENERS FOR THE 3 SETTINGS
        final EditTextPreference modemIPPref = (EditTextPreference)findPreference("modemIP");
        modemIPPref.setSummary(modemIPPref.getText());
        modemIPPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final EditTextPreference pref = (EditTextPreference)findPreference("username");

                pref.setSummary(pref.getText());
                return true;
            }
        });
    }



}
