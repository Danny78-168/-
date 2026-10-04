package com.slot.assistant;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.PixelCopy;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    public static final String CF_WORKER_BALANCE_URL = "https://openai.zhu90305.workers.dev/";

    private WebView webView;
    private EditText etUrl;
    private TextView tvBalance;
    private TextView tvStatusMain;
    private TextView tvBoardCounts;
    private TextView tvSpecialChances;
    private TextView tvStatusSub;
    private TextView tvStatsLine;
    private Button btnScanAi;
    private LinearLayout hudLayout;
    private double currentBalance = 1.15;

    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int statusBarHeight = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = getResources().getDimensionPixelSize(resourceId);
        }
        if (statusBarHeight <= 0) {
            statusBarHeight = dp(32);
        }

        FrameLayout rootLayout = new FrameLayout(this);
        rootLayout.setBackgroundColor(Color.parseColor("#0B1120"));

        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        // 頂部導航列
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setBackgroundColor(Color.parseColor("#0F172A"));
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(dp(10), statusBarHeight + dp(6), dp(10), dp(10));

        etUrl = new EditText(this);
        etUrl.setText("https://you888a.com/");
        etUrl.setTextColor(Color.WHITE);
        etUrl.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        etUrl.setHint("請輸入或貼上任何遊戲網址...");
        etUrl.setHintTextColor(Color.parseColor("#64748B"));
        etUrl.setBackground(createBoxDrawable(Color.parseColor("#1E293B"), Color.parseColor("#334155"), 6));
        etUrl.setPadding(dp(10), dp(8), dp(10), dp(8));
        etUrl.setSingleLine(true);
        LinearLayout.LayoutParams etParams = new LinearLayout.LayoutParams(0, dp(38), 1f);
        topBar.addView(etUrl, etParams);

        Button btnRefresh = new Button(this);
        btnRefresh.setText("重新整理");
        btnRefresh.setTextColor(Color.WHITE);
        btnRefresh.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btnRefresh.setTypeface(null, Typeface.BOLD);
        btnRefresh.setBackground(createBoxDrawable(Color.parseColor("#334155"), Color.parseColor("#64748B"), 6));
        LinearLayout.LayoutParams refreshParams = new LinearLayout.LayoutParams(dp(76), dp(38));
        refreshParams.setMargins(dp(6), 0, 0, 0);
        topBar.addView(btnRefresh, refreshParams);

        Button btnGo = new Button(this);
        btnGo.setText("前往");
        btnGo.setTextColor(Color.WHITE);
        btnGo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btnGo.setTypeface(null, Typeface.BOLD);
        btnGo.setBackground(createBoxDrawable(Color.parseColor("#2563EB"), Color.parseColor("#38BDF8"), 6));
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(dp(56), dp(38));
        btnParams.setMargins(dp(6), 0, 0, 0);
        topBar.addView(btnGo, btnParams);

        mainLayout.addView(topBar);

        webView = new WebView(this);
        LinearLayout.LayoutParams webParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        mainLayout.addView(webView, webParams);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setUserAgentString("Mozilla/5.0 (Linux; Android 14; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith("intent://") || url.startsWith("tg://") || url.startsWith("line://")) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(intent);
                        return true;
                    } catch (Exception ignored) {}
                }
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                etUrl.setText(url);
                fetchRealBalance();
            }
        });

        webView.setWebChromeClient(new WebChromeClient());

        btnRefresh.setOnClickListener(v -> {
            if (webView != null) {
                webView.reload();
                fetchRealBalance();
            }
        });

        btnGo.setOnClickListener(v -> {
            String url = etUrl.getText().toString().trim();
            if (!url.isEmpty()) {
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "https://" + url;
                }
                webView.loadUrl(url);
            }
        });

        rootLayout.addView(mainLayout);

        // 原生懸浮面板
        hudLayout = buildNativeHudView(statusBarHeight);
        rootLayout.addView(hudLayout);

        setContentView(rootLayout);
        webView.loadUrl("https://you888a.com/");
        fetchRealBalance();
    }

    @SuppressLint("ClickableViewAccessibility")
    private LinearLayout buildNativeHudView(int statusBarHeight) {
        final LinearLayout hud = new LinearLayout(this);
        hud.setOrientation(LinearLayout.VERTICAL);
        hud.setBackground(createBoxDrawable(Color.parseColor("#0B1120"), Color.parseColor("#38BDF8"), 10));

        FrameLayout.LayoutParams hudParams = new FrameLayout.LayoutParams(dp(260), FrameLayout.LayoutParams.WRAP_CONTENT);
        hudParams.gravity = Gravity.TOP | Gravity.END;
        hudParams.topMargin = statusBarHeight + dp(55);
        hudParams.rightMargin = dp(12);
        hud.setLayoutParams(hudParams);

        // 頂部標題列
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setBackground(createBoxDrawable(Color.parseColor("#1E293B"), 0, 10));
        header.setPadding(dp(10), dp(8), dp(10), dp(8));
        header.setGravity(Gravity.CENTER_VERTICAL);

        final TextView tvTitle = new TextView(this);
        tvTitle.setText("👁 Astra 深度推論");
        tvTitle.setTextColor(Color.parseColor("#38BDF8"));
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvTitle.setTypeface(null, Typeface.BOLD);
        header.addView(tvTitle, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        final TextView tvCollapse = new TextView(this);
        tvCollapse.setText("[收]");
        tvCollapse.setTextColor(Color.parseColor("#38BDF8"));
        tvCollapse.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvCollapse.setTypeface(null, Typeface.BOLD);
        tvCollapse.setPadding(dp(4), dp(2), dp(4), dp(2));
        header.addView(tvCollapse);

        hud.addView(header);

        final LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);

        // 分頁列
        LinearLayout tabBar = new LinearLayout(this);
        tabBar.setOrientation(LinearLayout.HORIZONTAL);
        tabBar.setBackgroundColor(Color.parseColor("#0F172A"));

        final TextView tabAi = new TextView(this);
        tabAi.setText("🎯 AI 精算");
        tabAi.setTextColor(Color.parseColor("#38BDF8"));
        tabAi.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tabAi.setTypeface(null, Typeface.BOLD);
        tabAi.setGravity(Gravity.CENTER);
        tabAi.setPadding(0, dp(8), 0, dp(8));
        tabBar.addView(tabAi, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        final TextView tabService = new TextView(this);
        tabService.setText("💬 反饋客服");
        tabService.setTextColor(Color.parseColor("#94A3B8"));
        tabService.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tabService.setTypeface(null, Typeface.BOLD);
        tabService.setGravity(Gravity.CENTER);
        tabService.setPadding(0, dp(8), 0, dp(8));
        tabBar.addView(tabService, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        body.addView(tabBar);

        final LinearLayout panelAi = new LinearLayout(this);
        panelAi.setOrientation(LinearLayout.VERTICAL);
        panelAi.setPadding(dp(10), dp(8), dp(10), dp(10));

        // 模型標籤
        LinearLayout engineBar = new LinearLayout(this);
        engineBar.setOrientation(LinearLayout.HORIZONTAL);
        engineBar.setGravity(Gravity.CENTER_VERTICAL);
        engineBar.setPadding(0, 0, 0, dp(6));

        TextView tvEngine = new TextView(this);
        tvEngine.setText("引擎: gpt-6-astra");
        tvEngine.setTextColor(Color.parseColor("#38BDF8"));
        tvEngine.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        engineBar.addView(tvEngine, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvVip = new TextView(this);
        tvVip.setText("VIP 旗艦");
        tvVip.setTextColor(Color.parseColor("#F59E0B"));
        tvVip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
        tvVip.setBackground(createBoxDrawable(Color.parseColor("#1E293B"), 0, 3));
        tvVip.setPadding(dp(5), dp(1), dp(5), dp(1));
        engineBar.addView(tvVip);

        panelAi.addView(engineBar);

        // 建議面板
        LinearLayout adviceBox = new LinearLayout(this);
        adviceBox.setOrientation(LinearLayout.VERTICAL);
        adviceBox.setBackground(createBoxDrawable(Color.parseColor("#0F172A"), Color.parseColor("#3B82F6"), 6));
        adviceBox.setPadding(dp(8), dp(6), dp(8), dp(6));

        // 1. 未來預測標題與大字
        tvStatusMain = new TextView(this);
        tvStatusMain.setText("待命中");
        tvStatusMain.setTextColor(Color.parseColor("#38BDF8"));
        tvStatusMain.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        tvStatusMain.setTypeface(null, Typeface.BOLD);
        tvStatusMain.setGravity(Gravity.CENTER);
        adviceBox.addView(tvStatusMain);

        // 2. 盤面統計顆數
        tvBoardCounts = new TextView(this);
        tvBoardCounts.setText("盤面統計: 莊 -- | 閒 -- | 和 -- | 超6 --");
        tvBoardCounts.setTextColor(Color.parseColor("#E2E8F0"));
        tvBoardCounts.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f);
        tvBoardCounts.setGravity(Gravity.CENTER);
        tvBoardCounts.setPadding(0, dp(3), 0, dp(2));
        adviceBox.addView(tvBoardCounts);

        // 3. 特殊牌型機會（對子、超6、超和）
        tvSpecialChances = new TextView(this);
        tvSpecialChances.setText("對子機會: -- | 超6: -- | 超和: --");
        tvSpecialChances.setTextColor(Color.parseColor("#FBBF24"));
        tvSpecialChances.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f);
        tvSpecialChances.setTypeface(null, Typeface.BOLD);
        tvSpecialChances.setGravity(Gravity.CENTER);
        tvSpecialChances.setPadding(0, 0, 0, dp(3));
        adviceBox.addView(tvSpecialChances);

        // 4. 路單形態精算說明
        tvStatusSub = new TextView(this);
        tvStatusSub.setText("進入牌桌後點擊下方按鈕由 Astra 推算");
        tvStatusSub.setTextColor(Color.parseColor("#94A3B8"));
        tvStatusSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f);
        tvStatusSub.setGravity(Gravity.CENTER);
        adviceBox.addView(tvStatusSub);

        panelAi.addView(adviceBox);

        // 費用與真實餘額
        LinearLayout costBox = new LinearLayout(this);
        costBox.setOrientation(LinearLayout.VERTICAL);
        costBox.setBackground(createBoxDrawable(Color.parseColor("#080D1A"), Color.parseColor("#1E293B"), 6));
        costBox.setPadding(dp(8), dp(7), dp(8), dp(7));
        LinearLayout.LayoutParams costBoxP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        costBoxP.setMargins(0, dp(8), 0, dp(8));
        costBox.setLayoutParams(costBoxP);

        LinearLayout rowToken = new LinearLayout(this);
        rowToken.setOrientation(LinearLayout.HORIZONTAL);
        TextView tvToken = new TextView(this);
        tvToken.setText("Token: In 1,480 / Out 120");
        tvToken.setTextColor(Color.parseColor("#38BDF8"));
        tvToken.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        rowToken.addView(tvToken, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvCostTag = new TextView(this);
        tvCostTag.setText("本次: $0.0086");
        tvCostTag.setTextColor(Color.parseColor("#F59E0B"));
        tvCostTag.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        tvCostTag.setTypeface(null, Typeface.BOLD);
        rowToken.addView(tvCostTag);
        costBox.addView(rowToken);

        LinearLayout rowBal = new LinearLayout(this);
        rowBal.setOrientation(LinearLayout.HORIZONTAL);
        rowBal.setPadding(0, dp(4), 0, dp(4));

        TextView tvBalLabel = new TextView(this);
        tvBalLabel.setText("真實餘額: ");
        tvBalLabel.setTextColor(Color.parseColor("#94A3B8"));
        tvBalLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        rowBal.addView(tvBalLabel);

        tvBalance = new TextView(this);
        tvBalance.setText("$1.15 USD");
        tvBalance.setTextColor(Color.parseColor("#22C55E"));
        tvBalance.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        tvBalance.setTypeface(null, Typeface.BOLD);
        rowBal.addView(tvBalance, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvMin = new TextView(this);
        tvMin.setText("(最低需 $0.01)");
        tvMin.setTextColor(Color.parseColor("#64748B"));
        tvMin.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f);
        rowBal.addView(tvMin);
        costBox.addView(rowBal);

        tvStatsLine = new TextView(this);
        tvStatsLine.setText("累計調用: 3,466 次 | 視覺推演模式已就緒");
        tvStatsLine.setTextColor(Color.parseColor("#64748B"));
        tvStatsLine.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f);
        tvStatsLine.setGravity(Gravity.CENTER);
        costBox.addView(tvStatsLine);

        panelAi.addView(costBox);

        // 推論按鈕
        btnScanAi = new Button(this);
        btnScanAi.setText("📸 截圖畫面並由 AI 辨識");
        btnScanAi.setTextColor(Color.WHITE);
        btnScanAi.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        btnScanAi.setTypeface(null, Typeface.BOLD);
        btnScanAi.setBackground(createBoxDrawable(Color.parseColor("#2563EB"), 0, 6));
        panelAi.addView(btnScanAi, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(38)));

        body.addView(panelAi);

        // 客服面板
        final LinearLayout panelService = new LinearLayout(this);
        panelService.setOrientation(LinearLayout.VERTICAL);
        panelService.setPadding(dp(10), dp(8), dp(10), dp(10));
        panelService.setVisibility(View.GONE);

        Button btnTg = new Button(this);
        btnTg.setText("✈ Telegram: @TG_APK1");
        btnTg.setTextColor(Color.WHITE);
        btnTg.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        btnTg.setTypeface(null, Typeface.BOLD);
        btnTg.setBackground(createBoxDrawable(Color.parseColor("#0284C7"), 0, 4));
        LinearLayout.LayoutParams tgP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(36));
        tgP.setMargins(0, 0, 0, dp(6));
        panelService.addView(btnTg, tgP);

        Button btnLine = new Button(this);
        btnLine.setText("💬 LINE 客服: @OSC168");
        btnLine.setTextColor(Color.WHITE);
        btnLine.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        btnLine.setTypeface(null, Typeface.BOLD);
        btnLine.setBackground(createBoxDrawable(Color.parseColor("#16A34A"), 0, 4));
        panelService.addView(btnLine, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(36)));

        body.addView(panelService);
        hud.addView(body);

        btnTg.setOnClickListener(v -> openExternalUrl("https://t.me/TG_APK1"));
        btnLine.setOnClickListener(v -> openExternalUrl("https://lin.ee/NfoQ9DH"));

        tabAi.setOnClickListener(v -> {
            tabAi.setTextColor(Color.parseColor("#38BDF8"));
            tabService.setTextColor(Color.parseColor("#94A3B8"));
            panelAi.setVisibility(View.VISIBLE);
            panelService.setVisibility(View.GONE);
        });

        tabService.setOnClickListener(v -> {
            tabService.setTextColor(Color.parseColor("#38BDF8"));
            tabAi.setTextColor(Color.parseColor("#94A3B8"));
            panelService.setVisibility(View.VISIBLE);
            panelAi.setVisibility(View.GONE);
        });

        tvCollapse.setOnClickListener(v -> {
            if (body.getVisibility() == View.VISIBLE) {
                body.setVisibility(View.GONE);
                tvTitle.setText("👁");
                tvCollapse.setText("[展]");
                hud.getLayoutParams().width = dp(75);
            } else {
                body.setVisibility(View.VISIBLE);
                tvTitle.setText("👁 Astra 深度推論");
                tvCollapse.setText("[收]");
                hud.getLayoutParams().width = dp(260);
            }
            hud.requestLayout();
        });

        // 點擊辨識：透過 PixelCopy 截圖
        btnScanAi.setOnClickListener(v -> {
            if (currentBalance < 0.0086) {
                tvStatusMain.setText("餘額不足");
                tvStatusMain.setTextColor(Color.parseColor("#EF4444"));
                tvStatusSub.setText("API 餘額低於 $0.01，請充值後使用");
                return;
            }

            btnScanAi.setText("📸 擷取畫面中...");
            btnScanAi.setEnabled(false);

            hud.setVisibility(View.INVISIBLE);

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                View decorView = getWindow().getDecorView();
                final Bitmap bitmap = Bitmap.createBitmap(decorView.getWidth(), decorView.getHeight(), Bitmap.Config.ARGB_8888);

                PixelCopy.request(getWindow(), bitmap, copyResult -> {
                    hud.setVisibility(View.VISIBLE);

                    if (copyResult == PixelCopy.SUCCESS) {
                        btnScanAi.setText("Astra 推演中...");

                        ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 75, baos);
                        final String base64Image = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);

                        sendScreenshotToWorker(base64Image);
                    } else {
                        btnScanAi.setText("📸 截圖畫面並由 AI 辨識");
                        btnScanAi.setEnabled(true);
                        tvStatusMain.setText("截圖失敗");
                        tvStatusSub.setText("PixelCopy GPU 複製失敗，請重試");
                    }
                }, new Handler(Looper.getMainLooper()));
            }, 50);
        });

        // 拖曳處理
        final float[] dX = new float[1];
        final float[] dY = new float[1];
        header.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    dX[0] = hud.getX() - event.getRawX();
                    dY[0] = hud.getY() - event.getRawY();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float newX = event.getRawX() + dX[0];
                    float newY = event.getRawY() + dY[0];
                    View parent = (View) hud.getParent();
                    if (parent != null) {
                        float maxX = parent.getWidth() - hud.getWidth();
                        float maxY = parent.getHeight() - hud.getHeight();
                        hud.setX(Math.max(0, Math.min(newX, maxX)));
                        hud.setY(Math.max(0, Math.min(newY, maxY)));
                    }
                    return true;
                default:
                    return false;
            }
        });

        return hud;
    }

    private void sendScreenshotToWorker(String base64Image) {
        new Thread(() -> {
            try {
                URL url = new URL(CF_WORKER_BALANCE_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(30000);
                conn.setReadTimeout(30000);

                JSONObject reqJson = new JSONObject();
                reqJson.put("image", base64Image);
                reqJson.put("user", "default_user");

                OutputStream os = conn.getOutputStream();
                os.write(reqJson.toString().getBytes("UTF-8"));
                os.close();

                int responseCode = conn.getResponseCode();
                BufferedReader br = new BufferedReader(new InputStreamReader(
                        responseCode >= 200 && responseCode < 300 ? conn.getInputStream() : conn.getErrorStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();

                JSONObject resJson = new JSONObject(sb.toString());

                runOnUiThread(() -> {
                    btnScanAi.setText("📸 截圖畫面並由 AI 辨識");
                    btnScanAi.setEnabled(true);

                    if (responseCode >= 200 && responseCode < 300 && resJson.optString("status").equals("success")) {
                        String analysis = resJson.optString("analysis", "");
                        double bal = resJson.optDouble("balance", currentBalance);
                        currentBalance = bal;

                        // 解析 4 行數據並分區渲染
                        String[] lines = analysis.split("\n");
                        if (lines.length > 0) {
                            String p1 = lines[0].replace("【", "").replace("】", "").trim();
                            tvStatusMain.setText(p1);
                            if (p1.contains("莊")) {
                                tvStatusMain.setTextColor(Color.parseColor("#EF4444"));
                            } else {
                                tvStatusMain.setTextColor(Color.parseColor("#38BDF8"));
                            }
                        }
                        if (lines.length > 1) {
                            tvBoardCounts.setText(lines[1].trim());
                        }
                        if (lines.length > 2) {
                            tvSpecialChances.setText(lines[2].trim());
                        }
                        if (lines.length > 3) {
                            tvStatusSub.setText(lines[3].trim());
                        }

                        if (tvBalance != null) {
                            tvBalance.setText(String.format("$%.4f USD", bal));
                        }
                    } else {
                        String errMsg = resJson.optString("message", "模型推演異常");
                        tvStatusMain.setText("推演失敗");
                        tvStatusMain.setTextColor(Color.parseColor("#EF4444"));
                        tvStatusSub.setText(errMsg);
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    btnScanAi.setText("📸 截圖畫面並由 AI 辨識");
                    btnScanAi.setEnabled(true);
                    tvStatusMain.setText("連線逾時");
                    tvStatusMain.setTextColor(Color.parseColor("#EF4444"));
                    tvStatusSub.setText("請確認網路或 API 狀態");
                });
            }
        }).start();
    }

    private void fetchRealBalance() {
        new Thread(() -> {
            try {
                URL url = new URL(CF_WORKER_BALANCE_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(6000);

                if (conn.getResponseCode() >= 200 && conn.getResponseCode() < 300) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                    br.close();

                    JSONObject json = new JSONObject(sb.toString());
                    final double bal = json.optDouble("balance", 1.15);
                    currentBalance = bal;

                    runOnUiThread(() -> {
                        if (tvBalance != null) {
                            tvBalance.setText(String.format("$%.4f USD", bal));
                            tvBalance.setTextColor(bal >= 0.01 ? Color.parseColor("#22C55E") : Color.parseColor("#EF4444"));
                        }
                    });
                }
            } catch (Exception ignored) {}
        }).start();
    }

    private void openExternalUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception ignored) {}
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    private GradientDrawable createBoxDrawable(int bgColor, int strokeColor, int radiusDp) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(bgColor);
        gd.setCornerRadius(radiusDp * 3);
        if (strokeColor != 0) gd.setStroke(3, strokeColor);
        return gd;
    }
}
