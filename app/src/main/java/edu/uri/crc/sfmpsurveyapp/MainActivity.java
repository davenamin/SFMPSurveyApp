package edu.uri.crc.sfmpsurveyapp;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.mapbox.mapboxsdk.tileprovider.tilesource.ITileLayer;
import com.mapbox.mapboxsdk.tileprovider.tilesource.WebSourceTileLayer;
import com.mapbox.mapboxsdk.views.MapView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * First time writing an Android app! Sorry to any grad students to follow in the wake of this beast...
 * <p/>
 * Derived from the sample MainActivity/Fragment sample in the Android SDK and the MapBox samples
 *
 * @author Daven Amin
 */
public class MainActivity extends ActionBarActivity {

    private MapView mv;

    private DrawerLayout drawerLayout;

    private ListView drawerList;
    private ArrayList<String> areaNames;
    private int currentArea = -1;

    private Map<String, SurveyAreaFragment> fragmentCache;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mv = (MapView) findViewById(R.id.mapview);
        mv.setUserLocationEnabled(true);
        mv.goToUserLocation(true);
        mv.setDiskCacheEnabled(true);

        WebSourceTileLayer ws = new WebSourceTileLayer("openstreetmap", "http://tile.openstreetmap.org/{z}/{x}/{y}.png");
        ws.setName("OpenStreetMap")
                .setAttribution("© OpenStreetMap Contributors");
                //.setMinimumZoomLevel(1)
                //.setMaximumZoomLevel(18);

        ArrayList<ITileLayer> layerlist = new ArrayList<>();
        layerlist.add(ws);
        // let's see if there are any MBTile files to get!
        layerlist.addAll(SFMPResourceHelper.getCommunityMaps());

        mv.setTileSource(layerlist.toArray(new ITileLayer[layerlist.size()]));



        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        drawerList = (ListView) findViewById(R.id.left_drawer);

        areaNames = new ArrayList<>();
        areaNames.addAll(SFMPResourceHelper.getCommunities());


        fragmentCache = new HashMap<>();

        drawerList.setAdapter(new ArrayAdapter<String>(this, R.layout.drawer_list_item, areaNames));
        drawerList.setOnItemClickListener(new DrawerItemClickListener());
    }

    @Override
    public void onPause()
    {
        super.onPause();
        // always force us to redraw/reload upon a pause...
        currentArea = -1;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_quit) {
            this.finish();
            System.exit(0);
        }
        else if (id == R.id.action_settings) {
            return true;
        }
        else if (id == R.id.action_list) {
            drawerLayout.openDrawer(Gravity.LEFT);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void selectItem(int position) {
        String area = areaNames.get(position);
        SurveyAreaFragment frag = null;
        if (position != currentArea) {
            if (!fragmentCache.containsKey(area)) {
                SurveyAreaFragment newfrag = new SurveyAreaFragment();
                newfrag.setSurveyArea(area);
                fragmentCache.put(area, newfrag);
            }

            frag = fragmentCache.get(area);
            // Insert the fragment by replacing any existing fragment
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.content_frame, frag)
                    .commit();

            // Highlight the selected item, update the title, and close the drawer
            drawerList.setItemChecked(position, true);
            setTitle(frag.getSurveyAreaString());
            drawerLayout.closeDrawer(drawerList);

            currentArea = position;
        }
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            System.out.println("clicked item: " + position);
            selectItem(position);
        }
    }

}
