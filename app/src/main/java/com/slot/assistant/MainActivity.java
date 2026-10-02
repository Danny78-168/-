package com.slot.assistant;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final String OPENAI_KEY_PART1 = "sk-proj-dwQyYlJrpRoJqtP9ZCcPjQzDUtQXJi1MT1sd6OfsMdW7RF";
    private static final String OPENAI_KEY_PART2 = "OIOwKJ1JSgi2Satw9WoTaiC8WHPxT3BlbkFJhhCPi2LwrFZ3k7mbJ_LSvLLm65LHzcjTbnqkvKEyKsBgbRlmJzX8X0pGNyrvgH-vPN9sAcwiwA";

    private static String getOpenAIKey() {
        return (OPENAI_KEY_PART1 + OPENAI_KEY_PART2).trim();
    }

    private WebView webView;
    private LinearLayout topBar;
    private EditText urlInput;
    private Button btnRestoreBar;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private boolean isAnalyzing = false;

    private final Runnable hudWatchdog = new Runnable() {
        @Override
        public void run() {
            if (webView != null) {
                injectAssistantScript(webView);
                webView.postDelayed(this, 1500);
            }
        }
    };

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FrameLayout rootLayout = new FrameLayout(this);
        rootLayout.setBackgroundColor(Color.parseColor("#0b1120"));
        setContentView(rootLayout);

        LinearLayout mainContainer = new LinearLayout(this);
        mainContainer.setOrientation(LinearLayout.VERTICAL);
        rootLayout.addView(mainContainer, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        // 頂部導航列
        topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setBackgroundColor(Color.parseColor("#0f172a"));
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(dp(6), dp(4), dp(6), dp(4));
        mainContainer.addView(topBar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(46)));

        Button btnBack = new Button(this);
        btnBack.setText("◀");
        btnBack.setTextColor(Color.WHITE);
        btnBack.setTextSize(13);
        btnBack.setBackgroundColor(Color.TRANSPARENT);
        topBar.addView(btnBack, new LinearLayout.LayoutParams(dp(36), dp(36)));
        btnBack.setOnClickListener(v -> {
            if (webView.canGoBack()) webView.goBack();
        });

        urlInput = new EditText(this);
        urlInput.setHint("輸入或貼上任何遊戲網址...");
        urlInput.setHintTextColor(Color.parseColor("#64748b"));
        urlInput.setTextColor(Color.parseColor("#38bdf8"));
        urlInput.setTextSize(12);
        urlInput.setSingleLine(true);
        urlInput.setImeOptions(EditorInfo.IME_ACTION_GO);
        urlInput.setPadding(dp(8), 0, dp(8), 0);

        GradientDrawable inputBg = new GradientDrawable();
        inputBg.setColor(Color.parseColor("#1e293b"));
        inputBg.setCornerRadius(dp(6));
        inputBg.setStroke(dp(1), Color.parseColor("#475569"));
        urlInput.setBackground(inputBg);

        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(0, dp(34), 1.0f);
        inputParams.setMargins(dp(4), 0, dp(4), 0);
        topBar.addView(urlInput, inputParams);

        Button btnGo = new Button(this);
        btnGo.setText("前往");
        btnGo.setTextColor(Color.WHITE);
        btnGo.setTextSize(12);

        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setColor(Color.parseColor("#2563eb"));
        btnBg.setCornerRadius(dp(6));
        btnGo.setBackground(btnBg);

        topBar.addView(btnGo, new LinearLayout.LayoutParams(dp(50), dp(34)));
        btnGo.setOnClickListener(v -> navigateUrl());
        urlInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO) {
                navigateUrl();
                return true;
            }
            return false;
        });

        Button btnHide = new Button(this);
        btnHide.setText("全螢幕");
        btnHide.setTextColor(Color.parseColor("#94a3b8"));
        btnHide.setTextSize(11);
        btnHide.setBackgroundColor(Color.TRANSPARENT);
        topBar.addView(btnHide, new LinearLayout.LayoutParams(dp(54), dp(34)));
        btnHide.setOnClickListener(v -> {
            topBar.setVisibility(View.GONE);
            btnRestoreBar.setVisibility(View.VISIBLE);
        });

        btnRestoreBar = new Button(this);
        btnRestoreBar.setText("網址列");
        btnRestoreBar.setTextColor(Color.WHITE);
        btnRestoreBar.setTextSize(10);
        btnRestoreBar.setVisibility(View.GONE);

        GradientDrawable restoreBg = new GradientDrawable();
        restoreBg.setColor(Color.parseColor("#1e293b"));
        restoreBg.setCornerRadius(dp(4));
        restoreBg.setStroke(dp(1), Color.parseColor("#38bdf8"));
        btnRestoreBar.setBackground(restoreBg);

        FrameLayout.LayoutParams rParams = new FrameLayout.LayoutParams(dp(54), dp(26));
        rParams.gravity = Gravity.TOP | Gravity.START;
        rParams.setMargins(dp(6), dp(6), 0, 0);
        rootLayout.addView(btnRestoreBar, rParams);
        btnRestoreBar.setOnClickListener(v -> {
            topBar.setVisibility(View.VISIBLE);
            btnRestoreBar.setVisibility(View.GONE);
        });

        webView = new WebView(this);
        mainContainer.addView(webView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f));

        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);
        webView.requestFocus(View.FOCUS_DOWN);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        }

        settings.setUserAgentString("Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36");

        webView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void requestVisualAnalysis(final String mode) {
                captureAndAnalyze(mode);
            }
            @JavascriptInterface
            public void openExternalUrl(final String url) {
                openExternal(url);
            }
        }, "AndroidBridge");

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                if (newProgress >= 70) injectAssistantScript(view);
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith("https://lin.ee/") || url.startsWith("https://t.me/") ||
                    url.startsWith("line://") || url.startsWith("tg://")) {
                    openExternal(url);
                    return true;
                }
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (url != null && !url.equals("about:blank")) {
                    urlInput.setText(url);
                }
                injectAssistantScript(view);
            }
        });

        webView.loadUrl("https://www.google.com");
        webView.postDelayed(hudWatchdog, 1000);
    }

    private void openExternal(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception ignored) {}
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void navigateUrl() {
        String target = urlInput.getText().toString().trim();
        if (target.isEmpty()) return;
        if (!target.startsWith("http://") && !target.startsWith("https://")) {
            target = "https://" + target;
        }
        webView.loadUrl(target);
    }

    private void captureAndAnalyze(final String mode) {
        if (isAnalyzing) return;
        isAnalyzing = true;

        runOnUiThread(() -> {
            try {
                int w = webView.getWidth();
                int h = webView.getHeight();
                if (w <= 0 || h <= 0) {
                    isAnalyzing = false;
                    updateHUDWithError("畫面載入中");
                    return;
                }

                float scale = 720f / w;
                int targetW = 720;
                int targetH = (int) (h * scale);

                Bitmap bitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                canvas.scale(scale, scale);
                webView.draw(canvas);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos);
                byte[] imageBytes = baos.toByteArray();
                final String base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP);
                bitmap.recycle();

                executor.execute(() -> {
                    try {
                        String resultJson = callOpenAIAstra(base64Image, mode);
                        updateHUDWithResult(resultJson);
                    } catch (Exception e) {
                        updateHUDWithError(e.getMessage());
                    } finally {
                        isAnalyzing = false;
                    }
                });
            } catch (Exception e) {
                isAnalyzing = false;
                updateHUDWithError("截圖失敗: " + e.getMessage());
            }
        });
    }

    private String callOpenAIAstra(String base64Image, String mode) throws Exception {
        URL url = new URL("https://api.openai.com/v1/responses");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + getOpenAIKey());
        conn.setConnectTimeout(20000);
        conn.setReadTimeout(25000);
        conn.setDoOutput(true);

        JSONObject jsonBody = new JSONObject();
        jsonBody.put("model", "gpt-6-astra");
        jsonBody.put("service_tier", "default");

        JSONObject reasoningObj = new JSONObject();
        reasoningObj.put("effort", "medium");
        jsonBody.put("reasoning", reasoningObj);

        JSONArray inputArray = new JSONArray();
        JSONObject inputItem = new JSONObject();
        inputItem.put("role", "user");

        JSONArray contentArray = new JSONArray();

        JSONObject textObj = new JSONObject();
        textObj.put("type", "input_text");
        String prompt = "你是頂尖百家樂視覺精算大師。當前模式設定為【" + mode.toUpperCase() + "】。\n" +
                "【通用視覺辨識指引】：\n" +
                "1. 觀察畫面中的路單走勢（大路、珠盤路、下三路）與即時底欄比分。\n" +
                "2. 嚴禁回答『畫面仍在載入』或『數據未知』！務必識別真實比分填入 stats！\n" +
                "3. 嚴禁輸出觀望！必須強制二選一【莊】或【閒】，信心度評估於 68%~92% 之間。\n\n" +
                "嚴格僅輸出純 JSON 物件：\n" +
                "{\"pick\":\"莊\",\"conf\":80,\"reason\":\"大路單跳形態，下三路齊整\",\"stats\":\"庄16 闲16 和7 (39局)\"}";
        textObj.put("text", prompt);
        contentArray.put(textObj);

        JSONObject imgObj = new JSONObject();
        imgObj.put("type", "input_image");
        imgObj.put("image_url", "data:image/jpeg;base64," + base64Image);
        contentArray.put(imgObj);

        inputItem.put("content", contentArray);
        inputArray.put(inputItem);
        jsonBody.put("input", inputArray);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.toString().getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();

        if (code >= 400) throw new Exception("HTTP " + code + " " + sb.toString());
        return extractJsonFromResponse(sb.toString());
    }

    private String extractJsonFromResponse(String responseText) {
        try {
            JSONObject root = new JSONObject(responseText);
            if (root.has("output")) {
                JSONArray output = root.getJSONArray("output");
                for (int i = 0; i < output.length(); i++) {
                    JSONObject outItem = output.getJSONObject(i);
                    if (outItem.has("content")) {
                        JSONArray contents = outItem.getJSONArray("content");
                        for (int j = 0; j < contents.length(); j++) {
                            JSONObject c = contents.getJSONObject(j);
                            if (c.has("text")) {
                                String t = cleanJson(c.getString("text"));
                                if (t.contains("pick")) return t;
                            }
                        }
                    }
                }
            }
            if (root.has("choices")) {
                String c = root.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
                return cleanJson(c);
            }
        } catch (Exception ignored) {}
        return cleanJson(responseText);
    }

    private String cleanJson(String text) {
        if (text == null) return "{}";
        text = text.trim();
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start != -1 && end > start) {
            return text.substring(start, end + 1).trim();
        }
        return text;
    }

    private void updateHUDWithResult(final String jsonStr) {
        runOnUiThread(() -> {
            try {
                JSONObject obj = new JSONObject(jsonStr);
                String pick = obj.optString("pick", "莊");
                int conf = obj.optInt("conf", 65);
                String reason = obj.optString("reason", "走勢推論");
                String stats = obj.optString("stats", "統計更新");

                String js = String.format("window.__updateAI && window.__updateAI('%s', %d, '%s', '%s');",
                        pick, conf, reason, stats);
                webView.evaluateJavascript(js, null);
            } catch (Exception e) {
                updateHUDWithError("解析格式異常");
            }
        });
    }

    private void updateHUDWithError(final String errMsg) {
        runOnUiThread(() -> {
            String js = String.format("window.__updateAIError && window.__updateAIError('%s');", errMsg);
            webView.evaluateJavascript(js, null);
        });
    }

    private void injectAssistantScript(WebView view) {
        String js = "javascript:(function() {" +
            "if (document.getElementById('slot-assistant-hud')) return;" +
            "var rootTarget = document.documentElement || document.body;" +
            "if (!rootTarget) return;" +

            "function bindTap(el, fn) {" +
            "  if (!el) return;" +
            "  var moved = false;" +
            "  el.addEventListener('touchstart', function(e) { moved = false; e.stopPropagation(); }, { passive: true });" +
            "  el.addEventListener('touchmove', function(e) { moved = true; }, { passive: true });" +
            "  el.addEventListener('touchend', function(e) {" +
            "    e.stopPropagation();" +
            "    if (!moved) { e.preventDefault(); fn(); }" +
            "  });" +
            "  el.addEventListener('click', function(e) { e.stopPropagation(); fn(); });" +
            "}" +

            "var curMode = '通用';" +

            "var hud = document.createElement('div');" +
            "hud.id = 'slot-assistant-hud';" +
            "hud.style.cssText = 'position:fixed;top:60px;right:10px;width:205px;background:rgba(11,17,32,0.96);border:1.5px solid #38bdf8;border-radius:10px;z-index:2147483647;color:#f1f5f9;font-size:11px;box-shadow:0 8px 30px rgba(0,0,0,0.9);font-family:sans-serif;user-select:none;backdrop-filter:blur(8px);';" +
            "hud.innerHTML = " +
                "'<div id=\"hud_header\" style=\"padding:7px 10px;background:#1e293b;border-radius:9px 9px 0 0;font-weight:bold;color:#38bdf8;display:flex;justify-content:space-between;align-items:center;border-bottom:1px solid #334155;cursor:move;touch-action:none;\">" +
                    "<span>👁 Astra 深度推論</span>" +
                    "<span id=\"hud_tog\" style=\"cursor:pointer;color:#94a3b8;font-size:10px;margin-left:3px;\">[收]</span>" +
                "</div>' +" +
                "'<div id=\"hud_content\" style=\"padding:10px;\">' +" +
                    // 導航入口：贊助作者 (USDT-TRC20) + 註冊
                    "'<div style=\"display:grid;grid-template-columns:1fr 1fr;gap:4px;margin-bottom:6px;\">" +
                        "<button id=\"nav_sponsor\" style=\"background:#0f172a;border:1px solid #f59e0b;color:#f59e0b;padding:5px 0;border-radius:4px;font-size:10px;font-weight:bold;cursor:pointer;\">🪙 贊助作者</button>" +
                        "<button id=\"nav_reg\" style=\"background:#2563eb;border:1px solid #38bdf8;color:#fff;padding:5px 0;border-radius:4px;font-size:10px;font-weight:bold;cursor:pointer;\">註冊入口</button>" +
                    "</div>' +" +
                    "'<div style=\"background:rgba(15,23,42,0.85);border:1px solid #3b82f6;border-radius:6px;padding:8px 4px;text-align:center;margin-bottom:6px;\">" +
                        "<div style=\"font-size:10px;color:#94a3b8;\">🎯 深度路單精算建議</div>" +
                        "<div id=\"ai_pick_target\" style=\"font-size:18px;font-weight:900;color:#ef4444;margin:2px 0;\">待命中</div>" +
                        "<div id=\"ai_pick_desc\" style=\"font-size:10px;color:#38bdf8;\">點擊下方進行大路與下三路分析</div>" +
                    "</div>' +" +
                    "'<button id=\"btn_do_ai\" style=\"width:100%;background:#2563eb;color:#fff;border:none;padding:8px 0;border-radius:4px;font-weight:bold;margin-bottom:6px;font-size:11px;cursor:pointer;\">📸 截圖畫面並由 AI 辨識</button>' +" +
                    "'<div style=\"display:flex;justify-content:space-between;font-size:10px;color:#94a3b8;\">" +
                        "<span id=\"hud_score\">未掃描</span>" +
                        "<span id=\"hud_sync_dot\" style=\"color:#4ade80;\">● 連線</span>" +
                    "</div>' +" +
                    // 代理聯繫按鈕
                    "'<div style=\"margin-top:8px;padding-top:6px;border-top:1px solid #334155;display:flex;flex-direction:column;gap:5px;\">' +" +
                        "'<button id=\"btn_line\" style=\"width:100%;background:#06c755;border:none;color:#fff;padding:5px 0;border-radius:4px;font-size:10px;font-weight:bold;cursor:pointer;\">💬 LINE: @OSC168</button>' +" +
                        "'<button id=\"btn_tg\" style=\"width:100%;background:#0088cc;border:none;color:#fff;padding:5px 0;border-radius:4px;font-size:10px;font-weight:bold;cursor:pointer;\">✈️ Telegram: @TG_APK1</button>' +" +
                    "'</div>' +" +
                    // 警語與免責聲明
                    "'<div style=\"margin-top:8px;padding-top:6px;border-top:1px dashed #475569;font-size:8.5px;color:#94a3b8;line-height:1.35;text-align:center;\">' +" +
                        "'<div style=\"color:#f87171;font-weight:bold;margin-bottom:2px;\">🔞 未滿 18 歲禁止使用</div>' +" +
                        "'<div style=\"color:#64748b;\">【免責聲明】本系統僅供演算法與大數據統計模擬，不保證獲利。本應用嚴禁且不提供任何真實金錢交易、儲值或博弈服務，請遵守當地法規。</div>' +" +
                    "'</div>' +" +
                "'</div>';" +
            "rootTarget.appendChild(hud);" +

            "var header = document.getElementById('hud_header');" +
            "var isDrag = false, sX, sY, iL, iT;" +
            "header.addEventListener('touchstart', function(e) {" +
            "  if (e.target.closest('button') || e.target.closest('#hud_tog')) { isDrag = false; return; }" +
            "  isDrag = true; var t = e.touches[0]; var r = hud.getBoundingClientRect();" +
            "  sX = t.clientX; sY = t.clientY; iL = r.left; iT = r.top;" +
            "}, { passive: true });" +
            "header.addEventListener('touchmove', function(e) {" +
            "  if (!isDrag) return; var t = e.touches[0];" +
            "  var nX = iL + (t.clientX - sX); var nY = iT + (t.clientY - sY);" +
            "  hud.style.left = Math.max(0, Math.min(nX, window.innerWidth - hud.offsetWidth)) + 'px';" +
            "  hud.style.top = Math.max(0, Math.min(nY, window.innerHeight - hud.offsetHeight)) + 'px';" +
            "  hud.style.right = 'auto';" +
            "}, { passive: false });" +
            "header.addEventListener('touchend', function() { isDrag = false; });" +

            "var tog = document.getElementById('hud_tog');" +
            "var cnt = document.getElementById('hud_content');" +
            "bindTap(tog, function() {" +
            "  if (cnt.style.display === 'none') { cnt.style.display = 'block'; tog.innerText = '[收]'; }" +
            "  else { cnt.style.display = 'none'; tog.innerText = '[展]'; }" +
            "});" +

            // 贊助作者：點擊複製 USDT-TRC20 地址
            "bindTap(document.getElementById('nav_sponsor'), function() {" +
            "  var addr = 'TLz5EaP1rKUfdFu1iZectuDxCP1URNEivm';" +
            "  if (navigator.clipboard && navigator.clipboard.writeText) {" +
            "    navigator.clipboard.writeText(addr).then(function() {" +
            "      alert('已複製 USDT-TRC20 贊助地址：\\n' + addr);" +
            "    }).catch(function() {" +
            "      prompt('USDT-TRC20 地址（請手動複製）：', addr);" +
            "    });" +
            "  } else {" +
            "    prompt('USDT-TRC20 地址（請手動複製）：', addr);" +
            "  }" +
            "});" +

            "bindTap(document.getElementById('nav_reg'), function() { window.location.href = 'https://osc188.com'; });" +

            "function openLink(u) {" +
            "  if (window.AndroidBridge && window.AndroidBridge.openExternalUrl) {" +
            "    window.AndroidBridge.openExternalUrl(u);" +
            "  } else {" +
            "    window.location.href = u;" +
            "  }" +
            "}" +
            "bindTap(document.getElementById('btn_line'), function() { openLink('https://lin.ee/NfoQ9DH'); });" +
            "bindTap(document.getElementById('btn_tg'), function() { openLink('https://t.me/TG_apk1'); });" +

            "var btnDo = document.getElementById('btn_do_ai');" +
            "bindTap(btnDo, function() {" +
            "  btnDo.innerText = '🧠 Astra 深度推論中...';" +
            "  btnDo.disabled = true;" +
            "  if (window.AndroidBridge && window.AndroidBridge.requestVisualAnalysis) {" +
            "    window.AndroidBridge.requestVisualAnalysis(curMode);" +
            "  }" +
            "});" +

            "window.__updateAI = function(pick, conf, reason, stats) {" +
            "  var t = document.getElementById('ai_pick_target');" +
            "  var d = document.getElementById('ai_pick_desc');" +
            "  var s = document.getElementById('hud_score');" +
            "  if (t) {" +
            "    t.innerText = '【' + pick + '】 ' + conf + '%';" +
            "    t.style.color = (pick === '莊' || pick === '庄') ? '#ef4444' : '#3b82f6';" +
            "  }" +
            "  if (d) d.innerText = reason;" +
            "  if (s) s.innerText = stats;" +
            "  btnDo.innerText = '📸 截圖畫面並由 AI 辨識';" +
            "  btnDo.disabled = false;" +
            "};" +

            "window.__updateAIError = function(msg) {" +
            "  var d = document.getElementById('ai_pick_desc');" +
            "  if (d) d.innerText = '辨識重試: ' + msg;" +
            "  btnDo.innerText = '重試';" +
            "  btnDo.disabled = false;" +
            "};" +
        "})();";

        view.evaluateJavascript(js, null);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
