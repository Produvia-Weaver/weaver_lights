//***************************************************************
// * (C) Copyrights 2014-2016, Produvia. All rights reserved.
// * PRODUVIA CONFIDENTIAL PROPRIETRY
// * Contains confidential and proprietary information of Produvia.
// * Reverse engineering is prohibited.
// * The copyright notice does not imply publication.
// ***************************************************************/
package produvia.com.weaverandroidsdk;


import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import com.produvia.eprServer.eprNetBle;
import com.produvia.eprServer.eprUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

//*********************************************
//   BLE networking function
//*********************************************
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BleActions implements eprNetBle.eprBleActionCallbacks {




    private Map<String, String> mBleDiscoveryResults = new HashMap<>();

    private Context mContext;
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothLeScanner mLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    private BluetoothGatt mBluetoothGatt;


    public BleActions(final Context context){
        mContext = context;
    }

    private boolean mInitialized = false;


    private Map<String, List<BluetoothGattCharacteristic>> mCharacteristicsHash = new HashMap<>();
    private Map<String, List<BluetoothGattService>> mServicesHash = new HashMap<>();

    private Map<String, byte[]> mReadValueHash = new HashMap<>();




    @Override
    public byte[] getServices(String bleMac, final int timeout) {
        initialize();
        if(!mInitialized)
            return null;

        List<BluetoothGattService> services = mServicesHash.get(bleMac);

        if(services == null){
            //// TODO: 3/1/16 scan for the services
        }
        String uuids = "";
        for(int i = 0; i < services.size(); i++){
            uuids += services.get(i).getUuid().toString();

        }
        return uuids.getBytes();
    }

    @Override
    public byte[] getChars(String bleMac, final String serviceId, final int timeout) {
        //not used for android
        return null;
    }

    @Override
    public byte[] getValue(final String bleMac, final String charId, final int timeout) {
        byte[] value = readWriteCharacteristics(false, bleMac, charId, null, timeout);
        eprUtils.debug(2, "[BLE] getValue: " + charId + ": " + value);
        return value;
    }

    @Override
    public void setValue(String bleMac, final String charId, final byte[] data, final int timeout) {
        eprUtils.debug(2, "[BLE] setValue: " + charId + ": " + eprUtils.dumpBytes(data));
        readWriteCharacteristics(true, bleMac, charId, data, timeout);
    }

    private BluetoothDevice getDevice(String address) {
        initialize();
        if(!mInitialized)
            return null;
        return mBluetoothAdapter.getRemoteDevice(address);
    }



    private  boolean ensureBleAvailable() {
        // Use this check to determine whether BLE is supported on the device. Then
        // you can selectively disable BLE-related features.
        return mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }


    private boolean initAdapter() {
        // Initializes Bluetooth adapter.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled())
            return false;


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
            settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();
            filters = new ArrayList<ScanFilter>();
        }

        return true;

    }







    private void initialize(){
        if(mInitialized)
            return;
        //only 18 and up:
        if (Build.VERSION.SDK_INT < 18) {
            return;
        }
        if( ensureBleAvailable() && initAdapter())
            mInitialized = true;

    }

    private synchronized List<String>returnResultsAsList(){
        List<String> results = new ArrayList<>();
        if(mBleDiscoveryResults == null)
            return results;

        Iterator it = mBleDiscoveryResults.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            results.add(pair.getKey() + " " + pair.getValue());
            System.out.println(pair.getKey() + " = " + pair.getValue());
        }
        return results;

    }

    @Override
    public List<String> bleDiscover(final int timeout) {
        initialize();
        if (!mInitialized)
            return returnResultsAsList();


        new Thread(new Runnable() {
            @Override
            public void run() {
                scanLeDevice(true, timeout);
            }
        }).start();

        //wait for scan timeout before returning:
        try {
            Thread.sleep(timeout);
        } catch (InterruptedException e) {

        } finally {
            return returnResultsAsList();
        }
    }





    private void scanLeDevice(final boolean enable, final int scan_period) {
        if (enable) {

            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (Build.VERSION.SDK_INT < 21)
                        mBluetoothAdapter.startLeScan(mLeScanCallback);
                    else
                        mLEScanner.startScan(filters, settings, mScanCallback);
                }
            }).start();
        }


        try {
            Thread.sleep(scan_period);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            if (Build.VERSION.SDK_INT < 21)
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            else
                mLEScanner.stopScan(mScanCallback);


        }

    }




    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     byte[] scanRecord) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            device.connectGatt(mContext, false, gattCallback);
                        }
                    }).start();
                }
            };

    @SuppressLint("NewApi")
    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            eprUtils.log("BLE callbackType " + String.valueOf(callbackType));
            eprUtils.log("BLE result " + result.toString());
            final BluetoothDevice btDevice = result.getDevice();
            String name = btDevice.getName();
            if(name == null || name.isEmpty() ){
                name = "(unknown)";
            }


            new Thread(new Runnable() {
                @Override
                public void run() {
                    btDevice.connectGatt(mContext, false, gattCallback);

                }
            }).start();

        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult sr : results) {
                eprUtils.log("BLE BTScanResult - Results " + sr.toString());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            eprUtils.log("BLE Scan Failed " + "Error Code: " + errorCode);
        }
    };



    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            eprUtils.log("BLE onConnectionStateChange Status: " + status);
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    eprUtils.log("BLE gattCallback STATE_CONNECTED");
                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    eprUtils.log("BLE gattCallback STATE_DISCONNECTED");
                    break;
                default:
                    eprUtils.log("BLE gattCallback STATE_OTHER");
            }

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            List<BluetoothGattService> services = gatt.getServices();
            mServicesHash.put(gatt.getDevice().getAddress(), services);
            String name = gatt.getDevice().getName();
            if(name == null || name.isEmpty() )
                name = "(unknown)";

            mBleDiscoveryResults.put(gatt.getDevice().getAddress(), name);

            eprUtils.log("BLE onServicesDiscovered " + services.toString());
            for(int i = 0; i< services.size(); i++) {
                mCharacteristicsHash.put(gatt.getDevice().getAddress(), services.get(i).getCharacteristics());
                //for( int j =0; j < services.get(i).getCharacteristics().size(); j++)
                //    gatt.readCharacteristic(services.get(i).getCharacteristics().get(j));

            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic
                                                 characteristic, int status) {
            eprUtils.log("BLE onCharacteristicRead " + characteristic.toString());
        }
    };






    class ReadWriteBluetoothGattCallback extends BluetoothGattCallback {

        private int mStatus = 0;
        private String mErrorDescription = "";
        private Object mOperationNotification;
        private String mCharId;
        private boolean mWrite;
        private String mHashKey;
        private byte[] mData;


        private void notifyOperationCompleted(BluetoothGatt gatt){
            synchronized(mOperationNotification) {
                mOperationNotification.notify();
                if(mStatus != 0)
                    eprUtils.log("[BLE-ERROR] "+mStatus +": " + mErrorDescription);
            }
        }


        public ReadWriteBluetoothGattCallback(Object opNotification,
                                              boolean opWrite,
                                              String hash_key,
                                              String bleMac,
                                              String charId,
                                              byte[] data ){
            mOperationNotification = opNotification;
            mCharId = charId;
            mWrite = opWrite;
            mHashKey = hash_key;
            mData = data;
            mStatus = 0;


        }

        public int getOperationStatus() {
            return mStatus;
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED)
                gatt.discoverServices();
            else {
                mStatus = status;
                mErrorDescription = "Failed to connect to device";
                notifyOperationCompleted(gatt);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            List<BluetoothGattService> services = gatt.getServices();
            mServicesHash.put(gatt.getDevice().getAddress(), services);

            eprUtils.log("BLE onServicesDiscovered " + services.toString());
            for (int i = 0; i < services.size(); i++) {
                BluetoothGattService service = services.get(i);
                List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                mCharacteristicsHash.put(gatt.getDevice().getAddress(), characteristics);
                for (int j = 0; j < characteristics.size(); j++) {
                    BluetoothGattCharacteristic characteristic = characteristics.get(j);
                    if (characteristic.getUuid().toString().startsWith(mCharId)) {
                        boolean stat;
                        if (mWrite) {
                            characteristic.setValue(mData);
                            stat = gatt.writeCharacteristic(characteristic);
                        }
                        else
                            stat = gatt.readCharacteristic(characteristic);

                        if (!stat) {
                            mStatus = -1;
                            mErrorDescription = "Failed to connect to device";
                            notifyOperationCompleted(gatt);
                        }
                    }
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

            mReadValueHash.put(mHashKey, characteristic.getValue());
            notifyOperationCompleted(gatt);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            eprUtils.log("BLE write status " + status);
            notifyOperationCompleted(gatt);
        }
    }


    private synchronized byte[] readWriteCharacteristics(final boolean write,
                                                         final String bleMac,
                                                         final String charId,
                                                         final byte[] data,
                                                         final int    timeout) {

        //run once for now:
        int retries = 1;
        final String hash_key = bleMac + charId;

        while (retries > 0) {
            retries -= 1;
            BluetoothDevice device = getDevice(bleMac);
            final Object operationNotification = new Object();

            ReadWriteBluetoothGattCallback gattCallback = new ReadWriteBluetoothGattCallback(operationNotification, write, hash_key, bleMac, charId, data);
            BluetoothGatt gatt = device.connectGatt(mContext, true, gattCallback);
            try {
                synchronized(operationNotification) {
                    operationNotification.wait(timeout + 1000);
                }

                if (gattCallback.getOperationStatus() == 0) {
                    retries = 0;
                } else {
                    //there was an error - let's try to reinitialize the adapter and try again:
                    mInitialized = false;
                }

            } catch (Exception e) {
                eprUtils.log(e.getMessage());
            } finally {
                gatt.disconnect();
                gatt.close();
            }
        }
        return mReadValueHash.get(hash_key);
    }


}
