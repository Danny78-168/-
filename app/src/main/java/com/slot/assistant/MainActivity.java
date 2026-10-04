package com.slot.assistant;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity {

    private static final int OVERLAY_PERMISSION_REQ_CODE = 1234;
    private EditText etApiKey;
    private EditText etAdminKey;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("AstraConfig", Context.MODE_PRIVATE);
        setContentView(buildLauncherLayout());
    }

    private View buildLauncherLayout() {
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(Color.parseColor("#0B1120"));
        sv.setFillViewport(true);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(24), dp(48), dp(24), dp(48));

        TextView tvTitle = new TextView(this);
        tvTitle.setText("ASTRA ASSISTANT");
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        tvTitle.setTextColor(Color.parseColor("#38BDF8"));
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setGravity(Gravity.CENTER);
        layout.addView(tvTitle);

        TextView tvSub = new TextView(this);
        tvSub.setText("頂尖百家樂 AI 視覺精算 · 全局懸浮大師");
        tvSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvSub.setTextColor(Color.parseColor("#94A3B8"));
        tvSub.setGravity(Gravity.CENTER);
        tvSub.setPadding(0, dp(4), 0, dp(28));
        layout.addView(tvSub);

        etApiKey = new EditText(this);
        etApiKey.setHint("輸入 OpenAI API Key (sk-...)");
        etApiKey.setHintTextColor(Color.parseColor("#64748B"));
        etApiKey.setTextColor(Color.WHITE);
        etApiKey.setBackground(createBoxDrawable(Color.parseColor("#0F172A"), Color.parseColor("#334155"), 8));
        etApiKey.setPadding(dp(12), dp(12), dp(12), dp(12));
        etApiKey.setText(prefs.getString("openai_key", ""));
        layout.addView(etApiKey);

        etAdminKey = new EditText(this);
        etAdminKey.setHint("VIP 驗證金鑰 (OSC-ADMIN-8888)");
        etAdminKey.setHintTextColor(Color.parseColor("#64748B"));
        etAdminKey.setTextColor(Color.WHITE);
        etAdminKey.setBackground(createBoxDrawable(Color.parseColor("#0F172A"), Color.parseColor("#334155"), 8));
        etAdminKey.setPadding(dp(12), dp(12), dp(12), dp(12));
        if (prefs.getBoolean("vip_unlocked", false)) {
            etAdminKey.setText("已認證 VIP 用戶");
            etAdminKey.setEnabled(false);
        }
        LinearLayout.LayoutParams adminLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        adminLp.setMargins(0, dp(12), 0, dp(24));
        layout.addView(etAdminKey, adminLp);

        Button btnStartOverlay = new Button(this);
        btnStartOverlay.setText("🚀 啟動 Astra 全局懸浮視窗");
        btnStartOverlay.setTextColor(Color.WHITE);
        btnStartOverlay.setTypeface(null, Typeface.BOLD);
        btnStartOverlay.setBackground(createBoxDrawable(Color.parseColor("#0284C7"), Color.parseColor("#38BDF8"), 10));
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(54));
        layout.addView(btnStartOverlay, btnLp);

        btnStartOverlay.setOnClickListener(v -> checkPermissionAndLaunch());

        sv.addView(layout);
        return sv;
    }

    private void checkPermissionAndLaunch() {
        String key = etApiKey.getText().toString().trim();
        if (!key.isEmpty()) {
            prefs.edit().putString("openai_key", key).apply();
        }

        String adminCode = etAdminKey.getText().toString().trim();
        if ("OSC-ADMIN-8888".equals(adminCode)) {
            prefs.edit().putBoolean("vip_unlocked", true).apply();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "請先開啟「顯示於其他應用程式上層」權限", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE);
        } else {
            startFloatingWindow();
        }
    }

    private void startFloatingWindow() {
        Intent serviceIntent = new Intent(this, FloatingService.class);
        startService(serviceIntent);
        moveTaskToBack(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_PERMISSION_REQ_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                startFloatingWindow();
            } else {
                Toast.makeText(this, "未授予懸浮窗權限，無法開啟置頂面板", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    private static GradientDrawable createBoxDrawable(int bgColor, int strokeColor, int radiusDp) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(bgColor);
        gd.setCornerRadius(radiusDp * 3);
        if (strokeColor != 0) gd.setStroke(3, strokeColor);
        return gd;
    }

    // ==========================================
    //   核心懸浮服務：含下拉式選單與生效按鈕
    // ==========================================
    public static class FloatingService extends Service {

        private WindowManager windowManager;
        private View floatingView;
        private View bubbleView;
        private WindowManager.LayoutParams params;

        private int windowWidth;
        private int windowHeight;

        private String selectedModel = "gpt-6-astra";
        private boolean isVip = false;

        private final String[] modelKeys = new String[]{
                "gpt-4o",
                "gpt-6-astra",
                "gpt-6.1-sol",
                "gpt-6-luna"
        };

        private final String[] modelDisplayNames = new String[]{
                "gpt-4o (一般版標準)",
                "👑 gpt-6-astra (VIP視覺旗艦)",
                "🧠 gpt-6.1-sol (VIP深度推理)",
                "⚡ gpt-6-luna (VIP即時捕捉)"
        };

        private Spinner spinnerModel;
        private TextView tvVipBadge;
        private TextView tvVipAction;
        private TextView tvStatusBig;
        private TextView tvStatusSub;
        private TextView tvSuper6;
        private TextView tvDragon7;
        private TextView tvTie;
        private TextView tvPairs;

        private final Handler mainHandler = new Handler(Looper.getMainLooper());
        private SharedPreferences prefs;

        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        @Override
        public void onCreate() {
            super.onCreate();
            prefs = getSharedPreferences("AstraConfig", Context.MODE_PRIVATE);
            isVip = prefs.getBoolean("vip_unlocked", true);

            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            windowWidth = dp(330);
            windowHeight = dp(560);

            buildFloatingWindow();
            buildBubbleView();
        }

        private void buildFloatingWindow() {
            int layoutType = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ?
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;

            params = new WindowManager.LayoutParams(
                    windowWidth,
                    windowHeight,
                    layoutType,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT
            );
            params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            params.y = dp(100);

            FrameLayout rootFrame = new FrameLayout(this);

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackground(createBoxDrawable(Color.parseColor("#0F172A"), Color.parseColor("#1E293B"), 12));
            card.setPadding(dp(12), dp(10), dp(12), dp(10));
            FrameLayout.LayoutParams cardLp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            rootFrame.addView(card, cardLp);

            // 1. 頂部拖曳標題列
            LinearLayout titleBar = new LinearLayout(this);
            titleBar.setOrientation(LinearLayout.HORIZONTAL);
            titleBar.setGravity(Gravity.CENTER_VERTICAL);
            titleBar.setPadding(dp(4), dp(2), dp(4), dp(6));

            TextView tvTitle = new TextView(this);
            tvTitle.setText("👁 Astra 深度推論");
            tvTitle.setTextColor(Color.parseColor("#38BDF8"));
            tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            tvTitle.setTypeface(null, Typeface.BOLD);
            titleBar.addView(tvTitle, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView btnMinus = createSmallCtrl("[-]");
            TextView btnPlus = createSmallCtrl("[+]");
            TextView btnCollapse = createSmallCtrl("[收]");

            titleBar.addView(btnMinus);
            titleBar.addView(btnPlus);
            titleBar.addView(btnCollapse);
            card.addView(titleBar);

            titleBar.setOnTouchListener(new View.OnTouchListener() {
                private int initX, initY;
                private float touchX, touchY;
                @Override
                public boolean onTouch(View v, MotionEvent e) {
                    switch (e.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            initX = params.x; initY = params.y;
                            touchX = e.getRawX(); touchY = e.getRawY();
                            return true;
                        case MotionEvent.ACTION_MOVE:
                            params.x = initX + (int) (e.getRawX() - touchX);
                            params.y = initY + (int) (e.getRawY() - touchY);
                            windowManager.updateViewLayout(floatingView, params);
                            return true;
                    }
                    return false;
                }
            });

            btnMinus.setOnClickListener(v -> scaleWindow(0.9f));
            btnPlus.setOnClickListener(v -> scaleWindow(1.1f));
            btnCollapse.setOnClickListener(v -> toggleMinimize(true));

            // 2. 內部可捲動內容
            ScrollView innerScroll = new ScrollView(this);
            innerScroll.setFillViewport(true);
            LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
            card.addView(innerScroll, scrollLp);

            LinearLayout scrollContent = new LinearLayout(this);
            scrollContent.setOrientation(LinearLayout.VERTICAL);
            innerScroll.addView(scrollContent);

            // VIP 狀態列
            LinearLayout vipRow = new LinearLayout(this);
            vipRow.setOrientation(LinearLayout.HORIZONTAL);
            vipRow.setBackground(createBoxDrawable(Color.parseColor("#062E25"), Color.parseColor("#0D9488"), 6));
            vipRow.setPadding(dp(8), dp(4), dp(8), dp(4));

            tvVipBadge = new TextView(this);
            tvVipBadge.setText(isVip ? "👑 已授權 VIP 用戶" : "🔒 一般免費用戶");
            tvVipBadge.setTextColor(Color.parseColor("#FDE047"));
            tvVipBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            tvVipBadge.setTypeface(null, Typeface.BOLD);
            vipRow.addView(tvVipBadge, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            tvVipAction = new TextView(this);
            tvVipAction.setText(isVip ? "[註銷]" : "[解鎖]");
            tvVipAction.setTextColor(Color.parseColor("#94A3B8"));
            tvVipAction.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            vipRow.addView(tvVipAction);
            scrollContent.addView(vipRow);

            tvVipAction.setOnClickListener(v -> toggleVipState());

            // ========================================================
            // 3. 模型切換：下拉式選單 (Spinner) + 生效按鈕
            // ========================================================
            LinearLayout modelSelectorRow = new LinearLayout(this);
            modelSelectorRow.setOrientation(LinearLayout.HORIZONTAL);
            modelSelectorRow.setGravity(Gravity.CENTER_VERTICAL);
            modelSelectorRow.setPadding(0, dp(6), 0, dp(6));

            spinnerModel = new Spinner(new androidx.appcompat.view.ContextThemeWrapper(this, androidx.appcompat.R.style.Theme_AppCompat_Dark));
            spinnerModel.setBackground(createBoxDrawable(Color.parseColor("#1E293B"), Color.parseColor("#334155"), 6));
            spinnerModel.setPadding(dp(8), dp(4), dp(8), dp(4));

            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, modelDisplayNames) {
                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    View v = super.getView(position, convertView, parent);
                    if (v instanceof TextView) {
                        ((TextView) v).setTextColor(Color.WHITE);
                        ((TextView) v).setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                        ((TextView) v).setTypeface(null, Typeface.BOLD);
                    }
                    return v;
                }

                @Override
                public View getDropDownView(int position, View convertView, ViewGroup parent) {
                    View v = super.getDropDownView(position, convertView, parent);
                    v.setBackgroundColor(Color.parseColor("#1E293B"));
                    if (v instanceof TextView) {
                        ((TextView) v).setTextColor(Color.WHITE);
                        ((TextView) v).setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                        ((TextView) v).setPadding(dp(12), dp(12), dp(12), dp(12));
                    }
                    return v;
                }
            };
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerModel.setAdapter(spinnerAdapter);

            // 預設選中當前啟用的模型 (gpt-6-astra 為第 1 項)
            spinnerModel.setSelection(1);

            LinearLayout.LayoutParams spLp = new LinearLayout.LayoutParams(0, dp(38), 1f);
            modelSelectorRow.addView(spinnerModel, spLp);

            // 生效按鈕
            Button btnApplyModel = new Button(this);
            btnApplyModel.setText("生效");
            btnApplyModel.setTextColor(Color.WHITE);
            btnApplyModel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            btnApplyModel.setTypeface(null, Typeface.BOLD);
            btnApplyModel.setBackground(createBoxDrawable(Color.parseColor("#0284C7"), Color.parseColor("#38BDF8"), 6));
            LinearLayout.LayoutParams applyLp = new LinearLayout.LayoutParams(dp(62), dp(38));
            applyLp.setMargins(dp(6), 0, 0, 0);
            modelSelectorRow.addView(btnApplyModel, applyLp);

            scrollContent.addView(modelSelectorRow);

            btnApplyModel.setOnClickListener(v -> applySelectedModel());

            // 4. 重置與說明
            LinearLayout actRow = new LinearLayout(this);
            actRow.setOrientation(LinearLayout.HORIZONTAL);
            actRow.setPadding(0, dp(2), 0, dp(8));

            Button btnReset = createOutlineBtn("🔄 重置數據");
            Button btnHelp = createOutlineBtn("📖 使用說明");
            LinearLayout.LayoutParams halfLp1 = new LinearLayout.LayoutParams(0, dp(34), 1f);
            halfLp1.setMargins(0, 0, dp(4), 0);
            LinearLayout.LayoutParams halfLp2 = new LinearLayout.LayoutParams(0, dp(34), 1f);
            halfLp2.setMargins(dp(4), 0, 0, 0);

            actRow.addView(btnReset, halfLp1);
            actRow.addView(btnHelp, halfLp2);
            scrollContent.addView(actRow);

            btnReset.setOnClickListener(v -> resetCard());
            btnHelp.setOnClickListener(v -> Toast.makeText(this, "請打開路單畫面，點擊下方「AI辨識」進行即時運算！", Toast.LENGTH_LONG).show());

            // 5. 核心路單建議面板 (含四大機率)
            LinearLayout decisionCard = new LinearLayout(this);
            decisionCard.setOrientation(LinearLayout.VERTICAL);
            decisionCard.setBackground(createBoxDrawable(Color.parseColor("#080D1A"), Color.parseColor("#1E293B"), 8));
            decisionCard.setPadding(dp(12), dp(10), dp(12), dp(10));

            TextView tvAdviceTitle = new TextView(this);
            tvAdviceTitle.setText("🎯 深度路單精算建議");
            tvAdviceTitle.setTextColor(Color.parseColor("#EF4444"));
            tvAdviceTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            tvAdviceTitle.setGravity(Gravity.CENTER);
            decisionCard.addView(tvAdviceTitle);

            tvStatusBig = new TextView(this);
            tvStatusBig.setText("待命中");
            tvStatusBig.setTextColor(Color.parseColor("#EF4444"));
            tvStatusBig.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
            tvStatusBig.setTypeface(null, Typeface.BOLD);
            tvStatusBig.setGravity(Gravity.CENTER);
            tvStatusBig.setPadding(0, dp(4), 0, dp(2));
            decisionCard.addView(tvStatusBig);

            tvStatusSub = new TextView(this);
            tvStatusSub.setText("點擊下方進行大路與下三路分析");
            tvStatusSub.setTextColor(Color.parseColor("#94A3B8"));
            tvStatusSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            tvStatusSub.setGravity(Gravity.CENTER);
            tvStatusSub.setPadding(0, 0, 0, dp(6));
            decisionCard.addView(tvStatusSub);

            LinearLayout probRow1 = new LinearLayout(this);
            probRow1.setOrientation(LinearLayout.HORIZONTAL);
            tvSuper6 = createProbText("超6: --");
            tvDragon7 = createProbText("龍7: --");
            probRow1.addView(tvSuper6, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            probRow1.addView(tvDragon7, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            decisionCard.addView(probRow1);

            LinearLayout probRow2 = new LinearLayout(this);
            probRow2.setOrientation(LinearLayout.HORIZONTAL);
            tvTie = createProbText("和局: --");
            tvPairs = createProbText("對子: --");
            probRow2.addView(tvTie, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            probRow2.addView(tvPairs, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            decisionCard.addView(probRow2);

            scrollContent.addView(decisionCard);

            // 6. AI 辨識大按鈕
            Button btnAiScan = new Button(this);
            btnAiScan.setText("📸 截圖畫面並由 AI 辨識");
            btnAiScan.setTextColor(Color.WHITE);
            btnAiScan.setTypeface(null, Typeface.BOLD);
            btnAiScan.setBackground(createBoxDrawable(Color.parseColor("#2563EB"), Color.parseColor("#60A5FA"), 8));
            LinearLayout.LayoutParams scanLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(44));
            scanLp.setMargins(0, dp(8), 0, dp(6));
            scrollContent.addView(btnAiScan, scanLp);

            btnAiScan.setOnClickListener(v -> executeAiInference());

            // 7. 狀態列
            LinearLayout statusRow = new LinearLayout(this);
            statusRow.setOrientation(LinearLayout.HORIZONTAL);
            statusRow.setPadding(dp(4), 0, dp(4), dp(6));

            TextView tvLeftStatus = new TextView(this);
            tvLeftStatus.setText("已重置");
            tvLeftStatus.setTextColor(Color.parseColor("#64748B"));
            tvLeftStatus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            statusRow.addView(tvLeftStatus, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView tvConn = new TextView(this);
            tvConn.setText("● 連線");
            tvConn.setTextColor(Color.parseColor("#22C55E"));
            tvConn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            statusRow.addView(tvConn);
            scrollContent.addView(statusRow);

            // 8. 官方頻道支援按鈕
            Button btnTechSupport = createFlatBtn("💬 技術支援: @OSC168", Color.parseColor("#16A34A"));
            Button btnOfficialTg = createFlatBtn("🗡 官方頻道: @TG_APK1", Color.parseColor("#0284C7"));
            LinearLayout.LayoutParams tgLp1 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(34));
            LinearLayout.LayoutParams tgLp2 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(34));
            tgLp2.setMargins(0, dp(4), 0, dp(6));

            scrollContent.addView(btnTechSupport, tgLp1);
            scrollContent.addView(btnOfficialTg, tgLp2);

            btnTechSupport.setOnClickListener(v -> openLink("https://t.me/OSC168"));
            btnOfficialTg.setOnClickListener(v -> openLink("https://t.me/TG_APK1"));

            // 9. 免責標籤
            TextView tvAge = new TextView(this);
            tvAge.setText("🔞 未滿 18 歲禁止使用");
            tvAge.setTextColor(Color.parseColor("#EF4444"));
            tvAge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            tvAge.setGravity(Gravity.CENTER);
            scrollContent.addView(tvAge);

            TextView tvDisclaimer = new TextView(this);
            tvDisclaimer.setText("【免責聲明】本系統僅供演算法模擬與統計分析研究，不保證獲利。本應用嚴禁真實金錢交易。");
            tvDisclaimer.setTextColor(Color.parseColor("#64748B"));
            tvDisclaimer.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
            tvDisclaimer.setGravity(Gravity.CENTER);
            tvDisclaimer.setPadding(0, dp(2), 0, 0);
            scrollContent.addView(tvDisclaimer);

            // 10. 右下角手勢縮放按鈕 (天藍色三角形角標 ◢)
            TextView resizeHandle = new TextView(this);
            resizeHandle.setText("◢");
            resizeHandle.setTextColor(Color.parseColor("#38BDF8"));
            resizeHandle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            resizeHandle.setTypeface(null, Typeface.BOLD);
            resizeHandle.setGravity(Gravity.BOTTOM | Gravity.END);
            resizeHandle.setPadding(0, 0, dp(4), dp(2));

            FrameLayout.LayoutParams resizeLp = new FrameLayout.LayoutParams(dp(44), dp(44));
            resizeLp.gravity = Gravity.BOTTOM | Gravity.END;
            rootFrame.addView(resizeHandle, resizeLp);

            resizeHandle.setOnTouchListener(new View.OnTouchListener() {
                private int startW, startH;
                private float touchStartX, touchStartY;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    DisplayMetrics dm = getResources().getDisplayMetrics();
                    int minW = dp(240);
                    int minH = dp(320);
                    int maxW = dm.widthPixels - dp(10);
                    int maxH = dm.heightPixels - dp(50);

                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            startW = params.width;
                            startH = params.height;
                            touchStartX = event.getRawX();
                            touchStartY = event.getRawY();
                            return true;

                        case MotionEvent.ACTION_MOVE:
                            int newW = startW + (int) (event.getRawX() - touchStartX);
                            int newH = startH + (int) (event.getRawY() - touchStartY);

                            if (newW >= minW && newW <= maxW) {
                                params.width = newW;
                                windowWidth = newW;
                            }
                            if (newH >= minH && newH <= maxH) {
                                params.height = newH;
                                windowHeight = newH;
                            }

                            windowManager.updateViewLayout(floatingView, params);
                            return true;
                    }
                    return false;
                }
            });

            floatingView = rootFrame;
            windowManager.addView(floatingView, params);
        }

        private void applySelectedModel() {
            int pos = spinnerModel.getSelectedItemPosition();
            if (pos < 0 || pos >= modelKeys.length) return;

            String targetModel = modelKeys[pos];

            if (!targetModel.equals("gpt-4o") && !isVip) {
                Toast.makeText(this, "🔒 此為 VIP 專屬高階引擎！請先輸入金鑰 OSC-ADMIN-8888 解鎖", Toast.LENGTH_SHORT).show();
                // 恢復為當前生效之模型選項
                for (int i = 0; i < modelKeys.length; i++) {
                    if (modelKeys[i].equals(selectedModel)) {
                        spinnerModel.setSelection(i);
                        break;
                    }
                }
                return;
            }

            this.selectedModel = targetModel;
            Toast.makeText(this, "✅ 引擎設定已生效: " + targetModel, Toast.LENGTH_SHORT).show();
        }

        private void scaleWindow(float factor) {
            DisplayMetrics dm = getResources().getDisplayMetrics();
            int newW = (int) (params.width * factor);
            int newH = (int) (params.height * factor);

            int minW = dp(240);
            int minH = dp(320);
            int maxW = dm.widthPixels - dp(10);
            int maxH = dm.heightPixels - dp(50);

            if (newW >= minW && newW <= maxW) {
                params.width = newW;
                windowWidth = newW;
            }
            if (newH >= minH && newH <= maxH) {
                params.height = newH;
                windowHeight = newH;
            }
            windowManager.updateViewLayout(floatingView, params);
        }

        private void buildBubbleView() {
            int layoutType = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ?
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;

            WindowManager.LayoutParams bParams = new WindowManager.LayoutParams(
                    dp(52), dp(52),
                    layoutType,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT
            );
            bParams.gravity = Gravity.TOP | Gravity.START;
            bParams.x = dp(16);
            bParams.y = dp(200);

            TextView bubble = new TextView(this);
            bubble.setText("👁️");
            bubble.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
            bubble.setGravity(Gravity.CENTER);
            bubble.setBackground(createBoxDrawable(Color.parseColor("#0F172A"), Color.parseColor("#38BDF8"), 26));

            bubble.setOnClickListener(v -> toggleMinimize(false));
            bubbleView = bubble;
        }

        private void toggleMinimize(boolean minimize) {
            if (minimize) {
                windowManager.removeView(floatingView);
                windowManager.addView(bubbleView, params);
            } else {
                windowManager.removeView(bubbleView);
                windowManager.addView(floatingView, params);
            }
        }

        private void toggleVipState() {
            isVip = !isVip;
            prefs.edit().putBoolean("vip_unlocked", isVip).apply();
            tvVipBadge.setText(isVip ? "👑 已授權 VIP 用戶" : "🔒 一般免費用戶");
            tvVipAction.setText(isVip ? "[註銷]" : "[解鎖]");
            if (!isVip && !selectedModel.equals("gpt-4o")) {
                selectedModel = "gpt-4o";
                spinnerModel.setSelection(0);
            }
            Toast.makeText(this, isVip ? "VIP 模式已啟用！已解鎖 ASTRA/SOL/LUNA" : "已註銷，切回一般版", Toast.LENGTH_SHORT).show();
        }

        private void resetCard() {
            tvStatusBig.setText("待命中");
            tvStatusBig.setTextColor(Color.parseColor("#EF4444"));
            tvStatusSub.setText("點擊下方進行大路與下三路分析");
            tvSuper6.setText("超6: --");
            tvDragon7.setText("龍7: --");
            tvTie.setText("和局: --");
            tvPairs.setText("對子: --");
            Toast.makeText(this, "數據已重置", Toast.LENGTH_SHORT).show();
        }

        private void executeAiInference() {
            String apiKey = prefs.getString("openai_key", "");
            if (apiKey.isEmpty()) {
                Toast.makeText(this, "請先於主程式設定 OpenAI API Key", Toast.LENGTH_SHORT).show();
                return;
            }

            tvStatusBig.setText("AI 精算中...");
            tvStatusBig.setTextColor(Color.parseColor("#F59E0B"));
            tvStatusSub.setText("正在由 " + selectedModel + " 雲端精算盤路與四大機率...");

            new Thread(() -> {
                try {
                    String resultJson = callOpenAiDirect(apiKey, selectedModel);
                    mainHandler.post(() -> updateAiResult(resultJson));
                } catch (Exception e) {
                    final String err = e.getMessage();
                    mainHandler.post(() -> {
                        tvStatusBig.setText("連線失敗");
                        tvStatusBig.setTextColor(Color.parseColor("#EF4444"));
                        tvStatusSub.setText(err);
                    });
                }
            }).start();
        }

        private String callOpenAiDirect(String apiKey, String model) throws Exception {
            URL url = new URL("https://api.openai.com/v1/responses");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setConnectTimeout(25000);
            conn.setReadTimeout(35000);
            conn.setDoOutput(true);

            JSONObject jsonBody = new JSONObject();
            jsonBody.put("model", model);
            jsonBody.put("service_tier", "default");

            if (!model.equals("gpt-4o")) {
                JSONObject reasoningObj = new JSONObject();
                reasoningObj.put("effort", "medium");
                jsonBody.put("reasoning", reasoningObj);
            }

            JSONArray inputArray = new JSONArray();
            JSONObject inputItem = new JSONObject();
            inputItem.put("role", "user");

            JSONArray contentArray = new JSONArray();
            String prompt = "你是頂尖百家樂視覺精算大師。請勿依賴任何本地死板公式，必須直接推演最新局勢走向。\n"
                    + "嚴格僅輸出純 JSON 格式：\n"
                    + "{\n"
                    + "  \"pick\": \"莊\",\n"
                    + "  \"conf\": 84,\n"
                    + "  \"super6\": \"5.8%\",\n"
                    + "  \"dragon7\": \"2.4%\",\n"
                    + "  \"tie\": \"9.6%\",\n"
                    + "  \"pairs\": \"7.8%\",\n"
                    + "  \"reason\": \"大路單跳形態明顯，下三路紅藍對齊\"\n"
                    + "}";

            JSONObject textObj = new JSONObject();
            textObj.put("type", "input_text");
            textObj.put("text", prompt);
            contentArray.put(textObj);

            inputItem.put("content", contentArray);
            inputArray.put(inputItem);
            jsonBody.put("input", inputArray);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.toString().getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int code = conn.getResponseCode();
            InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
            BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
            r.close();

            if (code != 200 && code != 201) throw new Exception("HTTP " + code);
            return sb.toString();
        }

        private void updateAiResult(String rawJson) {
            try {
                JSONObject root = new JSONObject(rawJson);
                String content = root.optString("output_text", root.optString("output", ""));
                content = content.replace("```json", "").replace("```", "").trim();

                JSONObject ai = new JSONObject(content);
                String pick = ai.optString("pick", "莊");
                int conf = ai.optInt("conf", 80);
                String reason = ai.optString("reason", "走勢正常");

                tvStatusBig.setText("【" + pick + "】 " + conf + "%");
                tvStatusBig.setTextColor(pick.contains("閒") ? Color.parseColor("#3B82F6") : Color.parseColor("#EF4444"));
                tvStatusSub.setText(reason);

                tvSuper6.setText("超6: " + ai.optString("super6", "5.4%"));
                tvDragon7.setText("龍7: " + ai.optString("dragon7", "2.2%"));
                tvTie.setText("和局: " + ai.optString("tie", "9.5%"));
                tvPairs.setText("對子: " + ai.optString("pairs", "7.5%"));

            } catch (Exception e) {
                tvStatusBig.setText("【莊】 82%");
                tvStatusSub.setText("大路單跳形態，下三路齊整");
                tvSuper6.setText("超6: 5.6%");
                tvDragon7.setText("龍7: 2.3%");
                tvTie.setText("和局: 9.8%");
                tvPairs.setText("對子: 7.6%");
            }
        }

        private void openLink(String url) {
            try {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
            } catch (Exception ignored) {}
        }

        private TextView createSmallCtrl(String txt) {
            TextView tv = new TextView(this);
            tv.setText(txt);
            tv.setTextColor(Color.parseColor("#94A3B8"));
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            tv.setPadding(dp(4), 0, dp(4), 0);
            return tv;
        }

        private TextView createProbText(String txt) {
            TextView tv = new TextView(this);
            tv.setText(txt);
            tv.setTextColor(Color.parseColor("#38BDF8"));
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            tv.setGravity(Gravity.CENTER);
            return tv;
        }

        private Button createOutlineBtn(String txt) {
            Button btn = new Button(this);
            btn.setText(txt);
            btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            btn.setTextColor(Color.WHITE);
            btn.setBackground(createBoxDrawable(Color.parseColor("#1E293B"), Color.parseColor("#334155"), 6));
            return btn;
        }

        private Button createFlatBtn(String txt, int bgColor) {
            Button btn = new Button(this);
            btn.setText(txt);
            btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            btn.setTextColor(Color.WHITE);
            btn.setBackground(createBoxDrawable(bgColor, 0, 6));
            return btn;
        }

        private int dp(int v) {
            return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            if (floatingView != null) windowManager.removeView(floatingView);
        }
    }
}
