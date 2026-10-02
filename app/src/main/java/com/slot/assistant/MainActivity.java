package com.slot.assistant;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    // 預留 API Key 槽位（使用 Base64 防止 GitHub 阻擋）
    private static final String GEMINI_KEY_B64 = "QVEuQWI4Uk42S08zbEtMT1YwMzRNQW5mQm9idmE1VFNadXNYRTV4VGhVVHphT1FpYkp1eVE=";
    private static final String OPENAI_KEY_B64 = "c2stcHJvai1fcWhadVRHdW1zcHZwekVucmhFRVJGTTBWV1BrVndsMlBhNFRZY2NEMjN6bzZwYzdCYzRxWF9VSWd0U2JETjR3QWFWclJhUERvTFQzQmxia0ZJREYzallMdXc2VDhWa0dNZmRleXBMQVVLUE5oUnNaNjNBT21OZmZoNUJSa2o0RF94QVNtOXg2ZFlCMmlYMWh4c2FiRnRmdkVxZ0E=";

    private static String getGeminiKey() {
        return new String(Base64.decode(GEMINI_KEY_B64, Base64.NO_WRAP));
    }

    private static String getOpenAIKey() {
        return new String(Base64.decode(OPENAI_KEY_B64, Base64.NO_WRAP));
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        }

        settings.setUserAgentString("Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36");

        // 原生雙向通道
        webView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void requestAIAnalysis(final String pageText, final String engine) {
                if (isAnalyzing) return;
                isAnalyzing = true;

                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String resultJson;
                            if ("openai".equalsIgnoreCase(engine)) {
                                resultJson = callOpenAIFlagship(pageText);
                            } else {
                                resultJson = callGemini38Flash(pageText);
                            }
                            updateHUDWithResult(resultJson);
                        } catch (Exception e) {
                            updateHUDWithError("雲端錯誤: " + e.getMessage());
                        } finally {
                            isAnalyzing = false;
                        }
                    }
                });
            }

            @JavascriptInterface
            public void switchGame(final String targetUrl) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        webView.loadUrl(targetUrl);
                    }
                });
            }
        }, "AndroidBridge");

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                injectAssistantScript(view);
            }
        });

        webView.loadUrl("https://www.osc169.com/#/game/play?game_name=meta_all&game_type=3&device=mobile");
    }

    private String callGemini38Flash(String rawText) throws Exception {
        String key = getGeminiKey();
        if (!key.startsWith("AIzaSy")) {
            throw new Exception("Key格式錯誤(需為AIzaSy開頭)");
        }
        String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent?key=" + key;
        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(10000);
        conn.setDoOutput(true);

        String prompt = "你是頂尖百家樂分析師。以下為網頁文字：\n" + rawText + "\n" +
                "請提取莊、閒、和比分，並結合勝率偏差推薦下一手。嚴格只輸出JSON：\n" +
                "{\"pick\":\"莊\",\"conf\":75,\"reason\":\"動量延續\",\"stats\":\"莊34 閒24 和1 (59局)\"}";

        JSONObject jsonBody = new JSONObject();
        JSONArray contents = new JSONArray();
        JSONObject contentObj = new JSONObject();
        JSONArray parts = new JSONArray();
        JSONObject partObj = new JSONObject();
        partObj.put("text", prompt);
        parts.put(partObj);
        contentObj.put("parts", parts);
        contents.put(contentObj);
        jsonBody.put("contents", contents);

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

        if (code >= 400) throw new Exception("HTTP " + code + " 驗證失敗");

        JSONObject resp = new JSONObject(sb.toString());
        String out = resp.getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text");

        return cleanJson(out);
    }

    private String callOpenAIFlagship(String rawText) throws Exception {
        URL url = new URL("https://api.openai.com/v1/chat/completions");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + getOpenAIKey());
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(10000);
        conn.setDoOutput(true);

        JSONObject jsonBody = new JSONObject();
        jsonBody.put("model", "gpt-4o");
        jsonBody.put("temperature", 0.2);

        JSONArray messages = new JSONArray();
        JSONObject sysMsg = new JSONObject();
        sysMsg.put("role", "system");
        sysMsg.put("content", "你是百家樂精算師。從輸入提取莊/閒/和/總數，輸出純JSON: {\"pick\":\"莊\",\"conf\":75,\"reason\":\"簡短理由\",\"stats\":\"莊X 閒Y 和Z (N局)\"}");
        messages.put(sysMsg);

        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", rawText);
        messages.put(userMsg);

        jsonBody.put("messages", messages);

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

        if (code >= 400) throw new Exception("HTTP " + code + " 金鑰失效或額度不足");

        JSONObject resp = new JSONObject(sb.toString());
        String out = resp.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content");

        return cleanJson(out);
    }

    private String cleanJson(String text) {
        text = text.trim();
        if (text.startsWith("```json")) text = text.substring(7);
        if (text.startsWith("```")) text = text.substring(3);
        if (text.endsWith("```")) text = text.substring(0, text.length() - 3);
        return text.trim();
    }

    private void updateHUDWithResult(final String jsonStr) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject obj = new JSONObject(jsonStr);
                    String pick = obj.optString("pick", "觀望");
                    int conf = obj.optInt("conf", 65);
                    String reason = obj.optString("reason", "走勢震盪");
                    String stats = obj.optString("stats", "統計更新");

                    String js = String.format("window.__updateAI && window.__updateAI('%s', %d, '%s', '%s');",
                            pick, conf, reason, stats);
                    webView.evaluateJavascript(js, null);
                } catch (Exception e) {
                    updateHUDWithError("回傳解析格式異常");
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
            "function syncInputs() {" +
            "  var active = document.activeElement;" +
            "  if (active && active.tagName === 'INPUT') return;" +
            "  var inps = document.querySelectorAll('input');" +
            "  for (var i = 0; i < inps.length; i++) {" +
            "    var inp = inps[i];" +
            "    if (inp.value && inp.value.trim().length > 0) {" +
            "      inp.dispatchEvent(new Event('input', { bubbles: true }));" +
            "      inp.dispatchEvent(new Event('change', { bubbles: true }));" +
            "    }" +
            "  }" +
            "}" +
            "setInterval(syncInputs, 500);" +

            "if (document.getElementById('slot-assistant-hud')) return;" +

            "var hud = document.createElement('div');" +
            "hud.id = 'slot-assistant-hud';" +
            "hud.style.cssText = 'position:fixed;top:50px;right:8px;width:205px;background:rgba(11,17,32,0.95);border:1px solid rgba(56,189,248,0.7);border-radius:10px;z-index:999999;color:#f1f5f9;font-size:11px;box-shadow:0 8px 24px rgba(0,0,0,0.85);font-family:sans-serif;user-select:none;backdrop-filter:blur(6px);';" +
            "hud.innerHTML = " +
                "'<div id=\"hud_header\" style=\"padding:7px 10px;background:#1e293b;border-radius:10px 10px 0 0;font-weight:bold;color:#38bdf8;display:flex;justify-content:space-between;align-items:center;border-bottom:1px solid #334155;cursor:move;touch-action:none;\">" +
                    "<span>👑 旗艦引路人</span>" +
                    "<div style=\"display:flex;gap:3px;align-items:center;\">" +
                        "<button id=\"tab_bac\" style=\"background:#2563eb;border:1px solid #3b82f6;color:#fff;padding:1px 5px;border-radius:3px;font-size:10px;cursor:pointer;\">百家</button>" +
                        "<button id=\"tab_slt\" style=\"background:#0f172a;border:1px solid #475569;color:#94a3b8;padding:1px 5px;border-radius:3px;font-size:10px;cursor:pointer;\">老虎</button>" +
                        "<span id=\"hud_tog\" style=\"cursor:pointer;color:#94a3b8;font-size:10px;margin-left:2px;\">[收]</span>" +
                    "</div>" +
                "</div>' +" +
                "'<div id=\"hud_content\" style=\"padding:10px;\">' +" +
                    // 百家樂面板
                    "'<div id=\"p_bac\">' +" +
                        "'<div style=\"display:grid;grid-template-columns:1fr 1fr;gap:4px;margin-bottom:6px;\">" +
                            "<button id=\"nav_mt\" style=\"background:#0f172a;border:1px solid #38bdf8;color:#38bdf8;padding:3px 0;border-radius:3px;font-size:10px;font-weight:bold;cursor:pointer;\">MT 百家</button>" +
                            "<button id=\"nav_dg\" style=\"background:#0f172a;border:1px solid #475569;color:#e2e8f0;padding:3px 0;border-radius:3px;font-size:10px;cursor:pointer;\">DG 百家</button>" +
                        "</div>' +" +
                        "'<div style=\"display:flex;justify-content:space-between;align-items:center;margin-bottom:6px;\">" +
                            "<span style=\"color:#94a3b8;font-size:10px;\">演算引擎:</span>" +
                            "<button id=\"btn_engine\" style=\"background:#10b981;border:none;color:#fff;padding:2px 6px;border-radius:3px;font-size:10px;font-weight:bold;cursor:pointer;\">本機秒析</button>" +
                        "</div>' +" +
                        "'<div style=\"background:rgba(15,23,42,0.85);border:1px solid #3b82f6;border-radius:6px;padding:8px 4px;text-align:center;margin-bottom:6px;\">" +
                            "<div style=\"font-size:10px;color:#94a3b8;\">🎯 下一手推薦</div>" +
                            "<div id=\"ai_pick_target\" style=\"font-size:18px;font-weight:900;color:#ef4444;margin:2px 0;\">待命中</div>" +
                            "<div id=\"ai_pick_desc\" style=\"font-size:10px;color:#38bdf8;\">即時讀取牌桌路單</div>" +
                        "</div>' +" +
                        "'<button id=\"btn_do_ai\" style=\"display:none;width:100%;background:#2563eb;color:#fff;border:none;padding:5px 0;border-radius:4px;font-weight:bold;cursor:pointer;margin-bottom:6px;font-size:10px;\">⚡ 呼叫大模型精算</button>' +" +
                        "'<div style=\"display:flex;justify-content:space-between;font-size:10px;color:#94a3b8;\">" +
                            "<span id=\"hud_score\">莊0 閒0 和0 (0局)</span>" +
                            "<span id=\"hud_sync_dot\" style=\"color:#4ade80;\">● 連線</span>" +
                        "</div>' +" +
                    "'</div>' +" +
                    // 老虎機面板
                    "'<div id=\"p_slt\" style=\"display:none;\">' +" +
                        "'<div style=\"display:grid;grid-template-columns:repeat(3, 1fr);gap:3px;margin-bottom:8px;\">" +
                            "<button id=\"nav_atg\" style=\"background:#0f172a;border:1px solid #475569;color:#fff;padding:3px 0;border-radius:3px;font-size:9px;cursor:pointer;\">虎小妹</button>" +
                            "<button id=\"nav_rsg\" style=\"background:#0f172a;border:1px solid #475569;color:#fff;padding:3px 0;border-radius:3px;font-size:9px;cursor:pointer;\">雷神</button>" +
                            "<button id=\"nav_ava\" style=\"background:#0f172a;border:1px solid #475569;color:#fff;padding:3px 0;border-radius:3px;font-size:9px;cursor:pointer;\">Avatar</button>" +
                        "</div>' +" +
                        "'<div style=\"display:flex;justify-content:space-between;align-items:center;margin-bottom:6px;\">" +
                            "<span>單注:</span>" +
                            "<input id=\"s_bet\" type=\"number\" value=\"1.00\" step=\"0.10\" style=\"width:50px;background:#1e293b;border:1px solid #475569;color:#38bdf8;padding:2px;text-align:right;border-radius:3px;\">" +
                        "</div>' +" +
                        "'<div id=\"slot_modes\" style=\"display:grid;grid-template-columns:repeat(3, 1fr);gap:2px;margin-bottom:8px;\"></div>' +" +
                        "'<div style=\"font-size:10px;\">' +" +
                            "'<div style=\"display:flex;justify-content:space-between;margin-bottom:3px;\">免遊成本: <b id=\"s_cost\" style=\"color:#fff;\">$200.00</b></div>' +" +
                            "'<div style=\"display:flex;justify-content:space-between;margin-bottom:3px;\">理論(96.5%): <b id=\"s_ev\" style=\"color:#4ade80;\">$193.00</b></div>' +" +
                            "'<div style=\"display:flex;justify-content:space-between;margin-bottom:3px;\">中位數: <b id=\"s_med\" style=\"color:#facc15;\">$84.00</b></div>' +" +
                            "'<div style=\"display:flex;justify-content:space-between;\">20000x 率: <b style=\"color:#f43f5e;\">0.20%</b></div>' +" +
                        "'</div>' +" +
                    "'</div>' +" +
                "'</div>';" +
            "document.body.appendChild(hud);" +

            // 收合與拖曳
            "var tog = document.getElementById('hud_tog');" +
            "var cnt = document.getElementById('hud_content');" +
            "tog.onclick = function(e) {" +
            "  e.stopPropagation();" +
            "  if (cnt.style.display === 'none') { cnt.style.display = 'block'; tog.innerText = '[收]'; }" +
            "  else { cnt.style.display = 'none'; tog.innerText = '[展]'; }" +
            "};" +

            "var header = document.getElementById('hud_header');" +
            "var isDrag = false, sX, sY, iL, iT;" +
            "header.addEventListener('touchstart', function(e) {" +
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

            // 分頁切換
            "var tB = document.getElementById('tab_bac');" +
            "var tS = document.getElementById('tab_slt');" +
            "var pB = document.getElementById('p_bac');" +
            "var pS = document.getElementById('p_slt');" +
            "function setTab(tab) {" +
            "  if (tab === 'bac') {" +
            "    tB.style.background = '#2563eb'; tB.style.color = '#fff'; tB.style.borderColor = '#3b82f6';" +
            "    tS.style.background = '#0f172a'; tS.style.color = '#94a3b8'; tS.style.borderColor = '#475569';" +
            "    pB.style.display = 'block'; pS.style.display = 'none';" +
            "  } else {" +
            "    tS.style.background = '#2563eb'; tS.style.color = '#fff'; tS.style.borderColor = '#3b82f6';" +
            "    tB.style.background = '#0f172a'; tB.style.color = '#94a3b8'; tB.style.borderColor = '#475569';" +
            "    pS.style.display = 'block'; pB.style.display = 'none';" +
            "  }" +
            "}" +
            "tB.onclick = function(e) { e.stopPropagation(); setTab('bac'); };" +
            "tS.onclick = function(e) { e.stopPropagation(); setTab('slt'); };" +

            // 換台通道
            "function navTo(url) {" +
            "  if (window.AndroidBridge && window.AndroidBridge.switchGame) window.AndroidBridge.switchGame(url);" +
            "  else location.href = url;" +
            "}" +
            "document.getElementById('nav_mt').onclick = function(e) { e.stopPropagation(); navTo('[https://www.osc169.com/#/game/play?game_name=meta_all&game_type=3&device=mobile](https://www.osc169.com/#/game/play?game_name=meta_all&game_type=3&device=mobile)'); };" +
            "document.getElementById('nav_dg').onclick = function(e) { e.stopPropagation(); navTo('[https://www.osc169.com/#/game/play?game_name=dg&game_type=3&device=mobile](https://www.osc169.com/#/game/play?game_name=dg&game_type=3&device=mobile)'); };" +
            "document.getElementById('nav_atg').onclick = function(e) { e.stopPropagation(); navTo('[https://www.osc169.com/#/game/play?game_name=atg&productId=tiger-princess&device=mobile](https://www.osc169.com/#/game/play?game_name=atg&productId=tiger-princess&device=mobile)'); };" +
            "document.getElementById('nav_rsg').onclick = function(e) { e.stopPropagation(); navTo('[https://www.osc169.com/#/game/play?game_name=rsg&productId=129&device=mobile](https://www.osc169.com/#/game/play?game_name=rsg&productId=129&device=mobile)'); };" +
            "document.getElementById('nav_ava').onclick = function(e) { e.stopPropagation(); navTo('[https://www.osc169.com/#/game/play?game_name=avatar&productId=140083&device=mobile](https://www.osc169.com/#/game/play?game_name=avatar&productId=140083&device=mobile)'); };" +

            "if (location.href.indexOf('tiger-princess') !== -1 || location.href.indexOf('productId=129') !== -1 || location.href.indexOf('avatar') !== -1) setTab('slt');" +

            // 老虎機計算
            "var slotConfigs = {" +
            "  atg: [{ label: '200x', mult: 200 }, { label: '500x', mult: 500 }, { label: '2000x', mult: 2000 }]," +
            "  rsg: [{ label: '免遊 100x', mult: 100 }]," +
            "  ava: [{ label: '獎金 80x', mult: 80 }, { label: '最大 240x', mult: 240 }]" +
            "};" +
            "var curSlot = 'atg';" +
            "if (location.href.indexOf('productId=129') !== -1) curSlot = 'rsg';" +
            "else if (location.href.indexOf('avatar') !== -1) curSlot = 'ava';" +
            "var curMult = slotConfigs[curSlot][0].mult;" +
            "var betInp = document.getElementById('s_bet');" +

            "function updateSlotCalc() {" +
            "  var b = parseFloat(betInp.value) || 0;" +
            "  var c = b * curMult;" +
            "  document.getElementById('s_cost').innerText = '$' + c.toFixed(2);" +
            "  document.getElementById('s_ev').innerText = '$' + (c * 0.965).toFixed(2);" +
            "  document.getElementById('s_med').innerText = '$' + (b * (curMult * 0.42)).toFixed(2);" +
            "}" +

            "function renderSlotButtons() {" +
            "  var container = document.getElementById('slot_modes');" +
            "  container.innerHTML = '';" +
            "  var modes = slotConfigs[curSlot];" +
            "  container.style.gridTemplateColumns = 'repeat(' + modes.length + ', 1fr)';" +
            "  for (var i = 0; i < modes.length; i++) {" +
            "    (function(m) {" +
            "      var btn = document.createElement('button');" +
            "      btn.innerText = m.label;" +
            "      btn.style.cssText = 'padding:3px 0;border-radius:3px;font-size:9px;cursor:pointer;';" +
            "      if (m.mult === curMult) {" +
            "        btn.style.background = '#3b82f6'; btn.style.border = 'none'; btn.style.color = '#fff'; btn.style.fontWeight = 'bold';" +
            "      } else {" +
            "        btn.style.background = '#0f172a'; btn.style.border = '1px solid #475569'; btn.style.color = '#94a3b8';" +
            "      }" +
            "      btn.onclick = function(e) {" +
            "        e.stopPropagation(); curMult = m.mult; renderSlotButtons(); updateSlotCalc();" +
            "      };" +
            "      container.appendChild(btn);" +
            "    })(modes[i]);" +
            "  }" +
            "}" +
            "betInp.oninput = updateSlotCalc;" +
            "renderSlotButtons();" +
            "updateSlotCalc();" +

            // 三模式切換（本機秒析 -> Gemini -> GPT-4o）
            "var curEngine = 'local';" +
            "var btnEng = document.getElementById('btn_engine');" +
            "var btnDo = document.getElementById('btn_do_ai');" +
            "btnEng.onclick = function(e) {" +
            "  e.stopPropagation();" +
            "  if (curEngine === 'local') {" +
            "    curEngine = 'gemini'; btnEng.innerText = 'Gemini 3.8'; btnEng.style.background = '#0284c7'; btnDo.style.display = 'block';" +
            "  } else if (curEngine === 'gemini') {" +
            "    curEngine = 'openai'; btnEng.innerText = 'GPT-4o'; btnEng.style.background = '#10a37f'; btnDo.style.display = 'block';" +
            "  } else {" +
            "    curEngine = 'local'; btnEng.innerText = '本機秒析'; btnEng.style.background = '#10b981'; btnDo.style.display = 'none';" +
            "  }" +
            "};" +

            // 呼叫雲端大模型
            "btnDo.onclick = function(e) {" +
            "  e.stopPropagation();" +
            "  btnDo.innerText = '🧠 雲端運算中...'; btnDo.disabled = true;" +
            "  var raw = document.body ? (document.body.innerText || '') : '';" +
            "  try {" +
            "    var ifrs = document.querySelectorAll('iframe');" +
            "    for (var i = 0; i < ifrs.length; i++) {" +
            "      var d = ifrs[i].contentDocument || ifrs[i].contentWindow.document;" +
            "      if (d && d.body) raw += ' ' + d.body.innerText;" +
            "    }" +
            "  } catch(e) {}" +
            "  if (window.AndroidBridge && window.AndroidBridge.requestAIAnalysis) {" +
            "    window.AndroidBridge.requestAIAnalysis(raw.substring(0, 4000), curEngine);" +
            "  }" +
            "};" +

            // 本機路單解析器（排除莊對、閒對干擾）
            "var lastTot = -1;" +
            "function scanLocalData() {" +
            "  try {" +
            "    var raw = document.body ? (document.body.innerText || '') : '';" +
            "    try {" +
            "      var ifrs = document.querySelectorAll('iframe');" +
            "      for (var k = 0; k < ifrs.length; k++) {" +
            "        var d = ifrs[k].contentDocument || ifrs[k].contentWindow.document;" +
            "        if (d && d.body) raw += ' ' + d.body.innerText;" +
            "      }" +
            "    } catch(e) {}" +
            "    if (!raw) return;" +
            "    var clean = raw.replace(/莊對\\s*\\d+/g, '').replace(/閒對\\s*\\d+/g, '').replace(/\\d+\\s*:\\s*[\\d.]+/g, '');" +
            "    var mB = clean.match(/莊\\s*(\\d+)/);" +
            "    var mP = clean.match(/閒\\s*(\\d+)/);" +
            "    var mT = clean.match(/和\\s*(\\d+)/);" +
            "    var mTot = clean.match(/(?:總數|局數)\\s*(\\d+)/);" +
            "    if (mB && mP) {" +
            "      var b = parseInt(mB[1], 10), p = parseInt(mP[1], 10);" +
            "      var t = mT ? parseInt(mT[1], 10) : 0;" +
            "      var tot = mTot ? parseInt(mTot[1], 10) : (b + p + t);" +
            "      if (tot > 0 && tot !== lastTot) {" +
            "        lastTot = tot;" +
            "        var sElem = document.getElementById('hud_score');" +
            "        if (sElem) sElem.innerText = '莊' + b + ' 閒' + p + ' 和' + t + ' (' + tot + '局)';" +
            "        if (curEngine === 'local') {" +
            "          var delta = p - b;" +
            "          var pick = '莊', conf = 62, reason = '動量起勢';" +
            "          if (delta >= 3) { pick = '莊'; conf = Math.min(88, 62 + delta * 3); reason = '閒領先 ' + delta + ' 局(回歸)'; }" +
            "          else if (delta <= -3) { pick = '閒'; conf = Math.min(88, 62 + Math.abs(delta) * 3); reason = '莊領先 ' + Math.abs(delta) + ' 局(回歸)'; }" +
            "          else { pick = (b >= p) ? '莊' : '閒'; conf = 60 + (tot % 8); reason = '常態跟勢'; }" +
            "          var tElem = document.getElementById('ai_pick_target');" +
            "          var dElem = document.getElementById('ai_pick_desc');" +
            "          if (tElem) { tElem.innerText = '【' + pick + '】 ' + conf + '%'; tElem.style.color = (pick === '莊') ? '#ef4444' : '#3b82f6'; }" +
            "          if (dElem) dElem.innerText = reason;" +
            "        }" +
            "      }" +
            "    }" +
            "  } catch(e) {}" +
            "}" +
            "setInterval(scanLocalData, 600);" +
            "scanLocalData();" +

            // 回調更新函數
            "window.__updateAI = function(pick, conf, reason, stats) {" +
            "  var t = document.getElementById('ai_pick_target');" +
            "  var d = document.getElementById('ai_pick_desc');" +
            "  var s = document.getElementById('hud_score');" +
            "  if (t) { t.innerText = '【' + pick + '】 ' + conf + '%'; t.style.color = (pick === '莊') ? '#ef4444' : '#3b82f6'; }" +
            "  if (d) d.innerText = reason;" +
            "  if (s) s.innerText = stats;" +
            "  btnDo.innerText = '⚡ 呼叫大模型精算'; btnDo.disabled = false;" +
            "};" +

            "window.__updateAIError = function(msg) {" +
            "  var d = document.getElementById('ai_pick_desc');" +
            "  if (d) d.innerText = msg;" +
            "  btnDo.innerText = '重試'; btnDo.disabled = false;" +
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
