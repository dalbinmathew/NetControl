package net.stargw.karma;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


import android.app.Application;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import androidx.core.app.NotificationCompat;

public class Global extends Application {

	
	private static Context mContext;

	// This is the main one and only app list
	// This must be populated before NetControl can start
	// This must be populated before the GUI can be shown
	static Map<Integer, AppInfo> appListFW = new ConcurrentHashMap<Integer, AppInfo>();

	static int appListState = 0;
	static final int APPLIST_DOING = 1;
	static final int APPLIST_DONE = 2;

	// This can only be reset by the GUI
	static boolean rebuildGUIappList = true;

	// to update the GUI
	static int packageMax = 0;
	static int packageCurrent = 0;

	static NotificationChannel newAppNotificationChannel;
	static NotificationManager notificationManager;

	// Settings variables
	static Boolean settingsEnableExpert;
	private static Boolean settingsNetControlStateOn = false;
	static Boolean settingsEnableNotifications = true;
	static Boolean settingsEnableBoot = false;
	static Boolean settingsEnableRestart = false;
	static String settingsSubnet = "";
	static int settingsLoggingLevel = 0;
	static int settingsSortOption = 0;
	static Boolean settingsEnableLogcat = false;




	static final String REBUILD_APPS_DONE = "net.stargw.karma.REBUILD_GUI_APP_LIST";
	static final String REBUILD_APPS_IN_PROGRESS = "net.stargw.karma.APPS_IN_PROGRESS";

	static final String NETCONTROL_STATE_CHANGE = "net.stargw.karma.NETCONTROL_STATE_CHANGE";

	static final String TOGGLE = "net.stargw.karma.TOGGLE";
	static final String TOGGLEAPP = "net.stargw.karma.TOGGLEAPP";
	static final String TOGGLEAPP_REFRESH = "net.stargw.karma.TOGGLEAPP_REFRESH";

	static final String NETCONTROL_START = "fw_start";
	static final String NETCONTROL_RESTART = "fw_restart";
	static final String NETCONTROL_BOOT = "fw_boot";
	static final String NETCONTROL_REPLACE = "fw_replace";
	static final String NETCONTROL_QS = "fw_start_qs";
	static final String NETCONTROL_WIDGET = "fw_widget";
	static final String NETCONTROL_STOP = "fw_stop";
	static final String NETCONTROL_STATUS = "fw_status";

	static final String WIDGET_ACTION = "widget_action";




	@Override
	public void onCreate() {
		super.onCreate();
		mContext = this;

		notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		Global.createNotificationChannel("FW1", "Non-interactive Start/Stop Alert");
		Global.createNotificationChannel("FW2", "New App Alert");
		Global.createNotificationChannel("FW3", "FW Abort Alert");

		Global.getSettings();

		Logs.getLoggingLevel(); // gets level + housekeeping

		Logs.myLog("Global - onCreate()", 2);

		Log.w("Karma", "Global - onCreate(): " + Global.settingsLoggingLevel);

		Logs.myLog("Global -> onCreate() -> Global.getAppListBackground()", 2);
		Global.getAppListBackground();
	}

	@Override
	public void onTerminate() {
		Logs.myLog("Global - onTerminate()", 2);

		super.onTerminate();
	}

	@Override
	public void onLowMemory() {
		Logs.myLog("Global - onLowMemory()", 2);

		super.onLowMemory();
	}

	public static Context getContext(){
		return mContext;
	}

	public static void infoMessageLeft(final Context context, String header, String message)
	{
		infoMessageDo(context, header, message, Gravity.LEFT);
	}

	public static void infoMessage(final Context context, String header, String message)
	{
		infoMessageDo(context, header, message, Gravity.CENTER_HORIZONTAL);

	}


	public static Boolean getNetControlState()
	{
		// SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(Global.getContext());
		// settingsNetControlStateOn = p.getBoolean("settingsNetControlStateOn", false);

		return settingsNetControlStateOn;
	}

	public static void setNetControlState(Boolean state)
	{
		Intent broadcastIntent = new Intent();

		broadcastIntent.setAction(Global.NETCONTROL_STATE_CHANGE);

		settingsNetControlStateOn = state;
		mContext.sendBroadcast(broadcastIntent);

		Logs.myLog("NetControl Service sent NetControl state broadcast!", 2);

	}

	//
	// Display a popup info screen
	//
	public static void infoMessageDo(final Context context, String header, String message, int i)
	{
		final Dialog info = new Dialog(context);

		info.setContentView(R.layout.dialog_info);
		info.setTitle(header);
		
		TextView text = (TextView) info.findViewById(R.id.infoMessage);
		text.setText(message);
		text.setGravity(i);
		
		Button dialogButton = (Button) info.findViewById(R.id.infoButton);

		
		dialogButton.setOnClickListener(new OnClickListener() {
			// @Override
			public void onClick(View v) {
				// notificationCancel(context);
				info.cancel();
			}
		});
		

		info.show();
		Logs.myLog(header + ":" + message,3);
	}



	public static void getSettings()
	{
		SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(Global.getContext());

		settingsEnableNotifications = p.getBoolean("settingsEnableNotifications", true);
		settingsEnableBoot = p.getBoolean("settingsEnableBoot", false);
		// settingsDetailedLogs = p.getBoolean("settingsDetailedLogs", false);
		settingsEnableRestart = p.getBoolean("settingsEnableRestart", false);
		settingsLoggingLevel = p.getInt("settingsLoggingLevel", 1);
		settingsSortOption = p.getInt("settingsSortOption", 0);
		settingsSubnet = p.getString("settingsSubnet", "");
		settingsEnableLogcat = p.getBoolean("settingsEnableLogcat", false);

	}

	public static void saveSetings()
	{
		SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(Global.getContext());

		p.edit().putBoolean("settingsEnableNotifications", settingsEnableNotifications).apply();
		p.edit().putInt("settingsLoggingLevel", settingsLoggingLevel).commit();
		p.edit().putBoolean("settingsEnableBoot", settingsEnableBoot).apply();
		p.edit().putBoolean("settingsEnableRestart", settingsEnableRestart).apply();
		p.edit().putInt("settingsSortOption", settingsSortOption).apply();
		p.edit().putString("settingsSubnet", settingsSubnet).apply();
	}





	public static void getAppListBackground()
	{
/*
		// If we are already building an app list - don't!
		if (Global.appListState == APPLIST_DOING)
		{
			Logs.myLog("Applist Build in progress...skipping", 2);
			return;
		}
*/
		// Build apps
		Thread thread = new Thread() {
			@Override
			public void run() {
				Global.getAppList();
			}
		};

		thread.start();
	}

	public static boolean getAppList()
	{
		Logs.myLog("getAppList()...START", 2);

		// If we are already building an app list - don't!
		if (Global.appListState == APPLIST_DOING)
		{
			Logs.myLog("Applist Build in progress...skipping", 2);
			return false;
		}

		Global.appListState = APPLIST_DOING;

		// better than iterator?
		// for(int i = 0, l = appListFW.size(); i < l; i++)

		Iterator<Integer> it = Global.appListFW.keySet().iterator();

		while (it.hasNext())
		{
			int key = it.next();
			AppInfo app = Global.appListFW.get(key);
			app.flush = true;
			app.enabled = false; // reset
			// app.expandView = false;
			// app.appInfoExtra = null; // these may have changed...
		}

		PackageInfo packageInfo;
		List<PackageInfo> packageInfoList = Global.getContext().getPackageManager().getInstalledPackages(0);

		/*
		// Package name for UID 0 returns null - we cannot block root
		String root = Global.getContext().getPackageManager().getNameForUid(0);
		if (root != null)
		{
			Logs.myLog("Root = " + root, 3);
		}
		*/

		Global.packageMax = packageInfoList.size();
		Global.packageCurrent = 0;

		for (int i = 0; i < packageInfoList.size(); i++)
		{
			Global.packageCurrent = i;
			try {
				packageInfo = packageInfoList.get(i);
				// Global.appList.add(Global.getAppListApp(packageInfo));
			} catch (Exception e) {
				Logs.myLog("Cannot get package info...skipping", 3);
				continue;
			}

			getAppDetail(packageInfo);

			// Logs.myLog(id + "added: " + app.name + " " + appList.size(), 1);

			// Logs.myLog("-------------------", 3);

			Intent broadcastIntent = new Intent();
			broadcastIntent.setAction(Global.REBUILD_APPS_IN_PROGRESS);
			mContext.sendBroadcast(broadcastIntent);
		}

		Logs.myLog("getAppList() = " +  Global.appListFW.size() + " (user + system)", 2);

		// Tidy up deleted apps - by going through all the shared preference files

		SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(Global.getContext());

		Map<String, ?> allEntries = p.getAll();
		for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
			String k = entry.getKey();
			// Logs.myLog("FW App Stored: " + k + ": " + entry.getValue().toString(),2);
			if (k.contains("FW-")) {
				String u = entry.getKey().substring(3);
				int uid = 0;
				// Logs.myLog("Got: " + u, 2);
				try {
					uid = Integer.parseInt(u);
					if (Global.appListFW.containsKey(uid)) {
						AppInfo app = Global.appListFW.get(uid);
						if (app.flush == true)
						{
							// remove
							p.edit().remove("FW-" + uid).apply();
							Logs.myLog("App Removed via Flush: " + uid + " " + app.name, 2);
							Global.appListFW.remove(uid);
							rebuildGUIappList = true;
						}
					} else {
						rebuildGUIappList = true;
						p.edit().remove("FW-" + uid).apply();
						Logs.myLog("App Removed via File: " +  uid, 2);
					}
				} catch(NumberFormatException nfe) {
					continue;
				}
			}

		}

		// Notify new apps
		String newAppText = "";

		boolean restart = false;

		it = Global.appListFW.keySet().iterator();

		//
		// Now we have all the apps - write to shared prefs
		// and check for new apps
		//
		while (it.hasNext()) {
			// Get if its blocked or not.
			int key = it.next();
			AppInfo thisApp = Global.appListFW.get(key);
			if (p.contains("FW-" + thisApp.UID2)) {
				thisApp.fw = p.getInt("FW-" + thisApp.UID2, 20);
				Logs.myLog("Known App: " + thisApp.name, 3);
			} else {
				// write key as false - lets us track apps
				if (p.getBoolean("settingsFirstRun", true) == false) {
					// New app!
					Logs.myLog("New App: " + thisApp.name, 3);
					int autoFW = p.getInt("settingsAutoFW", 0);
					if (autoFW == 0) {
						p.edit().putInt("FW-" + thisApp.UID2, 25).apply();
						thisApp.fw = 25;
					} else { // Mark for auto blocking
						p.edit().putInt("FW-" + thisApp.UID2, 45).apply();
						thisApp.fw = 45;
					}
				} else {
					p.edit().putInt("FW-" + thisApp.UID2, 10).apply();
					Logs.myLog("UnKnown App: " + thisApp.name, 3);
					thisApp.fw = 10;
				}
			}
			//
			// Check for new Apps
			//
			if (thisApp.fw == 25)
			{
				rebuildGUIappList = true;
				Logs.myLog("Notify App: " +  thisApp.name, 3);
				newAppText = newAppText + thisApp.name + "\n";
				// Change so does not notify again, but still new
				thisApp.fw = 20;
				p.edit().putInt("FW-" + thisApp.UID2, 20).apply();
			}
			if (thisApp.fw == 45)
			{
				rebuildGUIappList = true;
				restart = true;
				Logs.myLog("Notify App: " +  thisApp.name, 3);
				newAppText = newAppText + thisApp.name + "\n";
				// Change so does not notify again, but still new
				thisApp.fw = 40;
				p.edit().putInt("FW-" + thisApp.UID2, 40).apply();
			}
		}


		if (!newAppText.isEmpty()) {
			notifyNewApp(newAppText);
		}

		if ((restart == true) && (Global.getNetControlState() == true)) {
			Intent serviceIntent = new Intent(Global.getContext(), ServiceFW.class);
			serviceIntent.putExtra("command", Global.NETCONTROL_RESTART); // can we pass app
			Global.getContext().startService(serviceIntent);
		}


		// Send to GUI if its listening
		Intent broadcastIntent = new Intent();
		broadcastIntent.setAction(Global.REBUILD_APPS_DONE);
		mContext.sendBroadcast(broadcastIntent);

		Logs.myLog("getAppList()...DONE", 3);

		Global.updateMyWidgets();

		// Write to file?
		// writeAppListFWFile(Global.appListFW);

		// Finished building - let others build appList
		Global.appListState = APPLIST_DONE;

		p.edit().putBoolean("settingsFirstRun", false).apply();

		return restart;

	}


	public static void getAppDetail(PackageInfo packageInfo) {

		Logs.myLog("========= getAppDetail()", 3 );

		AppInfo app = new AppInfo();

		ApplicationInfo applicationInfo = packageInfo.applicationInfo;

		PackageManager pManager = mContext.getPackageManager();

		String packageName = packageInfo.packageName;


		Logs.myLog("Package: " + packageName + " UID = " + applicationInfo.uid, 3 );


		app.internet = false;
		try {
			packageInfo = Global.getContext().getPackageManager().getPackageInfo(packageName, PackageManager.GET_PERMISSIONS);

			if (packageInfo.requestedPermissions == null)
			{
				Logs.myLog("No permissions!", 3 );
			} else {

				for (String permission : packageInfo.requestedPermissions) {
					if (TextUtils.equals(permission, android.Manifest.permission.INTERNET)) {
						app.internet = true;
						break;
					}
				}
			}

		} catch (PackageManager.NameNotFoundException e) {
			if (Global.settingsLoggingLevel > 2) {
				e.printStackTrace();
			}
		} catch (Exception e) {
			if (Global.settingsLoggingLevel > 2) {
				e.printStackTrace();
			}
		}

		if (app.internet == false)
		{
			// We are not interested in package
			Logs.myLog("No Internet - Ignore!", 3 );
			return;
		}

		app.name = pManager.getApplicationLabel(applicationInfo).toString();
		if (app.name == null)
		{
			app.name = packageName;
		}

		app.enabled = applicationInfo.enabled;

		app.UID2 = applicationInfo.uid; // change to a string??

		// String[] parts = applicationInfo.processName.split(":");
		// app.processName = parts[0];

		app.system = false;
		// Is it a system apps
		if (!((applicationInfo.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) == 0)) {
			app.system = true;
		}

		try {
			// override system app if the app has a launcher
			if (pManager.getLaunchIntentForPackage(packageInfo.packageName) != null)
			{
				app.system = false;
			}
		} catch (Exception e) {
			if (Global.settingsLoggingLevel > 2) {
				e.printStackTrace();
			}
		}

		// override system app if the app has a UID of 1000 or smaller
		if (app.UID2 <= 1000)
		{
			app.system = true;
		}


		// if ( (app.internet == true) && (app.enabled == true) )
		if (app.internet == true) // we care about disabled apps now
		{
			AppInfo appFW;

			AppInfoExtra appInfoExtra = new AppInfoExtra();
			appInfoExtra.packageEnabled = app.enabled;
			appInfoExtra.packageFQDN = packageName;
			appInfoExtra.packageName = app.name;


			if (appListFW.containsKey(app.UID2)) {
				appFW = appListFW.get(app.UID2);
				Logs.myLog("Existing UID " + app.UID2 + " " + packageName, 4 );
				appFW.enabled = appFW.enabled | app.enabled;
				appFW.system = appFW.system | app.system;
			} else {
				Logs.myLog("New UID " + app.UID2 + " " + packageName, 4 );
				appFW = new AppInfo();
				appFW.name = app.name;
				appFW.UID2 = app.UID2;
				appFW.enabled = app.enabled;
				appFW.system = app.system;
				appFW.appInfoExtra = new HashMap<String, AppInfoExtra>();
				appListFW.put(app.UID2,appFW); // replacing?
			}

			if (!(appFW.appInfoExtra.containsKey(appInfoExtra.packageFQDN)))
			{
				appFW.appInfoExtra.put(appInfoExtra.packageFQDN,appInfoExtra);
			}

			if (appFW.appInfoExtra.size() > 1) {
				// appFW.icon = getContext().getResources().getDrawable(R.drawable.android);
				appFW.name = "_Apps (UID " + app.UID2 + ")";
			}

			appFW.internet = true;

			getIcon(pManager,appFW); // assigns it to app

/*
			Logs.myLog("App Name:      " + app.name, 3);
			Logs.myLog("Package Name:  " + packageName, 3);
			// Logs.myLog("Process Name:  " + app.processName, 3);
			Logs.myLog("App UID2:      " + app.UID2, 3);
			Logs.myLog("System:        " + app.system, 3);
			Logs.myLog("Enabled:        " + app.enabled, 3);
*/
			appFW.flush = false;

		}


		return;
	}

	public static Bitmap drawableToBitmap (Drawable drawable) {
		Bitmap bitmap = null;

		if (drawable instanceof BitmapDrawable) {
			BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
			if(bitmapDrawable.getBitmap() != null) {
				return bitmapDrawable.getBitmap();
			}
		}

		if(drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
			bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
		} else {
			bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
		}

		Canvas canvas = new Canvas(bitmap);
		drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
		drawable.draw(canvas);
		return bitmap;
	}

	public static void getIcon (PackageManager pManager, AppInfo apps)
	{

		// Logs.myLog("App get icon: " + apps.name, 1);
		if ( (apps.icon == null) && (apps.appInfoExtra.size() > 2) )
		{
			// More than two packages share same UID so we have
			// already assigned the system icon.
			return;
		}

		if (apps.appInfoExtra.size() > 1) {
			apps.icon = getContext().getResources().getDrawable(R.drawable.android);
		} else {
			if (apps.icon == null) {
				try {
					apps.icon = pManager.getApplicationIcon(apps.appInfoExtra.get(apps.appInfoExtra.keySet().toArray()[0]).packageFQDN);
				} catch (Exception e) {
					apps.icon = getContext().getResources().getDrawable(R.drawable.alert);
					Logs.myLog("Cannot get icon: " + apps.appInfoExtra.get(apps.appInfoExtra.keySet().toArray()[0]).packageFQDN, 3);
				}
			}
		}

	}

	public static NotificationChannel createNotificationChannel(String channelId, String channelName) {
		// Create the NotificationChannel, but only on API 26+ because
		// the NotificationChannel class is new and not in the support library
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			// String channelId = "FW2";
			// String channelName = "New App Alert";
			NotificationChannel notificationChannel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
			// omitted the LED color
			notificationChannel.setImportance(NotificationManager.IMPORTANCE_DEFAULT);
			notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
			notificationManager.createNotificationChannel(notificationChannel);
			return notificationChannel;
		} else {
			return null;
		}
	}

	public static void notifyNewApp(String myText) {
		Intent intent = new Intent(mContext, ActivityMain.class);
		// use System.currentTimeMillis() to have a unique ID for the pending intent
		// PendingIntent pIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, 0);

		Logs.myLog("Notify New App: " + myText , 2);

		PendingIntent contentIntent = PendingIntent.getActivity(mContext, 0,
				new Intent(mContext, ActivityMain.class), PendingIntent.FLAG_UPDATE_CURRENT);
				// new Intent(mContext, ActivityLogs.class), PendingIntent.FLAG_UPDATE_CURRENT);


		if (Build.VERSION.SDK_INT >= 26) {

			if (newAppNotificationChannel == null) {
				newAppNotificationChannel = createNotificationChannel("FW2","New App Alert");
			}

			Notification n = new NotificationCompat.Builder(mContext, newAppNotificationChannel.getId())
					.setContentTitle(Global.getContext().getString(R.string.app_name))
					.setContentText("New Apps Found!")
					.setSmallIcon(R.drawable.ic_lock_idle_lock2)
					.setLargeIcon(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.fw7))
					.setStyle(new NotificationCompat.BigTextStyle()
							.bigText(myText))
					.setAutoCancel(true)
					.setContentIntent(contentIntent)
					.setAutoCancel(true).build();
			notificationManager.notify(200, n); // we overwrite the current notitication



		} else {

			// build notification
			// the addAction re-use the same intent to keep the example short
			Notification n = new NotificationCompat.Builder(mContext)
					.setContentTitle(Global.getContext().getString(R.string.app_name))
					.setContentText("New Apps Found!")
					.setStyle(new NotificationCompat.BigTextStyle()
							.bigText(myText))
					.setSmallIcon(R.drawable.ic_lock_idle_lock2)
					.setLargeIcon(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.fw7))
					.setAutoCancel(true)
					.setContentIntent(contentIntent).build();
			notificationManager.notify(200, n); // we overwrite the current notitication
		}



	}


	public static void updateMyWidgets() {

		Logs.myLog("Updating widgets", 2);

		// Basically all widgets receive all the broadcasts
		// No matter how many Widgets there are the Widget Class
		// is only called once.

		// This is why you should cycle through Widget IDs in the
		// widget itself - not from here...

		Intent updateIntent = new Intent();
		// Action has to be a ACTION_APPWIDGET_* otherwise Widgets
		// will not receive
		updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
		updateIntent.putExtra(WIDGET_ACTION, "ALL"); // Do I need?
		mContext.sendBroadcast(updateIntent);

	}

}
