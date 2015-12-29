package produvia.com.weaverandroidsdk;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;

import com.produvia.sdk.WeaverSdk;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;


public class NetworkReceiver extends BroadcastReceiver {

    public static final String NET_CON_WIFI   = "wifi";
    public static final String NET_CON_ENET   = "enet";
    public static final String NET_CON_MOBILE = "mobile";
    public static final String NET_CON_NOCON  = "none";

    public static final String CONNECTION  = "connection";
    public static final String CHANGED  = "changed";


    public static final String NOTIFICATION = "com.produvia.android.NETWORK_CHANGE_CALLBACK";
    public static String mConnection = null;
    static AlertDialog.Builder mFreezeNoNetworkDialog;
    static AlertDialog alertDialog;
    public static WifiInfo mWifiInfo = new WifiInfo();


    public static class WifiInfo
    {
        WifiInfo()
        {
            reset();
        }
        String mSsid;
        String mGlobalIp;
        Integer mDefaultGateway;

        public String getGlobalIp(){
            if(mGlobalIp == null)
                IpChecker.setGlobalIp();
            if(mGlobalIp == null)
                return "";
            return mGlobalIp;
        }

        public String getDefaultGateway() {

            if (mDefaultGateway == null)
                return null;
            return "" + (mDefaultGateway&0xff) +"."+ ((mDefaultGateway>>8)&0xff) +"."+ ((mDefaultGateway>>16)&0xff) +"."+ ((mDefaultGateway>>24)&0xff);
        }
        public Boolean mConnected = false;
        public void reset(){ mSsid = null; mDefaultGateway = null; mConnected = false; mGlobalIp = null; }

    }
    //*****************************************************************************
    // getWifiConnectionStatus
    //*****************************************************************************
    public static WifiInfo getWifiConnectionStatus()
    {
        return mWifiInfo;
    }


    public static String getAndroidMMacAddress() {
        try {
            String interfaceName = "wlan0";
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                if (!intf.getName().equalsIgnoreCase(interfaceName)){
                    continue;
                }

                byte[] mac = intf.getHardwareAddress();
                if (mac==null){
                    return "";
                }

                StringBuilder buf = new StringBuilder();
                for (byte aMac : mac) {
                    buf.append(String.format("%02X:", aMac));
                }
                if (buf.length()>0) {
                    buf.deleteCharAt(buf.length() - 1);
                }
                return buf.toString();
            }
        } catch (Exception ex) { } // for now eat exceptions
        return "";
    }



    public static String getMacAddress(Context context)
    {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            return getAndroidMMacAddress();

        }
        else {

            WifiManager manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            return manager.getConnectionInfo().getMacAddress();
        }
    }

    public static String getIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = (NetworkInterface) en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()&&inetAddress instanceof Inet4Address) {
                        String ipAddress=inetAddress.getHostAddress().toString();
                        Log.e("IP address", "" + ipAddress);
                        return ipAddress;
                    }
                }
            }
        } catch (SocketException ex) {

        }
        return null;
    }




    public static ArrayList<String> getGatewayIp(){
        Class<?> SystemProperties;
        ArrayList<String> servers = new ArrayList<String>();
        try {
            SystemProperties = Class.forName("android.os.SystemProperties");

            Method method = SystemProperties.getMethod("get", new Class[] { String.class });

            for (String name : new String[] { "dhcp.wlan0.gateway" }) {
                String value = (String) method.invoke(null, name);
                if (value != null && !"".equals(value) && !servers.contains(value))
                    servers.add(value);
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return servers;
    }


    public static String getGatewayAddress()
    {
        ArrayList<String> gw = getGatewayIp();


        if(gw.size() > 0) {
            for(int i = 0; i < gw.size(); i++){
                if(!gw.get(i).contains(":"))
                    return gw.get(i);

            }
            return gw.get(0);
        }
        else
            return "0.0.0.0";
    }


    public static ArrayList<String> getDnsServerIp(){
        Class<?> SystemProperties;
        ArrayList<String> servers = new ArrayList<String>();
        try {
            SystemProperties = Class.forName("android.os.SystemProperties");

            Method method = SystemProperties.getMethod("get", new Class[] { String.class });

            for (String name : new String[] { "net.dns1", "net.dns2", "net.dns3", "net.dns4", }) {
                String value = (String) method.invoke(null, name);
                if (value != null && !"".equals(value) && !servers.contains(value))
                    servers.add(value);
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return servers;
    }


    public static String getDnsServer()
    {
        ArrayList<String> dnsServers = getDnsServerIp();


        if(dnsServers.size() > 0) {
            for(int i = 0; i < dnsServers.size(); i++){
                if(!dnsServers.get(i).contains(":"))
                    return dnsServers.get(i);

            }
            return dnsServers.get(0);
        }
        else
            return "0.0.0.0";
    }


    public static Integer ipToInteger(String ip) {
        if (ip == null)
            return null;

        int[] ip_array = new int[4];
        String[] parts = ip.split("\\.");
        for (int i = 0; i < 4; i++) {
            ip_array[i] = Integer.parseInt(parts[i]);
        }
        Integer ipNumbers = 0;
        for (int i = 0; i < 4; i++) {
            ipNumbers += ip_array[i] << (24 - (8 * i));
        }
        return ipNumbers;
    }


    static void setConnectionStatus(final Context context, Boolean changed ){
        final ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        final android.net.NetworkInfo wifi = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        final android.net.NetworkInfo mobile = connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        final android.net.NetworkInfo enet = connMgr.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);



        if (wifi != null && wifi.isAvailable() && wifi.isConnectedOrConnecting() )   	//clear the cache and refresh the data:
        {
            IpChecker.setGlobalIp();
            mConnection = NET_CON_WIFI;
            WifiManager wifim = ((WifiManager)context.getSystemService(Context.WIFI_SERVICE));

            mWifiInfo.mConnected = true;
            mWifiInfo.mSsid = wifim.getConnectionInfo().getSSID();
            DhcpInfo d = wifim.getDhcpInfo();
            mWifiInfo.mDefaultGateway = d.gateway;

        }
        else if(enet != null && enet.isAvailable() && enet.isConnectedOrConnecting()){
            IpChecker.setGlobalIp();
            mConnection = NET_CON_ENET;
            mWifiInfo.mConnected = true;
            mWifiInfo.mSsid = "LAN";

            mWifiInfo.mDefaultGateway = ipToInt(getGatewayAddress());

        }
        else if(mobile != null && mobile.isAvailable() && mobile.isConnectedOrConnecting())
        {
            mConnection  = NET_CON_MOBILE;
            mWifiInfo.reset();
        }
        else //no network connection freeze everything
        {
            mConnection = NET_CON_NOCON;
            mWifiInfo.reset();
        }
        //reset the com service cache!
        if(changed)
            WeaverSdk.resetCache();

        Intent i = new Intent(NOTIFICATION);
        i.putExtra(CONNECTION, mConnection);
        i.putExtra(CHANGED, changed);
        context.sendBroadcast(i);

        if(changed) {
            WeaverSdk.initNetworkParams(mWifiInfo.mConnected,
                    getIpAddress(),
                    mWifiInfo.mGlobalIp, mWifiInfo.mSsid, mWifiInfo.mDefaultGateway, getDnsServer());
        }
    }



    @Override
    public void onReceive(final Context context, final Intent intent) {
        setConnectionStatus(context, true);
    }


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    static void alertNoConnection(final Context context, final boolean problem_source_at_user, String overwrite_message ){
        mFreezeNoNetworkDialog = new AlertDialog.Builder(context);
        String message;
        if(problem_source_at_user)
            message = "Weaver requires an active internet connection\nSetup a network connection in order to resume usage";
        else
            message = "No connection to server - please make sure your internet connection is working and try again";
        if(overwrite_message != null)
            message = overwrite_message;

        mFreezeNoNetworkDialog.setMessage(message).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

                /*
    			Intent intent = new Intent(Intent.ACTION_MAIN);
    			intent.addCategory(Intent.CATEGORY_HOME);
    			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    			context.startActivity(intent);*/

            }

        }).setOnDismissListener(new DialogInterface.OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface dialog) {
                /*
    			Intent intent = new Intent(Intent.ACTION_MAIN);
    			intent.addCategory(Intent.CATEGORY_HOME);
    			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    			context.startActivity(intent);
    			*/
            }
        });
        // Create the AlertDialog object and return it
        alertDialog = mFreezeNoNetworkDialog.create();
        alertDialog.show();

    }

    public static int ipToInt(String ipAddress) {
        InetAddress inetAddress;
        try {
            inetAddress = InetAddress.getByName(ipAddress);
            byte[] byte_address = inetAddress.getAddress();

            for(int i = 0; i < byte_address.length / 2; i++)
            {
                byte temp = byte_address[i];
                byte_address[i] = byte_address[byte_address.length - i - 1];
                byte_address[byte_address.length - i - 1] = temp;
            }


            ByteBuffer wrapped = ByteBuffer.wrap(byte_address); // big-endian by default
            return wrapped.getInt();

        } catch (UnknownHostException e) {

            e.printStackTrace();
        }
        return 0;
    }



    private static class IpChecker {

        public static void setGlobalIp(){
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    mWifiInfo.mGlobalIp = getIp();
                }
            });
            t.start(); // spawn thread

            try {
                t.join();  // wait for thread to finish
            } catch (InterruptedException e) {
                e.printStackTrace();
            }


        }

        private static final String[] hosts = {
                "http://checkip.amazonaws.com",
                "http://icanhazip.com/",
                "http://curlmyip.com/",
                "http://www.trackip.net/ip",
                "http://produvia.mooo.com:8000/client_ip.php"
        };
        private static String getIp(){
            return getIp(0);
        }



        private static String getIp(int idx) {
            if(idx >= hosts.length)
                return null;
            try {
                URL url = new URL(hosts[idx]);
                URLConnection con = url.openConnection();
                con.setConnectTimeout(2000);
                con.setReadTimeout(2000);
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String ip = in.readLine();
                return ip;
            } catch (MalformedURLException e) {
                return getIp(idx+1);
            } catch (IOException e) {
                return getIp(idx+1);
            }

        }
    }


}