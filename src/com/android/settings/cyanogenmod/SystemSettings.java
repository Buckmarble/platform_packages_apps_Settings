/*
 * Copyright (C) 2012 CyanogenMod
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.cyanogenmod;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.content.Context;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.view.IWindowManager;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings;
import android.util.Log;
import android.view.IWindowManager;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SystemSettings extends SettingsPreferenceFragment implements
    Preference.OnPreferenceChangeListener{

    private static final String TAG = "SystemSettings";

    private static final String KEY_LOCK_CLOCK = "lock_clock";
    private static final String KEY_PIE_CONTROL = "pie_control";
    private static final String PREF_FORCE_DUAL_PANEL = "force_dualpanel";
    private static final String PREF_LOW_BATTERY_WARNING_POLICY = "pref_low_battery_warning_policy";
    private static final String PREF_NOTIFICATION_SHOW_WIFI_SSID = "notification_show_wifi_ssid";

    CheckBoxPreference mDualpane;
    private PreferenceScreen mPieControl;
    CheckBoxPreference mShowWifiName;
    private ListPreference mLowBatteryWarning;

    private boolean torchSupported() {
        return getResources().getBoolean(R.bool.has_led_flash);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.system_settings);

        // Dont display the lock clock preference if its not installed

        // Pie controls
        mPieControl = (PreferenceScreen) findPreference(KEY_PIE_CONTROL);

        mShowWifiName = (CheckBoxPreference) findPreference(PREF_NOTIFICATION_SHOW_WIFI_SSID);
            mShowWifiName.setOnPreferenceChangeListener(this);

        mLowBatteryWarning = (ListPreference) findPreference(PREF_LOW_BATTERY_WARNING_POLICY);
        int lowBatteryWarning = Settings.System.getInt(getActivity().getContentResolver(),
                                    Settings.System.POWER_UI_LOW_BATTERY_WARNING_POLICY, 3);
        mLowBatteryWarning.setValue(String.valueOf(lowBatteryWarning));
        mLowBatteryWarning.setSummary(mLowBatteryWarning.getEntry());
        mLowBatteryWarning.setOnPreferenceChangeListener(this);

        removePreferenceIfPackageNotInstalled(findPreference(KEY_LOCK_CLOCK));

        mDualpane = (CheckBoxPreference) findPreference(PREF_FORCE_DUAL_PANEL);
            mDualpane.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mPieControl != null) {
            updatePieControlDescription();
        }

    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        ContentResolver cr = getActivity().getContentResolver();

        if (preference == mDualpane) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.FORCE_DUAL_PANEL,
                    ((CheckBoxPreference)preference).isChecked() ? 0 : 1);
            return true;
        } else if (preference == mLowBatteryWarning) {
            int lowBatteryWarning = Integer.valueOf((String) objValue);
            int index = mLowBatteryWarning.findIndexOfValue((String) objValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.POWER_UI_LOW_BATTERY_WARNING_POLICY, lowBatteryWarning);
            mLowBatteryWarning.setSummary(mLowBatteryWarning.getEntries()[index]);
            return true;
         } else if (preference == mShowWifiName) {
            Settings.System.putInt(getActivity().getContentResolver(),
		    Settings.System.NOTIFICATION_SHOW_WIFI_SSID,
                    ((CheckBoxPreference)preference).isChecked() ? 0 : 1);
	    return true;
        }
        return false;
    }

    private void updatePieControlDescription() {
        if (Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.PIE_CONTROLS, 0) == 1) {
            mPieControl.setSummary(getString(R.string.pie_control_enabled));
        } else {
            mPieControl.setSummary(getString(R.string.pie_control_disabled));
        }
    }

    private boolean removePreferenceIfPackageNotInstalled(Preference preference) {
        String intentUri = ((PreferenceScreen) preference).getIntent().toUri(1);
        Pattern pattern = Pattern.compile("component=([^/]+)/");
        Matcher matcher = pattern.matcher(intentUri);

        String packageName = matcher.find() ? matcher.group(1) : null;
        if (packageName != null) {
            try {
                getPackageManager().getPackageInfo(packageName, 0);
            } catch (NameNotFoundException e) {
                Log.e(TAG, "package " + packageName + " not installed, hiding preference.");	
                getPreferenceScreen().removePreference(preference);
                return true;
            }
        }
        return false;
    }

}
