package org.webinos.demowallet;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.*;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.webinos.demowallet.WalletEngine;

public class TestWallet extends Activity {

    private final static String TAG = TestWallet.class.getName();
    private Activity thi$;
    private boolean haveSentMessage = false;
    private int contentViewTop = 0;
    private String totalAmount = "Nothing to buy";
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        thi$ = this;
        Intent intent = getIntent();
        try{
        // TODO: this used to be value but java bridge can't pass the double so this is a hack
            totalAmount = intent.getStringExtra(WalletServiceMessageHandler.ACTION_PARAMETER_TOTALPRICE);
        }catch (Exception ex){
            Log.e(TAG,"Error getting Intent data", ex);
        }
        if (totalAmount == null)
            totalAmount = "Nothing to buy";

        // Estimate contentViewTop (height of status bar + title bar). It's asynchronous!
        // http://stackoverflow.com/a/4832438
        LinearLayout lySpin = new LinearLayout(this);
        lySpin.setOrientation(LinearLayout.VERTICAL);
        lySpin.post(new Runnable() {
            public void run() {
                Rect rect = new Rect();
                Window window = getWindow();
                DisplayMetrics metrics = getResources().getDisplayMetrics();
                window.getDecorView().getWindowVisibleDisplayFrame(rect);
                int statusBarHeight = rect.top;
                contentViewTop = window.findViewById(Window.ID_ANDROID_CONTENT).getTop();
                int titleBarHeight = contentViewTop - statusBarHeight;
                Log.e(TAG,metrics.densityDpi+" TitleBarHeight: " + titleBarHeight
                        + ", StatusBarHeight: " + statusBarHeight
                        + ", contentViewTop: " + contentViewTop);
            }
        });

        setContentView(R.layout.main);
        haveSentMessage = false;
        fixLayout();
    }

    // Set item to buy.
    // Set the size of the main Layout so that it doesn't cover the background.
    // Bind the onclick event on the buttons since they are recreated after calling setContentView()
    private void fixLayout(){
        TextView textView = (TextView)findViewById(R.id.fldAmount);
        textView.setText(totalAmount);

        LinearLayout layout = (LinearLayout)findViewById(R.id.llMain);
        ViewGroup.LayoutParams params = layout.getLayoutParams();
        DisplayMetrics metrics = getResources().getDisplayMetrics();

        // contentViewTop (height of status bar + title bar) is not calculated on the first creation of the activity.
        // Use a pre-calculated one.
        if (contentViewTop == 0){
            switch (metrics.densityDpi) {
                case DisplayMetrics.DENSITY_XHIGH:
                    contentViewTop = 146; //130 landscape
                    break;
                case DisplayMetrics.DENSITY_HIGH:
                    contentViewTop = 110;
                    break;
                case DisplayMetrics.DENSITY_TV:
                    contentViewTop = 108;
                    break;
                case DisplayMetrics.DENSITY_MEDIUM:
                    contentViewTop = 81;
                    break;
                case DisplayMetrics.DENSITY_LOW:
                    contentViewTop = 55;
                    break;
                default:
                    contentViewTop = 81;
            }
        }
        // Fix LinearLayout height or width depending on the orientation.
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            // height: 180px out of 762px is the area in the design - 5px padding
            params.height = 175*(metrics.heightPixels-contentViewTop)/762;//metrics has the total screen size, remove the top bars.
        } else {
            // width: 210px out of 762px is the area in the design - 5px padding
            params.width = 205*metrics.widthPixels/762;
        }

        // Hook Buttons
        Button cmdOk = (Button)findViewById(R.id.cmdOk);
        cmdOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG,"Sending payed");
                sendMessage(WalletEngine.RESPONSE_CODE_CHECKOUT_OK);
                haveSentMessage = true;
                thi$.finish();
            }
        });

        Button cmdCancel = (Button)findViewById(R.id.cmdCancel);
        cmdCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG,"Sending NOT payed");
                sendMessage(WalletEngine.RESPONSE_CODE_CHECKOUT_FAIL);
                haveSentMessage = true;
                thi$.finish();
            }
        });
    }

    // We need to override this method in order not to restart the activity (default behaviour of Android).
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setContentView(R.layout.main);
        fixLayout();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause()");
        if (!haveSentMessage)  {// If we haven't sent the notification yet
            Log.d(TAG,"Sending NOT payed");
            sendMessage(WalletEngine.RESPONSE_CODE_CHECKOUT_FAIL);
        }
        thi$.finish();
    }

    private void sendMessage(int code) {
        if (TestWallet.respondTo!=null){
            Message answ = new Message();
            answ.what = code;
            Log.v(TAG, "Will send user input: "+code);
            try {
                TestWallet.respondTo.replyTo.send(answ);
            } catch (RemoteException e) {
                Log.e(TAG, "Can not reply",e);
            }
        }
    }


    // This is for demo purposes... You shouldn't have static sharing store in production
    private static Message respondTo;
    public static void setMessage(Message incomingMsg) {
        Message msg = new Message();
        msg.copyFrom(incomingMsg);
        TestWallet.respondTo = msg;
    }
}
