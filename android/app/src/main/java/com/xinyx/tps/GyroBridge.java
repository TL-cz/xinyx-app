package com.xinyx.tps;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.webkit.JavascriptInterface;

/**
 * 原生桥：WebView 在 http 非安全上下文下不触发 deviceorientation 事件、
 * navigator.clipboard 也不可用，游戏侧通过 window.XinyxNative 直接调用原生能力。
 */
public class GyroBridge {
    private final MainActivity activity;

    public GyroBridge(MainActivity activity) {
        this.activity = activity;
    }

    /** 设备是否有可用的旋转矢量传感器（游戏打开设置面板时调用） */
    @JavascriptInterface
    public boolean gyroAvailable() {
        return activity.hasGyroSensor();
    }

    /** 当前水平航向角（度，-180~180），绕世界竖直轴；右转手机角度增大 */
    @JavascriptInterface
    public float yaw() {
        return activity.getYaw();
    }

    /** 当前俯仰角（度，-90~90），屏幕顶部抬起为正 */
    @JavascriptInterface
    public float pitch() {
        return activity.getPitch();
    }

    /** 复制文本到系统剪贴板（http 环境 navigator.clipboard 不可用时的最终兜底） */
    @JavascriptInterface
    public void copyText(String text) {
        ClipboardManager cm = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("xinyx", text));
    }

    /** 清除 WebView HTTP 缓存（不动 Cookie/localStorage，登录态保留），清完游戏侧自行 reload */
    @JavascriptInterface
    public void clearHttpCache() {
        activity.runOnUiThread(() -> {
            try {
                activity.getBridge().getWebView().clearCache(true);
            } catch (Exception ignored) {
            }
        });
    }
}