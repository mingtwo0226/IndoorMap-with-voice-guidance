package com.sails.example;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
import com.sails.engine.Beacon;
import com.sails.engine.LocationRegion;
import com.sails.engine.SAILS;
import com.sails.engine.MarkerManager;
import com.sails.engine.PathRoutingManager;
import com.sails.engine.PinMarkerManager;
import com.sails.engine.SAILSMapView;
import com.sails.engine.core.model.GeoPoint;
import com.sails.engine.overlay.Marker;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import static android.speech.tts.TextToSpeech.ERROR;

public class MainActivity extends Activity {
    static SAILS mSails;
    static SAILSMapView mSailsMapView;

    ImageView zoomin, zoomout, lockcenter;
    Button setDestination;
    SlidingMenu menu;
    ExpandableListView expandableListView;
    ExpandableAdapter eAdapter;
    Vibrator mVibrator;
    Spinner floorList;
    ArrayAdapter<String> adapter;
    byte zoomSav = 0;
    static int result = 0; // The number of steps
    static int time = 1; // use in tts(routing info)
    static int des_time = 1; //use in tts(when near routing destination)
    private TextToSpeech tts;
    int stateNum;
    Button myLocation, buildingInfo, use;
    TextView info;

    static double latPoint, lonPoint; // start point
    static double latDest, lonDest; // Dest point
    double t_angle; // phone heading
    double distance;
    double Cur_Lat_radian, Cur_Lon_radian; // current location angle
    double Dest_Lat_radian, Dest_Lon_radian; // destination location angle
    double radian_distance;
    double radian_bearing;
    double true_bearing;
    double angle_result;
    String str_angle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        menu = new SlidingMenu(this);
        menu.setMode(SlidingMenu.LEFT);
        menu.setTouchModeAbove(SlidingMenu.TOUCHMODE_NONE);
        menu.setFadeDegree(0.0f);
        menu.attachToActivity(this, SlidingMenu.SLIDING_CONTENT);
        menu.setMenu(R.layout.expantablelist);
        mVibrator = (Vibrator) getSystemService(Service.VIBRATOR_SERVICE);
        zoomin = (ImageView) findViewById(R.id.zoomin);
        zoomout = (ImageView) findViewById(R.id.zoomout);
        lockcenter = (ImageView) findViewById(R.id.lockcenter);
        setDestination = (Button)findViewById(R.id.setDest);
        buildingInfo = (Button)findViewById(R.id.buildingInfo);
        use = (Button)findViewById(R.id.Use);
        final TextView info = (TextView)findViewById(R.id.infoText);

        Typeface font= Typeface.createFromAsset(getAssets(), "nanumbarungothic.ttf");
        info.setTypeface(font);
        use.setTypeface(font);
        setDestination.setTypeface(font);
        buildingInfo.setTypeface(font);

        floorList = (Spinner) findViewById(R.id.spinner);

        zoomin.setOnClickListener(controlListener);
        zoomout.setOnClickListener(controlListener);
        lockcenter.setOnClickListener(controlListener);
        expandableListView = (ExpandableListView) findViewById(R.id.expandableListView);
        expandableListView.setOnChildClickListener(childClickListener);
        setDestination.setOnClickListener(controlListener);
        buildingInfo.setOnClickListener(controlListener);
        use.setOnClickListener(controlListener);

        LocationRegion.FONT_LANGUAGE = LocationRegion.NORMAL;

        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != ERROR){
                    tts.setLanguage(Locale.KOREAN);
                }
            }
        });

        SensorManager sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        SensorEventListener mListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                float[] v= event.values;
                switch(event.sensor.getType()){
                    case Sensor.TYPE_ORIENTATION:
                        t_angle = v[0];
                        break;
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        };

        sensorManager.registerListener(mListener,
                sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_NORMAL);

        //new a SAILS engine.
        mSails = new SAILS(this);
        //set location mode.
        mSails.setMode(SAILS.WIFI_GFP_IMU);
        //set floor number sort rule from descending to ascending.
        mSails.setReverseFloorList(true);
        //create location change call back.
        mSails.setOnLocationChangeEventListener(new SAILS.OnLocationChangeEventListener() {
            @Override
            public void OnLocationChange() {
                if (mSailsMapView.isCenterLock() && !mSailsMapView.isInLocationFloor() && !mSails.getFloor().equals("") && mSails.isLocationFix()) {
                    //set the map that currently location engine recognize.
                    mSailsMapView.getMapViewPosition().setZoomLevel((byte) 20);
                    mSailsMapView.loadCurrentLocationFloorMap();
                    Toast t = Toast.makeText(getBaseContext(), mSails.getFloorDescription(mSails.getFloor()), Toast.LENGTH_SHORT);
                    t.show();
                }
            }
        });

        myLocation = (Button)findViewById(R.id.curLocation);
        myLocation.setTypeface(font);

        //파싱하는부분----------------------------------------------------------
        Document document = null;
        try {
            InputStream xml = this.getAssets().open(Parameters.MAP_FILE);
            XMLParser parser = new XMLParser(xml);
            document = parser.getDocument();
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        final HashMap<String, Polygon> ROOMS = creatRooms(document, Parameters.ROOMS);
        //----------------------------------------------------------------------


        myLocation.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Editor: E
                if(mSails.isLocationEngineStarted()==false)
                {
                    Toast.makeText(getApplication(), "측위 엔진이 켜지지 않았습니다.\n" +
                            "현재 위치를 다시 한 번 확인하시거나,\n" +
                            "엔진이 켜질 때까지 잠시만 기다려주세요.", Toast.LENGTH_SHORT).show();
                }
                else {
                    Double myLat = mSails.getLatitude(); //현재 위치의 위도와 경도를 입력받는다.
                    Double myLon = mSails.getLongitude();
                    if (myLat.isNaN() || myLon.isNaN()) {
                        Toast.makeText(getApplication(), "측위 엔진이 켜지지 않았습니다.\n" +
                                "현재 위치를 다시 한 번 확인하시거나,\n" +
                                "엔진이 켜질 때까지 잠시만 기다려주세요.", Toast.LENGTH_SHORT).show();
                    } else {
                        GeoPoint myP = new GeoPoint(myLat, myLon);
                        Point newP = mSailsMapView.getProjection().toPixels(myP, null); //현재 위치의 위도와 경도를 Projection한다.
                        GeoPoint startG = new GeoPoint(35.23544786468, 129.08248364858); //StartG와 distP는 기준점
                        GeoPoint distP = new GeoPoint(35.235384468, 129.08248281098);
                        Point startGtoInt = new Point();
                        mSailsMapView.getProjection().toPixels(startG, startGtoInt);
                        Point distPtoInt = new Point();
                        mSailsMapView.getProjection().toPixels(distP, distPtoInt);
                        Double zoomD = Math.sqrt(Math.pow(Math.abs(startGtoInt.x - distPtoInt.x), 2) + Math.pow(Math.abs(startGtoInt.y - distPtoInt.y), 2));
                        float realZoom = (float) 6 / zoomD.floatValue();
                        Double distance = Math.sqrt(Math.pow(Math.abs(startGtoInt.x - newP.x), 2) + Math.pow(Math.abs(startGtoInt.y - newP.y), 2));
                        int a = newP.y - startGtoInt.y;
                        int b = newP.x - startGtoInt.x;
                        Double castB = new Double(b);
                        Double sinA = castB / distance;
                        Double myAngle = Math.toDegrees(Math.asin(sinA));
                        Double gmlX = (distance * Math.cos(Math.toRadians(myAngle))) * realZoom + 17.77749720812225;
                        Double gmlY = distance * Math.sin(Math.toRadians(myAngle)) * realZoom + 131.0302343681601 + 7.9;
                        Set<Map.Entry<String, Polygon>> set = ROOMS.entrySet();
                        Iterator<Map.Entry<String, Polygon>> itr = set.iterator();

                        while (itr.hasNext()) {
                            Map.Entry<String, Polygon> e = (Map.Entry<String, Polygon>) itr.next();
                            if (e.getValue().contains(gmlX.floatValue(), gmlY.floatValue())) {
                                tts.speak("현재 위치는 " + e.getKey() + "입니다.", TextToSpeech.QUEUE_ADD, null);
                                info.setText("  " + e.getKey());
                                break;
                            }
                        }
                    }
                }
            }
        });

        mSails.setOnBLEPositionInitialzeCallback(10000,new SAILS.OnBLEPositionInitializeCallback() {
            @Override
            public void onStart() {
            }

            @Override
            public void onFixed() {
            }

            @Override
            public void onTimeOut() {
                if(!mSails.checkMode(SAILS.BLE_ADVERTISING))
                    mSails.stopLocatingEngine();
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Positioning Timeout")
                        .setMessage("Put some time out message!")
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialoginterface, int i) {
                                mSailsMapView.setMode(SAILSMapView.GENERAL);
                            }
                        }).setPositiveButton("Retry", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mSails.startLocatingEngine();
                    }
                }).show();
            }
        });

        mSails.setNoWalkAwayPushRepeatDuration(6000);
        mSails.setOnBTLEPushEventListener(new SAILS.OnBTLEPushEventListener() {
            @Override
            public void OnPush(final Beacon mB) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplication(),mB.push_name,Toast.LENGTH_SHORT).show();
                    }
                });

            }

            @Override
            public void OnNothingPush() {
                Log.e("Nothing Push","true");
            }
        });

        //new and insert a SAILS MapView from layout resource.
        mSailsMapView = new SAILSMapView(this);
        ((FrameLayout) findViewById(R.id.SAILSMap)).addView(mSailsMapView);
        //configure SAILS map after map preparation finish.
        mSailsMapView.post(new Runnable() {
            @Override
            public void run() {
                //please change token and building id to your own building project in cloud.
                mSails.loadCloudBuilding("424ef5e8bec34b0c9bb6e671bfcaf00b", "5b485a716ec90b000000006a", new SAILS.OnFinishCallback() {
                    @Override
                    public void onSuccess(String response) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mapViewInitial();
                                routingInitial();
                                slidingMenuInitial();
                            }
                        });
                    }

                    @Override
                    public void onFailed(String response) {
                        Toast t = Toast.makeText(getBaseContext(), "Load cloud project fail, please check network connection.", Toast.LENGTH_SHORT);
                        t.show();
                    }
                });
            }
        });
    }

    void mapViewInitial() {
        //establish a connection of SAILS engine into SAILS MapView.
        mSailsMapView.setSAILSEngine(mSails);

        //set location pointer icon.
        //mSailsMapView.setLocationMarker(R.drawable.circle, R.drawable.arrow, null, 100);
        mSailsMapView.setLocationMarker(R.drawable.myloc_arr, R.drawable.myloc_arr, null, 100);

        //set location marker visible.
        mSailsMapView.setLocatorMarkerVisible(true);

        //load first floor map in package.
        mSailsMapView.loadFloorMap(mSails.getFloorNameList().get(0));

        //Auto Adjust suitable map zoom level and position to best view position.
        mSailsMapView.autoSetMapZoomAndView();

        mSails.startLocatingEngine();
        //Toast.makeText(getApplication(),"엔진 상태: "+ mSails.isLocationEngineStarted() , Toast.LENGTH_SHORT).show();
        //((FrameLayout) findViewById(R.id.SAILSMap)).removeAllViews();
        //((FrameLayout) findViewById(R.id.SAILSMap)).setVisibility(View.INVISIBLE);


        //set location region click call back.
        mSailsMapView.setOnRegionClickListener(new SAILSMapView.OnRegionClickListener() {
            @Override
            public void onClick(List<LocationRegion> locationRegions) {
                LocationRegion lr = locationRegions.get(0);
                mVibrator.vibrate(200);
                String name;

                boolean dd = Pattern.matches("[가-힝]", lr.label.substring(0,1));
                if(dd == true){
                    name = lr.label;
                    Toast.makeText(getApplication(),name + "입니다",Toast.LENGTH_SHORT).show();
                    tts.speak(name + "입니다", TextToSpeech.QUEUE_ADD, null);
                } else{
                    name = "a" + lr.label;
                    String type = "string";
                    String packageName = getPackageName();
                    int ResId = getResources().getIdentifier(name, type, packageName);
                    String res_name = getResources().getString(ResId);
                    Toast.makeText(getApplicationContext(),res_name + "입니다",Toast.LENGTH_SHORT).show();
                    tts.speak(res_name + "입니다", TextToSpeech.QUEUE_ADD, null);
                }

                //begin to routing
                if (mSails.isLocationEngineStarted() && mSails.isInThisBuilding()) {
                    //set routing start point to current user location.
                    mSailsMapView.getRoutingManager().setStartRegion(PathRoutingManager.MY_LOCATION);
                    latPoint = mSails.getLatitude();
                    lonPoint = mSails.getLongitude();

                    //set routing end point marker icon.
                    mSailsMapView.getRoutingManager().setTargetMakerDrawable(Marker.boundCenterBottom(getResources().getDrawable(R.drawable.destination)));
                    latDest = lr.getCenterLatitude();
                    lonDest = lr.getCenterLongitude();

                    //set routing path's color.
                    mSailsMapView.getRoutingManager().getPathPaint().setColor(0xFF35b3e5);
                } else {
                    mSailsMapView.getRoutingManager().setTargetMakerDrawable(Marker.boundCenterBottom(getResources().getDrawable(R.drawable.map_destination)));
                    latDest = lr.getCenterLatitude();
                    lonDest = lr.getCenterLongitude();
                    mSailsMapView.getRoutingManager().getPathPaint().setColor(0xFF85b038);
                    if (mSailsMapView.getRoutingManager().getStartRegion() != null){}
                }
                //set routing end point location.
                mSailsMapView.getRoutingManager().setTargetRegion(lr);

                //begin to route.
                if (mSailsMapView.getRoutingManager().enableHandler()){
                }
            }
        });

        mSailsMapView.getPinMarkerManager().setOnPinMarkerClickCallback(new PinMarkerManager.OnPinMarkerClickCallback() {
            @Override
            public void OnClick(MarkerManager.LocationRegionMarker locationRegionMarker) {
                Toast.makeText(getApplication(), "(" + Double.toString(locationRegionMarker.locationRegion.getCenterLatitude()) + "," +
                        Double.toString(locationRegionMarker.locationRegion.getCenterLongitude()) + ")", Toast.LENGTH_SHORT).show();
            }
        });

        //set location region long click call back.
        mSailsMapView.setOnRegionLongClickListener(new SAILSMapView.OnRegionLongClickListener() {
            @Override
            public void onLongClick(List<LocationRegion> locationRegions) {
                if (mSails.isLocationEngineStarted())
                    return;
                mVibrator.vibrate(70);
                mSailsMapView.getMarkerManager().clear();
                mSailsMapView.getRoutingManager().setStartRegion(locationRegions.get(0));
                mSailsMapView.getMarkerManager().setLocationRegionMarker(locationRegions.get(0), Marker.boundCenter(getResources().getDrawable(R.drawable.arrow3)));
                latPoint = locationRegions.get(0).getCenterLatitude();
                lonPoint = locationRegions.get(0).getCenterLongitude();
            }
        });

        //design some action in floor change call back.
        mSailsMapView.setOnFloorChangedListener(new SAILSMapView.OnFloorChangedListener() {
            @Override
            public void onFloorChangedBefore(String floorName) {
                //get current map view zoom level.
                zoomSav = mSailsMapView.getMapViewPosition().getZoomLevel();
            }

            @Override
            public void onFloorChangedAfter(final String floorName) {
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        //check is locating engine is start and current brows map is in the locating floor or not.
                        if (mSails.isLocationEngineStarted() && mSailsMapView.isInLocationFloor()) {
                            //change map view zoom level with animation.
                            mSailsMapView.setAnimationToZoom(zoomSav);
                        }
                    }
                };
                new Handler().postDelayed(r, 1000);

                int position = 0;
                for (String mS : mSails.getFloorNameList()) {
                    if (mS.equals(floorName))
                        break;
                    position++;
                }
                floorList.setSelection(position);
            }
        });

        //design some action in mode change call back.
        mSailsMapView.setOnModeChangedListener(new SAILSMapView.OnModeChangedListener() {
            @Override
            public void onModeChanged(int mode) {
                if (((mode & SAILSMapView.LOCATION_CENTER_LOCK) == SAILSMapView.LOCATION_CENTER_LOCK) && ((mode & SAILSMapView.FOLLOW_PHONE_HEADING) == SAILSMapView.FOLLOW_PHONE_HEADING)) {
                    lockcenter.setImageDrawable(getResources().getDrawable(R.drawable.center3));
                } else if ((mode & SAILSMapView.LOCATION_CENTER_LOCK) == SAILSMapView.LOCATION_CENTER_LOCK) {
                    lockcenter.setImageDrawable(getResources().getDrawable(R.drawable.center2));
                } else {
                    lockcenter.setImageDrawable(getResources().getDrawable(R.drawable.center1));
                }
            }
        });

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, mSails.getFloorDescList());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        floorList.setAdapter(adapter);
        floorList.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!mSailsMapView.getCurrentBrowseFloorName().equals(mSails.getFloorNameList().get(position)))
                    mSailsMapView.loadFloorMap(mSails.getFloorNameList().get(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    void routingInitial() {
        mSailsMapView.getRoutingManager().setStartMakerDrawable(Marker.boundCenter(getResources().getDrawable(R.drawable.arrow3)));
        mSailsMapView.getRoutingManager().setTargetMakerDrawable(Marker.boundCenterBottom(getResources().getDrawable(R.drawable.map_destination)));
        mSailsMapView.getRoutingManager().setOnRoutingUpdateListener(new PathRoutingManager.OnRoutingUpdateListener() {
            @Override
            public void onArrived(LocationRegion targetRegion) {
                tts.speak("목적지에 도착하였습니다. 안내를 종료합니다", TextToSpeech.QUEUE_ADD, null);
                mSailsMapView.getRoutingManager().disableHandler();
                setDestination.setVisibility(View.VISIBLE);
            }

            @Override
            public void onRouteSuccess() {
                List<GeoPoint> gplist = mSailsMapView.getRoutingManager().getCurrentFloorRoutingPathNodes();
                mSailsMapView.autoSetMapZoomAndView(gplist);
                time = 1;
                des_time = 1;
            }

            @Override
            public void onRouteFail() {
                Toast.makeText(getApplication(), "Route Fail.", Toast.LENGTH_SHORT).show();
                mSailsMapView.getRoutingManager().disableHandler();
            }

            @Override
            public void onPathDrawFinish() {
            }

            @Override
            public void onTotalDistanceRefresh(int distance) {
                distance = distance * 230;
                result = distance / 60;
                setDestination.setVisibility(View.INVISIBLE);
                GetLocations();
                GetAngle();
                if(time != 0){
                    if(result != 0) {
                        tts.speak("목적지는 현재위치에서" + str_angle + "방향에 있습니다.", TextToSpeech.QUEUE_ADD, null);
                        tts.speak("목적지까지의 거리는 " + String.valueOf(result) + "걸음입니다", TextToSpeech.QUEUE_ADD, null);
                        time  = 0;
                    }
                }
                if(des_time != 0){
                    if(result <= 3){
                        tts.speak("목적지 부근입니다", TextToSpeech.QUEUE_ADD, null);
                        des_time = 0;
                    }
                }
            }

            @Override
            public void onReachNearestTransferDistanceRefresh(int distance, int nodeType) {
            }

            @Override
            public void onSwitchFloorInfoRefresh(List<PathRoutingManager.SwitchFloorInfo> infoList, int nearestIndex) {
            }
        });
    }


    void slidingMenuInitial() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //1st stage groups
                List<Map<String, String>> groups = new ArrayList<Map<String, String>>();
                //2nd stage groups
                List<List<Map<String, LocationRegion>>> childs = new ArrayList<List<Map<String, LocationRegion>>>();
                for (String mS : mSails.getFloorNameList()) {
                    Map<String, String> group_item = new HashMap<String, String>();
                    group_item.put("group", mSails.getFloorDescription(mS));
                    groups.add(group_item);

                    List<Map<String, LocationRegion>> child_items = new ArrayList<Map<String, LocationRegion>>();
                    for (LocationRegion mlr : mSails.getLocationRegionList(mS)) {
                        if (mlr.getName() == null || mlr.getName().length() == 0)
                            continue;

                        Map<String, LocationRegion> childData = new HashMap<String, LocationRegion>();
                        childData.put("child", mlr);
                        child_items.add(childData);
                    }
                    childs.add(child_items);
                }
                eAdapter = new ExpandableAdapter(getBaseContext(), groups, childs);
                expandableListView.setAdapter(eAdapter);
            }
        });
    }

    ExpandableListView.OnChildClickListener childClickListener = new ExpandableListView.OnChildClickListener() {
        @Override
        public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
            LocationRegion lr = eAdapter.childs.get(groupPosition).get(childPosition).get("child");
            mVibrator.vibrate(150);

            String name;
            boolean dd = Pattern.matches("[가-힝]", lr.label.substring(0,1));
            if(dd == true){
                name = lr.label;
                Toast.makeText(getApplication(),name + "입니다",Toast.LENGTH_SHORT).show();
                tts.speak(name + "입니다", TextToSpeech.QUEUE_ADD, null);
            } else{
                name = "a" + lr.label;
                String type = "string";
                String packageName = getPackageName();
                int ResId = getResources().getIdentifier(name, type, packageName);
                String res_name = getResources().getString(ResId);
                Toast.makeText(getApplicationContext(),res_name + "입니다",Toast.LENGTH_SHORT).show();
                tts.speak(res_name + "입니다", TextToSpeech.QUEUE_ADD, null);
            }

            if (!lr.getFloorName().equals(mSailsMapView.getCurrentBrowseFloorName())) {
                mSailsMapView.loadFloorMap(lr.getFloorName());
                mSailsMapView.getMapViewPosition().setZoomLevel((byte) 20);
                Toast.makeText(getBaseContext(), mSails.getFloorDescription(lr.getFloorName()), Toast.LENGTH_SHORT).show();
            }

            if (mSails.isLocationEngineStarted() && mSails.isInThisBuilding()) {
                //set routing start point to current user location.
                mSailsMapView.getRoutingManager().setStartRegion(PathRoutingManager.MY_LOCATION);
                latPoint = mSails.getLatitude();
                lonPoint = mSails.getLongitude();

                GeoPoint poi = new GeoPoint(lr.getCenterLatitude(), lr.getCenterLongitude());
                mSailsMapView.setAnimationMoveMapTo(poi);
                mSailsMapView.getMarkerManager().setLocationRegionMarker(lr, Marker.boundCenter(getResources().getDrawable(R.drawable.destination)));
                menu.showContent();
                latDest = lr.getCenterLatitude();
                lonDest = lr.getCenterLongitude();

                mSailsMapView.getRoutingManager().getPathPaint().setColor(0xFF35b3e5);
            } else {
                GeoPoint poi = new GeoPoint(lr.getCenterLatitude(), lr.getCenterLongitude());
                mSailsMapView.setAnimationMoveMapTo(poi);
                mSailsMapView.getMarkerManager().setLocationRegionMarker(lr, Marker.boundCenter(getResources().getDrawable(R.drawable.destination)));
                menu.showContent();
                latDest = lr.getCenterLatitude();
                lonDest = lr.getCenterLongitude();
                mSailsMapView.getRoutingManager().getPathPaint().setColor(0xFF85b038);
                if (mSailsMapView.getRoutingManager().getStartRegion() != null) { }
            }
            mSailsMapView.getRoutingManager().setTargetRegion(lr);
            if (mSailsMapView.getRoutingManager().enableHandler()){ }
            mSailsMapView.getMarkerManager().clear();
            return false;
        }
    };

    View.OnClickListener controlListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == zoomin) {
                //set map zoomin function.
                mSailsMapView.zoomIn();
            } else if (v == zoomout) {
                //set map zoomout function.
                mSailsMapView.zoomOut();
            } else if (v == lockcenter) {
                if (!mSails.isLocationFix() || !mSails.isLocationEngineStarted()) {
                    Toast t = Toast.makeText(getBaseContext(), "Location Not Found or Location Engine Turn Off.", Toast.LENGTH_SHORT);
                    t.show();
                    return;
                }
                if (!mSailsMapView.isCenterLock() && !mSailsMapView.isInLocationFloor()) {
                    //set the map that currently location engine recognize.
                    mSailsMapView.loadCurrentLocationFloorMap();

                    Toast t = Toast.makeText(getBaseContext(), "Go Back to Locating Floor First.", Toast.LENGTH_SHORT);
                    t.show();
                    return;
                }
                //set map mode.
                //FOLLOW_PHONE_HEADING: the map follows the phone's heading.
                //LOCATION_CENTER_LOCK: the map locks the current location in the center of map.
                //ALWAYS_LOCK_MAP: the map will keep the mode even user moves the map.
                if (mSailsMapView.isCenterLock()) {
                    if ((mSailsMapView.getMode() & SAILSMapView.FOLLOW_PHONE_HEADING) == SAILSMapView.FOLLOW_PHONE_HEADING)
                        //if map control mode is follow phone heading, then set mode to location center lock when button click.
                        mSailsMapView.setMode(mSailsMapView.getMode() & ~SAILSMapView.FOLLOW_PHONE_HEADING);
                    else
                        //if map control mode is location center lock, then set mode to follow phone heading when button click.
                        mSailsMapView.setMode(mSailsMapView.getMode() | SAILSMapView.FOLLOW_PHONE_HEADING);
                } else {
                    //if map control mode is none, then set mode to loction center lock when button click.
                    mSailsMapView.setMode(mSailsMapView.getMode() | SAILSMapView.LOCATION_CENTER_LOCK);
                }
            } else if(v == setDestination){
                menu.showMenu();
            } else if(v == buildingInfo){
                Intent intent=new Intent(MainActivity.this, BuildingActivity.class);
                startActivity(intent);
            } else if(v == use){
                new AlertDialog.Builder(MainActivity.this)
                        .setMessage("사용방법 설명 \n현재 위치 기능은 사용자의 현재 위치정보를 안내합니다.\n" +
                                "현재 위치에서 목적지까지의 경로 안내 시 제공되는 방향은 사용자가 서있는 자세에서 정면이 12시, 오른쪽이 3시, 왼쪽이 9시, 뒤가 6시 방향으로 안내됩니다.")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .show();
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case R.id.start_location_engine:
                if (!mSails.isLocationEngineStarted()) {
                    mSails.startLocatingEngine();
                    mSailsMapView.setLocatorMarkerVisible(true);
                    Toast.makeText(this, "Start Location Engine", Toast.LENGTH_SHORT).show();
                    mSailsMapView.setMode(SAILSMapView.LOCATION_CENTER_LOCK | SAILSMapView.FOLLOW_PHONE_HEADING);
                    lockcenter.setVisibility(View.VISIBLE);
                }
                return true;

            case R.id.stop_location_engine:
                if (mSails.isLocationEngineStarted()) {
                    mSails.stopLocatingEngine();
                    mSailsMapView.setLocatorMarkerVisible(false);
                    mSailsMapView.setMode(SAILSMapView.GENERAL);
                    mSailsMapView.getRoutingManager().disableHandler();
                    Toast.makeText(this, "Stop Location Engine", Toast.LENGTH_SHORT).show();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSailsMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSailsMapView.onPause();
    }

    @Override
    public void onBackPressed() {
        if(mSailsMapView.getRoutingManager().enableHandler()) {
            mSailsMapView.getRoutingManager().disableHandler();
            setDestination.setVisibility(View.VISIBLE);
            return;
        } else if(menu.isMenuShowing() || menu.isSecondaryMenuShowing()) {
            new AlertDialog.Builder(this)
                    .setMessage("목적지 지정 중입니다\n어플리케이션을 종료하시겠습니까?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();
        }
        else {
            new AlertDialog.Builder(this)
                    .setMessage("어플리케이션을 종료하시겠습니까?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();
        }
    }

    class ExpandableAdapter extends BaseExpandableListAdapter {
        private Context context;
        List<Map<String, String>> groups;
        List<List<Map<String, LocationRegion>>> childs;

        public ExpandableAdapter(Context context, List<Map<String, String>> groups, List<List<Map<String, LocationRegion>>> childs) {
            this.context = context;
            this.groups = groups;
            this.childs = childs;
        }

        @Override
        public int getGroupCount() {
            return groups.size();
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            return childs.get(groupPosition).size();
        }

        @Override
        public Object getGroup(int groupPosition) {
            return groups.get(groupPosition);
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            return childs.get(groupPosition).get(childPosition);
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            LinearLayout linearLayout = (LinearLayout) layoutInflater.inflate(R.layout.group, null);
            String text = ((Map<String, String>) getGroup(groupPosition)).get("group");
            TextView tv = (TextView) linearLayout.findViewById(R.id.group_tv);
            tv.setText(text);
            linearLayout.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_dark));
            tv.setTextColor(getResources().getColor(android.R.color.white));
            return linearLayout;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            LinearLayout linearLayout = (LinearLayout) layoutInflater.inflate(R.layout.child, null);
            LocationRegion lr = ((Map<String, LocationRegion>) getChild(groupPosition, childPosition)).get("child");
            TextView tv = (TextView) linearLayout.findViewById(R.id.child_tv);
            tv.setText(lr.getName());
            ImageView imageView = (ImageView) linearLayout.findViewById(R.id.child_iv);
            imageView.setImageResource(R.drawable.expand_item);
            return linearLayout;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }
    }

    public void GetLocations() {
        //거리 구하기
        //float[] dist = new float[3];
        //Location.distanceBetween(latPoint, lonPoint , latDest, lonDest, dist);
        //distance = dist[0] / 1000;
        //distance = Float.parseFloat(String.format("%.3f", distance));

        //각도 구하기
        Cur_Lat_radian = latPoint * (3.141592 / 180);
        Cur_Lon_radian = lonPoint * (3.141592 / 180);
        Dest_Lat_radian = latDest * (3.141592 / 180);
        Dest_Lon_radian = lonDest * (3.141592 / 180);

        radian_distance = Math.acos(Math.sin(Cur_Lat_radian) * Math.sin(Dest_Lat_radian) + Math.cos(Cur_Lat_radian) * Math.cos(Dest_Lat_radian) * Math.cos(Cur_Lon_radian - Dest_Lon_radian));
        radian_bearing = Math.acos((Math.sin(Dest_Lat_radian) - Math.sin(Cur_Lat_radian) * Math.cos(radian_distance)) / (Math.cos(Cur_Lat_radian) * Math.sin(radian_distance)));
        if (Math.sin(Dest_Lon_radian - Cur_Lon_radian) < 0) {
            true_bearing = radian_bearing * (180 / 3.141592);
            true_bearing = (360 - true_bearing);
        } else {
            true_bearing = (radian_bearing * (180 / 3.141592));
        }

    }

    public void GetAngle(){
        if(true_bearing >= t_angle && t_angle != 0) {
            angle_result = true_bearing - t_angle;
            if (angle_result >= 15 && angle_result < 45)
                str_angle = "1시";
            else if (angle_result >= 45 && angle_result < 75)
                str_angle = "2시";
            else if (angle_result >= 75 && angle_result < 105)
                str_angle = "3시";
            else if (angle_result >= 105 && angle_result < 135)
                str_angle = "4시";
            else if (angle_result >= 135 && angle_result < 165)
                str_angle = "5시";
            else if (angle_result >= 165 && angle_result < 195)
                str_angle = "6시";
            else if (angle_result >= 195 && angle_result < 225)
                str_angle = "7시";
            else if (angle_result >= 225 && angle_result < 255)
                str_angle = "8시";
            else if (angle_result >= 255 && angle_result < 285)
                str_angle = "9시";
            else if (angle_result >= 285 && angle_result < 315)
                str_angle = "10시";
            else if (angle_result >= 315 && angle_result < 345)
                str_angle = "11시";
            else
                str_angle = "12시";
        }
        else if(true_bearing < t_angle && t_angle != 0) {
            angle_result = t_angle - true_bearing;
            if (angle_result >= 15 && angle_result < 45)
                str_angle = "11시";
            else if (angle_result >= 45 && angle_result < 75)
                str_angle = "10시";
            else if (angle_result >= 75 && angle_result < 105)
                str_angle = "9시";
            else if (angle_result >= 105 && angle_result < 135)
                str_angle = "8시";
            else if (angle_result >= 135 && angle_result < 165)
                str_angle = "7시";
            else if (angle_result >= 165 && angle_result < 195)
                str_angle = "6시";
            else if (angle_result >= 195 && angle_result < 225)
                str_angle = "5시";
            else if (angle_result >= 225 && angle_result < 255)
                str_angle = "4시";
            else if (angle_result >= 255 && angle_result < 285)
                str_angle = "3시";
            else if (angle_result >= 285 && angle_result < 315)
                str_angle = "2시";
            else if (angle_result >= 315 && angle_result < 345)
                str_angle = "1시";
            else
                str_angle = "12시";
        }
    }

    // Takes GML document and graph's layer and returns the graph
    // For the information about the document format see the indoorGML standard
    private HashMap<String, Polygon> creatRooms(Document document, int level) {
        //multiLayeredGraph
        Element multiLayeredGraph = (Element) document.getElementsByTagName(Parameters.GML_MLG).item(0);
        Element spaceLayers = (Element) multiLayeredGraph.getElementsByTagName(Parameters.GML_SLS).item(0);
        NodeList spaceLayersMembers = spaceLayers.getElementsByTagName(Parameters.GML_SLM);
        Element roomSpaceLayerMember = (Element) spaceLayersMembers.item(level);
        Element roomSpaceLayer = (Element) roomSpaceLayerMember.getElementsByTagName(Parameters.GML_SL).item(0);
        //primalSpaceFeatures
        Element primalSpaceFeatures = (Element) document.getElementsByTagName(Parameters.GML_PSF).item(0);
        NodeList cellSpace = primalSpaceFeatures.getElementsByTagName(Parameters.GML_CSM);
        // get nodes
        Element roomNodes = (Element) roomSpaceLayer.getElementsByTagName(Parameters.GML_NODES).item(0);
        // get edges
        Element roomEdges = (Element) roomSpaceLayer.getElementsByTagName(Parameters.GML_EDGES).item(0);
        // get stateMember and get informations
        NodeList roomStateMembers = roomNodes.getElementsByTagName(Parameters.GML_STMEMB);
        HashMap<String, Polygon> stateMap = new HashMap<>();
        // define temp vars and get state's informations
        Element stateMemb, state, gmlGeom, gmlPoint, cellMemb, cell, cellGeom, cellGeom2D, gmlPolygon, tempPos;
        Element gmlExterior, gmlLinearRing;
        String id, label, tempStr;
        stateNum = roomStateMembers.getLength();
        for (int i = 0; i < roomStateMembers.getLength(); i++) {
            Polygon myPoly = new Polygon(stateNum);
            stateMemb = (Element) roomStateMembers.item(i);
            cellMemb = (Element) cellSpace.item(i);
            state = (Element) stateMemb.getElementsByTagName(Parameters.GML_STATE).item(0);
            cell = (Element) cellMemb.getElementsByTagName(Parameters.GML_CS).item(0);
            cellGeom = (Element) cell.getElementsByTagName(Parameters.GML_CGEOM).item(0);
            cellGeom2D = (Element) cellGeom.getElementsByTagName(Parameters.GML_GEOM2D).item(0);
            gmlPolygon = (Element) cellGeom2D.getElementsByTagName(Parameters.GML_POL).item(0);
            // get id
            id = state.getAttribute(Parameters.GML_ID);
            // get label
            label = cell.getElementsByTagName(Parameters.GML_NAME).item(0).getFirstChild().getTextContent().trim();
            // get exterior position
            NodeList posSet = gmlPolygon.getElementsByTagName(Parameters.GML_POS);
            float[] axisX = new float[posSet.getLength()];
            float[] axisY = new float[posSet.getLength()];
            for (int j = 0; j < posSet.getLength(); j++) {
                tempPos=(Element) posSet.item(j);
                tempStr=posSet.item(j).getTextContent();
                tempStr=tempStr.trim();
                axisX[j]=(float) Double.parseDouble(tempStr.split(" ")[0]);
                axisY[j]=(float) Double.parseDouble(tempStr.split(" ")[1]);
                myPoly.addPoint(axisX[j], axisY[j]);
                //System.out.println(j+"번째 점:"+label+ "=("+ axisX[j] + ", "+ axisY[j]+")");
            }
            // add room to the list
            Polygon newPol = new Polygon(myPoly.xpoints, myPoly.ypoints, myPoly.npoints);
            stateMap.put(label, newPol);
            //System.out.println("id: "+ id + ", label: " + label + ", x: " + axisX + ", y: " + axisY);
        }
        // construct the Graph
        //return new MapGraph(stateMap, transList);
        return stateMap;
    }

}
