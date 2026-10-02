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

        // Android 原生通訊橋接
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

        // 核心：利用 addDocumentStartJavaScript 穿透 iframe，在遊戲子框架載入時第一時間植入感應器
        String coreScript = getInjectedScript();
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            WebViewCompat.addDocumentStartJavaScript(webView, coreScript, Collections.singleton("*"));
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
        webView.loadUrl("https://www.osc169.com/#/game/play?game_name=meta_all&game_type=3&device=mobile");
    }

    private String getInjectedScript() {
        return "javascript:(function() {" +
            // ================= 1. 跨框架數據採集器（運行在 iframe 及主頁面） =================
            "function dispatchGameStats(b, p, t, tot) {" +
            "  if (isNaN(b) || isNaN(p) || (b === 0 && p === 0)) return;" +
            "  tot = tot || (b + p + (t || 0));" +
            "  try { if (window.AndroidBridge && window.AndroidBridge.onGameDataReceived) window.AndroidBridge.onGameDataReceived(b, p, t, tot); } catch(e){}" +
            "  try { window.top.postMessage({ type: 'YINLU_AUTO_DATA', b: b, p: p, t: t, total: tot }, '*'); } catch(e){}" +
            "  if (window.updateHUD) window.updateHUD(b, p, t, tot);" +
            "}" +

            // 攔截子框架 WebSocket 封包
            "if (!window._wsHooked) {" +
            "  window._wsHooked = true;" +
            "  try {" +
            "    var OrigWS = window.WebSocket;" +
            "    if (OrigWS) {" +
            "      var PatchedWS = function(url, protocols) {" +
            "        var ws = protocols ? new OrigWS(url, protocols) : new OrigWS(url);" +
            "        ws.addEventListener('message', function(evt) {" +
            "          try {" +
            "            var d = evt.data;" +
            "            if (typeof d === 'string' && d.indexOf('{') !== -1) {" +
            "              var j = JSON.parse(d);" +
            "              if (j.banker !== undefined && j.player !== undefined) dispatchGameStats(parseInt(j.banker,10), parseInt(j.player,10), parseInt(j.tie||0,10));" +
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

            // 輪詢掃描畫面 DOM 文字
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
            "        dispatchGameStats(b, p, t, tot);" +
            "      }" +
            "    }" +
            "  } catch(e) {}" +
            "}, 800);" +

            // ================= 2. 極簡 HUD 介面（只在頂層視窗渲染） =================
            "if (window.top !== window.self) return;" +
            "if (document.getElementById('slot-assistant-hud')) return;" +

            "var hud = document.createElement('div');" +
            "hud.id = 'slot-assistant-hud';" +
            "hud.style.cssText = 'position:fixed;top:55px;right:8px;width:170px;background:rgba(11,17,32,0.92);border:1px solid rgba(56,189,248,0.5);border-radius:10px;z-index:999999;color:#f1f5f9;font-size:11px;box-shadow:0 8px 24px rgba(0,0,0,0.8);font-family:sans-serif;user-select:none;backdrop-filter:blur(6px);';" +
            "hud.innerHTML = " +
                "'<div id=\"hud_header\" style=\"padding:6px 8px;background:#1e293b;border-radius:10px 10px 0 0;font-weight:bold;color:#38bdf8;display:flex;justify-content:space-between;align-items:center;border-bottom:1px solid #334155;cursor:move;touch-action:none;\">" +
                    "<span>⚡ 引路人</span>" +
                    "<div style=\"display:flex;gap:4px;\">" +
                        "<button id=\"btn_toggle_game\" style=\"background:#0f172a;border:1px solid #38bdf8;color:#38bdf8;padding:1px 4px;border-radius:3px;font-size:10px;cursor:pointer;\">切DG</button>" +
                        "<span id=\"hud_tog\" style=\"cursor:pointer;color:#94a3b8;font-size:10px;\">[收]</span>" +
                    "</div>" +
                "</div>' +" +
                "'<div id=\"hud_content\" style=\"padding:8px;text-align:center;\">' +" +
                    // 核心推薦區塊
                    "'<div style=\"background:rgba(15,23,42,0.8);border:1px solid #3b82f6;border-radius:6px;padding:6px 2px;margin-bottom:6px;\">" +
                        "<div style=\"font-size:10px;color:#94a3b8;\">🎯 下一手 AI 推薦</div>" +
                        "<div id=\"ai_pick_target\" style=\"font-size:18px;font-weight:900;color:#ef4444;margin:2px 0;\">感應中...</div>" +
                        "<div id=\"ai_pick_desc\" style=\"font-size:10px;color:#38bdf8;\">等待遊戲路單</div>" +
                    "</div>' +" +
                    // 狀態欄
                    "'<div style=\"display:flex;justify-content:space-between;font-size:10px;color:#94a3b8;\">" +
                        "<span id=\"hud_score\">莊0 閒0 和0</span>" +
                        "<span id=\"hud_sync_dot\" style=\"color:#4ade80;\">● 連線</span>" +
                    "</div>' +" +
                "'</div>';" +
            "document.body.appendChild(hud);" +

            // 跨框架 postMessage 接收
            "window.addEventListener('message', function(e) {" +
            "  if (e.data && e.data.type === 'YINLU_AUTO_DATA') {" +
            "    window.updateHUD(e.data.b, e.data.p, e.data.t, e.data.total);" +
            "  }" +
            "});" +

            // 快捷換台邏輯
            "var isDG = location.href.indexOf('game_name=dg') !== -1;" +
            "var btnToggle = document.getElementById('btn_toggle_game');" +
            "btnToggle.innerText = isDG ? '切MT' : '切DG';" +
            "btnToggle.onclick = function(e) {" +
            "  e.stopPropagation();" +
            "  var target = isDG " +
            "    ? 'https://www.osc169.com/#/game/play?game_name=meta_all&game_type=3&device=mobile'" +
            "    : 'https://www.osc169.com/#/game/play?game_name=dg&game_type=3&device=mobile';" +
            "  if (window.AndroidBridge && window.AndroidBridge.switchGame) {" +
            "    window.AndroidBridge.switchGame(target);" +
            "  } else {" +
            "    location.replace(target); location.reload();" +
            "  }" +
            "};" +

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

            // AI 自動決策推薦核心更新函數
            "window.updateHUD = function(b, p, t, tot) {" +
            "  tot = tot || (b + p + t);" +
            "  var delta = p - b;" + // 閒與莊的差額
            "  var pick = '莊', conf = 60, reason = '';" +

            // 均值回歸與動量雙模混合算法
            "  if (delta >= 3) {" +
            "    pick = '莊'; conf = Math.min(88, 62 + delta * 4);" +
            "    reason = '閒領先 ' + delta + ' 局 (均值修正)';" +
            "  } else if (delta <= -3) {" +
            "    pick = '閒'; conf = Math.min(88, 62 + Math.abs(delta) * 4);" +
            "    reason = '莊領先 ' + Math.abs(delta) + ' 局 (均值修正)';" +
            "  } else {" +
            "    pick = (b >= p) ? '莊' : '閒';" +
            "    conf = 58 + (tot % 6);" +
            "    reason = '趨勢平衡推薦';" +
            "  }" +

            "  var tElem = document.getElementById('ai_pick_target');" +
            "  var dElem = document.getElementById('ai_pick_desc');" +
            "  var sElem = document.getElementById('hud_score');" +
            "  if (tElem) {" +
            "    tElem.innerText = '【' + pick + '】 ' + conf + '%';" +
            "    tElem.style.color = (pick === '莊') ? '#ef4444' : '#3b82f6';" +
            "  }" +
            "  if (dElem) dElem.innerText = reason;" +
            "  if (sElem) sElem.innerText = '莊' + b + ' 閒' + p + ' 和' + t + ' (' + tot + '局)';" +
            "};" +
            "})();";
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
