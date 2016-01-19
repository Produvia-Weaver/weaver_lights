/**************************************************************************************************
 * Copyright (c) 2016-present, Produvia, LTD.
 * All rights reserved.
 * This source code is licensed under the MIT license
 **************************************************************************************************/
package produvia.com.lights;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.produvia.sdk.DateTimeFormatterEx;
import com.produvia.sdk.WeaverSdk;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;

import produvia.com.weaverandroidsdk.WeaverSdkApi;


/**
 * An activity representing a list of lighting services. This activity
 * is the main activity - it first scans for local services
 * and fetches the scanned services from the SDK
 * The activity displays the list in a fragment containing a RecyclerView
 */
public class SmartLightsActivity extends Activity implements SmartLightsFragment.Callbacks, WeaverSdk.WeaverSdkCallback{


    private static final boolean RUN_DISCOVERY = true;
    private static final int MAX_SCAN_CYCLES = 3;
    //let's not show services we haven't seen in two weeks:
    private static final long MAX_TIME_SINCE_LAST_SEEN_IN_MILLIS_TO_BE_CONSIDERED_ONLINE = (1000*60*60*24*14);
    public static boolean mErrorOccurred = false;
    public static String mErrorMessage = "";
    private static int mScanCycleCounter = 0;

    private boolean mActivityPaused = true;

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private SmartLightsFragment listFragment = null;


    private CustomRecyclerAdapter mCategoryListAdapter;
    public static ArrayList<CustomListItem> mIotLightServices = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mErrorOccurred = false;
        mErrorMessage = "";
        setContentView(R.layout.activity_smart_lights);

        listFragment = new SmartLightsFragment();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.main_fragment, listFragment);
        transaction.addToBackStack(null);

        // Commit the transaction
        transaction.commit();
        //Start running the discovery service in the background
        //any discovered services will be reported on the onTaskUpdate callback:
        if( RUN_DISCOVERY ) {
            WeaverSdkApi.discoveryService(this, true);
        }

        //fetch the services that have already been discovered in previous scans
        //these services will be returned in the onTaskCompleted callback:

        WeaverSdkApi.servicesGet(this, null);
    }


    @Override
    protected void onResume() {
        super.onResume();
        mActivityPaused = false;
    }

    @Override
    protected void onPause() {
        mActivityPaused = true;
        super.onPause();
    }

    /**
     * Callback method from {@link SmartLightsFragment.Callbacks}
     * indicating that the item with the given ID was selected.
     */
    @Override
    public void onItemSelected(CustomListItem item) {

        getFragmentManager().beginTransaction()
                .replace(R.id.main_fragment, new ColorSchemeFragment())
                .commit();
    }

    @Override
    public void onViewCreated(CustomRecyclerAdapter adapter) {
        mCategoryListAdapter = adapter;
        if (mCategoryListAdapter == null)
            return;
        mCategoryListAdapter.notifyDataSetChanged();
    }


    /******************************************************************
     * Starts up the LoginActivity - called on user sign out
     *****************************************************************/
    protected void runWelcomeActivity() {
        WeaverSdkApi.discoveryService(this, false);
        Intent intent = new Intent(SmartLightsActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }


    /*********************************************************************
     * The WeaverSdk callback indicating that a task has been completed:
     *********************************************************************/
    @Override
    public void onTaskCompleted(final int flag, final JSONObject response) {
        if (response== null || mActivityPaused)
            return;
        try {

            if (response.has("responseCode") && response.getInt("responseCode") == 401) {
                //unauthorized:
                runWelcomeActivity();
            }

            switch (flag) {
                case WeaverSdk.ACTION_USER_LOGOUT:
                    runWelcomeActivity();
                    break;

                case WeaverSdk.ACTION_SERVICES_GET:
                    if (response.getBoolean("success")) {
                        handleReceivedServices(response.getJSONObject("data"));
                    }
                    break;
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /*********************************************************************
     * The WeaverSdk callback indicating that a task update occurred.
     * for example, a new service was discovered in the current network:
     *********************************************************************/
    @Override
    public void onTaskUpdate(int flag, JSONObject response) {
        if(response == null || mActivityPaused)
            return;
        try {

            //this flag indicates that a new service was discovered in the scan:
            if (flag == WeaverSdk.ACTION_SERVICES_SCAN) {
                if (response.getBoolean("success")) {
                    handleReceivedServices(response.getJSONObject("data"));
                }
            }
            //when tha scan is running - it'll provide general state information from time to time:
            else if (flag == WeaverSdk.ACTION_SCAN_STATUS) {
                if (response.getBoolean("success")) {
                    if(response.getString("info").equals("Scan running")){
                        showScanProgress(true);
                    }else{

                        showScanProgress(false);
                        //if we haven't found any light services - we'll show an error message:
                        //if we finished the scan - check if we found any devices:
                        if (mScanCycleCounter > 0 && (mIotLightServices== null || mIotLightServices.size() <= 0)) {
                            setErrorMessage("Weaver didn't detect any smart lights devices\nPlease make sure the light devices are connected\nand restart Weaver Lights");
                            //stop the scan:
                            WeaverSdkApi.discoveryService( null, false );
                            return;
                        }
                        //stop the discovery service after max scan cycles:
                        if(mScanCycleCounter >= MAX_SCAN_CYCLES)
                            WeaverSdkApi.discoveryService( null, false );
                        mScanCycleCounter += 1;

                    }
                }
            }
        }catch (JSONException e) {
            e.printStackTrace();
        }


    }


    private void handleReceivedServices(JSONObject data) throws JSONException {

        JSONArray services = data.getJSONArray("services");
        for (int i = 0; i < services.length(); i++) {
            JSONObject service = services.getJSONObject(i);
            String service_type = service.getString("service");

            //if the services haven't been seen in a while - we won't display them anymore:
            Calendar last_seen = DateTimeFormatterEx.getCalendarFromISO(service.getString("last_seen"));
            long difference = Calendar.getInstance().getTimeInMillis() - last_seen.getTimeInMillis();
            if(difference > MAX_TIME_SINCE_LAST_SEEN_IN_MILLIS_TO_BE_CONSIDERED_ONLINE)
                continue;




            if (service_type.equals("login")) {
                //prompt login:
                promptLogin(service, data);
            }
            else if(service_type.startsWith("_light" )){//_light_color|| _light_dimmer || _light
                //yey! got a light_color service - let's add it to the list:
                addLightService(service,
                        data.getJSONObject("devices_info").getJSONObject(service.getString("device_id")),
                                data.getJSONObject("networks_info").getJSONObject(service.getString("network_id")));
            }


        }


    }



    private synchronized void addLightService(JSONObject service, JSONObject device, JSONObject network) throws JSONException {

        //if the service is local but the user isn't inside the network - they can't use  it
        // so we won't add it to the list.
        //if it's a global service - it can be used from anywhere and will be added:
        boolean is_user_inside_network = network.getBoolean("user_inside_network");
        boolean is_global = LightService.isGlobal(service);
        if( !is_user_inside_network && !is_global )
            return;
        //also if this is a global service that has an identical local service and the user is inside
        //the network - we'll prefer the local service:
        //make sure the service isn't already in the list:
        for(int i = 0; i < mIotLightServices.size(); i++ ) {
            if(mIotLightServices.get(i) instanceof LightService) {
                LightService lcs = (LightService) mIotLightServices.get(i);
                if(lcs.equals(service, true)) {
                    //if the services are equal we'll just update the service
                    //if one service is local and the other is global - we'll prefer the local:
                    if( lcs.isGlobal() || (lcs.isGlobal() == is_global)  ) {
                        //replace the services:
                        lcs.update(service, device, network);
                        notifyDataSetChanged();
                    }
                    return;
                }
            }
        }

        //fill the list:
        String network_name = network.getString("name").replaceAll("\"", "");
        boolean found_lights_in_network = (mIotLightServices.size()  > 0);

        if (!found_lights_in_network) {

            CustomListItem cli = new CustomListItem(network_name, "Lights at " + network_name,
                    R.drawable.produvia_man_home, true, false);

            mIotLightServices.add(cli);
            CustomListItem master_switch = new CustomListItem(" Master Switch",
                    "Click to select a lighting theme", R.drawable.ic_master, false, true);
            master_switch.setColorPickerEnabled(true);
            mIotLightServices.add(master_switch);
        }

        mIotLightServices.add(new LightService(service,device,network));

        notifyDataSetChanged();

    }

    private void notifyDataSetChanged() {
        if (mCategoryListAdapter == null)
            return;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mCategoryListAdapter.notifyDataSetChanged();
            }
        });
    }


    protected void showScanProgress(final boolean progress) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                View pbarLayout = findViewById(R.id.discovery_progress_layout);
                if(pbarLayout == null)
                    return;

                if(progress) {
                    if (pbarLayout.getVisibility() == View.GONE)
                        pbarLayout.setVisibility(View.VISIBLE);
                } else {
                    if (pbarLayout.getVisibility() != View.GONE)
                        pbarLayout.setVisibility(View.GONE);
                }
            }
        });
    }



    public void promptLogin(final JSONObject loginService, final JSONObject responseData) {

        runOnUiThread(new Runnable() {
            public void run() {
                try {
                    String type = loginService.getString("type");
                    //there was a login error. login again
                    if (type.equals(WeaverSdk.FIRST_LOGIN_TYPE_NORMAL)) {
                        //prompt for username and password and retry:
                        promptUsernamePassword(loginService, responseData, false, null);

                    } else if(type.equals(WeaverSdk.FIRST_LOGIN_TYPE_KEY)) {

                        promptUsernamePassword(loginService, responseData, true, loginService.getString("description"));


                    }
                    else if (type.equals(WeaverSdk.FIRST_LOGIN_TYPE_PRESS2LOGIN)) {
                        //prompt for username and password and retry:
                        int countdown = loginService.has("login_timeout")? loginService.getInt("login_timeout"): 15;
                        final AlertDialog alertDialog = new AlertDialog.Builder(SmartLightsActivity.this).create();
                        alertDialog.setTitle(loginService.getString("description"));
                        alertDialog.setCancelable(false);
                        alertDialog.setCanceledOnTouchOutside(false);
                        alertDialog.setMessage(loginService.getString("description") + "\n" + "Attempting to login again in " + countdown + " seconds...");
                        alertDialog.show();   //

                        new CountDownTimer(countdown * 1000, 1000) {
                            @Override
                            public void onTick(long millisUntilFinished) {
                                try {
                                    alertDialog.setMessage(loginService.getString("description") + "\n" + "Attempting to login again in " + millisUntilFinished / 1000 + " seconds...");
                                } catch (JSONException e) {

                                }
                            }

                            @Override
                            public void onFinish() {
                                alertDialog.dismiss();
                                new Thread(new Runnable() {
                                    public void run() {

                                        try {
                                            JSONArray services = new JSONArray();
                                            services.put(loginService);
                                            responseData.put("services", services);
                                            WeaverSdkApi.servicesSet(SmartLightsActivity.this, responseData);
                                        }catch (JSONException e){

                                        }
                                    }
                                }).start();

                            }
                        }.start();

                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }

        });

    }



    public void promptUsernamePassword(final JSONObject loginService,
                                       final JSONObject responseData,
                                       final boolean isKey,
                                       String description ) throws JSONException {

        LayoutInflater li = LayoutInflater.from(SmartLightsActivity.this);
        View promptsView = li.inflate(R.layout.prompt_userpass, null);
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(SmartLightsActivity.this);
        alertDialogBuilder.setView(promptsView);

        final EditText userInput = (EditText) promptsView.findViewById(R.id.pu_username);
        final EditText passInput = (EditText) promptsView.findViewById(R.id.pu_password);
        //if it's a key type input hide the password field:
        if(isKey) {
            passInput.setVisibility(View.GONE);
            userInput.setText(loginService.getJSONObject("properties").getString("key"));
            userInput.setHint("Enter key");
        } else{
            userInput.setText(loginService.getJSONObject("properties").getString("username"));
            passInput.setText(loginService.getJSONObject("properties").getString("password"));
        }


        final TextView prompt_user_pass = (TextView)promptsView.findViewById(R.id.user_pass_title);


        String name = responseData.getJSONObject("devices_info").getJSONObject(loginService.getString("device_id")).getString("name");

        String message;
        if(description == null) {
            message = "Enter " +
                    name + "'s username and password.";
        }else{
            message = description;
        }
        message += "\n(if it's disconnected just press cancel)";

        prompt_user_pass.setText(message);

        // set dialog message
        alertDialogBuilder.setCancelable(false).setNegativeButton("Go", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                String username = (userInput.getText()).toString();
                String password = (passInput.getText()).toString();
                try {
                    if (isKey) {
                        loginService.getJSONObject("properties").put("key", username);

                    } else {
                        loginService.getJSONObject("properties").put("username", username);
                        loginService.getJSONObject("properties").put("password", password);
                    }
                    //stick the service into the response data structure and set the service:
                    JSONArray services = new JSONArray();
                    services.put(loginService);
                    responseData.put("services", services);
                    WeaverSdkApi.servicesSet(SmartLightsActivity.this, responseData);
                } catch (JSONException e) {
                }

            }
        }).setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();

    }

    public void setErrorMessage(String error){
        mErrorOccurred = true;
        mErrorMessage = error;
        if (listFragment != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    listFragment.showError();
                }
            });
        }
    }

}
