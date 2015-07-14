package edu.uri.crc.sfmpsurveyapp;

import android.os.Environment;

import com.cocoahero.android.geojson.FeatureCollection;
import com.cocoahero.android.geojson.GeoJSON;
import com.cocoahero.android.geojson.Point;
import com.mapbox.mapboxsdk.tileprovider.tilesource.ITileLayer;
import com.mapbox.mapboxsdk.tileprovider.tilesource.MBTilesLayer;
import com.mapbox.mapboxsdk.util.DataLoadingUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.HashSet;

/**
 * A little class to handle the filesystem IO in one place
 */
public class SFMPResourceHelper {
    private static String SFMP_DIR = "/SFMP/";

    public static java.util.Set<String> getCommunities() {
        // open http://stackoverflow.com/questions/7030446/reading-all-files-in-the-android-file-system
        File root = Environment.getExternalStorageDirectory();
        String sfmpPath = root.getPath() + SFMP_DIR;
        String[] candidates = new File(sfmpPath).list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.endsWith(".geojson");
            }
        });
        for (int ii = 0; ii < candidates.length; ii++) {
            int dotpos = candidates[ii].indexOf(".");
            if (dotpos >= 0)
                candidates[ii] = candidates[ii].substring(0, dotpos);
        }
        return new HashSet<String>(Arrays.asList(candidates));
    }

    public static java.util.Collection<ITileLayer> getCommunityMaps() {
        // open http://stackoverflow.com/questions/7030446/reading-all-files-in-the-android-file-system
        File root = Environment.getExternalStorageDirectory();
        String sfmpPath = root.getPath() + SFMP_DIR;
        String[] candidates = new File(sfmpPath).list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.endsWith(".mbtiles");
            }
        });

        HashSet<ITileLayer> returnar = new HashSet<>();
        for (String cand : candidates)
        {
            returnar.add(new MBTilesLayer(cand));
        }
        return returnar;
    }


    /**
     * very poor form, but we're kind of pressed for time.
     * If this runs into an exception, it will eat it.
     * if it didn't work, you'll get back NULL.
     *
     * @param name
     * @return
     */
    public static FeatureCollection getCommunity(String name) {
        File root = Environment.getExternalStorageDirectory();
        String communityPath = root.getPath() + SFMP_DIR + name + ".geojson";
        File community = new File(communityPath);
        if (community.exists() && community.canRead()) {
            try {
                return DataLoadingUtils.loadGeoJSONFromUrl(community.toURI().toURL().toString());
            } catch (java.io.IOException ioe) {
                ioe.printStackTrace();
                return null;
            } catch (org.json.JSONException jse) {
                jse.printStackTrace();
                return null;
            }
        }
        return null;
    }

}
