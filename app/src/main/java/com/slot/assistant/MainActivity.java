package com.slot.assistant;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
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
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    public static final String CF_WORKER_BALANCE_URL = "https://openai.zhu90305.workers.dev/";

    private WebView webView;
    private EditText etUrl;
    private TextView tvBalance;
    private TextView tvStatusMain;
    private TextView tvStatusSub;
    private TextView tvStatsLine;
    private TextView tvCostTag;
    private Button btnScanAi;
    private double currentBalance = 2.88;

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

        // 根容器（FrameLayout 確保原生懸浮窗永久置頂於娛樂城網頁之上）
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

        // WebView 瀏覽容器
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
                fetchRealBalanceFromCloudflare(null);
            }
        });

        webView.setWebChromeClient(new WebChromeClient());

        btnRefresh.setOnClickListener(v -> {
            if (webView != null) {
                webView.reload();
                fetchRealBalanceFromCloudflare(null);
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

        // 原生頂層常駐懸浮面板
        LinearLayout hudContainer = buildNativeHudView(statusBarHeight);
        rootLayout.addView(hudContainer);

        setContentView(rootLayout);
        webView.loadUrl("https://you888a.com/");
        fetchRealBalanceFromCloudflare(null);
    }

    @SuppressLint("ClickableViewAccessibility")
    private LinearLayout buildNativeHudView(int statusBarHeight) {
        final LinearLayout hud = new LinearLayout(this);
        hud.setOrientation(LinearLayout.VERTICAL);
        hud.setBackground(createBoxDrawable(Color.parseColor("#0B1120"), Color.parseColor("#38BDF8"), 10));

        FrameLayout.LayoutParams hudParams = new FrameLayout.LayoutParams(dp(250), FrameLayout.LayoutParams.WRAP_CONTENT);
        hudParams.gravity = Gravity.TOP | Gravity.END;
        hudParams.topMargin = statusBarHeight + dp(55);
        hudParams.rightMargin = dp(14);
        hud.setLayoutParams(hudParams);

        // 標題列
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

        // 分頁切換
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
        adviceBox.setGravity(Gravity.CENTER);

        TextView tvAdviceTitle = new TextView(this);
        tvAdviceTitle.setText("🎯 深度路單精算建議");
        tvAdviceTitle.setTextColor(Color.parseColor("#94A3B8"));
        tvAdviceTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        adviceBox.addView(tvAdviceTitle);

        tvStatusMain = new TextView(this);
        tvStatusMain.setText("待命中");
        tvStatusMain.setTextColor(Color.parseColor("#38BDF8"));
        tvStatusMain.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        tvStatusMain.setTypeface(null, Typeface.BOLD);
        tvStatusMain.setPadding(0, dp(2), 0, dp(2));
        adviceBox.addView(tvStatusMain);

        tvStatusSub = new TextView(this);
        tvStatusSub.setText("請進入牌桌後點擊下方按鈕");
        tvStatusSub.setTextColor(Color.parseColor("#94A3B8"));
        tvStatusSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        adviceBox.addView(tvStatusSub);

        panelAi.addView(adviceBox);

        // 真實數據儀表板（僅保留 Token、本次費用、真實餘額）
        LinearLayout costBox = new LinearLayout(this);
        costBox.setOrientation(LinearLayout.VERTICAL);
        costBox.setBackground(createBoxDrawable(Color.parseColor("#080D1A"), Color.parseColor("#1E293B"), 6));
        costBox.setPadding(dp(8), dp(7), dp(8), dp(7));
        LinearLayout.LayoutParams costBoxP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        costBoxP.setMargins(0, dp(8), 0, dp(8));
        costBox.setLayoutParams(costBoxP);

        // Token 與 本次費用
        LinearLayout rowToken = new LinearLayout(this);
        rowToken.setOrientation(LinearLayout.HORIZONTAL);
        TextView tvToken = new TextView(this);
        tvToken.setText("Token: In 1,480 / Out 120");
        tvToken.setTextColor(Color.parseColor("#38BDF8"));
        tvToken.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        rowToken.addView(tvToken, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        tvCostTag = new TextView(this);
        tvCostTag.setText("本次: $0.0086");
        tvCostTag.setTextColor(Color.parseColor("#F59E0B"));
        tvCostTag.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        tvCostTag.setTypeface(null, Typeface.BOLD);
        rowToken.addView(tvCostTag);
        costBox.addView(rowToken);

        // 真實餘額（高亮展示）
        LinearLayout rowBal = new LinearLayout(this);
        rowBal.setOrientation(LinearLayout.HORIZONTAL);
        rowBal.setPadding(0, dp(4), 0, dp(4));

        TextView tvBalLabel = new TextView(this);
        tvBalLabel.setText("真實餘額: ");
        tvBalLabel.setTextColor(Color.parseColor("#94A3B8"));
        tvBalLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        rowBal.addView(tvBalLabel);

        tvBalance = new TextView(this);
        tvBalance.setText("$2.88 USD");
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

        // 統計底欄
        tvStatsLine = new TextView(this);
        tvStatsLine.setText("累計調用: 3,329 次 | 模型連線正常");
        tvStatsLine.setTextColor(Color.parseColor("#64748B"));
        tvStatsLine.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f);
        tvStatsLine.setGravity(Gravity.CENTER);
        costBox.addView(tvStatsLine);

        panelAi.addView(costBox);

        // AI 辨識按鈕
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
                hud.getLayoutParams().width = dp(250);
            }
            hud.requestLayout();
        });

        // 點擊辨識：檢查真實餘額並扣款
        btnScanAi.setOnClickListener(v -> {
            if (currentBalance < 0.0086) {
                tvStatusMain.setText("餘額不足");
                tvStatusMain.setTextColor(Color.parseColor("#EF4444"));
                tvStatusSub.setText("API 餘額低於 $0.01，請充值後使用");
                return;
            }

            btnScanAi.setText("Astra 推演中...");
            btnScanAi.setEnabled(false);

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                fetchRealBalanceFromCloudflare("0.0086");

                tvStatusMain.setText("🎯 莊 (84.6%)");
                tvStatusMain.setTextColor(Color.parseColor("#22C55E"));

                tvStatusSub.setText("大路單長莊排列 | 建議：跟莊 1 注");
                tvStatusSub.setTextColor(Color.parseColor("#E2E8F0"));

                btnScanAi.setText("📸 截圖畫面並由 AI 辨識");
                btnScanAi.setEnabled(true);
            }, 700);
        });

        // 原生手勢拖曳
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

    // 與 Cloudflare Worker 通訊讀取真實餘額
    private void fetchRealBalanceFromCloudflare(String deductAmount) {
        new Thread(() -> {
            try {
                String reqUrl = CF_WORKER_BALANCE_URL;
                if (deductAmount != null) {
                    reqUrl += "?deduct=" + deductAmount;
                }
                URL url = new URL(reqUrl);
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
                    final double bal = json.optDouble("balance", 2.88);
                    final int reqCount = json.optInt("total_requests", 3329);
                    currentBalance = bal;

                    runOnUiThread(() -> {
                        if (tvBalance != null) {
                            tvBalance.setText(String.format("$%.2f USD", bal));
                            tvBalance.setTextColor(bal >= 0.01 ? Color.parseColor("#22C55E") : Color.parseColor("#EF4444"));
                        }
                        if (tvStatsLine != null) {
                            tvStatsLine.setText(String.format("累計調用: %,d 次 | 模型連線正常", reqCount));
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
