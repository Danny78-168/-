package com.slot.assistant;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private WebView webView;

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
        settings從截圖中最關鍵的一點可以看到：帳號框上方依然掛著紫色的提示泡泡：**「✔ 請填寫使用者名稱」**。

---

### 為什麼會再次發生？

1. **按鈕有被點擊，但被前端網站攔截：**  
   這個紫色泡泡證明「登入按鈕其實點得下去」，但因為手機鍵盤輸入或瀏覽器記住密碼時，**沒有觸發 Vue / React 的 `input` 監聽事件**，網站底層的變數判定帳號欄位仍是「空白」，因此在點擊登入的瞬間被前端驗證直接擋下，沒有向伺服器發送登入請求。
2. **修復腳本在上一次更新時被覆蓋：**  
   上一回為了把「老虎機」與「百家樂」兩大功能重新融合，腳本中負責**自動派發表單事件（Form Fixer）**的代碼被遺漏了。

---

### 一、 當前畫面免重裝，3 秒手動登入法

在重新打包前，不用關閉 App，照以下步驟即可立刻通過：
1. 點擊 `JF6403` 帳號輸入框末端。
2. 按鍵盤刪除鍵把 **`3`** 刪掉，再手動重新輸入 **`3`**。
3. 點一下下方的「密碼輸入框」讓帳號框**失焦（Blur）**。
4. 此時上方的紫色泡泡**「✔ 請填寫使用者名稱」會立刻消失**，接著點擊「登入」即可成功進入。

---

### 二、 徹底修復：永久內嵌「表單自動同步引擎」

已將 **Vue / React 表單狀態自動同步器** 永久寫入腳本：只要畫面偵測到登入視窗，系統每 400ms 就會強制將輸入框內容同步給前端框架，並在點擊「登入」瞬間自動消除驗證警告，**同時完整保留老虎機與百家樂的所有功能**。

請將 `app/src/main/java/com/slot/assistant/MainActivity.java` 完整替換為以下程式碼：

```java
package com.slot.assistant;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;
import java.util.Collections;

public class MainActivity extends AppCompatActivity {
    private WebView webView;

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
        }

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(webView, true);
        }

        settings.setUserAgentString("Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36");

        // Android 原生切換網址通道
        webView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void switchGame(final String url) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        webView.loadUrl(url);
                        webView.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                webView.reload();
                            }
                        }, 350);
                    }
                });
            }

            @JavascriptInterface
            public void onGameDataReceived(final int b, final int p, final int t, final int total) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        webView.evaluateJavascript(
                            "if(window.updateHUD) window.updateHUD(" + b + "," + p + "," + t + "," + total + ");",
                            null
                        );
                    }
                });
            }
        }, "AndroidBridge");

        // 注入跨框架底層腳本
        String script = getInjectedScript();
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            WebViewCompat.addDocumentStartJavaScript(webView, script, Collections.singleton("*"));
        }

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                view.evaluateJavascript(getInjectedScript(), null);
            }
        });

        // 預設開啟 MT 百家
        webView.loadUrl("[https://www.osc169.com/#/game/play?game_name=meta_all&game_type=3&device=mobile](https://www.osc169.com/#/game/play?game_name=meta_all&game_type=3&device=mobile)");
    }

    private String getInjectedScript() {
        return "(function() {" +
            // ================= 1. 永久表單自動同步引擎 (徹底根治登入未響應) =================
            "function syncAllInputs() {" +
            "  var pwd = document.querySelector('input[type=\"password\"]');" +
            "  if (!pwd) return;" +
            "  var inputs = document.querySelectorAll('input');" +
            "  inputs.forEach(function(inp) {" +
            "    if (inp.value && inp.value.trim().length > 0) {" +
            "      try {" +
            "        var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;" +
            "        if (setter) setter.call(inp, inp.value);" +
            "      } catch(e) {}" +
            "      inp.dispatchEvent(new Event('input', { bubbles: true, cancelable: true }));" +
            "      inp.dispatchEvent(new Event('change', { bubbles: true, cancelable: true }));" +
            "    }" +
            "  });" +
            "}" +
            "setInterval(syncAllInputs, 400);" +

            "if (!window._loginCaptureBound) {" +
            "  window._loginCaptureBound = true;" +
            "  ['touchstart', 'pointerdown', 'click'].forEach(function(evtName) {" +
            "    document.addEventListener(evtName, function(e) {" +
            "      var el = e.target;" +
            "      if (!el) return;" +
            "      var txt = (el.innerText || el.textContent || '').trim();" +
            "      if (txt.indexOf('登入') !== -1 || (el.className && typeof el.className === 'string' && el.className.indexOf('login') !== -1)) {" +
            "        syncAllInputs();" +
            "        document.querySelectorAll('input').forEach(function(inp) {" +
            "          inp.dispatchEvent(new Event('blur', { bubbles: true }));" +
            "        });" +
            "      }" +
            "    }, true);" +
            "  });" +
            "}" +

            // ================= 2. 跨框架百家樂自動採集器 =================
            "function dispatchStats(b, p, t, tot) {" +
            "  if (isNaN(b) || isNaN(p) || (b === 0 && p === 0)) return;" +
            "  tot = tot || (b + p + (t || 0));" +
            "  try { if (window.AndroidBridge && window.AndroidBridge.onGameDataReceived) window.AndroidBridge.onGameDataReceived(b, p, t, tot); } catch(e){}" +
            "  try { window.top.postMessage({ type: 'YINLU_DATA', b: b, p: p, t: t, total: tot }, '*'); } catch(e){}" +
            "  if (window.updateHUD) window.updateHUD(b, p, t, tot);" +
            "}" +

            "if (!window._wsHooked) {" +
            "  window._wsHooked = true;" +
            "  try {" +
            "    var OrigWS = window.WebSocket;" +
            "    if (OrigWS) {" +
            "      var PatchedWS = function(u, pr) {" +
            "        var ws = pr ? new OrigWS(u, pr) : new OrigWS(u);" +
            "        ws.addEventListener('message', function(evt) {" +
            "          try {" +
            "            var d = evt.data;" +
            "            if (typeof d === 'string' && d.indexOf('{') !== -1) {" +
            "              var j = JSON.parse(d);" +
            "              if (j.banker !== undefined && j.player !== undefined) dispatchStats(parseInt(j.banker,10), parseInt(j.player,10), parseInt(j.tie||0,10));" +
            "            }" +
            "          } catch(e) {}" +
            "        });" +
            "        return ws;" +
            "      };" +
            "      PatchedWS.prototype = OrigWS.prototype;" +
            "      PatchedWS.CONNECTING = OrigWS.CONNECTING; PatchedWS.OPEN = OrigWS.OPEN; PatchedWS.CLOSING = OrigWS.CLOSING; PatchedWS.CLOSED = OrigWS.CLOSED;" +
            "      window.WebSocket = PatchedWS;" +
            "    }" +
            "  } catch(e) {}" +
            "}" +

            "setInterval(function() {" +
            "  try {" +
            "    if (document.querySelector('input[type=\"password\"]')) return;" +
            "    var txt = document.body ? (document.body.innerText || '') : '';" +
            "    if (txt) {" +
            "      var mb = txt.match(/(?:莊|庄)\\s*[:：]?\\s*(\\d+)/);" +
            "      var mp = txt.match(/(?:閒|闲)\\s*[:：]?\\s*(\\d+)/);" +
            "      var mt = txt.match(/(?:和)\\s*[:：]?\\s*(\\d+)/);" +
            "      var mtot = txt.match(/(?:總數|总数|總局|总局)\\s*[:：]?\\s*(\\d+)/);" +
            "      if (mb && mp) {" +
            "        var b = parseInt(mb[1], 10), p = parseInt(mp[1], 10);" +
            "        var t = mt ? parseInt(mt[1], 10) : 0;" +
            "        var tot = mtot ? parseInt(mtot[1], 10) : (b + p + t);" +
            "        dispatchStats(b, p, t, tot);" +
            "      }" +
            "    }" +
            "  } catch(e) {}" +
            "}, 900);" +

            // ================= 3. 主視窗 HUD 介面 =================
            "if (window.top !== window.self) return;" +
            "if (document.getElementById('slot-assistant-hud')) return;" +

            "var isSlot = location.href.indexOf('tiger-princess') !== -1 || location.href.indexOf('productId=129') !== -1 || location.href.indexOf('avatar') !== -1;" +

            "var hud = document.createElement('div');" +
            "hud.id = 'slot-assistant-hud';" +
            "hud.style.cssText = 'position:fixed;top:50px;right:8px;width:195px;background:rgba(11,17,32,0.94);border:1px solid rgba(56,189,248,0.6);border-radius:10px;z-index:999999;color:#f1f5f9;font-size:11px;box-shadow:0 8px 24px rgba(0,0,0,0.85);font-family:sans-serif;user-select:none;backdrop-filter:blur(6px);';" +
            "hud.innerHTML = " +
                "'<div id=\"hud_header\" style=\"padding:7px 10px;background:#1e293b;border-radius:10px 10px 0 0;font-weight:bold;color:#38bdf8;display:flex;justify-content:space-between;align-items:center;border-bottom:1px solid #334155;cursor:move;touch-action:none;\">" +
                    "<span>⚡ 引路人</span>" +
                    "<div style=\"display:flex;gap:3px;align-items:center;\">" +
                        "<button id=\"tab_bac\" style=\"background:' + (isSlot ? '#0f172a' : '#2563eb') + ';border:1px solid #3b82f6;color:#fff;padding:1px 5px;border-radius:3px;font-size:10px;cursor:pointer;\">百家</button>" +
                        "<button id=\"tab_slt\" style=\"background:' + (isSlot ? '#2563eb' : '#0f172a') + ';border:1px solid #475569;color:#94a3b8;padding:1px 5px;border-radius:3px;font-size:10px;cursor:pointer;\">老虎</button>" +
                        "<span id=\"hud_tog\" style=\"cursor:pointer;color:#94a3b8;font-size:10px;margin-left:2px;\">[收]</span>" +
                    "</div>" +
                "</div>' +" +
                "'<div id=\"hud_content\" style=\"padding:10px;\">' +" +

                    // ---------- [百家樂模式面板] ----------
                    "'<div id=\"p_bac\" style=\"display:' + (isSlot ? 'none' : 'block') + ';\">' + " +
                        "'<div style=\"display:grid;grid-template-columns:1fr 1fr;gap:4px;margin-bottom:8px;\">" +
                            "<button id=\"nav_mt\" style=\"background:#0f172a;border:1px solid #38bdf8;color:#38bdf8;padding:3px 0;border-radius:3px;font-size:10px;font-weight:bold;\">MT 百家</button>" +
                            "<button id=\"nav_dg\" style=\"background:#0f172a;border:1px solid #475569;color:#e2e8f0;padding:3px 0;border-radius:3px;font-size:10px;\">DG 百家</button>" +
                        "</div>' +" +
                        "'<div style=\"background:rgba(15,23,42,0.85);border:1px solid #3b82f6;border-radius:6px;padding:8px 4px;text-align:center;margin-bottom:6px;\">" +
                            "<div style=\"font-size:10px;color:#94a3b8;\">🎯 下一手 AI 推薦</div>" +
                            "<div id=\"ai_pick_target\" style=\"font-size:19px;font-weight:900;color:#ef4444;margin:2px 0;\">分析中...</div>" +
                            "<div id=\"ai_pick_desc\" style=\"font-size:10px;color:#38bdf8;\">等待遊戲路單</div>" +
                        "</div>' +" +
                        "'<div style=\"display:flex;justify-content:space-between;font-size:10px;color:#94a3b8;\">" +
                            "<span id=\"hud_score\">莊0 閒0 和0</span>" +
                            "<span id=\"hud_sync_dot\" style=\"color:#4ade80;\">● 連線</span>" +
                        "</div>' +" +
                    "'</div>' +" +

                    // ---------- [老虎機模式面板] ----------
                    "'<div id=\"p_slt\" style=\"display:' + (isSlot ? 'block' : 'none') + ';\">' + " +
                        "'<div style=\"display:grid;grid-template-columns:repeat(3, 1fr);gap:3px;margin-bottom:8px;\">" +
                            "<button id=\"nav_atg\" style=\"background:#0f172a;border:1px solid #475569;color:#fff;padding:3px 0;border-radius:3px;font-size:9px;\">虎小妹</button>" +
                            "<button id=\"nav_rsg\" style=\"background:#0f172a;border:1px solid #475569;color:#fff;padding:3px 0;border-radius:3px;font-size:9px;\">雷神</button>" +
                            "<button id=\"nav_ava\" style=\"background:#0f172a;border:1px solid #475569;color:#fff;padding:3px 0;border-radius:3px;font-size:9px;\">Avatar</button>" +
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

            // 收合與展開
            "var tog = document.getElementById('hud_tog');" +
            "var cnt = document.getElementById('hud_content');" +
            "tog.onclick = function(e) {" +
            "  e.stopPropagation();" +
            "  if (cnt.style.display === 'none') { cnt.style.display = 'block'; tog.innerText = '[收]'; }" +
            "  else { cnt.style.display = 'none'; tog.innerText = '[展]'; }" +
            "};" +

            // 觸控拖曳
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
            "tB.onclick = function() { setTab('bac'); };" +
            "tS.onclick = function() { setTab('slt'); };" +

            // 原生換台
            "function navTo(url) {" +
            "  if (window.AndroidBridge && window.AndroidBridge.switchGame) window.AndroidBridge.switchGame(url);" +
            "  else { location.replace(url); location.reload(); }" +
            "}" +
            "document.getElementById('nav_mt').onclick = function() { navTo('[https://www.osc169.com/#/game/play?game_name=meta_all&game_type=3&device=mobile](https://www.osc169.com/#/game/play?game_name=meta_all&game_type=3&device=mobile)'); };" +
            "document.getElementById('nav_dg').onclick = function() { navTo('[https://www.osc169.com/#/game/play?game_name=dg&game_type=3&device=mobile](https://www.osc169.com/#/game/play?game_name=dg&game_type=3&device=mobile)'); };" +
            "document.getElementBy
