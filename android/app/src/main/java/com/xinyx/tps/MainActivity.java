package com.xinyx.tps;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.Surface;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.Toast;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity implements SensorEventListener {
    private static final String PREFS = "xinyx_shell";
    private static final String KEY_VER = "last_version_code";

    private SensorManager sensorManager;
    private Sensor rotationSensor;
    private Sensor rawGyroSensor;
    private boolean useRawGyro = false;
    private long lastGyroNs = 0;
    private final float[] rMat = new float[16];
    private final float[] adjusted = new float[16];
    private final float[] orientation = new float[3];
    private volatile float yawDeg = 0f;
    private volatile float pitchDeg = 0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 启动时显示壳版本号（versionName 来自 build.gradle，CI 每次构建自动递增）
        Toast.makeText(this, "xinyx 壳 v" + BuildConfig.VERSION_NAME, Toast.LENGTH_SHORT).show();

        // 壳版本变化时自动清一次 HTTP 缓存（保留 Cookie/localStorage 登录态）
        clearCacheOnVersionUpdate();

        // 原生桥（陀螺仪/剪贴板/清缓存）：必须在 loadUrl 之前注入
        bridge.getWebView().addJavascriptInterface(new GyroBridge(this), "XinyxNative");
        // 强制加载远程游戏服务器，不走本地 assets
        bridge.getWebView().loadUrl("http://8.134.192.185:4000");

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            // 优先旋转矢量(融合传感器, 无漂移); 再退纯陀螺仪(积分, 极少老机型)
            rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
            if (rotationSensor == null) rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            if (rotationSensor == null) rawGyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        }
    }

    private void clearCacheOnVersionUpdate() {
        try {
            SharedPreferences sp = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            int last = sp.getInt(KEY_VER, 0);
            if (last != BuildConfig.VERSION_CODE) {
                WebView wv = bridge.getWebView();
                if (wv != null) wv.clearCache(true);
                sp.edit().putInt(KEY_VER, BuildConfig.VERSION_CODE).apply();
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (sensorManager == null) return;
        if (rotationSensor != null) {
            useRawGyro = false;
            sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_GAME);
        } else if (rawGyroSensor != null) {
            useRawGyro = true;
            lastGyroNs = 0;
            sensorManager.registerListener(this, rawGyroSensor, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (sensorManager != null) sensorManager.unregisterListener(this);
    }

    private int displayRotation() {
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        return wm != null ? wm.getDefaultDisplay().getRotation() : Surface.ROTATION_0;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!useRawGyro) {
            // ---- 旋转矢量: 直接输出稳定姿态角 ----
            SensorManager.getRotationMatrixFromVector(rMat, event.values);
            int rot = displayRotation();
            int axisX, axisY;
            switch (rot) {
                case Surface.ROTATION_90:
                    axisX = SensorManager.AXIS_Y; axisY = SensorManager.AXIS_MINUS_X; break;
                case Surface.ROTATION_180:
                    axisX = SensorManager.AXIS_MINUS_X; axisY = SensorManager.AXIS_MINUS_Y; break;
                case Surface.ROTATION_270:
                    axisX = SensorManager.AXIS_MINUS_Y; axisY = SensorManager.AXIS_X; break;
                default:
                    axisX = SensorManager.AXIS_X; axisY = SensorManager.AXIS_Y; break;
            }
            SensorManager.remapCoordinateSystem(rMat, axisX, axisY, adjusted);
            SensorManager.getOrientation(adjusted, orientation);
            yawDeg = (float) Math.toDegrees(orientation[0]);
            pitchDeg = (float) Math.toDegrees(orientation[1]);
        } else {
            // ---- 纯陀螺仪回退: 角速度积分 (有漂移, JS 侧有归零慢漂补偿) ----
            if (lastGyroNs == 0) { lastGyroNs = event.timestamp; return; }
            float dt = (event.timestamp - lastGyroNs) / 1e9f;
            lastGyroNs = event.timestamp;
            if (dt <= 0 || dt > 0.1f) return;
            int rot = displayRotation();
            float pitchRate;   // 绕屏幕横轴角速度 rad/s
            float yawRate;     // 绕屏幕法向角速度 rad/s, 右转为正
            switch (rot) {
                case Surface.ROTATION_90:
                    pitchRate = event.values[1]; yawRate = -event.values[2]; break;
                case Surface.ROTATION_270:
                    pitchRate = -event.values[1]; yawRate = event.values[2]; break;
                default:
                    pitchRate = event.values[0]; yawRate = -event.values[2]; break;
            }
            yawDeg += (float) Math.toDegrees(yawRate * dt);
            pitchDeg += (float) Math.toDegrees(pitchRate * dt);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    boolean hasGyroSensor() {
        return rotationSensor != null || rawGyroSensor != null;
    }

    float getYaw() {
        return yawDeg;
    }

    float getPitch() {
        return pitchDeg;
    }
}
