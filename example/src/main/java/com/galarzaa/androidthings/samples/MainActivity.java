package com.galarzaa.androidthings.samples;

import android.app.Activity;
import android.graphics.Color;
import android.net.wifi.p2p.WifiP2pManager;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.galarzaa.androidthings.Rc522;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManager;
import com.google.android.things.pio.SpiDevice;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity {
    private int redCount = 4;
    private int greenCount = 1;
    private int count = 0;
    private Rc522 mRc522;
    RfidTask mRfidTask;
    private TextView mTagDetectedView;
    private TextView mTagUidView;
    private TextView mTagResultsView;
    private Button button;
    private Gpio mLedGpioR;
    private Gpio mLedGpioG;
    private Gpio mLedGpioB;
    private SpiDevice spiDevice;
    private Gpio gpioReset;
    private static final String SPI_PORT = "SPI0.0";
    private static final String PIN_RESET = "BCM25";
    private int INTERVAL_BETWEEN_BLINKS_MS = 400;
    String resultsText = "";
    String result1 = "";
    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTagDetectedView = (TextView)findViewById(R.id.tag_read);
        mTagUidView = (TextView)findViewById(R.id.tag_uid);
        mTagResultsView = (TextView) findViewById(R.id.tag_results);
        button = (Button)findViewById(R.id.button);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mHandler.post(blueRun);
                button.setEnabled(false);
                ((Button)v).setText(R.string.reading);
               mHandler.post(mRunnable);
//                scanCard();
            }
        });

        PeripheralManager pioService = PeripheralManager.getInstance();
        try {
            String R = "BCM26";
            String G = "BCM16";
            String B = "BCM6";
            spiDevice = pioService.openSpiDevice(SPI_PORT);
            gpioReset = pioService.openGpio(PIN_RESET);
            mRc522 = new Rc522(spiDevice, gpioReset);
            mRc522.setDebugging(true);
            mLedGpioR = PeripheralManager.getInstance().openGpio(R);
            mLedGpioR.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            mLedGpioG = PeripheralManager.getInstance().openGpio(G);
            mLedGpioG.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            mLedGpioB = PeripheralManager.getInstance().openGpio(B);
            mLedGpioB.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
        } catch (IOException e) {
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try{
            if(spiDevice != null){
                spiDevice.close();
            }
            if(gpioReset != null){
                gpioReset.close();
            }
            mLedGpioR.close();
            mLedGpioG.close();
            mLedGpioB.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            mLedGpioR = null;
            mLedGpioG = null;
            mLedGpioB = null;
        }
    }

    private void redColor(){
        if (mLedGpioR == null) {
            return;
        }
        try {
            mLedGpioR.setValue(false);
            mLedGpioG.setValue(true);
            mLedGpioB.setValue(true);
        } catch (IOException e) {
            Log.e("Error", "Error on PeripheralIO API", e);
        }
    }

    private void blueColor(){
        if (mLedGpioR == null) {
            return;
        }
        try {
            mLedGpioB.setValue(false);
            mLedGpioG.setValue(true);
            mLedGpioR.setValue(true);
        } catch (IOException e) {
            Log.e("Error", "Error on PeripheralIO API", e);
        }
    }

    private void greenColor(){
        if (mLedGpioR == null) {
            return;
        }
        try {
            mLedGpioG.setValue(false);
            mLedGpioR.setValue(true);
            mLedGpioB.setValue(true);
        } catch (IOException e) {
            Log.e("Error", "Error on PeripheralIO API", e);
        }
    }

    private class RfidTask extends AsyncTask<Object, Object, Boolean> {
        private static final String TAG = "RfidTask";
        private Rc522 rc522;

        RfidTask(Rc522 rc522){
            this.rc522 = rc522;
        }

        @Override
        protected void onPreExecute() {
            button.setEnabled(false);
            resultsText = "";
        }

        @Override
        protected Boolean doInBackground(Object... params) {
            rc522.stopCrypto();
            Log.d("Hello", "run" );
            while(true){
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return null;
                }
                //Check if a RFID tag has been found
                if(!rc522.request()){
                    continue;
                }
                //Check for collision errors
                if(!rc522.antiCollisionDetect()){
                    continue;
                }
                byte[] uuid = rc522.getUid();
                return rc522.selectTag(uuid);
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if(!success){
                mTagResultsView.setText(R.string.unknown_error);
                return;
            }
            // Try to avoid doing any non RC522 operations until you're done communicating with it.
            byte address = Rc522.getBlockAddress(2,0);
            byte address1 = Rc522.getBlockAddress(2,1);
            byte address2 = Rc522.getBlockAddress(2,2);
            // Mifare's card default key A and key B, the key may have been changed previously
            byte[] key = {(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF};
            // Each sector holds 16 bytes
            // Data that will be written to sector 2, block 1
            byte[] name = {'V','U',' ','H','O','A','N','G',' ','K','H','A','N','G',0x00,0x00};
            byte[] id = {'1','5','5','2','1','6','4',0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00};
            byte[] dayOfBirth = {'2','6','/','2','/','1','9','9','7',0x00,0x00,0x00,0x00,0x00,0x00,0x00};
            // In this case, Rc522.AUTH_A or Rc522.AUTH_B can be used
            try {
                //We need to authenticate the card, each sector can have a different key
                boolean result = rc522.authenticateCard(Rc522.AUTH_A, address, key);
                boolean result1,result2;
                if (!result) {
                    mTagResultsView.setText(R.string.authetication_error);
                    return;
                }
//                result = rc522.writeBlock(address, name);
//                result1 = rc522.writeBlock(address1, dayOfBirth);
//                result2 = rc522.writeBlock(address2, id);
//
//                if(!result && !result1 && !result2){
//                    mTagResultsView.setText(R.string.write_error);
//                    return;
//                }
                byte[] buffer = new byte[16];
                byte[] buffer1 = new byte[16];
                byte[] buffer2 = new byte[16];
                //Since we're still using the same block, we don't need to authenticate again
                result = rc522.readBlock(address, buffer);
                result1 =  rc522.readBlock(address1,buffer1);
                result2 =  rc522.readBlock(address2,buffer2);
                if(!result && !result1 && !result2){
                    mTagResultsView.setText(R.string.read_error);
                    return;
                }
                resultsText += "\nName: "+ new String(buffer);
                resultsText += "\nDay Of Birth: " + new String(buffer1);
                String s = new String(buffer2);
                resultsText += "\nID: " + s;
                String arr2 = "1552064";
                String arr1 = "1552164";
                boolean retval = Objects.equals(s.trim(), arr2);
                boolean retval1 = Objects.equals(s.trim(), arr1);
                if (retval == true || retval == true) {
                    mHandler.post(greenRun);
                }
                else {
                    mHandler.post(redRun);
                }

                rc522.stopCrypto();
                Log.d("checked",resultsText);
                mTagResultsView.setText(resultsText);
            }finally{
                button.setEnabled(false);
                Log.d("checked",getString(R.string.tag_uid,rc522.getUidString()));
                mTagUidView.setText(getString(R.string.tag_uid,rc522.getUidString()));
                mTagResultsView.setVisibility(View.VISIBLE);
                mTagDetectedView.setVisibility(View.VISIBLE);
                mTagUidView.setVisibility(View.VISIBLE);
                mHandler.post(mRunnable);
            }
        }
    }

    public void scanCard(){
        mRfidTask = new RfidTask(mRc522);
        mRfidTask.execute();
    }

    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            runOnUiThread(new Runnable() {
                public void run() {
                    mRfidTask = new RfidTask(mRc522);
                    mRfidTask.execute();
                }
            });
        }
    };

    private Runnable blueRun = new Runnable() {
        @Override

        public void run() {
            if (mLedGpioR == null || mLedGpioG == null || mLedGpioB == null) {
                return;
            }
            try {
                blueColor();
            } catch (Exception e) {
                Log.e("Hello", "Error on PeripheralIO API", e);
            }
        }
    };
    private Runnable greenRun = new Runnable() {
        @Override
        public void run() {
            if (mLedGpioR == null || mLedGpioG == null || mLedGpioB == null) {
                return;
            }
            mHandler.removeCallbacks(redRun);
            if (count > greenCount) {
                mHandler.removeCallbacks(greenRun);
                mHandler.post(blueRun);
                count = 0;
            } else {
                try {
                    greenColor();
                } catch (Exception e) {
                    Log.e("Hello", "Error on PeripheralIO API", e);
                }
                mHandler.postDelayed(greenRun, 500);
                count++;
            }
        }
    };

    private Runnable redRun = new Runnable()  {
        @Override
        public void run() {
            if (mLedGpioR == null || mLedGpioG == null || mLedGpioB == null) {
                return;
            }
            mHandler.removeCallbacks(greenRun);
            if (count > redCount) {
                onStop();
                mHandler.post(blueRun);
                count = 0;
            } else {
                try {
                    mLedGpioR.setValue(false);
                    redColor();
                    mLedGpioR.setValue(true);

                } catch (Exception e) {
                    Log.e("Hello", "Error on PeripheralIO API", e);
                }
                mHandler.postDelayed(redRun, INTERVAL_BETWEEN_BLINKS_MS);
                count++;
            }
        }
    };

    protected void onStop() {
        super.onStop();
        mHandler.removeCallbacks(redRun);
        mHandler.post(blueRun);
    }
}
