package com.slot.assistant;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.util.Base64;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
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
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private boolean isAnalyzing = false;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

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
        
        // 關鍵修復：啟用多視窗支援，配合 WebChromeClient 轉發，解決 SA 真人彈窗失效問題
        settings.setSupportMultipleWindows(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        }

        settings.setUserAgentString("Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36");

        // 原生雙向通道
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

        // 處理彈出視窗轉發至同一畫面（解決 SA 等廠商 window.open 無反應）
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(view);
                resultMsg.sendToTarget();
                return true;
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                injectAssistantScript(view);
            }
        });

        // 🌐 預設首頁：Google
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

                    float scale = 540f / w;
                    int targetW = 540;
                    int targetH = (int) (h * scale);

                    Bitmap bitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(bitmap);
                    canvas.scale(scale, scale);
                    webView.draw(canvas);

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                    byte[] imageBytes = baos.toByteArray();
                    final String base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP);
                    bitmap.recycle();

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
        String prompt = "你是具備大數據統計背景的職業百家樂路單視覺精算大師。\n" +
                "請嚴格觀察圖片下半部【珠盤路、大路、下三路（大眼仔、小路、蟑螂路）及統計列】：\n\n" +
                "【嚴禁行為】：嚴禁輸出觀望！絕對嚴禁輸出觀望！每一局必須強制二選一給出【莊】或【閒】！\n\n" +
                "【任務一：提取盤面統計數據】\n" +
                "請精確辨識出畫面底部統計列數值：\n" +
                "- 本局總數 (total)\n" +
                "- 莊 (banker)\n" +
                "- 閒 (player)\n" +
                "- 和 (tie)\n\n" +
                "【任務二：高勝率形態強制推演】\n" +
                "1.【大路形態識別】：判斷當前處於連龍（>=2同向）、規律單跳（莊閒交替）、一廳兩房還是拍拍黐？最新一手落點在何處？\n" +
                "2.【下三路轉折與合流】：結合右下角大眼仔、小路、曱甴路問路。若三路齊紅，果斷順延強規律；若連龍逼近跳點臨界且下三路紅藍反轉，果斷反切抓跳！\n" +
                "3.【輸出信心評估】：置信度嚴格評估於 72% ~ 91% 之間。\n" +
                "4.【理由具體化】：必須精準點出幾何形態，例如：『大路4連龍走勢順延，下三路齊整紅筆合流』、『長龍逼近臨界跳點，大眼仔紅藍反轉抓跳』、『單跳走勢延續，下三路問閒齊腳』。\n\n" +
                "嚴格僅輸出純 JSON 物件：\n" +
                "{\"total\":35,\"banker\":16,\"player\":16,\"tie\":3,\"pick\":\"莊\",\"conf\":82,\"reason\":\"長龍第4口面臨跳點臨界，大眼仔紅藍反轉\"}";
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
        String js = "javascript:(function() {" +
            "if (document.getElementById('slot-assistant-hud')) return;" +

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

            "var hud = document.createElement('div');" +
            "hud.id = 'slot-assistant-hud';" +
            "hud.style.cssText = 'position:fixed;top:45px;right:8px;width:220px;background:rgba(11,17,32,0.96);border:1px solid #38bdf8;border-radius:10px;z-index:999999;color:#f1f5f9;font-size:11px;box-shadow:0 8px 24px rgba(0,0,0,0.85);font-family:sans-serif;user-select:none;backdrop-filter:blur(6px);';" +
            "hud.innerHTML = " +
                "'<div id=\"hud_header\" style=\"padding:7px 10px;background:#1e293b;border-radius:10px 10px 0 0;font-weight:bold;color:#38bdf8;display:flex;justify-content:space-between;align-items:center;border-bottom:1px solid #334155;cursor:move;touch-action:none;\">" +
                    "<span>👁 Astra 深度推論</span>" +
                    "<span id=\"hud_tog\" style=\"cursor:pointer;color:#94a3b8;font-size:10px;\">[收]</span>" +
                "</div>' +" +
                "'<div id=\"hud_content\" style=\"padding:8px;\">' +" +
                    // 單一下拉式選單（整合原平台 + 新增 6 款遊戲）
                    "'<div style=\"margin-bottom:6px;\">' +" +
                        "'<select id=\"game_select\" style=\"width:100%;background:#0f172a;border:1px solid #38bdf8;color:#38bdf8;padding:5px 6px;border-radius:4px;font-size:10px;font-weight:bold;outline:none;\">' +" +
                            "'<option value=\"\">⚡ 選擇遊戲 / 大廳切換 ⚡</option>' +" +
                            "'<option value=\"google\">🌐 Google 首頁</option>' +" +
                            "'<option value=\"lobby\">🏠 遊戲大廳列表</option>' +" +
                            "'<option value=\"meta_all\">MT 百家樂</option>' +" +
                            "'<option value=\"dg\">DG 百家樂</option>' +" +
                            "'<option value=\"allbet\">歐博 百家樂</option>' +" +
                            "'<option value=\"sa\">SA 真人 (SA LIVE)</option>' +" +
                            "'<option value=\"bg\">BG 百家樂 (BG LIVE)</option>' +" +
                            "'<option value=\"t9\">T9 百家樂 (T9 LIVE)</option>' +" +
                            "'<option value=\"astar\">ASTAR 百家樂 (ASTAR LIVE)</option>' +" +
                            "'<option value=\"gclub\">GCLUB 真人 (GCLUB LIVE)</option>' +" +
                            "'<option value=\"pt\">PT 百家樂 (PT LIVE)</option>' +" +
                        "'</select>' +" +
                    "'</div>' +" +
                    // 預測展示區（強制二選一）
                    "'<div style=\"background:rgba(15,23,42,0.85);border:1px solid #1e3a8a;border-radius:6px;padding:6px 4px;text-align:center;margin-bottom:6px;\">" +
                        "<div style=\"font-size:10px;color:#94a3b8;\">🎯 深度路單精算建議</div>" +
                        "<div id=\"ai_pick_target\" style=\"font-size:18px;font-weight:900;color:#ef4444;margin:2px 0;\">待命中</div>" +
                        "<div id=\"ai_pick_desc\" style=\"font-size:9px;color:#38bdf8;line-height:1.2;\">請進入牌桌後點擊下方按鈕</div>" +
                    "</div>' +" +
                    // 底欄即時顯示：本局總數 / 莊 / 和 / 閒
                    "'<div id=\"hud_stats_box\" style=\"background:#0f172a;border:1px dashed #334155;border-radius:4px;padding:4px;text-align:center;font-size:10px;color:#38bdf8;margin-bottom:6px;\">" +
                        "總數: -- | 莊: -- | 和: -- | 閒: --" +
                    "</div>' +" +
                    "'<button id=\"btn_do_ai\" style=\"width:100%;background:#2563eb;color:#fff;border:none;padding:7px 0;border-radius:4px;font-weight:bold;margin-bottom:4px;font-size:11px;\">📸 截圖畫面並由 AI 辨識</button>' +" +
                "'</div>';" +
            "document.body.appendChild(hud);" +

            // 拖曳處理
            "var header = document.getElementById('hud_header');" +
            "var isDrag = false, sX, sY, iL, iT;" +
            "header.addEventListener('touchstart', function(e) {" +
            "  if (e.target.closest('#hud_tog') || e.target.closest('select')) return;" +
            "  isDrag = true; var t = e.touches[0]; var r = hud.getBoundingClientRect();" +
            "  sX = t.clientX; sY = t.clientY; iL = r.left; iT = r.top;" +
            "}, { passive: true });" +
            "header.addEventListener('touchmove', function(e) {" +
            "  if (!isDrag) return;" +
            "  var t = e.touches[0];" +
            "  hud.style.left = Math.max(0, Math.min(iL + (t.clientX - sX), window.innerWidth - hud.offsetWidth)) + 'px';" +
            "  hud.style.top = Math.max(0, Math.min(iT + (t.clientY - sY), window.innerHeight - hud.offsetHeight)) + 'px';" +
            "  hud.style.right = 'auto';" +
            "}, { passive: false });" +
            "header.addEventListener('touchend', function() { isDrag = false; });" +

            // 展開收合
            "var tog = document.getElementById('hud_tog');" +
            "var cnt = document.getElementById('hud_content');" +
            "bindTap(tog, function() {" +
            "  if (cnt.style.display === 'none') { cnt.style.display = 'block'; tog.innerText = '[收]'; }" +
            "  else { cnt.style.display = 'none'; tog.innerText = '[展]'; }" +
            "});" +

            // 快捷跳轉通道
            "function safeNav(url) {" +
            "  if (window.AndroidBridge && window.AndroidBridge.switchGame) window.AndroidBridge.switchGame(url);" +
            "  else location.href = url;" +
            "}" +

            // 核心切換處理（支援原生卡片點擊，修復 SA 等廠商 Token 遺失問題）
            "function triggerGameSwitch(key) {" +
            "  if (!key) return;" +
            "  if (key === 'google') { safeNav('https://www.google.com'); return; }" +
            "  if (key === 'lobby') { safeNav('https://www.osc169.com/#/game'); return; }" +

            "  var kwMap = {" +
            "    'sa': ['SA', 'SA真人', 'SA LIVE', 'SAGAMING']," +
            "    'bg': ['BG', 'BG LIVE', 'BG百家樂']," +
            "    't9': ['T9', 'T9 LIVE', 'T9百家樂']," +
            "    'astar': ['ASTAR', 'ASTAR LIVE', 'ASTAR百家樂']," +
            "    'gclub': ['GCLUB', 'GCLUB LIVE', 'GCLUB真人']," +
            "    'pt': ['PT', 'PT LIVE', 'PT百家樂']," +
            "    'meta_all': ['MT', 'META', 'MT百家樂']," +
            "    'dg': ['DG', 'DG百家樂']," +
            "    'allbet': ['歐博', 'ALLBET']" +
            "  };" +

            "  var keywords = kwMap[key] || [key];" +
            "  var clicked = false;" +

            // 若當前在遊戲大廳列表，優先模擬點擊卡片（保證走官方原生認證 Token）
            "  var els = document.querySelectorAll('div, a, span, p, img');" +
            "  for (var i = 0; i < els.length; i++) {" +
            "    var el = els[i];" +
            "    var txt = (el.innerText || el.getAttribute('alt') || '').trim().toUpperCase();" +
            "    for (var k = 0; k < keywords.length; k++) {" +
            "      if (txt === keywords[k] || txt.indexOf(keywords[k]) !== -1) {" +
            "        var target = el.closest('a, [onclick], div[class*=\"card\"], div[class*=\"item\"]') || el;" +
            "        target.click();" +
            "        clicked = true;" +
            "        break;" +
            "      }" +
            "    }" +
            "    if (clicked) break;" +
            "  }" +

            // 若不在大廳，改走 Hash 網址跳轉
            "  if (!clicked) {" +
            "    var code = (key === 'sa') ? 'sagaming' : key;" +
            "    safeNav('https://www.osc169.com/#/game/play?game_name=' + code + '&game_type=3&device=mobile');" +
            "  }" +
            "}" +

            // 下拉選單監聽
            "var sel = document.getElementById('game_select');" +
            "sel.addEventListener('change', function() {" +
            "  var val = sel.value;" +
            "  triggerGameSwitch(val);" +
            "  sel.value = '';" + // 重置選單狀態以便下次選擇
            "});" +

            // 觸發視覺辨識
            "var btnDo = document.getElementById('btn_do_ai');" +
            "bindTap(btnDo, function() {" +
            "  btnDo.innerText = '🧠 Astra 深度推論中...';" +
            "  btnDo.disabled = true;" +
            "  if (window.AndroidBridge && window.AndroidBridge.requestVisualAnalysis) {" +
            "    window.AndroidBridge.requestVisualAnalysis();" +
            "  }" +
            "});" +

            // 回調更新介面
            "window.__updateAI = function(pick, conf, reason, stats) {" +
            "  var t = document.getElementById('ai_pick_target');" +
            "  var d = document.getElementById('ai_pick_desc');" +
            "  var s = document.getElementById('hud_stats_box');" +
            "  if (t) {" +
            "    t.innerText = '【' + pick + '】 ' + conf + '%';" +
            "    t.style.color = (pick === '莊') ? '#ef4444' : '#38bdf8';" +
            "  }" +
            "  if (d) d.innerText = reason;" +
            "  if (s) s.innerText = stats;" +
            "  btnDo.innerText = '📸 截圖畫面並由 AI 辨識';" +
            "  btnDo.disabled = false;" +
            "};" +

            "window.__updateAIError = function(msg) {" +
            "  var d = document.getElementById('ai_pick_desc');" +
            "  if (d) d.innerText = '錯誤: ' + msg;" +
            "  btnDo.innerText = '📸 截圖畫面並由 AI 辨識';" +
            "  btnDo.disabled = false;" +
            "};" +
        "})();";

        view.evaluateJavascript(js, null);
    }
}
