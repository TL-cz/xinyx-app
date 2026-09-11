package com.xinyx.tps;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.Toast;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity implements SensorEventListener {
    private SensorManager sensorManager;
    private boolean gyroSensor = false;
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

        // 原生陀螺仪桥：必须在 loadUrl 之前注入
        bridge.getWebView().addJavascriptInterface(new GyroBridge(this), "XinyxNative");
        // 强制加载远程游戏服务器，不走本地 assets
        bridge.getWebView().loadUrl("http://8.134.192.185:4000");

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor s = null;
        if (sensorManager != null) {
            // GAME_ROTATION_VECTOR 无磁力计漂移，转头更稳；缺失时回退普通旋转矢量
            s = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
            if (s == null) s = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        }
        gyroSensor = (s != null);
        if (gyroSensor) rotationSensor = s;
    }

    private Sensor rotationSensor;

    @Override
    public void onResume() {
        super.onResume();
        if (sensorManager != null && rotationSensor != null) {
            sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (sensorManager != null) sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        SensorManager.getRotationMatrixFromVector(rMat, event.values);
        int rot = Surface.ROTATION_0;
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        if (wm != null) rot = wm.getDefaultDisplay().getRotation();
        int axisX, axisY;
        switch (rot) {
            case Surface.ROTATION_90:
                axisX = SensorManager.AXIS_Y;
                axisY = SensorManager.AXIS_MINUS_X;
                break;
            case Surface.ROTATION_180:
                axisX = SensorManager.AXIS_MINUS_X;
                axisY = SensorManager.AXIS_MINUS_Y;
                break;
            case Surface.ROTATION_270:
                axisX = SensorManager.AXIS_MINUS_Y;
                axisY = SensorManager.AXIS_X;
                break;
            default:
                axisX = SensorManager.AXIS_X;
                axisY = SensorManager.AXIS_Y;
                break;
        }
        SensorManager.remapCoordinateSystem(rMat, axisX, axisY, adjusted);
        SensorManager.getOrientation(adjusted, orientation);
        yawDeg = (float) Math.toDegrees(orientation[0]);
        pitchDeg = (float) Math.toDegrees(orientation[1]);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    boolean hasGyroSensor() {
        return gyroSensor;
    }

    float getYaw() {
        return yawDeg;
    }

    float getPitch() {
        return pitchDeg;
    }
}
