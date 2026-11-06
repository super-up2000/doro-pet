package com.dsh.doropet;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import java.util.Random;

public class PetService extends Service {

    private static final int VIEW_W_DP = 240;
    private static final int VIEW_H_DP = 320;

    private WindowManager wm;
    private WindowManager.LayoutParams params;
    private WebView web;
    private Point realSize = new Point();

    // 自主走动状态
    private Random random = new Random();
    private long lastWalk = 0;
    private int walkDir = 1;
    private int walkTargetY = -1; // -1 表示本次走动的目标行
    private int walkFromY = 0;
    private int walkToY = 0;
    private long walkStart = 0;
    private static final long WALK_DURATION_MS = 1600;

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display d = wm.getDefaultDisplay();
        d.getRealSize(realSize);

        int scale = getResources().getDisplayMetrics().densityDpi / 160;
        int wPx = VIEW_W_DP * Math.max(1, scale);
        int hPx = VIEW_H_DP * Math.max(1, scale);

        params = new WindowManager.LayoutParams(
                wPx, hPx,
                Build.VERSION.SDK_INT >= 26
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = realSize.y - hPx - (int) (realSize.y * 0.12f); // 默认停在屏幕底部 12% 处

        web = new WebView(this);
        WebSettings ws = web.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setAllowFileAccess(true);
        ws.setAllowContentAccess(true);
        web.setBackgroundColor(0);
        web.loadUrl("file:///android_asset/web/index.html");

        attachGestures(web);
        showNotification();

        try {
            wm.addView(web, params);
        } catch (Exception e) {
            Toast.makeText(this, "无法显示悬浮窗：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void attachGestures(final WebView view) {
        view.setOnTouchListener(new View.OnTouchListener() {
            private long downX, downY, downTime;
            private boolean dragging = false;
            private int lastCX, lastCY;

            @Override
            public boolean onTouch(View v, MotionEvent ev) {
                int cx = (int) ev.getRawX();
                int cy = (int) ev.getRawY();
                switch (ev.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = cx; downY = cy;
                        downTime = System.currentTimeMillis();
                        dragging = false;
                        lastCX = cx; lastCY = cy;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        int dx = cx - lastCX;
                        int dy = cy - lastCY;
                        lastCX = cx; lastCY = cy;
                        if (Math.abs(cx - downX) > 8 || Math.abs(cy - downY) > 8) {
                            dragging = true;
                            int maxX = realSize.x - params.width;
                            int maxY = realSize.y - params.height;
                            params.x = clamp(params.x + dx, 0, maxX);
                            params.y = clamp(params.y + dy, 0, maxY);
                            wm.updateViewLayout(view, params);
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        long dur = System.currentTimeMillis() - downTime;
                        if (!dragging && dur < 300) {
                            // 轻点 doro：摸头 + 台词
                            view.evaluateJavascript(
                                "window.doroPet && window.doroPet.tap && window.doroPet.tap();", null);
                        } else if (dragging) {
                            int vx = cx - (int) downX;
                            // 甩抛：告诉前端播放挣扎动画 + 台词
                            view.evaluateJavascript(
                                "window.doroPet && window.doroPet.fling && window.doroPet.fling("
                                        + (vx >= 0 ? 1 : -1) + ");", null);
                        }
                        return true;
                    default:
                        return false;
                }
            }
        });
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    @Override
    public void onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    private void showNotification() {
        String channelId = "doro_pet";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(channelId,
                    "Doro 桌宠正在运行", NotificationManager.IMPORTANCE_MIN);
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
                    .createNotificationChannel(ch);
        }
        Notification.Builder b = new Notification.Builder(this, channelId)
                .setSmallIcon(android.R.drawable.stat_sys_upload)
                .setContentTitle("Doro 桌宠")
                .setContentText("正在陪伴你 ✨ 点 doro 说话 / 拖她走动 / 甩她玩")
                .setOngoing(true);
        startForeground(1, b.build());
    }

    @Override
    public void onDestroy() {
        if (web != null) {
            try { wm.removeView(web); } catch (Throwable ignored) {}
            web.destroy();
        }
        super.onDestroy();
    }
}
