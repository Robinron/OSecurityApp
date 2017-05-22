package com.osecurityapp.navigation;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.amazonaws.mobile.AWSMobileClient;
import com.amazonaws.mobile.user.IdentityManager;
import com.amazonaws.mobile.user.IdentityProvider;
import com.osecurityapp.R;
import com.osecurityapp.contentproviders.ArchiveFragment;
import com.osecurityapp.contentproviders.Configuration;
import com.osecurityapp.contentproviders.InstructionFragment;
import com.osecurityapp.contentproviders.HomeFragment;
import com.osecurityapp.contentproviders.SettingsFragment;

import static com.osecurityapp.R.string.app_name;

public class NavigationDrawer {
    private AppCompatActivity containingActivity;

    /** The helper class used to toggle the left navigation drawer open and closed. */
    private ActionBarDrawerToggle drawerToggle;

    /* The navigation drawer layout view control. */
    private DrawerLayout drawerLayout;

    /** The view group that will contain the navigation drawer menu items. */
    private ListView drawerItems;
    private ArrayAdapter<Configuration.DemoFeature> adapter;

    /** The id of the fragment container. */
    private int fragmentContainerId;

    /**
     * Constructs the Navigation Drawer.
     * @param activity the activity that will contain this navigation drawer.
     * @param toolbar the toolbar the activity is using.
     * @param layout the DrawerLayout for this navigation drawer.
     * @param drawerItemsContainer the parent view group for the navigation drawer items.
     */
    public NavigationDrawer(final AppCompatActivity activity,
                            final Toolbar toolbar,
                            final DrawerLayout layout,
                            final ListView drawerItemsContainer,
                            final int fragmentContainerId) {
        // Keep a reference to the activity containing this navigation drawer.
        this.containingActivity = activity;
        this.drawerItems = drawerItemsContainer;
        adapter = new ArrayAdapter<Configuration.DemoFeature>(activity, R.layout.nav_drawer_item) {
            @Override
            public View getView(final int position, final View convertView,
                                final ViewGroup parent) {
                View view = convertView;
                if (view == null) {
                    view = activity.getLayoutInflater().inflate(R.layout.nav_drawer_item, parent, false);
                }
                final Configuration.DemoFeature item = getItem(position);
                ((ImageView) view.findViewById(R.id.drawer_item_icon)).setImageResource(item.iconResId);
                ((TextView) view.findViewById(R.id.drawer_item_text)).setText(item.titleResId);
                return view;
            }
        };
        drawerItems.setAdapter(adapter);
        drawerItems.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(final AdapterView<?> parent, final View view,
                                    final int position, final long id) {
                final FragmentManager fragmentManager = activity.getSupportFragmentManager();

                // Clear back stack when navigating from the Nav Drawer.
                fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

                if (position == 0) {
                    // home
                    showHome();
                    return;
                } else if (position == 1) {
                    showArchive();
                } else if (position == 2) {
                    showSettings();
                }

                Configuration.DemoFeature item = adapter.getItem(position);
                final Fragment fragment = InstructionFragment.newInstance(item.name);

                activity.getSupportFragmentManager()
                    .beginTransaction()
                    .replace(fragmentContainerId, fragment, item.name)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit();

                closeDrawer();
            }
        });
        this.drawerLayout = layout;
        this.fragmentContainerId = fragmentContainerId;

        // Create the navigation drawer toggle helper.
        drawerToggle = new ActionBarDrawerToggle(activity, drawerLayout, toolbar,
            app_name, app_name) {
		
            @Override
            public void syncState() {
                super.syncState();
                updateUserName(activity);
                updateUserImage(activity);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                updateUserName(activity);
                updateUserImage(activity);
            }
        };

        // Set the listener to allow a swipe from the screen edge to bring up the navigation drawer.
        drawerLayout.setDrawerListener(drawerToggle);

        // Display the home button on the toolbar that will open the navigation drawer.
        final ActionBar supportActionBar = containingActivity.getSupportActionBar();
        supportActionBar.setDisplayHomeAsUpEnabled(true);
        supportActionBar.setHomeButtonEnabled(true);

        // Switch to display the hamburger icon for the home button.
        drawerToggle.syncState();
    }

    private void updateUserName(final AppCompatActivity activity) {
        final IdentityManager identityManager =
                AWSMobileClient.defaultMobileClient().getIdentityManager();
        final IdentityProvider identityProvider =
                identityManager.getCurrentIdentityProvider();

        final TextView userNameView = (TextView) activity.findViewById(R.id.userName);

        if (identityProvider == null) {
            // Not signed in
            userNameView.setText(activity.getString(R.string.main_nav_menu_default_user_text));
            userNameView.setBackgroundColor(activity.getResources().getColor(R.color.nav_drawer_no_user_background));
            return;
        }

        final String userName =
                identityProvider.getUserName();

        if (userName != null) {
            userNameView.setText(userName);
            userNameView.setBackgroundColor(
                    activity.getResources().getColor(R.color.nav_drawer_top_background));
        }
    }

    private void updateUserImage(final AppCompatActivity activity) {

        final IdentityManager identityManager =
                AWSMobileClient.defaultMobileClient().getIdentityManager();
        final IdentityProvider identityProvider =
                identityManager.getCurrentIdentityProvider();

        final ImageView imageView =
            (ImageView)activity.findViewById(R.id.userImage);

        if (identityProvider == null) {
            // Not signed in
            if (Build.VERSION.SDK_INT < 22) {
                imageView.setImageBitmap(BitmapFactory.decodeResource(activity.getResources(), R.mipmap.user));
            }
            else {
                imageView.setImageDrawable(activity.getDrawable(R.mipmap.user));
            }

            return;
        }

        final Bitmap userImage = identityManager.getUserImage();
        if (userImage != null) {
            imageView.setImageBitmap(userImage);
        }
    }

    public void showHome() {
        final Fragment fragment = new HomeFragment();

        containingActivity.getSupportFragmentManager()
                .beginTransaction()
                .replace(fragmentContainerId, fragment, HomeFragment.class.getSimpleName())
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .commit();

        // Set the title for the fragment.
        final ActionBar actionBar = containingActivity.getSupportActionBar();
        actionBar.setTitle(R.string.app_name);
        closeDrawer();
    }

    public void showArchive() {
        final Fragment fragment = new ArchiveFragment();

        containingActivity.getSupportFragmentManager()
                .beginTransaction()
                .replace(fragmentContainerId, fragment, ArchiveFragment.class.getSimpleName())
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .commit();

        // Set the title for the fragment.
        final ActionBar actionBar = containingActivity.getSupportActionBar();
        actionBar.setTitle(R.string.main_nav_menu_item_archive);
        closeDrawer();
    }
    public void showSettings() {
        final Fragment fragment = new SettingsFragment();

        containingActivity.getSupportFragmentManager()
                .beginTransaction()
                .replace(fragmentContainerId, fragment, ArchiveFragment.class.getSimpleName())
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .commit();

        // Set the title for the fragment.
        final ActionBar actionBar = containingActivity.getSupportActionBar();
        actionBar.setTitle(R.string.main_nav_menu_item_settings);
        closeDrawer();
    }

    public void addDemoFeatureToMenu(Configuration.DemoFeature demoFeature) {
        adapter.add(demoFeature);
        adapter.notifyDataSetChanged();
    }

    /**
     * Closes the navigation drawer.
     */
    public void closeDrawer() {
        drawerLayout.closeDrawers();
    }

    /**
     * @return true if the navigation drawer is open, otherwise false.
     */
    public boolean isDrawerOpen() {
        return drawerLayout.isDrawerOpen(GravityCompat.START);
    }
}
