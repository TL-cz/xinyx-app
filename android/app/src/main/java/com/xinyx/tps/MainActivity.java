package com.xinyx.tps;

import android.os.Bundle;
import android.widget.Toast;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 启动时显示壳版本号（versionName 来自 build.gradle，CI 每次构建自动递增）
        Toast.makeText(this, "xinyx 壳 v" + BuildConfig.VERSION_NAME, Toast.LENGTH_SHORT).show();
        // 强制加载远程游戏服务器，不走本地 assets
        bridge.getWebView().loadUrl("http://8.134.192.185:4000");
    }
}
