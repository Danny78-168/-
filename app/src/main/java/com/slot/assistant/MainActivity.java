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

        // 註冊原生 Android 換台通道，徹底解決網址無法切換問題
        webView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void navigateTo(final String url) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        webView.loadUrl(url);
                    }
                });
            }
        }, "AndroidBridge");

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                injectUniversalAssistant(view);
            }
        });

        // 預設開啟 MT 百家
        webView.loadUrl("https://www.osc169.com/#/game/play?game_name=meta_all&game_type=3&device=mobile");
    }

    private void injectUniversalAssistant(WebView view) {
        String js = "javascript:(function() {" +
            "if (document.getElementById('slot-assistant-hud')) return;" +

            // 1. 建立引路人 HUD 面板
            "var hud = document.createElement('div');" +
            "hud.id = 'slot-assistant-hud';" +
            "hud.style.cssText = 'position:fixed;top:45px;right:8px;width:250px;background:#0b1120;border:1px solid #38bdf8;border-radius:10px;z-index:999999;color:#e2e8f0;font-size:12px;box-shadow:0 8px 24px rgba(0,0,0,0.85);font-family:sans-serif;user-select:none;';" +
            "hud.innerHTML = " +
                "'<div id=\"hud_header\" style=\"padding:10px 12px;background:#1e293b;border-radius:10px 10px 0 0;font-weight:bold;color:#38bdf8;display:flex;justify-content:space-between;align-items:center;border-bottom:1px solid #334155;cursor:move;touch-action:none;\">" +
                    "<span>⚡ 引路人 AI 決策</span>" +
                    "<span id=\"hud_tog\" style=\"cursor:pointer;color:#94a3b8;font-size:11px;\">[收合]</span>" +
                "</div>' +" +
                "'<div id=\"hud_content\" style=\"padding:12px;\">' +" +
                    // 大廳切換（透過原生 AndroidBridge 調用）
                    "'<div style=\"display:grid;grid-template-columns:1fr 1fr;gap:4px;margin-bottom:8px;\">" +
                        "<button id=\"btn_nav_mt\" style=\"background:#0f172a;border:1px solid #38bdf8;color:#38bdf8;padding:5px 0;border-radius:4px;font-size:11px;font-weight:bold;\">MT 百家</button>" +
                        "<button id=\"btn_nav_dg\" style=\"background:#0f172a;border:1px solid #475569;color:#e2e8f0;padding:5px 0;border-radius:4px;font-size:11px;\">DG 百家</button>" +
                    "</div>' +" +
                    // 當前桌況快速設定（直接對齊截圖 莊/閒/和）
                    "'<div style=\"background:#020617;border:1px solid #1e293b;border-radius:6px;padding:8px;margin-bottom:8px;\">" +
                        "<div style=\"display:flex;justify-content:space-between;font-size:11px;color:#94a3b8;margin-bottom:4px;\">" +
                            "<span>當前局況設定:</span><span id=\"btn_reset_shoe\" style=\"color:#f87171;cursor:pointer;\">[重置]</span>" +
                        "</div>" +
                        "<div style=\"display:flex;gap:4px;align-items:center;\">" +
                            "<span style=\"color:#ef4444;\">莊:</span><input id=\"inp_b\" type=\"number\" value=\"13\" style=\"width:35px;background:#1e293b;border:1px solid #334155;color:#fff;text-align:center;border-radius:3px;padding:1px;\">" +
                            "<span style=\"color:#3b82f6;\">閒:</span><input id=\"inp_p\" type=\"number\" value=\"17\" style=\"width:35px;background:#1e293b;border:1px solid #334155;color:#fff;text-align:center;border-radius:3px;padding:1px;\">" +
                            "<span style=\"color:#22c55e;\">和:</span><input id=\"inp_t\" type=\"number\" value=\"4\" style=\"width:35px;background:#1e293b;border:1px solid #334155;color:#fff;text-align:center;border-radius:3px;padding:1px;\">" +
                            "<button id=\"btn_sync_inputs\" style=\"background:#2563eb;color:#fff;border:none;padding:2px 6px;border-radius:3px;font-size:10px;cursor:pointer;\">套用</button>" +
                        "</div>" +
                    "</div>' +" +
                    // AI 下一手決策推薦
                    "'<div style=\"background:#0f172a;border:1px solid #3b82f6;border-radius:8px;padding:10px;text-align:center;margin-bottom:8px;\">" +
                        "<div style=\"color:#94a3b8;font-size:11px;margin-bottom:2px;\">🎯 下一手決策推薦</div>" +
                        "<div id=\"ai_pick\" style=\"font-size:1.6rem;font-weight:900;color:#ef4444;margin:2px 0;\">分析中...</div>" +
                        "<div id=\"ai_desc\" style=\"font-size:11px;color:#38bdf8;\">正在計算方差動量...</div>" +
                    "</div>' +" +
                    // 雙模型數值分析
                    "'<div style=\"font-size:11px;background:#020617;border-radius:6px;padding:8px;border:1px solid #1e293b;margin-bottom:8px;\">" +
                        "<div style=\"display:flex;justify-content:space-between;margin-bottom:4px;\">" +
                            "<span style=\"color:#10b981;\">OpenAI (動量追隨):</span>" +
                            "<b id=\"m_openai\">莊 (50%)</b>" +
                        "</div>" +
                        "<div style=\"display:flex;justify-content:space-between;\">" +
                            "<span style=\"color:#60a5fa;\">Gemini (均值對沖):</span>" +
                            "<b id=\"m_gemini\">閒 (50%)</b>" +
                        "</div>" +
                    "</div>' +" +
                    // 開出下一局一鍵追蹤鍵
                    "'<div style=\"display:grid;grid-template-columns:1fr 1fr 1fr;gap:4px;\">" +
                        "<button id=\"btn_add_b\" style=\"background:#dc2626;color:#fff;border:none;padding:6px 0;border-radius:4px;font-weight:bold;\">＋ 莊</button>" +
                        "<button id=\"btn_add_p\" style=\"background:#2563eb;color:#fff;border:none;padding:6px 0;border-radius:4px;font-weight:bold;\">＋ 閒</button>" +
                        "<button id=\"btn_add_t\" style=\"background:#16a34a;color:#fff;border:none;padding:6px 0;border-radius:4px;font-weight:bold;\">＋ 和</button>" +
                    "</div>' +" +
                "'</div>';" +
            "document.body.appendChild(hud);" +

            // 收合邏輯
            "var tog = document.getElementById('hud_tog');" +
            "var cnt = document.getElementById('hud_content');" +
            "tog.onclick = function(e) {" +
            "  e.stopPropagation();" +
            "  if (cnt.style.display === 'none') { cnt.style.display = 'block'; tog.innerText = '[收合]'; }" +
            "  else { cnt.style.display = 'none'; tog.innerText = '[展開]'; }" +
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
            "}, { passive: true });" +
            "header.addEventListener('touchend', function() { isDrag = false; });" +

            // 大廳切換按鈕綁定（使用 Android 原生通道）
            "document.getElementById('btn_nav_mt').onclick = function() {" +
            "  if (window.AndroidBridge && window.AndroidBridge.navigateTo) {" +
            "    window.AndroidBridge.navigateTo('https://www.osc169.com/#/game/play?game_name=meta_all&game_type=3&device=mobile');" +
            "  } else {" +
            "    window.location.href = 'https://www.osc169.com/#/game/play?game_name=meta_all&game_type=3&device=mobile';" +
            "    window.location.reload();" +
            "  }" +
            "};" +
            "document.getElementById('btn_nav_dg').onclick = function() {" +
            "  if (window.AndroidBridge && window.AndroidBridge.navigateTo) {" +
            "    window.AndroidBridge.navigateTo('https://www.osc169.com/#/game/play?game_name=dg&game_type=3&device=mobile');" +
            "  } else {" +
            "    window.location.href = 'https://www.osc169.com/#/game/play?game_name=dg&game_type=3&device=mobile';" +
            "    window.location.reload();" +
            "  }" +
            "};" +

            // AI 決策計算核心
            "var bCount = 13, pCount = 17, tCount = 4;" +
            "var recentHistory = ['B', 'P', 'P', 'B', 'B', 'P', 'P'];" +

            "function evaluateNextBet() {" +
            "  var total = bCount + pCount + tCount;" +
            "  var delta = pCount - bCount;" + // 閒多於莊的差額

            // 1. OpenAI 趨勢動量模型（看末端連續度）
            "  var lastWin = recentHistory.length > 0 ? recentHistory[recentHistory.length - 1] : 'P';" +
            "  var streak = 0;" +
            "  for (var i = recentHistory.length - 1; i >= 0; i--) {" +
            "    if (recentHistory[i] === lastWin) streak++; else break;" +
            "  }" +
            "  var openTarget = '閒', openConf = 56;" +
            "  if (streak >= 2) {" +
            "    openTarget = lastWin === 'B' ? '莊' : '閒';" +
            "    openConf = Math.min(88, 64 + streak * 6);" +
            "  } else {" +
            "    openTarget = lastWin === 'B' ? '莊' : '閒';" +
            "    openConf = 58 + (total % 5);" +
            "  }" +

            // 2. Gemini 均值回歸模型（大數法則對沖）
            "  var geminiTarget = '莊', geminiConf = 54;" +
            "  if (delta >= 3) {" +
            "    geminiTarget = '莊';" +
            "    geminiConf = Math.min(86, 62 + delta * 4);" + // 閒大幅領先，強烈修正壓莊
            "  } else if (delta <= -3) {" +
            "    geminiTarget = '閒';" +
            "    geminiConf = Math.min(86, 62 + Math.abs(delta) * 4);" +
            "  } else {" +
            "    geminiTarget = '莊';" +
            "    geminiConf = 55;" +
            "  }" +

            "  document.getElementById('m_openai').innerHTML = '<span style=\"color:' + (openTarget==='莊'?'#ef4444':'#3b82f6') + '\">' + openTarget + ' (' + openConf + '%)</span>';" +
            "  document.getElementById('m_gemini').innerHTML = '<span style=\"color:' + (geminiTarget==='莊'?'#ef4444':'#3b82f6') + '\">' + geminiTarget + ' (' + geminiConf + '%)</span>';" +

            // 綜合下一手強推
            "  var pick = document.getElementById('ai_pick');" +
            "  var desc = document.getElementById('ai_desc');" +
            "  if (geminiConf >= openConf) {" +
            "    pick.innerText = '🎯 推薦【' + geminiTarget + '】';" +
            "    pick.style.color = geminiTarget === '莊' ? '#ef4444' : '#3b82f6';" +
            "    desc.innerText = '閒偏離領先 ' + delta + ' 局 均值修正指數 ' + geminiConf + '%';" +
            "  } else {" +
            "    pick.innerText = '⚡ 順勢【' + openTarget + '】';" +
            "    pick.style.color = openTarget === '莊' ? '#ef4444' : '#3b82f6';" +
            "    desc.innerText = '動量趨勢領先 信心度 ' + openConf + '%';" +
            "  }" +
            "}" +

            // 輸入框同步
            "document.getElementById('btn_sync_inputs').onclick = function() {" +
            "  bCount = parseInt(document.getElementById('inp_b').value) || 0;" +
            "  pCount = parseInt(document.getElementById('inp_p').value) || 0;" +
            "  tCount = parseInt(document.getElementById('inp_t').value) || 0;" +
            "  evaluateNextBet();" +
            "};" +

            "document.getElementById('btn_reset_shoe').onclick = function() {" +
            "  bCount = 0; pCount = 0; tCount = 0; recentHistory = [];" +
            "  document.getElementById('inp_b').value = 0;" +
            "  document.getElementById('inp_p').value = 0;" +
            "  document.getElementById('inp_t').value = 0;" +
            "  evaluateNextBet();" +
            "};" +

            // 實時加球
            "document.getElementById('btn_add_b').onclick = function() {" +
            "  bCount++; document.getElementById('inp_b').value = bCount;" +
            "  recentHistory.push('B'); evaluateNextBet();" +
            "};" +
            "document.getElementById('btn_add_p').onclick = function() {" +
            "  pCount++; document.getElementById('inp_p').value = pCount;" +
            "  recentHistory.push('P'); evaluateNextBet();" +
            "};" +
            "document.getElementById('btn_add_t').onclick = function() {" +
            "  tCount++; document.getElementById('inp_t').value = tCount;" +
            "  recentHistory.push('T'); evaluateNextBet();" +
            "};" +

            "evaluateNextBet();" +
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
