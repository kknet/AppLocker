package com.twinone.locker.ui;

import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.View;

import com.twinone.androidlib.DebugTools;
import com.twinone.locker.Constants;
import com.twinone.locker.LockerAnalytics;
import com.twinone.locker.R;
import com.twinone.locker.lock.AppLockService;
import com.twinone.locker.lock.LockService;
import com.twinone.locker.pro.ProUtils;
import com.twinone.locker.ui.NavigationFragment.NavigationListener;
import com.twinone.locker.util.PrefUtils;
import com.twinone.util.Analytics;
import com.twinone.util.DialogSequencer;

public class MainActivity extends ActionBarActivity implements
		NavigationListener {
	// private static final String RUN_ONCE =
	// "com.twinone.locker.pref.run_once";

	// public static final boolean DEBUG = false;
	private static final String VERSION_URL_PRD = "https://twinone.org/apps/locker/update.php";
	private static final String VERSION_URL_DBG = "https://twinone.org/apps/locker/dbg-update.php";
	public static final String VERSION_URL = Constants.DEBUG ? VERSION_URL_DBG
			: VERSION_URL_PRD;

	// private VersionManager mVersionManager;

	// private static final String TAG = "Main";

	private DialogSequencer mSequencer;
	// private Analytics mAnalytics;

	public static final String EXTRA_UNLOCKED = "com.twinone.locker.unlocked";

	private void doTest() {
		Log.d("", "Querying from test");
		// new VersionManager(c).queryServer(null);

		// analytics
		Analytics analytics = new Analytics(this);
		ProUtils proUtils = new ProUtils(this);
		Map<String, String> data = new HashMap<String, String>();
		data.put(LockerAnalytics.PRO_TYPE, proUtils.getProTypeString());
		data.put(LockerAnalytics.LOCKED_APPS_COUNT,
				String.valueOf(PrefUtils.getLockedApps(this).size()));
		analytics.setDefaultUrl(LockerAnalytics.URL).query(data);
	}

	private Fragment mCurrentFragment;

	/**
	 * 
	 * @return True if the service is allowed to start
	 */
	private boolean showDialogs() {
		boolean deny = false;

		// Recovery code
		mSequencer.addDialog(Dialogs.getRecoveryCodeDialog(this));

		// Empty password
		deny = Dialogs.addEmptyPasswordDialog(this, mSequencer);

		mSequencer.start();
		return !deny;
	}

	@Override
	public void setTitle(CharSequence title) {
		super.setTitle(title);
		mTitle = title;
		getSupportActionBar().setTitle(title);
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.d("Main", "onResume");
		showLockerIfNotUnlocked(true);
		registerReceiver(mReceiver, mFilter);
		updateLayout();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.global, menu);
		return true;
	}

	private void showLockerIfNotUnlocked(boolean relock) {
		DebugTools.lap("posting onStartCommand (relock=" + relock + ")");
		boolean unlocked = getIntent().getBooleanExtra(EXTRA_UNLOCKED, false);
		if (new PrefUtils(this).isCurrentPasswordEmpty()) {
			unlocked = true;
		}
		if (!unlocked) {
			LockService.showCompare(this, getPackageName());
		}
		getIntent().putExtra(EXTRA_UNLOCKED, !relock);
	}

	@Override
	protected void onPause() {
		super.onPause();
		// mSequencer.stop();
		LockService.hide(this);
		unregisterReceiver(mReceiver);
		mSequencer.stop();

		// if (mCurrentFragmentType != NavigationElement.TYPE_SETTINGS) {
		// // Keep settings open because the user could navigate to gallery to
		// // change background
		// finish();
		// }
	}

	@Override
	protected void onDestroy() {
		Log.v("Main", "onDestroy");
		super.onDestroy();
	}

	/**
	 * Provide a way back to {@link MainActivity} without having to provide a
	 * password again. It finishes the calling {@link Activity}
	 * 
	 * @param context
	 */
	public static final void showWithoutPassword(Context context) {
		Intent i = new Intent(context, MainActivity.class);
		i.putExtra(EXTRA_UNLOCKED, true);
		if (!(context instanceof Activity)) {
			i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		}
		context.startActivity(i);
	}

	/**
	 * Fragment managing the behaviors, interactions and presentation of the
	 * navigation drawer.
	 */
	private NavigationFragment mNavFragment;

	/**
	 * Used to store the last screen title. For use in
	 * {@link #restoreActionBar()}.
	 */
	private CharSequence mTitle;

	private ActionBar mActionBar;
	private BroadcastReceiver mReceiver;
	private IntentFilter mFilter;

	private class ServiceStateReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d("MainACtivity",
					"Received broadcast (action=" + intent.getAction());
			updateLayout();
		}
	}

	private void updateLayout() {
		Log.d("Main",
				"UPDATE LAYOUT Setting service state: "
						+ AppLockService.isRunning(this));
		mNavFragment.getAdapter().setServiceState(
				AppLockService.isRunning(this));
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		handleIntent();

		mReceiver = new ServiceStateReceiver();
		mFilter = new IntentFilter();
		mFilter.addCategory(AppLockService.CATEGORY_STATE_EVENTS);
		mFilter.addAction(AppLockService.BROADCAST_SERVICE_STARTED);
		mFilter.addAction(AppLockService.BROADCAST_SERVICE_STOPPED);

		mNavFragment = (NavigationFragment) getSupportFragmentManager()
				.findFragmentById(R.id.navigation_drawer);
		// Set up the drawer.
		mNavFragment.setUp(R.id.navigation_drawer,
				(DrawerLayout) findViewById(R.id.drawer_layout));
		mTitle = getTitle();

		mActionBar = getSupportActionBar();
		mCurrentFragment = new AppsFragment();
		getSupportFragmentManager().beginTransaction()
				.add(R.id.container, mCurrentFragment).commit();
		mCurrentFragmentType = NavigationElement.TYPE_APPS;

		mSequencer = new DialogSequencer();
		if (showDialogs()) {
			// Don't auto-start, for debugging purposes
			// AppLockService.start(this);
		}
		showLockerIfNotUnlocked(false);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		Log.d("", "onNewIntent");
		super.onNewIntent(intent);
		setIntent(intent);
		handleIntent();
	}

	/**
	 * Handle this Intent for searching...
	 */
	private void handleIntent() {
		if (getIntent() != null && getIntent().getAction() != null) {
			if (getIntent().getAction().equals(Intent.ACTION_SEARCH)) {
				Log.d("MainActivity", "Action search!");
				if (mCurrentFragmentType == NavigationElement.TYPE_APPS) {
					final String query = getIntent().getStringExtra(
							SearchManager.QUERY);
					if (query != null) {
						((AppsFragment) mCurrentFragment).onSearch(query);
					}
				}
			}
		}
	}

	public void setActionBarTitle(int resId) {
		mActionBar.setTitle(resId);
	}

	private boolean mNavPending;
	int mCurrentFragmentType;
	int mNavPendingType = -1;

	@Override
	public boolean onNavigationElementSelected(int type) {
		if (type == NavigationElement.TYPE_TEST) {
			doTest();
			return false;
		} else if (type == NavigationElement.TYPE_STATUS) {
			toggleService();
			return false;
		}
		mNavPending = true;
		mNavPendingType = type;
		return true;
	}

	private void toggleService() {
		boolean newState = false;
		if (AppLockService.isRunning(this)) {
			AppLockService.stop(this);
		} else if (Dialogs.addEmptyPasswordDialog(this, mSequencer)) {
			mSequencer.start();
		} else {
			newState = AppLockService.toggle(this);
		}
		if (mNavFragment != null)
			mNavFragment.getAdapter().setServiceState(newState);
	}

	@Override
	public void onDrawerOpened(View drawerView) {
		getSupportActionBar().setTitle(mTitle);
	}

	@Override
	public void onDrawerClosed(View drawerView) {
		getSupportActionBar().setTitle(mTitle);
		if (mNavPending) {
			navigateToFragment(mNavPendingType);
			mNavPending = false;
		}
	}

	/**
	 * Open a specific Fragment
	 * 
	 * @param type
	 */
	public void navigateToFragment(int type) {
		if (type == mCurrentFragmentType) {
			// Don't duplicate
			return;
		}
		if (type == NavigationElement.TYPE_CHANGE) {
			Dialogs.getChangePasswordDialog(this).show();
			// Don't change current fragment type
			return;
		}

		switch (type) {
		case NavigationElement.TYPE_APPS:
			mCurrentFragment = new AppsFragment();
			break;
		case NavigationElement.TYPE_SETTINGS:
			mCurrentFragment = new SettingsFragment();
			break;
		case NavigationElement.TYPE_STATISTICS:
			mCurrentFragment = new StatisticsFragment();
			break;
		case NavigationElement.TYPE_PRO:
			mCurrentFragment = new ProFragment();
			break;
		}
		FragmentManager fm = getSupportFragmentManager();
		// Don't re-add the already present fragment
		fm.beginTransaction().replace(R.id.container, mCurrentFragment)
				.commit();
		mCurrentFragmentType = type;
	}

	@Override
	public void onShareButton() {
		Dialogs.getShareEditDialog(this).show();
	}

	@Override
	public void onRateButton() {
		toGooglePlay();
	}

	private void toGooglePlay() {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse("market://details?id=" + getPackageName()));
		startActivity(intent);
	}
}