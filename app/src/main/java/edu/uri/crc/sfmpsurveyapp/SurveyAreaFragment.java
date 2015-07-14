package edu.uri.crc.sfmpsurveyapp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.cocoahero.android.geojson.Feature;
import com.cocoahero.android.geojson.FeatureCollection;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mapbox.mapboxsdk.geometry.BoundingBox;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.Marker;
import com.cocoahero.android.geojson.Point;
import com.mapbox.mapboxsdk.views.MapView;

import org.json.JSONArray;
import org.json.JSONException;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;


/**
 * This fragment is supposed to represent a map view of a particular survey location...
 */
public class SurveyAreaFragment extends Fragment {

    // my barely-qualifies-as-a-regex matchers could use some help...
    private static Pattern IDMATCH = Pattern.compile(".*id.*|name", Pattern.CASE_INSENSITIVE);
    private static Pattern TYPEMATCH = Pattern.compile("type|description", Pattern.CASE_INSENSITIVE);

    private static final String DEFAULT_MARKER_TYPE = "unknown";

    private MapView mv = null;
    private String area = null;
    private String surveyAreaString = "No Area!";


    private Multimap<String, Marker> markermap = HashMultimap.create();
    private Map<Integer, Marker> idmap = Collections.synchronizedMap(new HashMap<Integer, Marker>());
    private volatile boolean showingSamples = false;
    private volatile boolean showingBackups = false;

    Button sampBtn = null;
    View.OnClickListener sampleListener = null;
    Button backBtn = null;
    View.OnClickListener backupListener = null;
    Button findBtn = null;
    View.OnClickListener findListener = null;
    Button meBtn = null;
    View.OnClickListener meListener = null;


    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        final View view = inflater.inflate(R.layout.fragment_main, container, false);

        mv = (MapView) container.findViewById(R.id.mapview);

        boolean success = false;

        FeatureCollection comm = SFMPResourceHelper.getCommunity(area);

        if (comm != null) {
            ArrayList<LatLng> pos = new ArrayList<>();
            try {
                for (Feature f : comm.getFeatures()) {
                    if (f.getGeometry() instanceof Point) {
                        JSONArray coordinates = (JSONArray) f.getGeometry().toJSON().get("coordinates");
                        double lon = (Double) coordinates.get(0);
                        double lat = (Double) coordinates.get(1);
                        LatLng markerpos = new LatLng(lat, lon);

                        // this might be painfully unoptimized

                        String idkey = "";
                        String typekey = "";
                        Iterator<String> keys = f.getProperties().keys();
                        while (keys.hasNext()) {
                            String s = keys.next();
                            if (IDMATCH.matcher(s).matches()) {
                                idkey = s;
                            } else if (TYPEMATCH.matcher(s).matches()) {
                                typekey = s;
                            }
                        }

                        int id = f.getProperties().optInt(idkey);
                        String type = f.getProperties().optString(typekey, DEFAULT_MARKER_TYPE).toLowerCase();

                        Marker marker = new Marker(String.valueOf(id), type, markerpos);
                        markermap.put(type, marker);
                        idmap.put(id, marker);
                        pos.add(markerpos);
                        System.out.println("added marker at: " + markerpos.toString());
                        System.out.println("with properties: " + f.getProperties().toString(3));
                        success = true;
                    } else {
                        System.out.println(f.getGeometry().toString());
                    }
                }
                for (String type : markermap.keySet()) {

                    for (Marker m : markermap.get(type)) {
                        if (type.contains("sample")) {
                            m.setMarker(getResources().getDrawable(R.drawable.greenmarker));
                            showingSamples = true;
                            System.out.println("greenpin");
                        } else if (type.contains("backup")) {
                            m.setMarker(getResources().getDrawable(R.drawable.orangemarker));
                            showingBackups = true;
                            System.out.println("orangepin");
                        }
                        mv.addMarker(m);
                    }
                }
                mv.invalidate();
                mv.zoomToBoundingBox(BoundingBox.fromLatLngs(pos), true);
            } catch (JSONException jse) {
                jse.printStackTrace();
            }
        } else {
            System.out.println("comm was null!");
        }

        sampBtn = (Button) view.findViewById(R.id.sampbut);
        sampleListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showingSamples = !showingSamples;

                for (String type : markermap.keySet()) {
                    if (type.contains("sample")) {
                        for (Marker m : markermap.get(type)) {
                            mv.removeMarker(m);
                            if (showingSamples) {
                                mv.addMarker(m);
                            }
                        }
                    }
                }
            }
        };
        sampBtn.setOnClickListener(sampleListener);


        backBtn = (Button) view.findViewById(R.id.backbut);
        backupListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showingBackups = !showingBackups;

                for (String type : markermap.keySet()) {
                    if (type.contains("backup")) {
                        for (Marker m : markermap.get(type)) {
                            mv.removeMarker(m);
                            if (showingBackups) {
                                mv.addMarker(m);
                            }
                        }
                    }
                }
            }
        };
        backBtn.setOnClickListener(backupListener);


        findBtn = (Button) view.findViewById(R.id.findbut);
        findListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // open http://stackoverflow.com/questions/10903754/input-text-dialog-android
                AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                builder.setTitle("Enter Number!");
                final EditText input = new EditText(view.getContext());
                input.setInputType(InputType.TYPE_CLASS_NUMBER);
                builder.setView(input);


                // Set up the buttons
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String findStr = input.getText().toString();
                        try {
                            Integer val = Integer.valueOf(findStr);
                            if (val != null) {
                                Marker m = idmap.get(val);
                                if (m != null) {
                                    mv.setCenter(m.getPosition());
                                    m.showBubble(m.getToolTip(mv), mv, true);
                                }
                            }
                        } catch (NumberFormatException nfe) {
                            //ignore
                            System.out.println("NFE in Finding coordinate!");
                            nfe.printStackTrace();
                        }
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                builder.show();
            }
        };
        findBtn.setOnClickListener(findListener);

        meBtn = (Button) view.findViewById(R.id.mebut);
        meListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mv.goToUserLocation(true);
            }
        };
        meBtn.setOnClickListener(meListener);

        if (!success) {
            surveyAreaString = "Couldn't load: " + area;
        } else {
            surveyAreaString = area;
        }
        getActivity().setTitle(surveyAreaString);
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        for (Marker m : markermap.values()) {
            mv.removeMarker(m);
        }
    }

    public void setSurveyArea(String area) {
        //TODO:
        System.out.println("setting Survey Area: " + area);
        this.area = area;
    }

    public String getSurveyAreaString() {
        return surveyAreaString;
    }


}
