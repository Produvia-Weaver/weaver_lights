package produvia.com.weaverandroidsdk;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.telephony.TelephonyManager;

import com.produvia.sdk.WeaverSdk;

import org.json.JSONException;
import org.json.JSONObject;

public class WeaverSdkApi {




    private static SharedPreferences mSharedPreferences;

    /**
	 * name: init
	 * description:  initializes the sdk
 	 * @param callback - callback
	 * @param api_key - the api key
	 * @param context - the application context
	 */
	public static void init(final WeaverSdk.WeaverSdkCallback callback, String api_key, Context context) {

		mSharedPreferences = context.getSharedPreferences("CurrentUser", Activity.MODE_PRIVATE);
        String authentication_token = context.getSharedPreferences("CurrentUser", Activity.MODE_PRIVATE).getString("AuthToken", "");
        WeaverSdk.init(api_key, authentication_token, 1);
        //setup the device parameters:
        setupDeviceFields(context);
        //setup network:
        NetworkReceiver.setConnectionStatus(context, true);
        if (WeaverSdk.isNetworkConnected())
            setLocation(context);


        if(authentication_token.isEmpty()){
            String json_str = "{ \"success\": false, \"info\": \"User not logged in\"}";
            try {
                callback.onTaskCompleted(WeaverSdk.ACTION_INITIALIZE, new JSONObject(json_str));
            } catch (JSONException e) {}
        }
        else //verify the auth token is valid:
            verifyAuthToken(callback, WeaverSdk.ACTION_INITIALIZE);
    }

	/**
	 * name: discoveryService
	 * description: starts and stops service discovery in the current network.
	 * 				Once started, the task keeps monitoring periodically for services.
	 * 				When stopped - the scanning is paused
	 * @param callback - the callback is triggered whenever a new service is discovered
	 * @param start    - true - starts the scan (if the scan is running - it adds another callback
	 *                          to be triggered when new services are discovered
	 *                 - false = stops the scan and unregisters the callbacks
	 */
	public static void discoveryService(WeaverSdk.WeaverSdkCallback callback, boolean start) {
		if (start) {
			WeaverSdk.startScan(callback);
		} else
			WeaverSdk.stopScan();
	}

	/**
	 * name: isDiscoveryRunning
	 * description: returns true if discovery is running, false if not
	 * @return
	 */
	public static boolean isDiscoveryRunning(){
		return WeaverSdk.isDiscoveryRunning();
	}




	/**
	 * name: setServices
	 * description: sets services
	 *
	 * @param callback - WeaverSdk callback - called when service is set
	 * @param modifiedServices - the modified service JSONS
	 * @throws JSONException
	 */
	public static void servicesSet(WeaverSdk.WeaverSdkCallback callback, JSONObject modifiedServices) throws JSONException {
		WeaverSdk.setServices(callback, modifiedServices);
	}


    /**
     * name: servicesGet
     * description: calls the callback with a JSON structure specifying the services in "network_id"
     * @param callback - the callback
     * @param network_id - the network id or - if the network id is null - weaver will attempt to
     *                     guess the current network and return it's services
     */
    public static void servicesGet(WeaverSdk.WeaverSdkCallback callback, String network_id) {
        WeaverSdk.servicesGet(network_id, callback);
    }


	public static void register(WeaverSdk.WeaverSdkCallback callback, String email, String username, String password, String passwordConfirmation){
        AndroidApiCallback loginCallback = new AndroidApiCallback(callback, null);
		WeaverSdk.register(email, username, password, passwordConfirmation, loginCallback);
	}


    /**
     * name: userLogin
     * description: logs a user in and returns the authentication token via the callback
     * @param callback - the callback that returns the authentication token
     * @param email - the user's email
     * @param password - the user's password
     */
	public static void userLogin(WeaverSdk.WeaverSdkCallback callback, String email, String password) {
		AndroidApiCallback loginCallback = new AndroidApiCallback(callback, null);
		WeaverSdk.login(email, password, loginCallback);
	}

    /**
     * name: userLogout
     * description: logs out the user
     * @param callback - called after the user is logged out
     */
	public static void userLogout(WeaverSdk.WeaverSdkCallback callback) {
        deleteSharedPreferences();
        WeaverSdk.doAction(WeaverSdk.ACTION_USER_LOGOUT, null, null, null, null, callback);
	}

    /*****************************************************
     * PRIVATE FUNCTIONS:
     *****************************************************/
	private static void setupDeviceFields(Context context) {
		String deviceType = "Mobile";

		BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        String name = null;
        if( mBluetoothAdapter != null){
            mBluetoothAdapter.getName();
            if (name == null) {
                name = mBluetoothAdapter.getAddress();
            }
        }
        if(name == null)
            name = Build.MANUFACTURER +" " + Build.MODEL;

		TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		if (manager.getPhoneType() == TelephonyManager.PHONE_TYPE_NONE)
			deviceType = "Tablet";


		WeaverSdk.initDeviceParams(
				name,
				NetworkReceiver.getMacAddress(context),
				deviceType,
				Build.MODEL,
				Build.MANUFACTURER,
				System.getProperty("os.version"),
				Build.SERIAL);


	}


	private static void setLocation(Context context) {
		LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		Location lastLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		if (lastLocation != null)
			WeaverSdk.setCurrentLocation(lastLocation.getLatitude(), lastLocation.getLongitude(), 0L);
	}

	public static class AndroidApiCallback implements WeaverSdk.WeaverSdkCallback {
		private WeaverSdk.WeaverSdkCallback mOriginalCallback;
        private Integer mOverrideFlag;

        public AndroidApiCallback(WeaverSdk.WeaverSdkCallback originalCallback, Integer override_flag){
            mOriginalCallback = originalCallback;
            mOverrideFlag = override_flag;
        }
		@Override
		public void onTaskCompleted(int flag, JSONObject json) {
            try {

                if (!json.has("success"))
                    json.put("success", false);

                if (json.getBoolean("success")) {
                    SharedPreferences.Editor editor = mSharedPreferences.edit();
                    JSONObject data = json.getJSONObject("data");
                    editor.putString("AuthToken", data.getString("auth_token"));
                    if (data.has("email"))
                        editor.putString("UserEmail", data.getString("email"));
                    if (data.has("username"))
                        editor.putString("Username", data.getString("username"));
                    if (data.has("omniauth_user_id"))
                        editor.putString("OmniId", data.getString("omniauth_user_id"));
                    editor.commit();
                }
                else{
                    if(json.has("responseCode") && json.getInt("responseCode") == 401)
                        deleteSharedPreferences();
                }
            } catch (Exception e) {
            }

			if(mOriginalCallback != null) {
                if(mOverrideFlag != null)
                    flag = mOverrideFlag;
                mOriginalCallback.onTaskCompleted(flag, json);
            }

		}

		@Override
		public void onTaskUpdate(int flag, JSONObject data) {
			if(mOriginalCallback != null)
				mOriginalCallback.onTaskUpdate(flag, data);
		}
	}

    private static void deleteSharedPreferences() {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.remove("AuthToken");
        editor.commit();
    }


    private static void verifyAuthToken(WeaverSdk.WeaverSdkCallback callback, int flag){
        AndroidApiCallback loginCallback = new AndroidApiCallback(callback, flag);
        WeaverSdk.login(null, null, loginCallback);

    }



}


