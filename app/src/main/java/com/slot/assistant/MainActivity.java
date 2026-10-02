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

        // 支援 ATG / RSG 跨域第三方 Cookie
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(webView, true);
        }

        // 模擬手機版 Chrome 標頭
        settings.setUserAgentString("Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                injectMultiGameAssistant(view);
            }
        });

        // 預設開啟 ATG 虎姬
        webView.loadUrl("https://www.osc169.com/#/game/play?game_name=atg&productId=tiger-princess&device=mobile");
    }

    private void injectMultiGameAssistant(WebView view) {
        String js = "javascript:(function() {" +
            "if (document.getElementById('slot-assistant-hud')) return;" +
            "var hud = document.createElement('div');" +
            "hud.id = 'slot-assistant-hud';" +
            "hud.style.cssText = 'position:fixed;top:50px;right:8px;width:220px;background:#0b1120;border:1px solid #38bdf8;border-radius:10px;z-index:999999;color:#e2e8f0;font-size:12px;padding:12px;box-shadow:0 6px 20px rgba(0,0,0,0.8);font-family:sans-serif;';" +
            "hud.innerHTML = '<div style=\"font-weight:bold;color:#38bdf8;margin-bottom:8px;display:flex;justify-content:space-between;\">" +
                "<span>🎰 20,000X 助手</span>" +
                "<span id=\"hud_tog\" style=\"cursor:pointer;color:#94a3b8;\">[收合]</span>" +
            "</div>' +" +
            "'<div id=\"hud_content\">' +" +
                "'<div style=\"margin-bottom:8px;\">" +
                    "<label style=\"color:#94a3b8;font-size:11px;\">快捷換台：</label>" +
                    "<div style=\"display:grid;grid-template-columns:repeat(3, 1fr);gap:4px;margin-top:4px;\">" +
                        "<button onclick=\"location.href=\\'https://www.osc169.com/#/game/play?game_name=atg&productId=tiger-princess&device=mobile\\'\" style=\"background:#1e293b;border:1px solid #475569;color:#fff;padding:6px 2px;border-radius:4px;font-size:10px;\">ATG 虎姬</button>" +
                        "<button onclick=\"location.href=\\'https://www.osc169.com/#/game/play?game_name=rsg&productId=129&device=mobile\\'\" style=\"background:#1e293b;border:1px solid #475569;color:#fff;padding:6px 2px;border-radius:4px;font-size:10px;\">RSG 129</button>" +
                        "<button onclick=\"location.href=\\'https://www.osc169.com/#/game/play?game_name=avatar&productId=140083&device=mobile\\'\" style=\"background:#1e293b;border:1px solid #475569;color:#fff;padding:6px 2px;border-radius:4px;font-size:10px;\">Avatar</button>" +
                    "</div>" +
                "</div>' +" +
                "'<hr style=\"border:0;border-top:1px solid #334155;margin:8px 0;\">' +" +
                "'<div style=\"display:flex;justify-content:space-between;margin-bottom:4px;\">單注: <input id=\"h_b\" type=\"number\" value=\"0.10\" step=\"0.05\" style=\"width:55px;background:#1e293b;border:1px solid #475569;color:#38bdf8;padding:2px;border-radius:3px;text-align:right;\"></div>' +" +
                "'<div style=\"display:flex;justify-content:space-between;margin-bottom:4px;\">免遊成本(100x): <b id=\"h_c\" style=\"color:#fff;\">$10.00</b></div>' +" +
                "'<div style=\"display:flex;justify-content:space-between;margin-bottom:4px;\">理論預計(96.5%): <b id=\"h_e\" style=\"color:#4ade80;\">$9.66</b></div>' +" +
                "'<div style=\"display:flex;justify-content:space-between;margin-bottom:4px;\">常態中位數: <b id=\"h_m\" style=\"color:#facc15;\">$4.20</b></div>' +" +
                "'<div style=\"display:flex;justify-content:space-between;margin-bottom:6px;\">20000x 爆分率: <b style=\"color:#f43f5e;\">0.20%</b></div>' +" +
                "'<div id=\"h_s\" style=\"font-size:10px;color:#94a3b8;background:#020617;padding:5px;border-radius:4px;text-align:center;\">封包監聽中...</div>' +" +
            "'</div>';" +
            "document.body.appendChild(hud);" +
            "var tog = document.getElementById('hud_tog');" +
            "var cnt = document.getElementById('hud_content');" +
            "tog.onclick = function() {" +
            "  if (cnt.style.display === 'none') { cnt.style.display = 'block'; tog.innerText = '[收合]'; }" +
            "  else { cnt.style.display = 'none'; tog.innerText = '[展開]'; }" +
            "};" +
            "var bInput = document.getElementById('h_b');" +
            "function calc() {" +
            "  var b = parseFloat(bInput.value) || 0;" +
            "  document.getElementById('h_c').innerText = '$' + (b * 100).toFixed(2);" +
            "  document.getElementById('h_e').innerText = '$' + (b * 96.56).toFixed(2);" +
            "  document.getElementById('h_m').innerText = '$' + (b * 42).toFixed(2);" +
            "}" +
            "bInput.oninput = calc;" +
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
