package com.thomasdh.roosterpgplus;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Scanner;

public class MainActivity extends ActionBarActivity {

    public static WeakReference<MenuItem> refreshItem;
    public static ActionBarSpinnerAdapter actionBarSpinnerAdapter;
    public static ActionBar actionBar;
    public static int selectedWeek = -1;
    private static ArrayList<String> weken;
    public PlaceholderFragment mainFragment;
    public ActionBarDrawerToggle actionBarDrawerToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        actionBar = getSupportActionBar();

        mainFragment = new PlaceholderFragment(PlaceholderFragment.Type.PERSOONLIJK_ROOSTER);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, mainFragment)
                    .commit();
        }

        // Maak de navigation drawer
        String[] keuzes = {"Persoonlijk rooster", "Leerlingrooster", "Docentenrooster"};

        final DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawerlayout);

        final ListView drawerList = (ListView) findViewById(R.id.drawer);
        drawerList.setAdapter(new ArrayAdapter<String>(this, R.layout.drawer_list_item, keuzes));
        drawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    mainFragment = new PlaceholderFragment(PlaceholderFragment.Type.PERSOONLIJK_ROOSTER);
                    android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
                    fragmentManager.beginTransaction()
                            .replace(R.id.container, mainFragment, "Persoonlijk roosterFragment")
                            .commit();
                } else if (position == 1) {
                    mainFragment = new PlaceholderFragment(PlaceholderFragment.Type.LEERLINGROOSTER);
                    android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
                    fragmentManager.beginTransaction()
                            .replace(R.id.container, mainFragment, "LeerlingroosterFragment")
                            .commit();
                } else if (position == 2) {
                    mainFragment = new PlaceholderFragment(PlaceholderFragment.Type.DOCENTENROOSTER);
                    android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
                    fragmentManager.beginTransaction()
                            .replace(R.id.container, mainFragment, "DocentenroosterFragment")
                            .commit();
                }
                drawerLayout.closeDrawer(drawerList);
            }
        });

        actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close);
        drawerLayout.setDrawerListener(actionBarDrawerToggle);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        // Schakel List navigatie in

        getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

        ActionBar.OnNavigationListener onNavigationListener = new

                ActionBar.OnNavigationListener() {
                    @Override
                    public boolean onNavigationItemSelected(int i, long l) {
                        return false;
                    }
                };
        actionBarSpinnerAdapter = new ActionBarSpinnerAdapter(this, new ArrayList<String>());
        //Voeg beide toe
        getSupportActionBar().setListNavigationCallbacks(actionBarSpinnerAdapter, onNavigationListener);

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        actionBarDrawerToggle.syncState();
        // new Notify(this);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        actionBarDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        refreshItem = new WeakReference<MenuItem>(menu.findItem(R.id.menu_item_refresh));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (actionBarDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent optiesIntent = new Intent(this, PreferencesActivity.class);
                startActivity(optiesIntent);
                return true;
            case R.id.menu_item_refresh:
                if (mainFragment.type == PlaceholderFragment.Type.PERSOONLIJK_ROOSTER) {
                    new RoosterDownloader(this, mainFragment.rootView, mainFragment.viewPager, true, item, selectedWeek).execute();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class PlaceholderFragment extends Fragment {

        public View rootView;
        public ViewPager viewPager;
        public Type type;

        public PlaceholderFragment(Type type) {
            this.type = type;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            if (type == Type.PERSOONLIJK_ROOSTER) {
                rootView = inflater.inflate(R.layout.fragment_main, container, false);
                viewPager = (ViewPager) rootView.findViewById(R.id.viewPager);
                viewPager.setAdapter(new MyPagerAdapter());

                if (PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("key", null) == null) {
                    //Laat de gebruiker inloggen -> wel rooster laden daarna
                    new LoginDialogClass(getActivity(), rootView, this).showLoginDialog(true);
                }


            } else if (type == Type.DOCENTENROOSTER) {
                rootView = inflater.inflate(R.layout.fragment_main_docenten, container, false);
                viewPager = (ViewPager) rootView.findViewById(R.id.viewPager_docenten);
                viewPager.setAdapter(new MyPagerAdapter());



            } else if (type == Type.LEERLINGROOSTER) {
                rootView = inflater.inflate(R.layout.fragment_main_leerling, container, false);
                viewPager = (ViewPager) rootView.findViewById(R.id.viewPager_leerling);
                viewPager.setAdapter(new MyPagerAdapter());

                Spinner klasspinner = (Spinner) rootView.findViewById(R.id.main_fragment_spinner_klas);
                ArrayList<String> klassen = new ArrayList<String>();
                klassen.add("5A");
                klassen.add("5B");
                klassen.add("5C");
                klassen.add("5D");
                klassen.add("5E");
                klassen.add("5F");
                klasspinner.setAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, klassen.toArray(new String[0])));
                klasspinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        new RoosterDownloader(getActivity(), rootView, viewPager, true, refreshItem.get(), selectedWeek,
                                ((TextView) view).getText().toString(), "Thomas den Hollander", Type.LEERLINGROOSTER).execute();
                    }


                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });
            }

            laadWeken(getActivity());

            return rootView;

        }

        private void laadWeken(final Context context) {
            new AsyncTask<String, Void, String>() {
                @Override
                protected String doInBackground(String... params) {
                    HttpClient httpclient = new DefaultHttpClient();
                    HttpGet Get = new HttpGet("http://rooster.fwest98.nl/api/rooster/info?weken");

                    try {
                        HttpResponse response = httpclient.execute(Get);
                        int status = response.getStatusLine().getStatusCode();

                        if (status == 200) {
                            String s = "";
                            Scanner sc = new Scanner(response.getEntity().getContent());
                            while (sc.hasNext()) {
                                s += sc.nextLine();
                            }
                            return s;
                        } else {
                            return "error:Onbekende status: " + status;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(String string) {
                    System.out.println("!!!!WEKENE:" + string);
                    if (string != null && string.startsWith("error:")) {
                        Toast.makeText(getActivity(), string.substring(6), Toast.LENGTH_LONG).show();
                    } else {
                        try {
                            weken = new ArrayList<String>();
                            final ArrayList<String> strings = new ArrayList<String>();

                            if (string == null) {
                                string = PreferenceManager.getDefaultSharedPreferences(context).getString("weken", null);
                            }
                            if (string != null) {

                                PreferenceManager.getDefaultSharedPreferences(context).edit().putString("weken", string).commit();

                                JSONArray weekArray = new JSONArray(string);

                                for (int i = 0; i < weekArray.length(); i++) {
                                    JSONObject week = weekArray.getJSONObject(i);
                                    weken.add(week.getString("week"));
                                }
                                //Get the index of the current week
                                int indexCurrentWeek = -1;
                                for (int u = 0; u < weken.size(); u++) {

                                    int correctionForWeekends = 0;
                                    if (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == 1 || Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == 7) {
                                        correctionForWeekends = 1;
                                    }

                                    if (Integer.parseInt(weken.get(u)) == Calendar.getInstance().get(Calendar.WEEK_OF_YEAR) + correctionForWeekends) {
                                        indexCurrentWeek = u;
                                        break;
                                    }
                                }
                                final int NUMBER_OF_WEEKS_IN_SPINNER = 10;
                                for (int c = 0; c < NUMBER_OF_WEEKS_IN_SPINNER; c++) {
                                    strings.add("Week " + weken.get((indexCurrentWeek + c) % weken.size()));
                                }
                            } else {
                                strings.add("Week " + Calendar.getInstance().get(Calendar.WEEK_OF_YEAR));
                            }

                            actionBarSpinnerAdapter = new ActionBarSpinnerAdapter(getActivity(), strings);
                            actionBar.setListNavigationCallbacks(actionBarSpinnerAdapter, new ActionBar.OnNavigationListener() {
                                final ActionBar.OnNavigationListener onNavigationListener = this;

                                @Override
                                public boolean onNavigationItemSelected(int i, long l) {
                                    if (PreferenceManager.getDefaultSharedPreferences(context).getString("key", null) != null) {
                                        String itemString = (String) actionBarSpinnerAdapter.getItem(i);
                                        int week = Integer.parseInt(itemString.substring(5));
                                        selectedWeek = week;
                                        laadRooster(context, viewPager, rootView);
                                    }
                                    return true;
                                }
                            });

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }.execute();
        }

        public void laadRooster(Context context, ViewPager viewPager, View rootView) {

            //Probeer de string uit het geheugen te laden
            String JSON = laadInternal(selectedWeek, getActivity());

            //Als het de goede week is, gebruik hem
            if (JSON.contains("\"week\":\"" + (selectedWeek) + "\"")) {
                new RoosterBuilder(context, viewPager, rootView, selectedWeek).buildLayout(JSON);
                Log.d("MainActivity", "Het uit het geheugen geladen rooster is van de goede week");
                new RoosterDownloader(context, rootView, viewPager, false, refreshItem.get(), selectedWeek).execute();
            } else {
                if (JSON.startsWith("error:")) {
                    Log.w("MainActivity", JSON.substring(6));
                } else {
                    Log.d("MainActivity", "Het uit het geheugen geladen rooster is niet van de goede week, de gewilde week is " + selectedWeek);
                    Log.d("MainActivity", "De uit het geheugen geladen string is: " + JSON);
                }
                new RoosterDownloader(context, rootView, viewPager, true, refreshItem.get(), selectedWeek).execute();
            }
        }

        String laadInternal(int weeknr, Context context) {
            if (weeknr == -1) {
                weeknr = Calendar.getInstance().get(Calendar.WEEK_OF_YEAR);
            }
            return PreferenceManager.getDefaultSharedPreferences(context).getString("week" + (weeknr % context.getResources().getInteger(R.integer.number_of_saved_weeks)), "error:Er is nog geen rooster in het geheugen opgeslagen voor week " + weeknr);
        }

        public static enum Type {
            PERSOONLIJK_ROOSTER,
            LEERLINGROOSTER,
            DOCENTENROOSTER,
        }
    }

}
