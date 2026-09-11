package com.xinyx.tps;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bridge.getWebView().loadUrl("http://8.134.192.185:4000");
    }
}
