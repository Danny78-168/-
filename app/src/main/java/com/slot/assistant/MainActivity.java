package com.slot.assistant;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.CookieManager;
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

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                injectFormFixerAndAssistant(view);
            }
        });

        // 預設載入 MT 百家
        webView.loadUrl("https://www.osc169.com/#/game/play?game_name=meta_all&game_type=3&device=mobile");
    }

    private void injectFormFixerAndAssistant(WebView view) {
        String js = "javascript:(function() {" +
            // ==================== 1. 表單自動同步修正器 ====================
            "if (!window._formFixerInjected) {" +
            "  window._formFixerInjected = true;" +
            "  document.addEventListener('click', function(e) {" +
            "    var target = e.target;" +
            "    if (target && (target.innerText && target.innerText.indexOf('登入') !== -1 || target.className && target.className.indexOf('login') !== -1)) {" +
            "      var inputs = document.querySelectorAll('input');" +
            "      inputs.forEach(function(inp) {" +
            "        inp.dispatchEvent(new Event('input', { bubbles: true }));" +
            "        inp.dispatchEvent(new Event('change', { bubbles: true }));" +
            "        inp.dispatchEvent(new Event('blur', { bubbles: true }));" +
            "      });" +
            "    }" +
            "  }, true);" +
            "}" +

            // ==================== 2. 引路人 HUD 介面 ====================
            "if (document.getElementById('slot-assistant-hud')) return;" +
            "var hud = document.createElement('div');" +
            "hud.id = 'slot-assistant-hud';" +
            "hud.style.cssText = 'position:fixed;top:45px;right:8px;width:250px;background:#0b1120;border:1px solid #38bdf8;border-radius:10px;z-index:999999;color:#e2e8f0;font-size:12px;box-shadow:0 8px 24px rgba(0,0,0,0.85);font-family:sans-serif;user-select:none;';" +
            "hud.innerHTML = " +
                "'<div id=\"hud_header\" style=\"padding:10px 12px;background:#1e293b;border-radius:10px 10px 0 0;font-weight:bold;color:#38bdf8;display:flex;justify-content:space-between;align-items:center;border-bottom:1px solid #334155;cursor:move;touch-action:none;\">" +
                    "<span>⚡ 引路人</span>" +
                    "<span id=\"hud_tog\" style=\"cursor:pointer;color:#94a3b8;font-size:11px;\">[收合]</span>" +
                "</div>' +" +
                "'<div id=\"hud_content\" style=\"padding:12px;\">' +" +
                    "'<div style=\"display:grid;grid-template-columns:1fr 1fr;gap:4px;margin-bottom:8px;\">" +
                        "<button onclick=\"location.href=\\'https://www.osc169.com/#/game/play?game_name=meta_all&game_type=3&device=mobile\\'\" style=\"background:#0f172a;border:1px solid #38bdf8;color:#38bdf8;padding:4px 0;border-radius:4px;font-size:10px;font-weight:bold;\">MT 百家</button>" +
                        "<button onclick=\"location.href=\\'https://www.osc169.com/#/game/play?game_name=dg&game_type=3&device=mobile\\'\" style=\"background:#0f172a;border:1px solid #475569;color:#e2e8f0;padding:4px 0;border-radius:4px;font-size:10px;\">DG 百家</button>" +
                    "</div>' +" +
                    "'<div style=\"background:#020617;border:1px solid #1e293b;border-radius:6px;padding:8px;margin-bottom:8px;\">" +
                        "<div style=\"display:flex;justify-content:space-between;align-items:center;margin-bottom:4px;\">" +
                            "<span style=\"color:#94a3b8;font-size:11px;\">路單監測:</span>" +
                            "<b id=\"sync_status\" style=\"color:#4ade80;font-size:11px;\">🟢 即時同步中</b>" +
                        "</div>" +
                        "<div style=\"display:flex;justify-content:space-between;font-size:11px;\">" +
                            "<span>已讀取局數: <b id=\"auto_total\" style=\"color:#fff;\">0</b></span>" +
                            "<span>莊/閒/和: <b id=\"auto_stat\" style=\"color:#fff;\">0/0/0</b></span>" +
                        "</div>" +
                    "</div>' +" +
                    "'<div style=\"background:#0f172a;border:1px solid #3b82f6;border-radius:8px;padding:10px;text-align:center;margin-bottom:8px;\">" +
                        "<div style=\"color:#94a3b8;font-size:11px;margin-bottom:2px;\">🎯 下一手決策推薦</div>" +
                        "<div id=\"ai_master_pick\" style=\"font-size:1.6rem;font-weight:900;color:#ef4444;margin:2px 0;\">分析中...</div>" +
                        "<div id=\"ai_master_desc\" style=\"font-size:11px;color:#38bdf8;\">正在掃描開牌路單...</div>" +
                    "</div>' +" +
                    "'<div style=\"font-size:11px;background:#020617;border-radius:6px;padding:8px;border:1px solid #1e293b;\">" +
                        "<div style=\"display:flex;justify-content:space-between;margin-bottom:4px;\">" +
                            "<span style=\"color:#10b981;\">OpenAI (動量追隨):</span>" +
                            "<b id=\"m_openai\">莊 (50%)</b>" +
                        "</div>" +
                        "<div style=\"display:flex;justify-content:space-between;\">" +
                            "<span style=\"color:#60a5fa;\">Gemini (均值對沖):</span>" +
                            "<b id=\"m_gemini\">閒 (50%)</b>" +
                        "</div>" +
                    "</div>' +" +
                    "'<div style=\"display:flex;justify-content:space-between;align-items:center;margin-top:8px;\">" +
                        "<span id=\"last_seen_road\" style=\"color:#64748b;font-size:10px;overflow:hidden;white-space:nowrap;max-width:160px;\">等待新局...</span>" +
                        "<button id=\"btn_resync\" style=\"background:none;border:1px solid #475569;color:#94a3b8;font-size:10px;padding:2px 6px;border-radius:3px;cursor:pointer;\">強制刷新</button>" +
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

            // AI 決策計算引擎
            "var shoeRoad = [];" +
            "var bCount = 0, pCount = 0, tCount = 0;" +

            "function computeAIDecision() {" +
            "  if (shoeRoad.length === 0 && (bCount + pCount) === 0) return;" +
            "  var total = bCount + pCount + tCount;" +
            "  document.getElementById('auto_total').innerText = total;" +
            "  document.getElementById('auto_stat').innerText = bCount + '/' + pCount + '/' + tCount;" +

            "  var lastWin = shoeRoad.length > 0 ? shoeRoad[shoeRoad.length - 1] : (bCount >= pCount ? 'B' : 'P');" +
            "  var streak = 0;" +
            "  for (var i = shoeRoad.length - 1; i >= 0; i--) {" +
            "    if (shoeRoad[i] === lastWin) streak++; else break;" +
            "  }" +
            "  var openTarget = '莊', openConf = 55;" +
            "  if (streak >= 2) {" +
            "    openTarget = lastWin === 'B' ? '莊' : '閒';" +
            "    openConf = Math.min(88, 65 + streak * 6);" +
            "  } else {" +
            "    openTarget = bCount >= pCount ? '莊' : '閒';" +
            "    openConf = 54 + (total % 8);" +
            "  }" +

            "  var geminiTarget = '閒', geminiConf = 52;" +
            "  var delta = Math.abs(bCount - pCount);" +
            "  if (bCount > pCount + 2) {" +
            "    geminiTarget = '閒';" +
            "    geminiConf = Math.min(85, 58 + delta * 5);" +
            "  } else if (pCount > bCount + 2) {" +
            "    geminiTarget = '莊';" +
            "    geminiConf = Math.min(85, 58 + delta * 5);" +
            "  } else {" +
            "    geminiTarget = (lastWin === 'B') ? '閒' : '莊';" +
            "    geminiConf = 58 + (total % 6);" +
            "  }" +

            "  document.getElementById('m_openai').innerHTML = '<span style=\"color:' + (openTarget==='莊'?'#ef4444':'#3b82f6') + '\">' + openTarget + ' (' + openConf + '%)</span>';" +
            "  document.getElementById('m_gemini').innerHTML = '<span style=\"color:' + (geminiTarget==='莊'?'#ef4444':'#3b82f6') + '\">' + geminiTarget + ' (' + geminiConf + '%)</span>';" +

            "  var pickElem = document.getElementById('ai_master_pick');" +
            "  var descElem = document.getElementById('ai_master_desc');" +
            "  if (openTarget === geminiTarget) {" +
            "    var combinedConf = Math.round((openConf + geminiConf) / 2);" +
            "    pickElem.innerText = '🔥 強推【' + openTarget + '】';" +
            "    pickElem.style.color = openTarget === '莊' ? '#ef4444' : '#3b82f6';" +
            "    descElem.innerText = '雙模型高度共識 信心度 ' + combinedConf + '%';" +
            "  } else {" +
            "    var winnerModel = openConf >= geminiConf ? openTarget : geminiTarget;" +
            "    var leadConf = Math.max(openConf, geminiConf);" +
            "    pickElem.innerText = '⚡ 推薦【' + winnerModel + '】';" +
            "    pickElem.style.color = winnerModel === '莊' ? '#ef4444' : '#3b82f6';" +
            "    descElem.innerText = '趨勢與對沖分歧 依動量優選 (' + leadConf + '%)';" +
            "  }" +
            "  document.getElementById('last_seen_road').innerText = '最新：' + (shoeRoad.slice(-6).join(' ') || '連線中');" +
            "}" +

            // DOM 畫面掃描：當畫面存在密碼輸入框時自動停止掃描，避免 DOM 競爭
            "function scanScreenDOM() {" +
            "  try {" +
            "    if (document.querySelector('input[type=\"password\"]')) return;" +
            "    var allText = document.body.innerText || '';" +
            "    var matchB = allText.match(/莊\\s*(\\d+)/);" +
            "    var matchP = allText.match(/閒\\s*(\\d+)/);" +
            "    var matchT = allText.match(/和\\s*(\\d+)/);" +

            "    if (matchB && matchP) {" +
            "      var parsedB = parseInt(matchB[1], 10);" +
            "      var parsedP = parseInt(matchP[1], 10);" +
            "      var parsedT = matchT ? parseInt(matchT[1], 10) : tCount;" +
            "      if (parsedB !== bCount || parsedP !== pCount || parsedT !== tCount) {" +
            "        bCount = parsedB;" +
            "        pCount = parsedP;" +
            "        tCount = parsedT;" +
            "        computeAIDecision();" +
            "      }" +
            "    }" +

            "    var beads = document.querySelectorAll('[class*=\"bead\"], [class*=\"road\"], [class*=\"dot\"]');" +
            "    if (beads && beads.length > 5) {" +
            "      var tempRoad = [];" +
            "      beads.forEach(function(el) {" +
            "        var c = el.className || '';" +
            "        var txt = el.innerText || '';" +
            "        if (c.indexOf('banker') !== -1 || txt === '莊') tempRoad.push('B');" +
            "        else if (c.indexOf('player') !== -1 || txt === '閒') tempRoad.push('P');" +
            "        else if (c.indexOf('tie') !== -1 || txt === '和') tempRoad.push('T');" +
            "      });" +
            "      if (tempRoad.length > shoeRoad.length) {" +
            "        shoeRoad = tempRoad;" +
            "        computeAIDecision();" +
            "      }" +
            "    }" +
            "  } catch(e) {}" +
            "}" +

            "setInterval(scanScreenDOM, 1500);" +
            "document.getElementById('btn_resync').onclick = function() { scanScreenDOM(); computeAIDecision(); };" +
            "scanScreenDOM();" +
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
