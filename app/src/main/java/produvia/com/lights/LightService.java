/**************************************************************************************************
 * Copyright (c) 2016-present, Produvia, LTD.
 * All rights reserved.
 * This source code is licensed under the MIT license
 **************************************************************************************************/
package produvia.com.lights;

import com.produvia.sdk.ColorConverter;
import com.produvia.sdk.WeaverSdk;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import produvia.com.weaverandroidsdk.WeaverSdkApi;


public class LightService extends CustomListItem implements WeaverSdk.WeaverSdkCallback {

    JSONObject mService;
    JSONObject mDevice;
    JSONObject mNetwork;

    public static String extractName(JSONObject service, JSONObject device) throws JSONException {
        String name = service.getJSONObject("properties").getString("name");

        if(device.has("iot_device"))
            name += " (" + device.getString("iot_device").toLowerCase() + ")";
        return name;
    }

    public LightService(JSONObject service, JSONObject device, JSONObject network) throws JSONException {
        super(extractName(service, device),
                "",
                R.drawable.ic_light_bulb,
                false,
                true);
        update(service, device, network);

    }

    public boolean isLightDimmer(){

        return getServiceType().startsWith("_light_dimmer");
    }

    public boolean isLightColor(){
        return getServiceType().startsWith("_light_color");
    }


    /****************************************************************************************
     * update the service:
     ****************************************************************************************/
    public void update(JSONObject service, JSONObject device, JSONObject network) throws JSONException {
        setName(extractName(service,device));

        mService = service;
        mDevice = device;
        mNetwork = network;
        setDescription(getLocalGlobal());
        setLeftImage(getIcon());

        JSONObject properties = mService.getJSONObject("properties");

        //update the properties accordingly - if it's a light dimmer/light color/light
        if(isLightDimmer()) {
            JSONObject dimmer = properties.getJSONObject("dimmer");
            float level = (float)dimmer.getDouble("level");
            Integer rgb = ColorConverter.hsbToRgb((float) 360,
                    (float) 1,
                    level);
            setColor(rgb);
            setNewColor(rgb);
            setColorPickerEnabled(false);
            setDimmerEnabled(true);
        } else if(isLightColor()){

            JSONObject color = properties.getJSONObject("color");
            Integer rgb = ColorConverter.hsbToRgb((float) color.getDouble("hue"),
                    (float) color.getDouble("sat"),
                    (float) color.getDouble("bri"));
            setColor(rgb);
            setNewColor(rgb);


            setColorPickerEnabled(true);
            setDimmerEnabled(false);
        } else{ //just an on off device:
            setColorPickerEnabled(false);
            setDimmerEnabled(false);
        }

    }

    public String getId() {
        return getId(mService);
    }
    private static String getId(JSONObject service) {
        try {
            return service.getString("id");
        }catch (JSONException e){
            return "";
        }
    }

    private static String getServiceType(JSONObject service){
        try {
            return service.getString("service");
        }catch (JSONException e){
            return "";
        }

    }


    public String getServiceType(){
        return getServiceType(mService);
    }


    public String getDeviceId() {
        return getDeviceId(mService);
    }
    private static String getDeviceId(JSONObject service) {
        try {
            return service.getString("device_id");
        }catch (JSONException e){
            return "";
        }
    }

    public String getNetworkId() {
        return getNetworkId(mService);
    }
    private static String getNetworkId(JSONObject service) {
        try {
            return service.getString("network_id");
        }catch (JSONException e){
            return "";
        }
    }


    public String getLocalGlobal() throws JSONException {
        return isGlobal()? "cloud": "local";
    }


    public static Boolean isGlobal(JSONObject service) {
        try {
            return service.getString("service").endsWith(".global");
        }catch (JSONException e ){return false;}

    }

    public boolean isGlobal(){
        return isGlobal(mService);
    }


    public Integer getIcon() {
        return R.drawable.ic_light_bulb;
    }



    public JSONObject getServiceJson(){
        return mService;
    }


    //change the color:
    @Override
    public void onColorChanged(boolean commitChanges) {
        int color = getColor();
        onColorChanged(ColorConverter.rgbToHsb(((color >> 16) & 0xff), ((color >> 8) & 0xff), ((color) & 0xff)), commitChanges);

    }

    //toggle the power:
    @Override
    public void onClick() {
        boolean on = !getPower();
        onPowerChanged(on, true);
    }

    //convert the new color to hsb
    private synchronized void onColorChanged(Float[] hsb, boolean commitChanges){
        try {

            JSONObject properties = mService.getJSONObject("properties");
            if(isLightDimmer()) {
                JSONObject dimmer = properties.getJSONObject("dimmer");
                dimmer.put("level", hsb[2] );
            }else {
                JSONObject color = properties.getJSONObject("color");
                color.put("hue", hsb[0]);
                color.put("sat", hsb[1]);
                color.put("bri", hsb[2]);
            }
        }catch (JSONException e) {
        }
        if(commitChanges)
            setService();
    }


    //true if the power is on:
    public boolean getPower(){
        try {
            JSONObject power = mService.getJSONObject("properties").getJSONObject("power");
            return power.getBoolean("on");
        }catch(JSONException e){}
        return false;

    }


    /********************************************************************
     *update the power - on/off
     ********************************************************************/
    public synchronized void onPowerChanged(boolean on, boolean commitChanges){
        try {
            JSONObject power = mService.getJSONObject("properties").getJSONObject("power");
            power.put("on", on);
        }catch(JSONException e){}
        if(commitChanges)
            setService();
    }


    /********************************************************************
     * Sets the changes in the service:
     * this function sends the modified service json to the sdk
     * the sdk executes the changes:
     ********************************************************************/
    public synchronized void setService(){
        JSONObject servicesJson = new JSONObject();
        try {

            JSONObject networks_info = new JSONObject();
            JSONObject devices_info = new JSONObject();
            networks_info.put(mService.getString("network_id"), mNetwork);
            devices_info.put(mService.getString("device_id"), mDevice);
            servicesJson.put("networks_info", networks_info);
            servicesJson.put("devices_info", devices_info);
            JSONArray services = new JSONArray();
            services.put(mService);
            servicesJson.put("services", services);
            WeaverSdkApi.servicesSet(this, servicesJson);
        }catch(Exception e){}


    }

    @Override
    public void onTaskCompleted(int flag, JSONObject data) {

    }

    @Override
    public void onTaskUpdate(int flag, JSONObject data) {

    }


    @Override
    public boolean getToggle() {
        return getPower();
    }


    public boolean equals(JSONObject service, boolean ignore_local_global)  {
            return  (ignore_local_global || (isGlobal() == isGlobal(service)))&&
                    (getServiceType().equals(getServiceType(service))) &&
                    (getId().equals(getId(service)) &&
                    (getDeviceId().equals(getDeviceId(service))) );
    }

}
