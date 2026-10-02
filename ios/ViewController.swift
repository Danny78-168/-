import UIKit
import WebKit

class ViewController: UIViewController, WKNavigationDelegate, WKScriptMessageHandler {
    private var webView: WKWebView!
    private var isAnalyzing = false

    // 🔑 拆分字串內建 OpenAI 金鑰
    private let OPENAI_KEY_PART1 = "sk-proj-dwQyYlJrpRoJqtP9ZCcPjQzDUtQXJi1MT1sd6OfsMdW7RF"
    private let OPENAI_KEY_PART2 = "OIOwKJ1JSgi2Satw9WoTaiC8WHPxT3BlbkFJhhCPi2LwrFZ3k7mbJ_LSvLLm65LHzcjTbnqkvKEyKsBgbRlmJzX8X0pGNyrvgH-vPN9sAcwiwA"

    private var openAIKey: String {
        return (OPENAI_KEY_PART1 + OPENAI_KEY_PART2).trimmingCharacters(in: .whitespacesAndNewlines)
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        setupWebView()
    }

    private func setupWebView() {
        let contentController = WKUserContentController()
        
        // 註冊原生通道
        contentController.add(self, name: "requestVisualAnalysis")
        contentController.add(self, name: "switchGame")

        // 注入 AndroidBridge 相容腳本，保持 HUD 程式碼完全相容
        let bridgePolyfill = """
        window.AndroidBridge = {
            requestVisualAnalysis: function() {
                window.webkit.messageHandlers.requestVisualAnalysis.postMessage("");
            },
            switchGame: function(url) {
                window.webkit.messageHandlers.switchGame.postMessage(url);
            }
        };
        """
        let userScript = WKUserScript(source: bridgePolyfill, injectionTime: .atDocumentStart, forMainFrameOnly: false)
        contentController.addUserScript(userScript)

        let config = WKWebViewConfiguration()
        config.userContentController = contentController
        config.allowsInlineMediaPlayback = true
        config.mediaTypesRequiringUserActionForPlayback = []

        webView = WKWebView(frame: view.bounds, configuration: config)
        webView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        webView.navigationDelegate = self
        webView.customUserAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1"
        view.addSubview(webView)

        if let url = URL(string: "https://www.osc169.com/#/game/play?game_name=meta_all&game_type=3&device=mobile") {
            webView.load(URLRequest(url: url))
        }
    }

    // 處理 JS 傳來的訊息
    func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
        if message.name == "requestVisualAnalysis" {
            captureAndAnalyze()
        } else if message.name == "switchGame", let targetUrl = message.body as? String, let url = URL(string: targetUrl) {
            webView.load(URLRequest(url: url))
        }
    }

    // iOS WebKit 原生畫布截圖
    private func captureAndAnalyze() {
        guard !isAnalyzing else { return }
        isAnalyzing = true

        let snapConfig = WKSnapshotConfiguration()
        snapConfig.rect = webView.bounds

        webView.takeSnapshot(with: snapConfig) { [weak self] image, error in
            guard let self = self, let snapshot = image else {
                self?.isAnalyzing = false
                self?.updateHUDWithError("畫面截圖失敗")
                return
            }

            // 等比縮小寬度至 540px
            let targetWidth: CGFloat = 540.0
            let scale = targetWidth / snapshot.size.width
            let targetSize = CGSize(width: targetWidth, height: snapshot.size.height * scale)

            UIGraphicsBeginImageContextWithOptions(targetSize, false, 1.0)
            snapshot.draw(in: CGRect(origin: .zero, size: targetSize))
            let resizedImage = UIGraphicsGetImageFromCurrentImageContext()
            UIGraphicsEndImageContext()

            guard let jpegData = resizedImage?.jpegData(compressionQuality: 0.7) else {
                self.isAnalyzing = false
                self.updateHUDWithError("壓縮影像失敗")
                return
            }

            let base64String = jpegData.base64EncodedString()
            self.callOpenAIAstra(base64Image: base64String)
        }
    }

    // 呼叫 GPT-6 Astra 模型
    private func callOpenAIAstra(base64Image: String) {
        guard let url = URL(string: "https://api.openai.com/v1/responses") else { return }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("Bearer \(openAIKey)", forHTTPHeaderField: "Authorization")
        request.timeoutInterval = 25

        let prompt = """
        你是具備大數據統計背景的職業百家樂路單視覺精算大師。
        請嚴格觀察圖片下半部【珠盤路、大路、下三路（大眼仔、小路、蟑螂路）】：
        【嚴禁行為】：嚴禁僅以『比分接近/莊天然勝率』作為預測理由！嚴禁輸出觀望！必須強制二選一【莊】或【閒】！
        1.【大路形態識別】：觀察最新列走勢，是處於連龍還是單跳？
        2.【下三路拍整度驗證】：觀察右下角三行標記，紅多為整齊順延，藍多為變盤轉向。
        3.【均值修正】：若莊閒比分差距 >= 3 局，將大數回歸作為權重輔助。
        4.【精算輸出】：信心度評估於 68%~92% 之間，理由需具體點出形態。
        嚴格僅輸出純 JSON 物件：
        {"pick":"莊","conf":82,"reason":"大路單跳形態，下三路齊整","stats":"莊11 閒12 和4 (27局)"}
        """

        let body: [String: Any] = [
            "model": "gpt-6-astra",
            "service_tier": "default",
            "reasoning": ["effort": "medium"],
            "input": [
                [
                    "role": "user",
                    "content": [
                        ["type": "input_text", "text": prompt],
                        ["type": "input_image", "image_url": "data:image/jpeg;base64,\(base64Image)"]
                    ]
                ]
            ]
        ]

        request.httpBody = try? JSONSerialization.data(withJSONObject: body)

        URLSession.shared.dataTask(with: request) { [weak self] data, response, error in
            guard let self = self else { return }
            self.isAnalyzing = false

            if let error = error {
                self.updateHUDWithError(error.localizedDescription)
                return
            }

            guard let data = data, let rawStr = String(data: data, encoding: .utf8) else {
                self.updateHUDWithError("無回傳數據")
                return
            }

            let clean = self.extractJson(from: rawStr)
            self.updateHUDWithResult(clean)
        }.resume()
    }

    private func extractJson(from text: String) -> String {
        guard let start = text.firstIndex(of: "{"),
              let end = text.lastIndex(of: "}") else {
            return "{}"
        }
        return String(text[start...end])
    }

    private func updateHUDWithResult(_ jsonStr: String) {
        DispatchQueue.main.async { [weak self] in
            guard let data = jsonStr.data(using: .utf8),
                  let obj = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
                self?.updateHUDWithError("解析格式異常")
                return
            }

            let pick = obj["pick"] as? String ?? "莊"
            let conf = obj["conf"] as? Int ?? 65
            let reason = obj["reason"] as? String ?? "走勢推論"
            let stats = obj["stats"] as? String ?? "統計更新"

            let js = "window.__updateAI && window.__updateAI('\(pick)', \(conf), '\(reason)', '\(stats)');"
            self?.webView.evaluateJavaScript(js, completionHandler: nil)
        }
    }

    private func updateHUDWithError(_ msg: String) {
        DispatchQueue.main.async { [weak self] in
            let js = "window.__updateAIError && window.__updateAIError('\(msg)');"
            self?.webView.evaluateJavaScript(js, completionHandler: nil)
        }
    }

    func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
        injectAssistantScript()
    }

    private func injectAssistantScript() {
        let js = """
        (function() {
            if (document.getElementById('slot-assistant-hud')) return;

            function bindTap(el, fn) {
                if (!el) return;
                var moved = false;
                el.addEventListener('touchstart', function(e) { moved = false; e.stopPropagation(); }, { passive: true });
                el.addEventListener('touchmove', function(e) { moved = true; }, { passive: true });
                el.addEventListener('touchend', function(e) {
                    e.stopPropagation();
                    if (!moved) { e.preventDefault(); fn(); }
                });
                el.addEventListener('click', function(e) { e.stopPropagation(); fn(); });
            }

            var hud = document.createElement('div');
            hud.id = 'slot-assistant-hud';
            hud.style.cssText = 'position:fixed;top:60px;right:8px;width:215px;background:rgba(11,17,32,0.96);border:1px solid rgba(56,189,248,0.7);border-radius:10px;z-index:999999;color:#f1f5f9;font-size:11px;box-shadow:0 8px 24px rgba(0,0,0,0.85);font-family:sans-serif;user-select:none;-webkit-user-select:none;backdrop-filter:blur(6px);';
            hud.innerHTML =
                '<div id="hud_header" style="padding:7px 10px;background:#1e293b;border-radius:10px 10px 0 0;font-weight:bold;color:#38bdf8;display:flex;justify-content:space-between;align-items:center;border-bottom:1px solid #334155;cursor:move;touch-action:none;">' +
                    '<span>👁 Astra 深度推論 (iOS)</span>' +
                    '<div style="display:flex;gap:3px;align-items:center;">' +
                        '<button id="tab_bac" style="background:#2563eb;border:1px solid #3b82f6;color:#fff;padding:2px 6px;border-radius:3px;font-size:10px;">百家</button>' +
                        '<button id="tab_slt" style="background:#0f172a;border:1px solid #475569;color:#94a3b8;padding:2px 6px;border-radius:3px;font-size:10px;">老虎</button>' +
                        '<span id="hud_tog" style="cursor:pointer;color:#94a3b8;font-size:10px;margin-left:3px;">[收]</span>' +
                    '</div>' +
                '</div>' +
                '<div id="hud_content" style="padding:10px;">' +
                    '<div id="p_bac">' +
                        '<div style="display:grid;grid-template-columns:1fr 1fr;gap:4px;margin-bottom:6px;">' +
                            '<button id="nav_mt" style="background:#0f172a;border:1px solid #38bdf8;color:#38bdf8;padding:4px 0;border-radius:3px;font-size:10px;font-weight:bold;">MT 百家</button>' +
                            '<button id="nav_dg" style="background:#0f172a;border:1px solid #475569;color:#e2e8f0;padding:4px 0;border-radius:3px;font-size:10px;">DG 百家</button>' +
                        '</div>' +
                        '<div style="background:rgba(15,23,42,0.85);border:1px solid #3b82f6;border-radius:6px;padding:8px 4px;text-align:center;margin-bottom:6px;">' +
                            '<div style="font-size:10px;color:#94a3b8;">🎯 深度路單精算建議</div>' +
                            '<div id="ai_pick_target" style="font-size:18px;font-weight:900;color:#ef4444;margin:2px 0;">待命中</div>' +
                            '<div id="ai_pick_desc" style="font-size:10px;color:#38bdf8;">點擊下方進行大路與下三路分析</div>' +
                        '</div>' +
                        '<button id="btn_do_ai" style="width:100%;background:#2563eb;color:#fff;border:none;padding:7px 0;border-radius:4px;font-weight:bold;margin-bottom:6px;font-size:11px;">📸 截圖畫面並由 AI 辨識</button>' +
                        '<div style="display:flex;justify-content:space-between;font-size:10px;color:#94a3b8;">' +
                            '<span id="hud_score">未掃描</span>' +
                            '<span id="hud_sync_dot" style="color:#4ade80;">● 連線</span>' +
                        '</div>' +
                    '</div>' +
                    '<div id="p_slt" style="display:none;">' +
                        '<div style="display:grid;grid-template-columns:repeat(3, 1fr);gap:3px;margin-bottom:8px;">' +
                            '<button id="nav_atg" style="background:#0f172a;border:1px solid #475569;color:#fff;padding:4px 0;border-radius:3px;font-size:9px;">虎小妹</button>' +
                            '<button id="nav_rsg" style="background:#0f172a;border:1px solid #475569;color:#fff;padding:4px 0;border-radius:3px;font-size:9px;">雷神</button>' +
                            '<button id="nav_ava" style="background:#0f172a;border:1px solid #475569;color:#fff;padding:4px 0;border-radius:3px;font-size:9px;">Avatar</button>' +
                        '</div>' +
                        '<div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:6px;">' +
                            '<span>單注:</span>' +
                            '<input id="s_bet" type="number" value="1.00" step="0.10" style="width:50px;background:#1e293b;border:1px solid #475569;color:#38bdf8;padding:2px;text-align:right;border-radius:3px;">' +
                        '</div>' +
                        '<div id="slot_modes" style="display:grid;grid-template-columns:repeat(3, 1fr);gap:2px;margin-bottom:8px;"></div>' +
                        '<div style="font-size:10px;">' +
                            '<div style="display:flex;justify-content:space-between;margin-bottom:3px;">免遊成本: <b id="s_cost" style="color:#fff;">$200.00</b></div>' +
                            '<div style="display:flex;justify-content:space-between;margin-bottom:3px;">理論(96.5%): <b id="s_ev" style="color:#4ade80;">$193.00</b></div>' +
                            '<div style="display:flex;justify-content:space-between;">中位數: <b id="s_med" style="color:#facc15;">$84.00</b></div>' +
                        '</div>' +
                    '</div>' +
                '</div>';
            document.body.appendChild(hud);

            var header = document.getElementById('hud_header');
            var isDrag = false, sX, sY, iL, iT;
            header.addEventListener('touchstart', function(e) {
                if (e.target.closest('button') || e.target.closest('#hud_tog')) { isDrag = false; return; }
                isDrag = true; var t = e.touches[0]; var r = hud.getBoundingClientRect();
                sX = t.clientX; sY = t.clientY; iL = r.left; iT = r.top;
            }, { passive: true });
            header.addEventListener('touchmove', function(e) {
                if (!isDrag) return; var t = e.touches[0];
                var nX = iL + (t.clientX - sX); var nY = iT + (t.clientY - sY);
                hud.style.left = Math.max(0, Math.min(nX, window.innerWidth - hud.offsetWidth)) + 'px';
                hud.style.top = Math.max(0, Math.min(nY, window.innerHeight - hud.offsetHeight)) + 'px';
                hud.style.right = 'auto';
            }, { passive: false });
            header.addEventListener('touchend', function() { isDrag = false; });

            var tog = document.getElementById('hud_tog');
            var cnt = document.getElementById('hud_content');
            bindTap(tog, function() {
                if (cnt.style.display === 'none') { cnt.style.display = 'block'; tog.innerText = '[收]'; }
                else { cnt.style.display = 'none'; tog.innerText = '[展]'; }
            });

            var tB = document.getElementById('tab_bac'), tS = document.getElementById('tab_slt');
            var pB = document.getElementById('p_bac'), pS = document.getElementById('p_slt');
            bindTap(tB, function() {
                pB.style.display = 'block'; pS.style.display = 'none';
                tB.style.background = '#2563eb'; tB.style.color = '#fff';
                tS.style.background = '#0f172a'; tS.style.color = '#94a3b8';
            });
            bindTap(tS, function() {
                pS.style.display = 'block'; pB.style.display = 'none';
                tS.style.background = '#2563eb'; tS.style.color = '#fff';
                tB.style.background = '#0f172a'; tB.style.color = '#94a3b8';
            });

            function navTo(u) {
                if (window.AndroidBridge && window.AndroidBridge.switchGame) window.AndroidBridge.switchGame(u);
                else location.href = u;
            }
            bindTap(document.getElementById('nav_mt'), function() { navTo('https://www.osc169.com/#/game/play?game_name=meta_all&game_type=3&device=mobile'); });
            bindTap(document.getElementById('nav_dg'), function() { navTo('https://www.osc169.com/#/game/play?game_name=dg&game_type=3&device=mobile'); });
            bindTap(document.getElementById('nav_atg'), function() { navTo('https://www.osc169.com/#/game/play?game_name=atg&productId=tiger-princess&device=mobile'); });
            bindTap(document.getElementById('nav_rsg'), function() { navTo('https://www.osc169.com/#/game/play?game_name=rsg&productId=129&device=mobile'); });
            bindTap(document.getElementById('nav_ava'), function() { navTo('https://www.osc169.com/#/game/play?game_name=avatar&productId=140083&device=mobile'); });

            var btnDo = document.getElementById('btn_do_ai');
            bindTap(btnDo, function() {
                btnDo.innerText = '🧠 Astra 深度推論中...';
                btnDo.disabled = true;
                if (window.AndroidBridge && window.AndroidBridge.requestVisualAnalysis) {
                    window.AndroidBridge.requestVisualAnalysis();
                }
            });

            window.__updateAI = function(pick, conf, reason, stats) {
                var t = document.getElementById('ai_pick_target');
                var d = document.getElementById('ai_pick_desc');
                var s = document.getElementById('hud_score');
                if (t) {
                    t.innerText = '【' + pick + '】 ' + conf + '%';
                    t.style.color = (pick === '莊') ? '#ef4444' : '#3b82f6';
                }
                if (d) d.innerText = reason;
                if (s) s.innerText = stats;
                btnDo.innerText = '📸 截圖畫面並由 AI 辨識';
                btnDo.disabled = false;
            };

            window.__updateAIError = function(msg) {
                var d = document.getElementById('ai_pick_desc');
                if (d) d.innerText = '辨識重試: ' + msg;
                btnDo.innerText = '重試';
                btnDo.disabled = false;
            };
        })();
        """
        webView.evaluateJavaScript(js, completionHandler: nil)
    }
}
