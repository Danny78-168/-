package com.slot.assistant;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.webkit.CookieManager;
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

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setMediaPlaybackRequiresUserGesture(false);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(webView, true);
        }

        settings.setUserAgentString("Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                injectDynamicSlotAssistant(view);
            }
        });

        // 預設開啟 ATG 虎小妹 (虎姬)
        webView.loadUrl("https://www.osc169.com/#/game/play?game_name=atg&productId=tiger-princess&device=mobile");
    }

    private void injectDynamicSlotAssistant(WebView view) {
        String js = "javascript:(function() {" +
            "if (document.getElementById('slot-assistant-hud')) return;" +
            "var hud = document.createElement('div');" +
            "hud.id = 'slot-assistant-hud';" +
            "hud.style.cssText = 'position:fixed;top:60px;right:10px;width:240px;background:#0b1120;border:1px solid #38bdf8;border-radius:10px;z-index:999999;color:#e2e8f0;font-size:12px;box-shadow:0 8px 24px rgba(0,0,0,0.85);font-family:sans-serif;touch-action:none;user-select:none;';" +
            "hud.innerHTML = " +
                "'<div id=\"hud_header\" style=\"padding:10px 12px;background:#1e293b;border-radius:10px 10px 0 0;font-weight:bold;color:#38bdf8;display:flex;justify-content:space-between;align-items:center;border-bottom:1px solid #334155;cursor:move;\">" +
                    "<span>🎰 20,000X 智慧助手</span>" +
                    "<span id=\"hud_tog\" style=\"cursor:pointer;color:#94a3b8;font-size:11px;\">[收合]</span>" +
                "</div>' +" +
                "'<div id=\"hud_content\" style=\"padding:12px;\">' +" +
                    "'<div style=\"margin-bottom:8px;\">" +
                        "<label style=\"color:#94a3b8;font-size:11px;\">切換機台：</label>" +
                        "<div style=\"display:grid;grid-template-columns:repeat(3, 1fr);gap:4px;margin-top:4px;\">" +
                            "<button id=\"nav_atg\" style=\"background:#2563eb;border:1px solid #3b82f6;color:#fff;padding:5px 0;border-radius:4px;font-size:10px;font-weight:bold;\">虎小妹</button>" +
                            "<button id=\"nav_rsg\" style=\"background:#1e293b;border:1px solid #475569;color:#94a3b8;padding:5px 0;border-radius:4px;font-size:10px;\">RSG 雷神</button>" +
                            "<button id=\"nav_avatar\" style=\"background:#1e293b;border:1px solid #475569;color:#94a3b8;padding:5px 0;border-radius:4px;font-size:10px;\">Avatar</button>" +
                        "</div>" +
                    "</div>' +" +
                    "'<hr style=\"border:0;border-top:1px solid #334155;margin:8px 0;\">' +" +
                    "'<div style=\"display:flex;justify-content:space-between;align-items:center;margin-bottom:6px;\">" +
                        "<span>基礎單注:</span>" +
                        "<input id=\"h_b\" type=\"number\" value=\"1.00\" step=\"0.10\" style=\"width:60px;background:#1e293b;border:1px solid #475569;color:#38bdf8;padding:2px 4px;border-radius:3px;text-align:right;\">" +
                    "</div>' +" +
                    "'<div style=\"margin-bottom:8px;\">" +
                        "<label style=\"color:#94a3b8;font-size:11px;\">免遊購買檔位：</label>" +
                        "<div id=\"bonus_btn_group\" style=\"display:grid;grid-template-columns:repeat(3, 1fr);gap:3px;margin-top:3px;\"></div>" +
                    "</div>' +" +
                    "'<div style=\"display:flex;justify-content:space-between;margin-bottom:4px;\">免遊成本: <b id=\"h_c\" style=\"color:#fff;\">$200.00</b></div>' +" +
                    "'<div style=\"display:flex;justify-content:space-between;margin-bottom:4px;\">理論得分(96.5%): <b id=\"h_e\" style=\"color:#4ade80;\">$193.00</b></div>' +" +
                    "'<div style=\"display:flex;justify-content:space-between;margin-bottom:4px;\">常態中位數: <b id=\"h_m\" style=\"color:#facc15;\">$84.00</b></div>' +" +
                    "'<div style=\"display:flex;justify-content:space-between;margin-bottom:6px;\">20000x 爆分率: <b style=\"color:#f43f5e;\">0.20%</b></div>' +" +
                    "'<div id=\"h_s\" style=\"font-size:10px;color:#94a3b8;background:#020617;padding:5px;border-radius:4px;text-align:center;\">即時封包監聽中...</div>' +" +
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

            // 手機觸控拖曳 (Touch Drag)
            "var header = document.getElementById('hud_header');" +
            "var isDragging = false, startX, startY, initL, initT;" +
            "header.addEventListener('touchstart', function(e) {" +
            "  isDragging = true;" +
            "  var t = e.touches[0];" +
            "  var r = hud.getBoundingClientRect();" +
            "  startX = t.clientX;" +
            "  startY = t.clientY;" +
            "  initL = r.left;" +
            "  initT = r.top;" +
            "}, { passive: true });" +
            "document.addEventListener('touchmove', function(e) {" +
            "  if (!isDragging) return;" +
            "  var t = e.touches[0];" +
            "  var nX = initL + (t.clientX - startX);" +
            "  var nY = initT + (t.clientY - startY);" +
            "  var maxW = window.innerWidth - hud.offsetWidth;" +
            "  var maxH = window.innerHeight - hud.offsetHeight;" +
            "  hud.style.left = Math.max(0, Math.min(nX, maxW)) + 'px';" +
            "  hud.style.top = Math.max(0, Math.min(nY, maxH)) + 'px';" +
            "  hud.style.right = 'auto';" +
            "}, { passive: false });" +
            "document.addEventListener('touchend', function() { isDragging = false; });" +

            // 機台配置設定
            "var gameConfigs = {" +
            "  atg: {" +
            "    url: 'https://www.osc169.com/#/game/play?game_name=atg&productId=tiger-princess&device=mobile'," +
            "    modes: [{ label: '普通 200x', mult: 200 }, { label: '特殊 500x', mult: 500 }, { label: '降臨 2000x', mult: 2000 }]" +
            "  }," +
            "  rsg: {" +
            "    url: 'https://www.osc169.com/#/game/play?game_name=rsg&productId=129&device=mobile'," +
            "    modes: [{ label: '免遊 100x', mult: 100 }]" +
            "  }," +
            "  avatar: {" +
            "    url: 'https://www.osc169.com/#/game/play?game_name=avatar&productId=140083&device=mobile'," +
            "    modes: [{ label: '獎金 80x', mult: 80 }, { label: '最大 240x', mult: 240 }]" +
            "  }" +
            "};" +

            // 判斷當前網址處於哪款機台
            "var currentGame = 'atg';" +
            "if (location.href.includes('rsg')) currentGame = 'rsg';" +
            "else if (location.href.includes('avatar')) currentGame = 'avatar';" +

            "var currentMult = gameConfigs[currentGame].modes[0].mult;" +
            "var bInput = document.getElementById('h_b');" +

            "function updateCalc() {" +
            "  var b = parseFloat(bInput.value) || 0;" +
            "  var cost = b * currentMult;" +
            "  var ev = cost * 0.965;" +
            "  var median = b * (currentMult * 0.42);" +
            "  document.getElementById('h_c').innerText = '$' + cost.toFixed(2);" +
            "  document.getElementById('h_e').innerText = '$' + ev.toFixed(2);" +
            "  document.getElementById('h_m').innerText = '$' + median.toFixed(2);" +
            "}" +

            "function renderBonusButtons() {" +
            "  var container = document.getElementById('bonus_btn_group');" +
            "  container.innerHTML = '';" +
            "  var modes = gameConfigs[currentGame].modes;" +
            "  container.style.gridTemplateColumns = 'repeat(' + modes.length + ', 1fr)';" +
            "  modes.forEach(function(m, idx) {" +
            "    var btn = document.createElement('button');" +
            "    btn.innerText = m.label;" +
            "    btn.style.cssText = 'padding:4px 0;border-radius:3px;font-size:10px;cursor:pointer;';" +
            "    if (m.mult === currentMult) {" +
            "      btn.style.background = '#3b82f6'; btn.style.border = 'none'; btn.style.color = '#fff'; btn.style.fontWeight = 'bold';" +
            "    } else {" +
            "      btn.style.background = '#1e293b'; btn.style.border = '1px solid #475569'; btn.style.color = '#94a3b8';" +
            "    }" +
            "    btn.onclick = function() {" +
            "      currentMult = m.mult;" +
            "      renderBonusButtons();" +
            "      updateCalc();" +
            "    };" +
            "    container.appendChild(btn);" +
            "  });" +
            "}" +

            "function switchGameTab(key) {" +
            "  ['atg', 'rsg', 'avatar'].forEach(function(k) {" +
            "    var btn = document.getElementById('nav_' + k);" +
            "    if (k === key) {" +
            "      btn.style.background = '#2563eb'; btn.style.color = '#fff'; btn.style.border = '1px solid #3b82f6'; btn.style.fontWeight = 'bold';" +
            "    } else {" +
            "      btn.style.background = '#1e293b'; btn.style.color = '#94a3b8'; btn.style.border = '1px solid #475569'; btn.style.fontWeight = 'normal';" +
            "    }" +
            "  });" +
            "  currentGame = key;" +
            "  currentMult = gameConfigs[key].modes[0].mult;" +
            "  renderBonusButtons();" +
            "  updateCalc();" +
            "  if (!location.href.includes(key)) {" +
            "    location.href = gameConfigs[key].url;" +
            "  }" +
            "}" +

            "document.getElementById('nav_atg').onclick = function() { switchGameTab('atg'); };" +
            "document.getElementById('nav_rsg').onclick = function() { switchGameTab('rsg'); };" +
            "document.getElementById('nav_avatar').onclick = function() { switchGameTab('avatar'); };" +

            "bInput.oninput = updateCalc;" +
            "switchGameTab(currentGame);" +
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
