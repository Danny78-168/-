package com.slot.assistant;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
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
import android.widget.TextView;
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
    private EditText etUrlInput;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private boolean isAnalyzing = false;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));

        int statusBarHeight = 0;
        int resId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resId > 0) {
            statusBarHeight = getResources().getDimensionPixelSize(resId);
        }

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
        LinearLayout.LayoutParams etParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
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
        etUrlInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
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

            @JavascriptInterface
            public void openExternalUrl(final String targetUrl) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl));
                            MainActivity.this.startActivity(intent);
                        } catch (Exception e) {
                            webView.loadUrl(targetUrl);
                        }
                    }
                });
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

                    Bitmap fullBmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(fullBmp);
                    webView.draw(canvas);

                    int cropY = (int) (h * 0.45);
                    int cropH = h - cropY;
                    Bitmap cropBmp = Bitmap.createBitmap(fullBmp, 0, cropY, w, cropH);
                    fullBmp.recycle();

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

        String prompt = "你是頂尖百家樂路單視覺精算大師。請精準解析圖片下半部的路單與統計數據：\n\n" +
                "【任務一：全廳通用數據提取（請依畫面實際排版判斷）】\n" +
                "請在路單上方或下方尋找數據列，依據以下特徵提取：\n" +
                "1. 庄/莊 (banker)：紅圈或標有「庄/莊/B」旁的數字。\n" +
                "2. 闲/閒 (player)：藍圈或標有「闲/閒/P」旁的數字。\n" +
                "3. 和 (tie)：綠圈或標有「和/T」旁的數字。\n" +
                "4. 總數 (total)：\n" +
                "   - 若有「总/總/#」標籤，直接讀取其數值（如 DG 寫在最右邊『总 33』）。\n" +
                "   - 若最左側為無標籤的大數字，該數字即為總數（如 DB 的『40 13 26 1』）。\n" +
                "   - 若畫面未印總數（如 AE Sexy 僅有『庄 20 闲 22 和 4』），則自動加總：total = 庄 + 闲 + 和 (20+22+4=46)！\n\n" +
                "【任務二：客觀大路走向分析】\n" +
                "1. 嚴格鎖定大路【最右側最新一列】！上一列若是紅、最新這列只有1顆藍，屬【單跳/轉向】，絕不可胡扯為連莊！\n" +
                "2. 嚴禁輸出觀望！必須強制二選一輸出【莊】或【閒】。conf 介於 72%~91%。\n\n" +
                "輸出標準純 JSON：\n" +
                "{\"total\":33,\"banker\":13,\"player\":18,\"tie\":2,\"pick\":\"莊\",\"conf\":82,\"reason\":\"大路最新落點出閒轉向，下三路合流齊腳\"}";

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
        String rawContent = resObj.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
        return cleanJson(rawContent);
    }

    private void updateHUDWithResult(final String jsonStr) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject obj = new JSONObject(jsonStr);
                    String pick = obj.optString("pick", "莊");
                    int conf = obj.optInt("conf", 78);
                    String reason = obj.optString("reason", "走勢分析完成");

                    int total = obj.optInt("total", 0);
                    int banker = obj.optInt("banker", 0);
                    int player = obj.optInt("player", 0);
                    int tie = obj.optInt("tie", 0);

                    String stats = String.format("總數: %d | 莊: %d | 和: %d | 閒: %d", total, banker, tie, player);

                    String js = String.format("window.__updateAI && window.__updateAI('%s', %d, '%s', '%s');",
                            pick, conf, reason, stats);
                    webView.evaluateJavascript(js, null);
                } catch (Exception e) {
                    updateHUDWithError("數據解析異常: " + e.getMessage());
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
        sb.append("    h += '<div style=\"display:flex;border-bottom:1px solid #334155;margin-bottom:8px;\">';");
        sb.append("    h += '<div id=\"tab_btn_1\" style=\"flex:1;text-align:center;padding:5px 0;cursor:pointer;font-weight:bold;color:#38bdf8;border-bottom:2px solid #38bdf8;\">🎯 AI 精算</div>';");
        sb.append("    h += '<div id=\"tab_btn_2\" style=\"flex:1;text-align:center;padding:5px 0;cursor:pointer;font-weight:bold;color:#94a3b8;border-bottom:2px solid transparent;\">💬 反饋客服</div>';");
        sb.append("    h += '</div>';");
        sb.append("    h += '<div id=\"tab_page_1\">';");
        sb.append("    h += '<div style=\"background:rgba(15,23,42,0.85);border:1px solid #1e3a8a;border-radius:6px;padding:6px 4px;text-align:center;margin-bottom:6px;\">';");
        sb.append("    h += '<div style=\"font-size:10px;color:#94a3b8;\">🎯 深度路單精算建議</div>';");
        sb.append("    h += '<div id=\"ai_pick_target\" style=\"font-size:18px;font-weight:900;color:#ef4444;margin:2px 0;\">待命中</div>';");
        sb.append("    h += '<div id=\"ai_pick_desc\" style=\"font-size:9px;color:#38bdf8;line-height:1.2;\">請進入牌桌後點擊下方按鈕</div>';");
        sb.append("    h += '</div>';");
        sb.append("    h += '<div id=\"hud_stats_box\" style=\"background:#0f172a;border:1px dashed #334155;border-radius:4px;padding:4px;text-align:center;font-size:10px;color:#38bdf8;margin-bottom:6px;\">';");
        sb.append("    h += '總數: -- | 莊: -- | 和: -- | 閒: --';");
        sb.append("    h += '</div>';");
        sb.append("    h += '<button id=\"btn_do_ai\" style=\"width:100%;background:#2563eb;color:#fff;border:none;padding:7px 0;border-radius:4px;font-weight:bold;font-size:11px;\">📸 截圖畫面並由 AI 辨識</button>';");
        sb.append("    h += '</div>';");
        sb.append("    h += '<div id=\"tab_page_2\" style=\"display:none;\">';");
        sb.append("    h += '<div style=\"background:#0f172a;border:1px solid #1e293b;border-radius:6px;padding:8px;margin-bottom:8px;\">';");
        sb.append("    h += '<div style=\"display:flex;align-items:center;margin-bottom:4px;\">';");
        sb.append("    h += '<span style=\"font-size:13px;margin-right:4px;\">✈️</span><span style=\"font-weight:bold;color:#38bdf8;\">Telegram 技術反饋</span>';");
        sb.append("    h += '</div>';");
        sb.append("    h += '<div style=\"font-size:10px;color:#cbd5e1;margin-bottom:6px;\">飛機帳號: <b style=\"color:#facc15;\">@TG_APK1</b></div>';");
        sb.append("    h += '<button id=\"btn_open_tg\" style=\"width:100%;background:#0284c7;color:#fff;border:none;padding:6px 0;border-radius:4px;font-weight:bold;font-size:10px;\">開啟 Telegram 反饋</button>';");
        sb.append("    h += '</div>';");
        sb.append("    h += '<div style=\"background:#0f172a;border:1px solid #1e293b;border-radius:6px;padding:8px;\">';");
        sb.append("    h += '<div style=\"display:flex;align-items:center;margin-bottom:4px;\">';");
        sb.append("    h += '<span style=\"font-size:13px;margin-right:4px;\">🟢</span><span style=\"font-weight:bold;color:#22c55e;\">LINE 官方帳號</span>';");
        sb.append("    h += '</div>';");
        sb.append("    h += '<div style=\"font-size:10px;color:#cbd5e1;margin-bottom:6px;\">搜尋官方帳號: <b style=\"color:#facc15;\">@osc168</b></div>';");
        sb.append("    h += '<button id=\"btn_open_line\" style=\"width:100%;background:#16a34a;color:#fff;border:none;padding:6px 0;border-radius:4px;font-weight:bold;font-size:10px;\">開啟 LINE 官方帳號</button>';");
        sb.append("    h += '</div>';");
        sb.append("    h += '</div>';");
        sb.append("    h += '</div>';");
        sb.append("    hud.innerHTML = h;");
        sb.append("    document.body.appendChild(hud);");
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
        sb.append("    var tog = document.getElementById('hud_tog');");
        sb.append("    var cnt = document.getElementById('hud_content');");
        sb.append("    bindTap(tog, function() {");
        sb.append("      var hide = (cnt.style.display === 'none');");
        sb.append("      cnt.style.display = hide ? 'block' : 'none';");
        sb.append("      tog.innerText = hide ? '[收]' : '[展]';");
        sb.append("    });");
        sb.append("    var t1Btn = document.getElementById('tab_btn_1');");
        sb.append("    var t2Btn = document.getElementById('tab_btn_2');");
        sb.append("    var p1 = document.getElementById('tab_page_1');");
        sb.append("    var p2 = document.getElementById('tab_page_2');");
        sb.append("    bindTap(t1Btn, function() {");
        sb.append("      p1.style.display = 'block'; p2.style.display = 'none';");
        sb.append("      t1Btn.style.color = '#38bdf8'; t1Btn.style.borderBottom = '2px solid #38bdf8';");
        sb.append("      t2Btn.style.color = '#94a3b8'; t2Btn.style.borderBottom = '2px solid transparent';");
        sb.append("    });");
        sb.append("    bindTap(t2Btn, function() {");
        sb.append("      p1.style.display = 'none'; p2.style.display = 'block';");
        sb.append("      t2Btn.style.color = '#38bdf8'; t2Btn.style.borderBottom = '2px solid #38bdf8';");
        sb.append("      t1Btn.style.color = '#94a3b8'; t1Btn.style.borderBottom = '2px solid transparent';");
        sb.append("    });");
        sb.append("    bindTap(document.getElementById('btn_open_tg'), function() {");
        sb.append("      if (window.AndroidBridge && window.AndroidBridge.openExternalUrl) {");
        sb.append("        window.AndroidBridge.openExternalUrl('https://t.me/TG_apk1');");
        sb.append("      } else { location.href = 'https://t.me/TG_apk1'; }");
        sb.append("    });");
        sb.append("    bindTap(document.getElementById('btn_open_line'), function() {");
        sb.append("      if (window.AndroidBridge && window.AndroidBridge.openExternalUrl) {");
        sb.append("        window.AndroidBridge.openExternalUrl('https://lin.ee/xquprrD');");
        sb.append("      } else { location.href = 'https://lin.ee/xquprrD'; }");
        sb.append("    });");
        sb.append("    var btnDo = document.getElementById('btn_do_ai');");
        sb.append("    bindTap(btnDo, function() {");
        sb.append("      btnDo.innerText = '🧠 Astra 深度推論中...';");
        sb.append("      btnDo.disabled = true;");
        sb.append("      var t = document.getElementById('ai_pick_target');");
        sb.append("      var d = document.getElementById('ai_pick_desc');");
        sb.append("      if (t) { t.innerText = '分析中...'; t.style.color = '#facc15'; }");
        sb.append("      if (d) { d.innerText = '正在定位最新落點與排版數據...'; }");
        sb.append("      if (window.AndroidBridge && window.AndroidBridge.requestVisualAnalysis) {");
        sb.append("        window.AndroidBridge.requestVisualAnalysis();");
        sb.append("      }");
        sb.append("    });");
        sb.append("  }");
        sb.append("  if (document.readyState === 'loading') {");
        sb.append("    document.addEventListener('DOMContentLoaded', initHUD);");
        sb.append("  } else { initHUD(); }");
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
        sb.append("    var t = document.getElementById('ai_pick_target');");
        sb.append("    var d = document.getElementById('ai_pick_desc');");
        sb.append("    var b = document.getElementById('btn_do_ai');");
        sb.append("    if (t) { t.innerText = '【辨識異常】'; t.style.color = '#ef4444'; }");
        sb.append("    if (d) d.innerText = '錯誤: ' + msg;");
        sb.append("    if (b) { b.innerText = '📸 截圖畫面並由 AI 辨識'; b.disabled = false; }");
        sb.append("  };");
        sb.append("})();");

        view.evaluateJavascript(sb.toString(), null);
    }
}
