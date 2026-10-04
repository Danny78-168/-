package com.slot.assistant;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private EditText etUrl;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. 取得手機狀態列實際高度，避免頂部按鈕被時間、電量覆蓋
        int statusBarHeight = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = getResources().getDimensionPixelSize(resourceId);
        }
        if (statusBarHeight <= 0) {
            statusBarHeight = dp(32);
        }

        // 2. 根佈局
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#0B1120"));

        // 3. 頂部網址列（加入頂部狀態列安全距離）
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setBackgroundColor(Color.parseColor("#0F172A"));
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(dp(10), statusBarHeight + dp(6), dp(10), dp(10));

        etUrl = new EditText(this);
        etUrl.setText("https://www.google.com/");
        etUrl.setTextColor(Color.WHITE);
        etUrl.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        etUrl.setHint("請輸入或貼上任何遊戲網址...");
        etUrl.setHintTextColor(Color.parseColor("#64748B"));
        etUrl.setBackground(createBoxDrawable(Color.parseColor("#1E293B"), Color.parseColor("#334155"), 6));
        etUrl.setPadding(dp(10), dp(8), dp(10), dp(8));
        etUrl.setSingleLine(true);
        LinearLayout.LayoutParams etParams = new LinearLayout.LayoutParams(0, dp(38), 1f);
        topBar.addView(etUrl, etParams);

        // 重新整理按鈕
        Button btnRefresh = new Button(this);
        btnRefresh.setText("重新整理");
        btnRefresh.setTextColor(Color.WHITE);
        btnRefresh.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btnRefresh.setTypeface(null, Typeface.BOLD);
        btnRefresh.setBackground(createBoxDrawable(Color.parseColor("#334155"), Color.parseColor("#64748B"), 6));
        LinearLayout.LayoutParams refreshParams = new LinearLayout.LayoutParams(dp(76), dp(38));
        refreshParams.setMargins(dp(6), 0, 0, 0);
        topBar.addView(btnRefresh, refreshParams);

        // 前往按鈕
        Button btnGo = new Button(this);
        btnGo.setText("前往");
        btnGo.setTextColor(Color.WHITE);
        btnGo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btnGo.setTypeface(null, Typeface.BOLD);
        btnGo.setBackground(createBoxDrawable(Color.parseColor("#2563EB"), Color.parseColor("#38BDF8"), 6));
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(dp(56), dp(38));
        btnParams.setMargins(dp(6), 0, 0, 0);
        topBar.addView(btnGo, btnParams);

        root.addView(topBar);

        // 4. WebView 容器
        webView = new WebView(this);
        LinearLayout.LayoutParams webParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        root.addView(webView, webParams);

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

        webView.addJavascriptInterface(new AndroidBridge(), "AndroidBridge");

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
                injectAssistantScript();
            }
        });

        webView.setWebChromeClient(new WebChromeClient());

        btnRefresh.setOnClickListener(v -> {
            if (webView != null) webView.reload();
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

        setContentView(root);
        webView.loadUrl("https://www.google.com/");
    }

    public class AndroidBridge {
        @JavascriptInterface
        public void openExternalUrl(String url) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            } catch (Exception ignored) {}
        }

        @JavascriptInterface
        public void requestVisualAnalysis() {
            runOnUiThread(() -> {
                String js = "if(window.__showInsufficientBalance){ window.__showInsufficientBalance(); }";
                webView.evaluateJavascript(js, null);
            });
        }
    }

    private void injectAssistantScript() {
        String js = "(function() {"
                + "  if (document.getElementById('astra-hud-container')) return;"
                + "  var root = document.documentElement || document.body;"
                + "  if (!root) return;"
                + ""
                + "  function bindTap(el, fn) {"
                + "    if (!el) return;"
                + "    var moved = false;"
                + "    el.addEventListener('touchstart', function(e) { moved = false; e.stopPropagation(); }, { passive: true });"
                + "    el.addEventListener('touchmove', function(e) { moved = true; }, { passive: true });"
                + "    el.addEventListener('touchend', function(e) {"
                + "      e.stopPropagation();"
                + "      if (!moved) { e.preventDefault(); fn(); }"
                + "    });"
                + "    el.addEventListener('click', function(e) { e.stopPropagation(); fn(); });"
                + "  }"
                + ""
                + "  var hud = document.createElement('div');"
                + "  hud.id = 'astra-hud-container';"
                + "  hud.style.cssText = 'position:fixed;top:100px;right:14px;width:235px;background:rgba(11,17,32,0.96);border:1.5px solid #38bdf8;border-radius:10px;z-index:2147483647;color:#f1f5f9;font-size:11px;box-shadow:0 8px 30px rgba(0,0,0,0.9);font-family:sans-serif;user-select:none;-webkit-user-select:none;backdrop-filter:blur(8px);display:flex;flex-direction:column;overflow:hidden;touch-action:manipulation;';"
                + ""
                + "  hud.innerHTML = "
                + "    '<div id=\"hud_header\" style=\"padding:8px 10px;background:#1e293b;border-radius:9px 9px 0 0;font-weight:bold;color:#38bdf8;display:flex;justify-content:space-between;align-items:center;border-bottom:1px solid #334155;cursor:move;touch-action:none;\">' +"
                + "      '<span id=\"hud_title\">👁 Astra 深度推論</span>' +"
                + "      '<span id=\"hud_collapse\" style=\"cursor:pointer;color:#38bdf8;font-size:12px;font-weight:bold;padding:2px 6px;\">[收]</span>' +"
                + "    '</div>' +"
                + "    '<div id=\"hud_body\">' +"
                + "      '<div style=\"display:flex;border-bottom:1px solid #334155;background:#0f172a;\">' +"
                + "        '<div id=\"tab_ai\" style=\"flex:1;text-align:center;padding:8px 0;font-size:11px;font-weight:bold;color:#38bdf8;border-bottom:2px solid #38bdf8;cursor:pointer;\">🎯 AI 精算</div>' +"
                + "        '<div id=\"tab_service\" style=\"flex:1;text-align:center;padding:8px 0;font-size:11px;font-weight:bold;color:#94a3b8;border-bottom:2px solid transparent;cursor:pointer;\">💬 反饋客服</div>' +"
                + "      '</div>' +"
                + "      '<div id=\"panel_ai\" style=\"padding:10px 8px 12px 8px;\">' +"
                + "        '<div style=\"background:rgba(15,23,42,0.9);border:1px solid #3b82f6;border-radius:6px;padding:8px 4px;text-align:center;margin-bottom:8px;\">' +"
                + "          '<div style=\"font-size:10px;color:#94a3b8;\">🎯 深度路單精算建議</div>' +"
                + "          '<div id=\"ai_status_main\" style=\"font-size:18px;font-weight:900;color:#ef4444;margin:3px 0;\">待命中</div>' +"
                + "          '<div id=\"ai_status_sub\" style=\"font-size:10px;color:#94a3b8;\">請進入牌桌後點擊下方按鈕</div>' +"
                + "        '</div>' +"
                + "        '<div style=\"background:#080d1a;border:1px solid #1e293b;border-radius:4px;padding:5px 0;text-align:center;margin-bottom:8px;\">' +"
                + "          '<span id=\"ai_stats_line\" style=\"font-size:10px;color:#64748b;\">總數: -- | 莊: -- | 和: -- | 閒: --</span>' +"
                + "        '</div>' +"
                + "        '<button id=\"btn_scan_ai\" style=\"width:100%;background:#2563eb;color:#fff;border:none;padding:9px 0;border-radius:6px;font-weight:bold;font-size:11px;cursor:pointer;box-shadow:0 2px 8px rgba(37,99,235,0.4);\">📸 截圖畫面並由 AI 辨識</button>' +"
                + "      '</div>' +"
                + "      '<div id=\"panel_service\" style=\"padding:10px 8px 12px 8px;display:none;flex-direction:column;gap:6px;\">' +"
                + "        '<button id=\"btn_tg_cs\" style=\"width:100%;background:#0284c7;color:#fff;border:none;padding:8px 0;border-radius:4px;font-size:10px;font-weight:bold;cursor:pointer;\">✈ Telegram: @TG_APK1</button>' +"
                + "        '<button id=\"btn_line_cs\" style=\"width:100%;background:#16a34a;color:#fff;border:none;padding:8px 0;border-radius:4px;font-size:10px;font-weight:bold;cursor:pointer;\">💬 LINE 客服: @OSC168</button>' +"
                + "      '</div>' +"
                + "    '</div>';"
                + "  root.appendChild(hud);"
                + ""
                + "  // 懸浮窗拖曳處理"
                + "  var header = document.getElementById('hud_header');"
                + "  var isDrag = false, startX, startY, initLeft, initTop;"
                + "  header.addEventListener('touchstart', function(e) {"
                + "    if (e.target.id === 'hud_collapse') return;"
                + "    isDrag = true; var t = e.touches[0]; var r = hud.getBoundingClientRect();"
                + "    startX = t.clientX; startY = t.clientY; initLeft = r.left; initTop = r.top;"
                + "  }, { passive: true });"
                + "  header.addEventListener('touchmove', function(e) {"
                + "    if (!isDrag) return;"
                + "    var t = e.touches[0];"
                + "    var nx = initLeft + (t.clientX - startX);"
                + "    var ny = initTop + (t.clientY - startY);"
                + "    hud.style.left = Math.max(0, Math.min(nx, window.innerWidth - hud.offsetWidth)) + 'px';"
                + "    hud.style.top = Math.max(0, Math.min(ny, window.innerHeight - hud.offsetHeight)) + 'px';"
                + "    hud.style.right = 'auto';"
                + "  }, { passive: false });"
                + "  header.addEventListener('touchend', function() { isDrag = false; });"
                + ""
                + "  // 收合 / 展開綁定"
                + "  var colBtn = document.getElementById('hud_collapse');"
                + "  var body = document.getElementById('hud_body');"
                + "  var title = document.getElementById('hud_title');"
                + "  bindTap(colBtn, function() {"
                + "    if (body.style.display === 'none') {"
                + "      body.style.display = 'block';"
                + "      hud.style.width = '235px';"
                + "      title.innerText = '👁 Astra 深度推論';"
                + "      colBtn.innerText = '[收]';"
                + "    } else {"
                + "      body.style.display = 'none';"
                + "      hud.style.width = '70px';"
                + "      title.innerText = '👁';"
                + "      colBtn.innerText = '[展]';"
                + "    }"
                + "  });"
                + ""
                + "  // 分頁切換綁定"
                + "  var tabAi = document.getElementById('tab_ai');"
                + "  var tabService = document.getElementById('tab_service');"
                + "  var panelAi = document.getElementById('panel_ai');"
                + "  var panelService = document.getElementById('panel_service');"
                + "  bindTap(tabAi, function() {"
                + "    tabAi.style.color = '#38bdf8'; tabAi.style.borderBottom = '2px solid #38bdf8';"
                + "    tabService.style.color = '#94a3b8'; tabService.style.borderBottom = '2px solid transparent';"
                + "    panelAi.style.display = 'block'; panelService.style.display = 'none';"
                + "  });"
                + "  bindTap(tabService, function() {"
                + "    tabService.style.color = '#38bdf8'; tabService.style.borderBottom = '2px solid #38bdf8';"
                + "    tabAi.style.color = '#94a3b8'; tabAi.style.borderBottom = '2px solid transparent';"
                + "    panelService.style.display = 'flex'; panelAi.style.display = 'none';"
                + "  });"
                + ""
                + "  // 客服按鈕綁定"
                + "  bindTap(document.getElementById('btn_tg_cs'), function() {"
                + "    if (window.AndroidBridge) window.AndroidBridge.openExternalUrl('https://t.me/TG_APK1');"
                + "  });"
                + "  bindTap(document.getElementById('btn_line_cs'), function() {"
                + "    if (window.AndroidBridge) window.AndroidBridge.openExternalUrl('https://lin.ee/NfoQ9DH');"
                + "  });"
                + ""
                + "  // AI 辨識按鈕綁定"
                + "  var btnScan = document.getElementById('btn_scan_ai');"
                + "  bindTap(btnScan, function() {"
                + "    btnScan.innerText = '推演中...'; btnScan.disabled = true;"
                + "    setTimeout(function() {"
                + "      if (window.AndroidBridge) window.AndroidBridge.requestVisualAnalysis();"
                + "    }, 300);"
                + "  });"
                + ""
                + "  window.__showInsufficientBalance = function() {"
                + "    var mainT = document.getElementById('ai_status_main');"
                + "    var subT = document.getElementById('ai_status_sub');"
                + "    var statT = document.getElementById('ai_stats_line');"
                + "    if (mainT) { mainT.innerText = '餘額不足'; mainT.style.color = '#ef4444'; }"
                + "    if (subT) { subT.innerText = 'API 餘額不足，請充值後使用'; subT.style.color = '#94a3b8'; }"
                + "    if (statT) { statT.innerText = '總數: -- | 扣款失敗'; statT.style.color = '#f87171'; }"
                + "    if (btnScan) { btnScan.innerText = '📸 截圖畫面並由 AI 辨識'; btnScan.disabled = false; }"
                + "  };"
                + "})();";
        webView.evaluateJavascript(js, null);
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
