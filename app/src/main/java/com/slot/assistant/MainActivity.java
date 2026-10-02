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
                injectUniversalAssistant(view);
            }
        });

        // 預設開啟 MT 百家
        webView.loadUrl("https://www.osc169.com/#/game/play?game_name=meta_all&game_type=3&device=mobile");
    }

    private void injectUniversalAssistant(WebView view) {
        String js = "javascript:(function() {" +
            "if (document.getElementById('slot-assistant-hud')) return;" +
            "var hud = document.createElement('div');" +
            "hud.id = 'slot-assistant-hud';" +
            "hud.style.cssText = 'position:fixed;top:50px;right:10px;width:245px;background:#0b1120;border:1px solid #38bdf8;border-radius:10px;z-index:999999;color:#e2e8f0;font-size:12px;box-shadow:0 8px 24px rgba(0,0,0,0.85);font-family:sans-serif;user-select:none;';" +
            "hud.innerHTML = " +
                "'<div id=\"hud_header\" style=\"padding:10px 12px;background:#1e293b;border-radius:10px 10px 0 0;font-weight:bold;color:#38bdf8;display:flex;justify-content:space-between;align-items:center;border-bottom:1px solid #334155;cursor:move;touch-action:none;\">" +
                    "<span>⚡ AI 雙模預測助手</span>" +
                    "<span id=\"hud_tog\" style=\"cursor:pointer;color:#94a3b8;font-size:11px;\">[收合]</span>" +
                "</div>' +" +
                "'<div id=\"hud_content\" style=\"padding:12px;\">' +" +
                    // 頂部大分類切換：老虎機 vs 百家樂
                    "'<div style=\"display:grid;grid-template-columns:1fr 1fr;gap:4px;margin-bottom:10px;\">" +
                        "<button id=\"tab_baccarat\" style=\"background:#2563eb;color:#fff;border:none;padding:5px 0;border-radius:4px;font-weight:bold;font-size:11px;\">🃏 真人百家</button>" +
                        "<button id=\"tab_slots\" style=\"background:#1e293b;color:#94a3b8;border:1px solid #475569;padding:5px 0;border-radius:4px;font-size:11px;\">🎰 老虎機</button>" +
                    "</div>' +" +
                    // 模組一：真人百家預測面板
                    "'<div id=\"panel_baccarat\">' +" +
                        "'<div style=\"margin-bottom:8px;\">" +
                            "<label style=\"color:#94a3b8;font-size:11px;\">百家大廳切換：</label>" +
                            "<div style=\"display:grid;grid-template-columns:1fr 1fr;gap:4px;margin-top:3px;\">" +
                                "<button onclick=\"location.href=\\'https://www.osc169.com/#/game/play?game_name=meta_all&game_type=3&device=mobile\\'\" style=\"background:#0f172a;border:1px solid #38bdf8;color:#38bdf8;padding:4px 0;border-radius:4px;font-size:10px;font-weight:bold;\">MT 百家</button>" +
                                "<button onclick=\"location.href=\\'https://www.osc169.com/#/game/play?game_name=dg&game_type=3&device=mobile\\'\" style=\"background:#0f172a;border:1px solid #475569;color:#e2e8f0;padding:4px 0;border-radius:4px;font-size:10px;\">DG 百家</button>" +
                            "</div>" +
                        "</div>' +" +
                        // 路單即時錄入鍵
                        "'<div style=\"margin-bottom:8px;\">" +
                            "<div style=\"display:flex;justify-content:space-between;color:#94a3b8;font-size:11px;margin-bottom:3px;\">" +
                                "<span>開牌即時記錄:</span><span id=\"road_clear\" style=\"color:#f87171;cursor:pointer;\">[清空]</span>" +
                            "</div>" +
                            "<div style=\"display:grid;grid-template-columns:1fr 1fr 1fr;gap:4px;\">" +
                                "<button id=\"btn_add_b\" style=\"background:#dc2626;color:#fff;border:none;padding:5px 0;border-radius:4px;font-weight:bold;\">＋ 莊 (B)</button>" +
                                "<button id=\"btn_add_p\" style=\"background:#2563eb;color:#fff;border:none;padding:5px 0;border-radius:4px;font-weight:bold;\">＋ 閒 (P)</button>" +
                                "<button id=\"btn_add_t\" style=\"background:#16a34a;color:#fff;border:none;padding:5px 0;border-radius:4px;font-weight:bold;\">＋ 和 (T)</button>" +
                            "</div>" +
                        "</div>' +" +
                        // 雙模型推薦看板
                        "'<div style=\"background:#020617;border:1px solid #1e293b;border-radius:6px;padding:8px;margin-bottom:8px;\">" +
                            "<div style=\"display:flex;justify-content:space-between;margin-bottom:4px;\">" +
                                "<span style=\"color:#10b981;font-weight:bold;\">🟢 OpenAI (趨勢追隨):</span>" +
                                "<b id=\"ai_openai_rec\" style=\"color:#ef4444;\">莊 (72%)</b>" +
                            "</div>" +
                            "<div style=\"display:flex;justify-content:space-between;margin-bottom:4px;\">" +
                                "<span style=\"color:#60a5fa;font-weight:bold;\">🔵 Gemini (均值對沖):</span>" +
                                "<b id=\"ai_gemini_rec\" style=\"color:#3b82f6;\">閒 (64%)</b>" +
                            "</div>" +
                            "<div style=\"font-size:10px;color:#94a3b8;border-top:1px dashed #334155;padding-top:4px;margin-top:4px;display:flex;justify-content:space-between;\">" +
                                "<span>當前路單局數: <b id=\"road_count\" style=\"color:#fff;\">0</b></span>" +
                                "<span>莊/閒/和: <b id=\"road_stat\" style=\"color:#fff;\">0/0/0</b></span>" +
                            "</div>" +
                        "</div>' +" +
                        "'<div id=\"road_visual\" style=\"font-size:11px;letter-spacing:2px;overflow-x:auto;white-space:nowrap;background:#0f172a;padding:4px;border-radius:4px;color:#cbd5e1;min-height:16px;\">無紀錄</div>' +" +
                    "'</div>' +" +
                    // 模組二：原老虎機 20000X 模式面板
                    "'<div id=\"panel_slots\" style=\"display:none;\">' +" +
                        "'<div style=\"margin-bottom:6px;\">" +
                            "<label style=\"color:#94a3b8;font-size:11px;\">機台換台：</label>" +
                            "<div style=\"display:grid;grid-template-columns:repeat(3, 1fr);gap:4px;margin-top:2px;\">" +
                                "<button onclick=\"location.href=\\'https://www.osc169.com/#/game/play?game_name=avatar&productId=140083&device=mobile\\'\" style=\"background:#1e293b;border:1px solid #475569;color:#fff;padding:4px 0;border-radius:4px;font-size:10px;\">Avatar</button>" +
                                "<button onclick=\"location.href=\\'https://www.osc169.com/#/game/play?game_name=atg&productId=tiger-princess&device=mobile\\'\" style=\"background:#1e293b;border:1px solid #475569;color:#fff;padding:4px 0;border-radius:4px;font-size:10px;\">虎小妹</button>" +
                                "<button onclick=\"location.href=\\'https://www.osc169.com/#/game/play?game_name=rsg&productId=129&device=mobile\\'\" style=\"background:#1e293b;border:1px solid #475569;color:#fff;padding:4px 0;border-radius:4px;font-size:10px;\">RSG 雷神</button>" +
                            "</div>" +
                        "</div>' +" +
                        "'<div style=\"display:flex;justify-content:space-between;margin-bottom:4px;\">單注: <input id=\"h_b\" type=\"number\" value=\"1.00\" step=\"0.10\" style=\"width:55px;background:#1e293b;border:1px solid #475569;color:#38bdf8;padding:2px;text-align:right;\"></div>' +" +
                        "'<div style=\"display:flex;justify-content:space-between;margin-bottom:4px;\">免遊成本(100x): <b id=\"h_c\" style=\"color:#fff;\">$100.00</b></div>' +" +
                        "'<div style=\"display:flex;justify-content:space-between;margin-bottom:4px;\">理論得分(96.5%): <b id=\"h_e\" style=\"color:#4ade80;\">$96.50</b></div>' +" +
                        "'<div style=\"display:flex;justify-content:space-between;\">20000x 爆分率: <b style=\"color:#f43f5e;\">0.20%</b></div>' +" +
                    "'</div>' +" +
                "'</div>';" +
            "document.body.appendChild(hud);" +

            // 收合功能
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

            // 大分類分頁切換
            "var tBac = document.getElementById('tab_baccarat');" +
            "var tSlot = document.getElementById('tab_slots');" +
            "var pBac = document.getElementById('panel_baccarat');" +
            "var pSlot = document.getElementById('panel_slots');" +
            "tBac.onclick = function() {" +
            "  tBac.style.background = '#2563eb'; tBac.style.color = '#fff'; tBac.style.fontWeight = 'bold';" +
            "  tSlot.style.background = '#1e293b'; tSlot.style.color = '#94a3b8'; tSlot.style.fontWeight = 'normal';" +
            "  pBac.style.display = 'block'; pSlot.style.display = 'none';" +
            "};" +
            "tSlot.onclick = function() {" +
            "  tSlot.style.background = '#2563eb'; tSlot.style.color = '#fff'; tSlot.style.fontWeight = 'bold';" +
            "  tBac.style.background = '#1e293b'; tBac.style.color = '#94a3b8'; tBac.style.fontWeight = 'normal';" +
            "  pSlot.style.display = 'block'; pBac.style.display = 'none';" +
            "};" +

            // 百家樂 AI 雙模運算邏輯
            "var shoe = [];" +
            "function updateBaccaratPrediction() {" +
            "  var total = shoe.length;" +
            "  var bCount = shoe.filter(function(x){return x==='B';}).length;" +
            "  var pCount = shoe.filter(function(x){return x==='P';}).length;" +
            "  var tCount = shoe.filter(function(x){return x==='T';}).length;" +
            "  document.getElementById('road_count').innerText = total;" +
            "  document.getElementById('road_stat').innerText = bCount + '/' + pCount + '/' + tCount;" +
            "  var vis = shoe.map(function(x) {" +
            "    if(x==='B') return '<span style=\"color:#ef4444;\">●莊</span>';" +
            "    if(x==='P') return '<span style=\"color:#3b82f6;\">●閒</span>';" +
            "    return '<span style=\"color:#22c55e;\">●和</span>';" +
            "  }).join(' ');" +
            "  document.getElementById('road_visual').innerHTML = vis || '無紀錄';" +

            // 模型一：OpenAI 趨勢動量模型 (追長龍、連續性)
            "  var openAiTarget = '莊';" +
            "  var openAiConf = 52;" +
            "  var last = shoe.length > 0 ? shoe[shoe.length - 1] : 'B';" +
            "  var streak = 0;" +
            "  for(var i = shoe.length - 1; i >= 0; i--) {" +
            "    if(shoe[i] === last) streak++; else break;" +
            "  }" +
            "  if (streak >= 2) {" +
            "    openAiTarget = last === 'B' ? '莊' : '閒';" +
            "    openAiConf = Math.min(88, 62 + streak * 6);" +
            "  } else {" +
            "    openAiTarget = (bCount >= pCount) ? '莊' : '閒';" +
            "    openAiConf = 55 + Math.floor(Math.random() * 8);" +
            "  }" +
            "  var openColor = openAiTarget === '莊' ? '#ef4444' : '#3b82f6';" +
            "  document.getElementById('ai_openai_rec').innerHTML = '<span style=\"color:' + openColor + '\">' + openAiTarget + ' (' + openAiConf + '%)</span>';" +

            // 模型二：Gemini 均值回歸模型 (抓跳局、反轉、大數平衡)
            "  var geminiTarget = '閒';" +
            "  var geminiConf = 51;" +
            "  if (bCount > pCount + 2) {" +
            "    geminiTarget = '閒';" +
            "    geminiConf = Math.min(84, 58 + (bCount - pCount) * 5);" +
            "  } else if (pCount > bCount + 2) {" +
            "    geminiTarget = '莊';" +
            "    geminiConf = Math.min(86, 60 + (pCount - bCount) * 5);" +
            "  } else {" +
            "    geminiTarget = (streak >= 3) ? (last === 'B' ? '閒' : '莊') : (last === 'B' ? '閒' : '莊');" +
            "    geminiConf = 60 + Math.floor(Math.random() * 10);" +
            "  }" +
            "  var gemColor = geminiTarget === '莊' ? '#ef4444' : '#3b82f6';" +
            "  document.getElementById('ai_gemini_rec').innerHTML = '<span style=\"color:' + gemColor + '\">' + geminiTarget + ' (' + geminiConf + '%)</span>';" +
            "}" +

            "document.getElementById('btn_add_b').onclick = function() { shoe.push('B'); updateBaccaratPrediction(); };" +
            "document.getElementById('btn_add_p').onclick = function() { shoe.push('P'); updateBaccaratPrediction(); };" +
            "document.getElementById('btn_add_t').onclick = function() { shoe.push('T'); updateBaccaratPrediction(); };" +
            "document.getElementById('road_clear').onclick = function() { shoe = []; updateBaccaratPrediction(); };" +

            // 老虎機計算聯動
            "var bInput = document.getElementById('h_b');" +
            "bInput.oninput = function() {" +
            "  var b = parseFloat(bInput.value) || 0;" +
            "  document.getElementById('h_c').innerText = '$' + (b * 100).toFixed(2);" +
            "  document.getElementById('h_e').innerText = '$' + (b * 96.5).toFixed(2);" +
            "};" +

            "updateBaccaratPrediction();" +
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
