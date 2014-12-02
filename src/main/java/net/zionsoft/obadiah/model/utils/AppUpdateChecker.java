package net.zionsoft.obadiah.model.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.text.format.DateUtils;

import com.crashlytics.android.Crashlytics;

import net.zionsoft.obadiah.Constants;
import net.zionsoft.obadiah.model.network.NetworkHelper;

import org.json.JSONObject;

import java.util.concurrent.RejectedExecutionException;

public class AppUpdateChecker {
    public static interface OnAppUpdateCheckListener {
        public void onAppUpdateChecked(boolean needsUpdate);
    }

    public static void checkAppUpdate(final Context context, final OnAppUpdateCheckListener onChecked) {
        final AsyncTask<Void, Void, Boolean> asyncTask = new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    // we only check if at least 24 hours is passed
                    final SharedPreferences preferences
                            = context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
                    final long now = System.currentTimeMillis();
                    final long lastCheckedTimestamp = preferences.getLong(
                            Constants.PREF_KEY_CHECKED_APPLICATION_VERSION_TIMESTAMP, 0);
                    if (now - lastCheckedTimestamp < DateUtils.DAY_IN_MILLIS)
                        return false;

                    // we only check if the user has active WiFi or WiMAX
                    final NetworkInfo networkInfo = ((ConnectivityManager) context
                            .getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
                    if (networkInfo == null || !networkInfo.isConnected())
                        return false;
                    final int networkType = networkInfo.getType();
                    if (networkType != ConnectivityManager.TYPE_WIFI
                            && networkType != ConnectivityManager.TYPE_WIMAX) {
                        return false;
                    }

                    final String response = new String(NetworkHelper.get(NetworkHelper.CLIENT_VERSION_URL), "UTF-8");
                    final JSONObject versionObject = new JSONObject(response);
                    final int latestVersion = versionObject.getInt("versionCode");
                    preferences.edit().putLong(Constants.PREF_KEY_CHECKED_APPLICATION_VERSION_TIMESTAMP, now).apply();

                    // for each new version, we only ask once
                    if (latestVersion == preferences.getInt(Constants.PREF_KEY_CHECKED_APPLICATION_VERSION, 0))
                        return false;
                    preferences.edit().putInt(Constants.PREF_KEY_CHECKED_APPLICATION_VERSION, latestVersion).apply();

                    return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode < latestVersion;
                } catch (Exception e) {
                    Crashlytics.logException(e);
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean newVersionAvailable) {
                onChecked.onAppUpdateChecked(newVersionAvailable);
            }
        };
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            try {
                asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            } catch (RejectedExecutionException e) {
                Crashlytics.logException(e);
            }
        } else {
            asyncTask.execute();
        }
    }
}
