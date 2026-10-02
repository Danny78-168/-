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
        settings.setSaveFormData(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(webView, true);
        }

        settings.setUserAgentString("Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36");

        // 原生換台通道
        webView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void switchGame(final String targetUrl) {
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        webView.loadUrl(targetUrl);
                        webView.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                webView.reload();
                            }
                        }, 250);
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
            "if (!window._loginBound) {" +
            "  window._loginBound = true;" +
            "  document.addEventListener('click', function(e) {" +
            "    var el = e.target;" +
            "    if (el && (el.innerText || '').indexOf('登入') !== -1) {" +
            "      syncInputs();" +
            "      var inps = document.querySelectorAll('input');" +
            "      for (var i = 0; i < inps.length; i++) inps[i].dispatchEvent(new Event('blur', { bubbles: true }));" +
            "    }" +
            "  }, true);" +
            "}" +

            "if (document.getElementById('slot-assistant-hud')) return;" +
            "var hud = document.createElement('div');" +
            "hud.id = 'slot-assistant-hud';" +
            "hud.style.cssText = 'position:fixed;top:50px;right:8px;width:195px;background:rgba(11,17,32,0.94);border:1px solid rgba(56,189,248,0.6);border-radius:10px;z-index:999999;color:#f1f5f9;font-size:11px;box-shadow:0 8px 24px rgba(0,0,0,0.85);font-family:sans-serif;user-select:none;backdrop-filter:blur(6px);';" +
            "hud.innerHTML = " +
                "'<div id=\"hud_header\" style=\"padding:7px 10px;background:#1e293b;border-radius:10px 10px 0 0;font-weight:bold;color:#38bdf8;display:flex;justify-content:space-between;align-items:center;border-bottom:1px solid #334155;cursor:move;touch-action:none;\">" +
                    "<span>⚡ 引路人</span>" +
                    "<div style=\"display:flex;gap:3px;align-items:center;\">" +
                        "<button id=\"tab_bac\" style=\"background:#2563eb;border:1px solid #3b82f6;color:#fff;padding:1px 5px;border-radius:3px;font-size:10px;cursor:pointer;\">百家</button>" +
                        "<button id=\"tab_slt\" style=\"background:#0f172a;border:1px solid #475569;color:#94a3b8;padding:1px 5px;border-radius:3px;font-size:10px;cursor:pointer;\">老虎</button>" +
                        "<span id=\"hud_tog\" style=\"cursor:pointer;color:#94a3b8;font-size:10px;margin-left:2px;\">[收]</span>" +
                    "</div>" +
                "</div>' +" +
                "'<div id=\"hud_content\" style=\"padding:10px;\">' +" +
                    "'<div id=\"p_bac\">' +" +
                        "'<div style=\"display:grid;grid-template-columns:1fr 1fr;gap:4px;margin-bottom:8px;\">" +
                            "<button id=\"nav_mt\" style=\"background:#0f172a;border:1px solid #38bdf8;color:#38bdf8;padding:3px 0;border-radius:3px;font-size:10px;font-weight:bold;cursor:pointer;\">MT 百家</button>" +
                            "<button id=\"nav_dg\" style=\"background:#0f172a;border:1px solid #475569;color:#e2e8f0;padding:3px 0;border-radius:3px;font-size:10px;cursor:pointer;\">DG 百家</button>" +
                        "</div>' +" +
                        "'<div style=\"background:rgba(15,23,42,0.85);border:1px solid #3b82f6;border-radius:6px;padding:8px 4px;text-align:center;margin-bottom:6px;\">" +
                            "<div style=\"font-size:10px;color:#94a3b8;\">🎯 下一手 AI 推薦</div>" +
                            "<div id=\"ai_pick_target\" style=\"font-size:18px;font-weight:900;color:#ef4444;margin:2px 0;\">分析中...</div>" +
                            "<div id=\"ai_pick_desc\" style=\"font-size:10px;color:#38bdf8;\">正在感應牌桌路單</div>" +
                        "</div>' +" +
                        "'<div style=\"display:flex;justify-content:space-between;font-size:10px;color:#94a3b8;\">" +
                            "<span id=\"hud_score\">莊0 閒0 和0 (0局)</span>" +
                            "<span id=\"hud_sync_dot\" style=\"color:#4ade80;\">● 連線</span>" +
                        "</div>' +" +
                    "'</div>' +" +
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

            "function navTo(url) {" +
            "  if (window.AndroidBridge && window.AndroidBridge.switchGame) {" +
            "    window.AndroidBridge.switchGame(url);" +
            "  } else {" +
            "    location.href = url;" +
            "  }" +
            "}" +
            "document.getElementById('nav_mt').onclick = function(e) { e.stopPropagation(); navTo('https://www.osc169.com/#/game/play?game_name=meta_all&game_type=3&device=mobile'); };" +
            "document.getElementById('nav_dg').onclick = function(e) { e.stopPropagation(); navTo('https://www.osc169.com/#/game/play?game_name=dg&game_type=3&device=mobile'); };" +
            "document.getElementById('nav_atg').onclick = function(e) { e.stopPropagation(); navTo('https://www.osc169.com/#/game/play?game_name=atg&productId=tiger-princess&device=mobile'); };" +
            "document.getElementById('nav_rsg').onclick = function(e) { e.stopPropagation(); navTo('https://www.osc169.com/#/game/play?game_name=rsg&productId=129&device=mobile'); };" +
            "document.getElementById('nav_ava').onclick = function(e) { e.stopPropagation(); navTo('https://www.osc169.com/#/game/play?game_name=avatar&productId=140083&device=mobile'); };" +

            "var isSlot = location.href.indexOf('tiger-princess') !== -1 || location.href.indexOf('productId=129') !== -1 || location.href.indexOf('avatar') !== -1;" +
            "if (isSlot) setTab('slt'); else setTab('bac');" +

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

            "var lastB = -1, lastP = -1, lastT = -1, lastTot = -1;" +
            "function scanGameData() {" +
            "  try {" +
            "    var active = document.activeElement;" +
            "    if (active && active.tagName === 'INPUT') return;" +
            "    var raw = '';" +
            "    if (document.body) raw = document.body.innerText || '';" +
            "    try {" +
            "      var ifrs = document.getElementsByTagName('iframe');" +
            "      for (var k = 0; k < ifrs.length; k++) {" +
            "        try {" +
            "          var d = ifrs[k].contentDocument || ifrs[k].contentWindow.document;" +
            "          if (d && d.body) raw += ' ' + d.body.innerText;" +
            "        } catch(e) {}" +
            "      }" +
            "    } catch(e) {}" +
            "    if (!raw) return;" +
            "    var txt = raw.replace(/\\d+\\s*:\\s*[\\d.]+/g, '');" +
            "    var b = -1, p = -1, t = 0, tot = -1;" +
            "    var mTable = txt.match(/莊\\s*(\\d+)[\\s\\S]*?閒\\s*(\\d+)[\\s\\S]*?和\\s*(\\d+)[\\s\\S]*?總數\\s*(\\d+)/);" +
            "    if (mTable) {" +
            "      b = parseInt(mTable[1], 10); p = parseInt(mTable[2], 10); t = parseInt(mTable[3], 10); tot = parseInt(mTable[4], 10);" +
            "    } else {" +
            "      var mLobby = txt.match(/局數\\s*(\\d+)[\\s\\S]*?莊\\s*(\\d+)[\\s\\S]*?閒\\s*(\\d+)[\\s\\S]*?和\\s*(\\d+)/);" +
            "      if (mLobby) {" +
            "        tot = parseInt(mLobby[1], 10); b = parseInt(mLobby[2], 10); p = parseInt(mLobby[3], 10); t = parseInt(mLobby[4], 10);" +
            "      } else {" +
            "        var mFlex = txt.match(/莊\\s*(\\d+)[\\s\\S]*?閒\\s*(\\d+)[\\s\\S]*?和\\s*(\\d+)/);" +
            "        if (mFlex) {" +
            "          b = parseInt(mFlex[1], 10); p = parseInt(mFlex[2], 10); t = parseInt(mFlex[3], 10); tot = b + p + t;" +
            "        }" +
            "      }" +
            "    }" +
            "    if (b !== -1 && p !== -1 && (b + p + t) > 0) {" +
            "      if (tot <= 0) tot = b + p + t;" +
            "      if (b !== lastB || p !== lastP || t !== lastT || tot !== lastTot) {" +
            "        lastB = b; lastP = p; lastT = t; lastTot = tot;" +
            "        var delta = p - b;" +
            "        var pick = '莊', conf = 60, reason = '';" +
            "        if (delta >= 3) {" +
            "          pick = '莊'; conf = Math.min(88, 62 + delta * 4); reason = '閒領先 ' + delta + ' 局 (均值修正)';" +
            "        } else if (delta <= -3) {" +
            "          pick = '閒'; conf = Math.min(88, 62 + Math.abs(delta) * 4); reason = '莊領先 ' + Math.abs(delta) + ' 局 (均值修正)';" +
            "        } else {" +
            "          pick = (b >= p) ? '莊' : '閒'; conf = 58 + (tot % 6); reason = '動量趨勢推薦';" +
            "        }" +
            "        var tElem = document.getElementById('ai_pick_target');" +
            "        var dElem = document.getElementById('ai_pick_desc');" +
            "        var sElem = document.getElementById('hud_score');" +
            "        if (tElem) {" +
            "          tElem.innerText = '【' + pick + '】 ' + conf + '%';" +
            "          tElem.style.color = (pick === '莊') ? '#ef4444' : '#3b82f6';" +
            "        }" +
            "        if (dElem) dElem.innerText = reason;" +
            "        if (sElem) sElem.innerText = '莊' + b + ' 閒' + p + ' 和' + t + ' (' + tot + '局)';" +
            "      }" +
            "    }" +
            "  } catch(e) {}" +
            "}" +
            "setInterval(scanGameData, 600);" +
            "scanGameData();" +
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
