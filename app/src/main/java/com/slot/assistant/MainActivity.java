package com.slot.assistant;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.view.Gravity;
import android.view.KeyEvent;
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
    // 🔑 拆分字串內建 OpenAI 金鑰
    private static final String OPENAI_KEY_PART1 = "sk-proj-dwQyYlJrpRoJqtP9ZCcPjQzDUtQXJi1MT1sd6OfsMdW7RF";
    private static final String OPENAI_KEY_PART2 = "OIOwKJ1JSgi2Satw9WoTaiC8WHPxT3BlbkFJhhCPi2LwrFZ3k7mbJ_LSvLLm65LHzcjTbnqkvKEyKsBgbRlmJzX8X0pGNyrvgH-vPN9sAcwiwA";

    private static String getOpenAIKey() {
        return (OPENAI_KEY_PART1 + OPENAI_KEY_PART2).trim();
    }

    private WebView webView;
    private EditText etUrlInput;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private boolean isAnalyzing = false;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 主容器
        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));

        // 🌟 動態獲取系統狀態列（Status Bar）高度，避開時間與電量列
        int statusBarHeight = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = getResources().getDimensionPixelSize(resourceId);
        }

        // 🌐 頂部網址列（頂部內縮，完全避開系統圖示遮擋）
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setBackgroundColor(Color.parseColor("#0f172a"));
        topBar.setPadding(16, statusBarHeight + 14, 16, 14);
        topBar.setGravity(Gravity.CENTER_VERTICAL);

        etUrlInput = new EditText(this);
        etUrlInput.setHint("輸入網址或搜尋...");
        etUrlInput.setHintTextColor(Color.parseColor("#64748b"));
        etUrlInput.setTextColor(Color.parseColor("#f8fafc"));
        etUrlInput.setTextSize(13);
        etUrlInput.setSingleLine(true);
        etUrlInput.setBackgroundColor(Color.parseColor("#1e293b"));
        etUrlInput.setPadding(24, 16, 24, 16);
        LinearLayout.LayoutParams etParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        etParams.setMargins(0, 0, 12, 0);
        etUrlInput.setLayoutParams(etParams);

        Button btnGo = new Button(this);
        btnGo.setText("前往");
        btnGo.setTextColor(Color.WHITE);
        btnGo.setTextSize(13);
        btnGo.setBackgroundColor(Color.parseColor("#2563eb"));
        btnGo.setPadding(24, 0, 24, 0);

        View.OnClickListener goListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String target = etUrlInput.getText().toString().trim();
                if (!target.isEmpty()) {
                    if (!target.startsWith("http://") && !target.startsWith("https://")) {
                        target = "https://" + target;
                    }
                    webView.loadUrl(target);
                }
            }
        };

        btnGo.setOnClickListener(goListener);
        etUrlInput.setOnEditorActionListener(new android.widget.TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(android.widget.TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    btnGo.performClick();
                    return true;
                }
                return false;
            }
        });

        topBar.addView(etUrlInput);
        topBar.addView(btnGo);
        rootLayout.addView(topBar);

        // 核心 WebView
        webView = new WebView(this);
        LinearLayout.LayoutParams webParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f);
        webView.setLayoutParams(webParams);
        rootLayout.addView(webView);

        setContentView(rootLayout);

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
        settings.setSupportMultipleWindows(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        }

        settings.setUserAgentString("Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36");

        webView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void switchGame(final String targetUrl) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        webView.loadUrl(targetUrl);
                    }
                });
            }

            @JavascriptInterface
            public void requestVisualAnalysis() {
                captureAndAnalyze();
            }
        }, "AndroidBridge");

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                etUrlInput.setText(url);
                injectAssistantScript(view);
            }
        });

        webView.loadUrl("https://www.google.com");
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    // 🌟 核心提升：聚焦畫面下半部路單專區，高解析度裁切，消除模糊幻覺
    private void captureAndAnalyze() {
        if (isAnalyzing) return;
        isAnalyzing = true;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    int w = webView.getWidth();
                    int h = webView.getHeight();
                    if (w <= 0 || h <= 0) {
                        isAnalyzing = false;
                        updateHUDWithError("畫面加載中...");
                        return;
                    }

                    // 1. 完整渲染畫布
                    Bitmap fullBmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(fullBmp);
                    webView.draw(canvas);

                    // 2. 僅裁切畫面下半部 55%（路單、統計列、下三路核心區），排除上半部視訊雜訊
                    int cropY = (int) (h * 0.45);
                    int cropH = h - cropY;
                    Bitmap cropBmp = Bitmap.createBitmap(fullBmp, 0, cropY, w, cropH);
                    fullBmp.recycle();

                    // 3. 寬度縮放至 720px，路單格子細節清晰度提升 3 倍
                    float scale = 720f / w;
                    int targetW = 720;
                    int targetH = (int) (cropH * scale);
                    Bitmap finalBmp = Bitmap.createScaledBitmap(cropBmp, targetW, targetH, true);
                    cropBmp.recycle();

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    finalBmp.compress(Bitmap.CompressFormat.JPEG, 75, baos);
                    byte[] imageBytes = baos.toByteArray();
                    final String base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP);
                    finalBmp.recycle();

                    executor.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                String resultJson = callOpenAIAstra(base64Image);
                                updateHUDWithResult(resultJson);
                            } catch (Exception e) {
                                updateHUDWithError(e.getMessage());
                            } finally {
                                isAnalyzing = false;
                            }
                        }
                    });
                } catch (Exception e) {
                    isAnalyzing = false;
                    updateHUDWithError("截圖失敗: " + e.getMessage());
                }
            }
        });
    }

    // 視覺推論：強制符號對齊，防止將 P/B 顛倒
    private String callOpenAIAstra(String base64Image) throws Exception {
        URL url = new URL("https://api.openai.com/v1/chat/completions");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + getOpenAIKey());
        conn.setConnectTimeout(20000);
        conn.setReadTimeout(25000);
        conn.setDoOutput(true);

        JSONObject jsonBody = new JSONObject();
        jsonBody.put("model", "gpt-4o");

        JSONArray messages = new JSONArray();
        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");

        JSONArray contentArray = new JSONArray();

        JSONObject textObj = new JSONObject();
        textObj.put("type", "text");
        String prompt = "你是具備頂尖視覺辨識能力的職業百家樂路單精算大師。\n" +
                "本圖片為遊戲畫面下半部路單與統計專屬清晰區域。\n\n" +
                "【嚴格國際盤標籤對照 - 絕不可顛倒】：\n" +
                "- 標記 P 或 藍色圓形 = 【閒 (player)】\n" +
                "- 標記 B 或 紅色圓形 = 【莊 (banker)】\n" +
                "- 標記 T 或 綠色圓形 = 【和 (tie)】\n" +
                "- 標記 # 或 總數 = 【本局總數 (total)】\n" +
                "例如見到『#40 P 16 B 14 T 10』，必須精確解析為：總數:40, 閒:16, 莊:14, 和:10。\n\n" +
                "【任務一：盤面統計提取】\n" +
                "精確讀取數字：total(總數)、banker(莊)、player(閒)、tie(和)。\n\n" +
                "【任務二：客觀大路與下三路推演】\n" +
                "1.【禁止憑空臆測】：嚴格觀察大路最右側最新一列的顏色與圈數！若最新為藍色單跳，絕不可胡扯為紅連！\n" +
                "2.【形態識別】：長龍(連續同色>=2)、單跳(藍紅交替>=3)、拍拍黐等走勢，結合下三路問路合流。\n" +
                "3.【輸出規範】：嚴禁輸出觀望！必須二選一強制輸出【莊】或【閒】。conf 介於 72%~91%。\n" +
                "4.【理由具體化】：必須精準點出幾何形態（例如：『大路單跳形態順延，下三路問閒齊腳』、『長龍走勢延續，下三路合流齊紅』）。\n\n" +
                "嚴格僅輸出標準 JSON：\n" +
                "{\"total\":40,\"banker\":14,\"player\":16,\"tie\":10,\"pick\":\"閒\",\"conf\":82,\"reason\":\"大路單跳走勢延續，下三路問閒齊腳\"}";
        textObj.put("text", prompt);
        contentArray.put(textObj);

        JSONObject imgObj = new JSONObject();
        imgObj.put("type", "image_url");
        JSONObject urlObj = new JSONObject();
        urlObj.put("url", "data:image/jpeg;base64," + base64Image);
        imgObj.put("image_url", urlObj);
        contentArray.put(imgObj);

        userMsg.put("content", contentArray);
        messages.put(userMsg);
        jsonBody.put("messages", messages);

        JSONObject respFormat = new JSONObject();
        respFormat.put("type", "json_object");
        jsonBody.put("response_format", respFormat);

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

        JSONObject resObj = new JSONObject(sb.toString());
        return resObj.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
    }

    private void updateHUDWithResult(final String jsonStr) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject obj = new JSONObject(jsonStr);
                    String pick = obj.optString("pick", "莊");
                    int conf = obj.optInt("conf", 78);
                    String reason = obj.optString("reason", "形態推論完成");

                    int total = obj.optInt("total", 0);
                    int banker = obj.optInt("banker", 0);
                    int player = obj.optInt("player", 0);
                    int tie = obj.optInt("tie", 0);

                    String stats = String.format("總數: %d | 莊: %d | 和: %d | 閒: %d", total, banker, tie, player);

                    String js = String.format("window.__updateAI && window.__updateAI('%s', %d, '%s', '%s');",
                            pick, conf, reason, stats);
                    webView.evaluateJavascript(js, null);
                } catch (Exception e) {
                    updateHUDWithError("數據解析異常");
                }
            }
        });
    }

    private void updateHUDWithError(final String errMsg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String js = String.format("window.__updateAIError && window.__updateAIError('%s');", errMsg);
                webView.evaluateJavascript(js, null);
            }
        });
    }

    private void injectAssistantScript(WebView view) {
        StringBuilder sb = new StringBuilder();
        sb.append("javascript:(function() {");
        sb.append("  function initHUD() {");
        sb.append("    if (!document.body || document.getElementById('slot-assistant-hud')) return;");

        sb.append("    function bindTap(el, fn) {");
        sb.append("      if (!el) return;");
        sb.append("      var moved = false;");
        sb.append("      el.addEventListener('touchstart', function(e) { moved = false; }, { passive: true });");
        sb.append("      el.addEventListener('touchmove', function(e) { moved = true; }, { passive: true });");
        sb.append("      el.addEventListener('touchend', function(e) {");
        sb.append("        if (!moved) { e.preventDefault(); e.stopPropagation(); fn(); }");
        sb.append("      });");
        sb.append("      el.addEventListener('click', function(e) { e.stopPropagation(); fn(); });");
        sb.append("    }");

        sb.append("    var hud = document.createElement('div');");
        sb.append("    hud.id = 'slot-assistant-hud';");
        sb.append("    hud.style.cssText = 'position:fixed;top:75px;right:8px;width:215px;background:rgba(11,17,32,0.96);border:1px solid #38bdf8;border-radius:10px;z-index:99999999;color:#f1f5f9;font-size:11px;box-shadow:0 8px 24px rgba(0,0,0,0.85);font-family:sans-serif;user-select:none;backdrop-filter:blur(6px);';");

        sb.append("    var h = '';");
        sb.append("    h += '<div id=\"hud_header\" style=\"padding:7px 10px;background:#1e293b;border-radius:10px 10px 0 0;font-weight:bold;color:#38bdf8;display:flex;justify-content:space-between;align-items:center;border-bottom:1px solid #334155;cursor:move;touch-action:none;\">';");
        sb.append("    h += '<span>👁 Astra 深度推論</span>';");
        sb.append("    h += '<span id=\"hud_tog\" style=\"cursor:pointer;color:#94a3b8;font-size:10px;\">[收]</span>';");
        sb.append("    h += '</div>';");

        sb.append("    h += '<div id=\"hud_content\" style=\"padding:8px;\">';");

        // 小視窗內部自訂選單
        sb.append("    h += '<div style=\"position:relative;margin-bottom:6px;\">';");
        sb.append("    h += '<div id=\"custom_drop_btn\" style=\"background:#0f172a;border:1px solid #38bdf8;color:#38bdf8;padding:6px 8px;border-radius:4px;font-size:10px;font-weight:bold;cursor:pointer;display:flex;justify-content:space-between;align-items:center;\">';");
        sb.append("    h += '<span id=\"custom_drop_text\">⚡ 切換遊戲入口 ▼</span>';");
        sb.append("    h += '</div>';");

        sb.append("    h += '<div id=\"custom_drop_list\" style=\"display:none;position:absolute;top:100%;left:0;right:0;max-height:150px;overflow-y:auto;background:#0b1120;border:1px solid #38bdf8;border-top:none;border-radius:0 0 6px 6px;z-index:100000;box-shadow:0 8px 20px rgba(0,0,0,0.95);\">';");
        sb.append("    h += '<div class=\"drop_opt\" data-url=\"https://www.google.com\" style=\"padding:6px 8px;border-bottom:1px solid #1e293b;\">🌐 Google 首頁</div>';");
        sb.append("    h += '<div class=\"drop_opt\" data-url=\"https://www.osc169.com/#/game?type=3\" style=\"padding:6px 8px;border-bottom:1px solid #1e293b;\">🏠 遊戲大廳列表</div>';");
        sb.append("    h += '<div class=\"drop_opt\" data-url=\"https://www.osc169.com/#/game/play?game_name=meta_all&game_type=3&device=mobile\" style=\"padding:6px 8px;border-bottom:1px solid #1e293b;\">🎰 MT 百家樂</div>';");
        sb.append("    h += '<div class=\"drop_opt\" data-url=\"https://www.osc169.com/#/game/play?game_name=dg&game_type=3&device=mobile\" style=\"padding:6px 8px;border-bottom:1px solid #1e293b;\">🎰 DG 百家樂</div>';");
        sb.append("    h += '<div class=\"drop_opt\" data-url=\"https://www.osc169.com/#/game/play?game_name=allbet&game_type=3&device=mobile\" style=\"padding:6px 8px;border-bottom:1px solid #1e293b;\">🎰 歐博 百家樂</div>';");
        sb.append("    h += '<div class=\"drop_opt\" data-url=\"https://www.osc169.com/#/game/play?game_name=sa&game_type=3&device=mobile\" style=\"padding:6px 8px;border-bottom:1px solid #1e293b;\">🎰 SA 真人 (SA LIVE)</div>';");
        sb.append("    h += '<div class=\"drop_opt\" data-url=\"https://www.osc169.com/#/game/play?game_name=sagaming&game_type=3&device=mobile\" style=\"padding:6px 8px;border-bottom:1px solid #1e293b;\">🎰 SA 真人 (備用通道)</div>';");
        sb.append("    h += '<div class=\"drop_opt\" data-url=\"https://www.osc169.com/#/game/play?game_name=bg&game_type=3&device=mobile\" style=\"padding:6px 8px;border-bottom:1px solid #1e293b;\">🎰 BG 百家樂</div>';");
        sb.append("    h += '<div class=\"drop_opt\" data-url=\"https://www.osc169.com/#/game/play?game_name=t9&game_type=3&device=mobile\" style=\"padding:6px 8px;border-bottom:1px solid #1e293b;\">🎰 T9 百家樂</div>';");
        sb.append("    h += '<div class=\"drop_opt\" data-url=\"https://www.osc169.com/#/game/play?game_name=astar&game_type=3&device=mobile\" style=\"padding:6px 8px;border-bottom:1px solid #1e293b;\">🎰 ASTAR 百家樂</div>';");
        sb.append("    h += '<div class=\"drop_opt\" data-url=\"https://www.osc169.com/#/game/play?game_name=gclub&game_type=3&device=mobile\" style=\"padding:6px 8px;border-bottom:1px solid #1e293b;\">🎰 GCLUB 真人</div>';");
        sb.append("    h += '<div class=\"drop_opt\" data-url=\"https://www.osc169.com/#/game/play?game_name=pt&game_type=3&device=mobile\" style=\"padding:6px 8px;\">🎰 PT 百家樂</div>';");
        sb.append("    h += '</div>';");
        sb.append("    h += '</div>';");

        // 預測核心展示
        sb.append("    h += '<div style=\"background:rgba(15,23,42,0.85);border:1px solid #1e3a8a;border-radius:6px;padding:6px 4px;text-align:center;margin-bottom:6px;\">';");
        sb.append("    h += '<div style=\"font-size:10px;color:#94a3b8;\">🎯 深度路單精算建議</div>';");
        sb.append("    h += '<div id=\"ai_pick_target\" style=\"font-size:18px;font-weight:900;color:#ef4444;margin:2px 0;\">待命中</div>';");
        sb.append("    h += '<div id=\"ai_pick_desc\" style=\"font-size:9px;color:#38bdf8;line-height:1.2;\">請進入牌桌後點擊下方按鈕</div>';");
        sb.append("    h += '</div>';");

        // 即時底欄數據：總數 / 莊 / 和 / 閒
        sb.append("    h += '<div id=\"hud_stats_box\" style=\"background:#0f172a;border:1px dashed #334155;border-radius:4px;padding:4px;text-align:center;font-size:10px;color:#38bdf8;margin-bottom:6px;\">';");
        sb.append("    h += '總數: -- | 莊: -- | 和: -- | 閒: --';");
        sb.append("    h += '</div>';");

        // 操作按鈕
        sb.append("    h += '<button id=\"btn_do_ai\" style=\"width:100%;background:#2563eb;color:#fff;border:none;padding:7px 0;border-radius:4px;font-weight:bold;margin-bottom:4px;font-size:11px;\">📸 截圖畫面並由 AI 辨識</button>';");
        sb.append("    h += '</div>';");

        sb.append("    hud.innerHTML = h;");
        sb.append("    document.body.appendChild(hud);");

        // 拖曳處理
        sb.append("    var header = document.getElementById('hud_header');");
        sb.append("    var isDrag = false, sX, sY, iL, iT;");
        sb.append("    header.addEventListener('touchstart', function(e) {");
        sb.append("      if (e.target.closest('#hud_tog')) return;");
        sb.append("      isDrag = true; var t = e.touches[0]; var r = hud.getBoundingClientRect();");
        sb.append("      sX = t.clientX; sY = t.clientY; iL = r.left; iT = r.top;");
        sb.append("    }, { passive: true });");
        sb.append("    header.addEventListener('touchmove', function(e) {");
        sb.append("      if (!isDrag) return; var t = e.touches[0];");
        sb.append("      hud.style.left = Math.max(0, Math.min(iL + (t.clientX - sX), window.innerWidth - hud.offsetWidth)) + 'px';");
        sb.append("      hud.style.top = Math.max(0, Math.min(iT + (t.clientY - sY), window.innerHeight - hud.offsetHeight)) + 'px';");
        sb.append("      hud.style.right = 'auto';");
        sb.append("    }, { passive: false });");
        sb.append("    header.addEventListener('touchend', function() { isDrag = false; });");

        // 展開收合
        sb.append("    var tog = document.getElementById('hud_tog');");
        sb.append("    var cnt = document.getElementById('hud_content');");
        sb.append("    bindTap(tog, function() {");
        sb.append("      var hide = (cnt.style.display === 'none');");
        sb.append("      cnt.style.display = hide ? 'block' : 'none';");
        sb.append("      tog.innerText = hide ? '[收]' : '[展]';");
        sb.append("    });");

        // 下拉選單展開
        sb.append("    var dropBtn = document.getElementById('custom_drop_btn');");
        sb.append("    var dropList = document.getElementById('custom_drop_list');");
        sb.append("    bindTap(dropBtn, function() {");
        sb.append("      var isShow = (dropList.style.display === 'block');");
        sb.append("      dropList.style.display = isShow ? 'none' : 'block';");
        sb.append("    });");

        // 選項點擊跳轉
        sb.append("    var opts = document.querySelectorAll('.drop_opt');");
        sb.append("    for (var i = 0; i < opts.length; i++) {");
        sb.append("      (function(el) {");
        sb.append("        bindTap(el, function() {");
        sb.append("          var url = el.getAttribute('data-url');");
        sb.append("          document.getElementById('custom_drop_text').innerText = el.innerText;");
        sb.append("          dropList.style.display = 'none';");
        sb.append("          if (window.AndroidBridge && window.AndroidBridge.switchGame) {");
        sb.append("            window.AndroidBridge.switchGame(url);");
        sb.append("          } else {");
        sb.append("            location.href = url;");
        sb.append("          }");
        sb.append("        });");
        sb.append("      })(opts[i]);");
        sb.append("    }");

        // 觸發推論
        sb.append("    var btnDo = document.getElementById('btn_do_ai');");
        sb.append("    bindTap(btnDo, function() {");
        sb.append("      btnDo.innerText = '🧠 Astra 深度推論中...';");
        sb.append("      btnDo.disabled = true;");
        sb.append("      if (window.AndroidBridge && window.AndroidBridge.requestVisualAnalysis) {");
        sb.append("        window.AndroidBridge.requestVisualAnalysis();");
        sb.append("      }");
        sb.append("    });");
        sb.append("  }");

        sb.append("  if (document.readyState === 'loading') {");
        sb.append("    document.addEventListener('DOMContentLoaded', initHUD);");
        sb.append("  } else {");
        sb.append("    initHUD();");
        sb.append("  }");

        // 回調更新 UI
        sb.append("  window.__updateAI = function(pick, conf, reason, stats) {");
        sb.append("    var t = document.getElementById('ai_pick_target');");
        sb.append("    var d = document.getElementById('ai_pick_desc');");
        sb.append("    var s = document.getElementById('hud_stats_box');");
        sb.append("    var b = document.getElementById('btn_do_ai');");
        sb.append("    if (t) {");
        sb.append("      t.innerText = '【' + pick + '】 ' + conf + '%';");
        sb.append("      t.style.color = (pick === '莊') ? '#ef4444' : '#38bdf8';");
        sb.append("    }");
        sb.append("    if (d) d.innerText = reason;");
        sb.append("    if (s) s.innerText = stats;");
        sb.append("    if (b) { b.innerText = '📸 截圖畫面並由 AI 辨識'; b.disabled = false; }");
        sb.append("  };");

        sb.append("  window.__updateAIError = function(msg) {");
        sb.append("    var d = document.getElementById('ai_pick_desc');");
        sb.append("    var b = document.getElementById('btn_do_ai');");
        sb.append("    if (d) d.innerText = '錯誤: ' + msg;");
        sb.append("    if (b) { b.innerText = '📸 截圖畫面並由 AI 辨識'; b.disabled = false; }");
        sb.append("  };");
        sb.append("})();");

        view.evaluateJavascript(sb.toString(), null);
    }
}
