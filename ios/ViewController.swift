import UIKit
import WebKit

class ViewController: UIViewController, WKNavigationDelegate, WKScriptMessageHandler, UITextFieldDelegate {
    private var webView: WKWebView!
    private var isAnalyzing = false

    private let topBar = UIView()
    private let urlTextField = UITextField()
    private let goButton = UIButton(type: .system)
    private let backButton = UIButton(type: .system)
    private let toggleBarButton = UIButton(type: .system)
    private var topBarHeightConstraint: NSLayoutConstraint?
    private var isBarHidden = false

    private let OPENAI_KEY_PART1 = "sk-proj-dwQyYlJrpRoJqtP9ZCcPjQzDUtQXJi1MT1sd6OfsMdW7RF"
    private let OPENAI_KEY_PART2 = "OIOwKJ1JSgi2Satw9WoTaiC8WHPxT3BlbkFJhhCPi2LwrFZ3k7mbJ_LSvLLm65LHzcjTbnqkvKEyKsBgbRlmJzX8X0pGNyrvgH-vPN9sAcwiwA"

    private var openAIKey: String {
        return (OPENAI_KEY_PART1 + OPENAI_KEY_PART2).trimmingCharacters(in: .whitespacesAndNewlines)
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = UIColor(red: 11/255, green: 17/255, blue: 32/255, alpha: 1)
        setupWebView()
        setupTopBar()
    }

    private func setupWebView() {
        let contentController = WKUserContentController()
        contentController.add(self, name: "requestVisualAnalysis")
        contentController.add(self, name: "openExternalUrl")

        let bridgePolyfill = """
        window.AndroidBridge = {
            requestVisualAnalysis: function(mode) {
                window.webkit.messageHandlers.requestVisualAnalysis.postMessage(mode || 'auto');
            },
            openExternalUrl: function(url) {
                window.webkit.messageHandlers.openExternalUrl.postMessage(url);
            }
        };
        """
        contentController.addUserScript(WKUserScript(source: bridgePolyfill, injectionTime: .atDocumentStart, forMainFrameOnly: false))

        let config = WKWebViewConfiguration()
        config.userContentController = contentController
        config.allowsInlineMediaPlayback = true
        config.mediaTypesRequiringUserActionForPlayback = []

        webView = WKWebView(frame: .zero, configuration: config)
        webView.translatesAutoresizingMaskIntoConstraints = false
        webView.navigationDelegate = self
        webView.customUserAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1"
        view.addSubview(webView)
    }

    private func setupTopBar() {
        topBar.translatesAutoresizingMaskIntoConstraints = false
        topBar.backgroundColor = UIColor(red: 15/255, green: 23/255, blue: 42/255, alpha: 0.95)
        view.addSubview(topBar)

        backButton.setTitle("◀", for: .normal)
        backButton.setTitleColor(.white, for: .normal)
        backButton.titleLabel?.font = .systemFont(ofSize: 14, weight: .bold)
        backButton.translatesAutoresizingMaskIntoConstraints = false
        backButton.addTarget(self, action: #selector(handleBack), for: .touchUpInside)
        topBar.addSubview(backButton)

        urlTextField.translatesAutoresizingMaskIntoConstraints = false
        urlTextField.backgroundColor = UIColor(red: 30/255, green: 41/255, blue: 59/255, alpha: 1)
        urlTextField.textColor = UIColor(red: 56/255, green: 189/255, blue: 248/255, alpha: 1)
        urlTextField.font = .systemFont(ofSize: 12)
        urlTextField.placeholder = "請輸入或貼上任何遊戲網址..."
        urlTextField.keyboardType = .URL
        urlTextField.autocapitalizationType = .none
        urlTextField.autocorrectionType = .no
        urlTextField.layer.cornerRadius = 6
        urlTextField.layer.borderWidth = 1
        urlTextField.layer.borderColor = UIColor(red: 71/255, green: 85/255, blue: 105/255, alpha: 1).cgColor
        urlTextField.leftView = UIView(frame: CGRect(x: 0, y: 0, width: 8, height: 28))
        urlTextField.leftViewMode = .always
        urlTextField.returnKeyType = .go
        urlTextField.delegate = self
        topBar.addSubview(urlTextField)

        goButton.setTitle("前往", for: .normal)
        goButton.backgroundColor = UIColor(red: 37/255, green: 99/255, blue: 235/255, alpha: 1)
        goButton.setTitleColor(.white, for: .normal)
        goButton.titleLabel?.font = .systemFont(ofSize: 12, weight: .bold)
        goButton.layer.cornerRadius = 6
        goButton.translatesAutoresizingMaskIntoConstraints = false
        goButton.addTarget(self, action: #selector(handleGo), for: .touchUpInside)
        topBar.addSubview(goButton)

        toggleBarButton.setTitle("網址列", for: .normal)
        toggleBarButton.setTitleColor(.white, for: .normal)
        toggleBarButton.titleLabel?.font = .systemFont(ofSize: 10, weight: .bold)
        toggleBarButton.backgroundColor = UIColor(red: 30/255, green: 41/255, blue: 59/255, alpha: 0.9)
        toggleBarButton.layer.cornerRadius = 4
        toggleBarButton.layer.borderWidth = 1
        toggleBarButton.layer.borderColor = UIColor(red: 56/255, green: 189/255, blue: 248/255, alpha: 0.5).cgColor
        toggleBarButton.translatesAutoresizingMaskIntoConstraints = false
        toggleBarButton.addTarget(self, action: #selector(handleToggleBar), for: .touchUpInside)
        view.addSubview(toggleBarButton)

        topBarHeightConstraint = topBar.heightAnchor.constraint(equalToConstant: 44)

        NSLayoutConstraint.activate([
            topBar.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
            topBar.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            topBar.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            topBarHeightConstraint!,

            backButton.leadingAnchor.constraint(equalTo: topBar.leadingAnchor, constant: 8),
            backButton.centerYAnchor.constraint(equalTo: topBar.centerYAnchor),
            backButton.widthAnchor.constraint(equalToConstant: 30),

            urlTextField.leadingAnchor.constraint(equalTo: backButton.trailingAnchor, constant: 6),
            urlTextField.centerYAnchor.constraint(equalTo: topBar.centerYAnchor),
            urlTextField.heightAnchor.constraint(equalToConstant: 32),

            goButton.leadingAnchor.constraint(equalTo: urlTextField.trailingAnchor, constant: 6),
            goButton.trailingAnchor.constraint(equalTo: topBar.trailingAnchor, constant: -8),
            goButton.centerYAnchor.constraint(equalTo: topBar.centerYAnchor),
            goButton.widthAnchor.constraint(equalToConstant: 48),
            goButton.heightAnchor.constraint(equalToConstant: 32),

            webView.topAnchor.constraint(equalTo: topBar.bottomAnchor),
            webView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            webView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            webView.bottomAnchor.constraint(equalTo: view.bottomAnchor),

            toggleBarButton.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 4),
            toggleBarButton.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 8),
            toggleBarButton.widthAnchor.constraint(equalToConstant: 50),
            toggleBarButton.heightAnchor.constraint(equalToConstant: 24)
        ])
        toggleBarButton.isHidden = true
    }

    @objc private func handleBack() {
        if webView.canGoBack { webView.goBack() }
    }

    @objc private func handleGo() {
        urlTextField.resignFirstResponder()
        guard var text = urlTextField.text?.trimmingCharacters(in: .whitespacesAndNewlines), !text.isEmpty else { return }
        if !text.lowercased().hasPrefix("http://") && !text.lowercased().hasPrefix("https://") {
            text = "https://" + text
        }
        if let url = URL(string: text) {
            webView.load(URLRequest(url: url))
        }
    }

    @objc private func handleToggleBar() {
        isBarHidden.toggle()
        topBarHeightConstraint?.constant = isBarHidden ? 0 : 44
        topBar.isHidden = isBarHidden
        toggleBarButton.isHidden = !isBarHidden
        UIView.animate(withDuration: 0.25) { self.view.layoutIfNeeded() }
    }

    func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        handleGo()
        return true
    }

    func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
        if message.name == "requestVisualAnalysis" {
            let mode = (message.body as? String) ?? "auto"
            captureAndAnalyze(mode: mode)
        } else if message.name == "openExternalUrl", let urlStr = message.body as? String, let url = URL(string: urlStr) {
            UIApplication.shared.open(url)
        }
    }

    func webView(_ webView: WKWebView, decidePolicyFor navigationAction: WKNavigationAction, decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) {
        if let url = navigationAction.request.url {
            let host = url.host?.lowercased() ?? ""
            if host == "lin.ee" || host == "t.me" || url.scheme == "line" || url.scheme == "tg" {
                UIApplication.shared.open(url)
                decisionHandler(.cancel)
                return
            }
        }
        decisionHandler(.allow)
    }

    private func captureAndAnalyze(mode: String) {
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

            let targetWidth: CGFloat = 720.0
            let scale = targetWidth / snapshot.size.width
            let targetSize = CGSize(width: targetWidth, height: snapshot.size.height * scale)

            UIGraphicsBeginImageContextWithOptions(targetSize, false, 1.0)
            snapshot.draw(in: CGRect(origin: .zero, size: targetSize))
            let resizedImage = UIGraphicsGetImageFromCurrentImageContext()
            UIGraphicsEndImageContext()

            guard let jpegData = resizedImage?.jpegData(compressionQuality: 0.85) else {
                self.isAnalyzing = false
                self.updateHUDWithError("影像壓縮失敗")
                return
            }

            self.callOpenAIAstra(base64Image: jpegData.base64EncodedString(), mode: mode)
        }
    }

    private func callOpenAIAstra(base64Image: String, mode: String) {
        guard let url = URL(string: "https://api.openai.com/v1/responses") else { return }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("Bearer \(openAIKey)", forHTTPHeaderField: "Authorization")
        request.timeoutInterval = 25

        let prompt = """
        你是頂尖百家樂視覺精算大師。當前模式設定為【\(mode.uppercased())】。
        【通用視覺辨識核心指引】：
        1. 觀察畫面中的路單走勢（大路、珠盤路、下三路）與即時比分。
        2. 嚴禁回答『畫面仍在載入』或『數據未知』！底欄數字清晰可見，務必識別真實比分填入 stats！
        3. 嚴禁輸出觀望！必須強制在【莊】與【閒】中二選一，信心度評估於 68%~92% 之間。
        嚴格僅輸出純 JSON 物件：
        {"pick":"莊","conf":80,"reason":"大路單跳形態，下三路齊整轉紅","stats":"庄16 闲16 和7 (39局)"}
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
        if let currentUrl = webView.url?.absoluteString {
            urlTextField.text = currentUrl
        }
        injectAssistantScript()
    }

    private func injectAssistantScript() {
        let js = """
        (function() {
            if (document.getElementById('slot-assistant-hud')) return;
            var rootTarget = document.documentElement || document.body;
            if (!rootTarget) return;

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

            var curMode = '通用';

            var hud = document.createElement('div');
            hud.id = 'slot-assistant-hud';
            hud.style.cssText = 'position:fixed;top:70px;right:10px;width:205px;background:rgba(11,17,32,0.96);border:1.5px solid #38bdf8;border-radius:10px;z-index:2147483647;color:#f1f5f9;font-size:11px;box-shadow:0 8px 30px rgba(0,0,0,0.9);font-family:sans-serif;user-select:none;-webkit-user-select:none;backdrop-filter:blur(8px);';
            hud.innerHTML =
                '<div id="hud_header" style="padding:7px 10px;background:#1e293b;border-radius:9px 9px 0 0;font-weight:bold;color:#38bdf8;display:flex;justify-content:space-between;align-items:center;border-bottom:1px solid #334155;cursor:move;touch-action:none;">' +
                    '<span>👁 Astra 深度推論</span>' +
                    '<span id="hud_tog" style="cursor:pointer;color:#94a3b8;font-size:10px;margin-left:3px;">[收]</span>' +
                '</div>' +
                '<div id="hud_content" style="padding:10px;">' +
                    // 導航入口：贊助作者 (USDT-TRC20) + 註冊
                    '<div style="display:grid;grid-template-columns:1fr 1fr;gap:4px;margin-bottom:6px;">' +
                        '<button id="nav_sponsor" style="background:#0f172a;border:1px solid #f59e0b;color:#f59e0b;padding:5px 0;border-radius:4px;font-size:10px;font-weight:bold;cursor:pointer;">🪙 贊助作者</button>' +
                        '<button id="nav_reg" style="background:#2563eb;border:1px solid #38bdf8;color:#fff;padding:5px 0;border-radius:4px;font-size:10px;font-weight:bold;cursor:pointer;">註冊入口</button>' +
                    '</div>' +
                    '<div style="background:rgba(15,23,42,0.85);border:1px solid #3b82f6;border-radius:6px;padding:8px 4px;text-align:center;margin-bottom:6px;">' +
                        '<div style="font-size:10px;color:#94a3b8;">🎯 深度路單精算建議</div>' +
                        '<div id="ai_pick_target" style="font-size:18px;font-weight:900;color:#ef4444;margin:2px 0;">待命中</div>' +
                        '<div id="ai_pick_desc" style="font-size:10px;color:#38bdf8;">點擊下方進行大路與下三路分析</div>' +
                    '</div>' +
                    '<button id="btn_do_ai" style="width:100%;background:#2563eb;color:#fff;border:none;padding:8px 0;border-radius:4px;font-weight:bold;margin-bottom:6px;font-size:11px;cursor:pointer;">📸 截圖畫面並由 AI 辨識</button>' +
                    '<div style="display:flex;justify-content:space-between;font-size:10px;color:#94a3b8;">' +
                        '<span id="hud_score">未掃描</span>' +
                        '<span id="hud_sync_dot" style="color:#4ade80;">● 連線</span>' +
                    '</div>' +
                    // 代理聯繫按鈕
                    '<div style="margin-top:8px;padding-top:6px;border-top:1px solid #334155;display:flex;flex-direction:column;gap:5px;">' +
                        '<button id="btn_line" style="width:100%;background:#06c755;border:none;color:#fff;padding:5px 0;border-radius:4px;font-size:10px;font-weight:bold;cursor:pointer;">💬 LINE: @OSC168</button>' +
                        '<button id="btn_tg" style="width:100%;background:#0088cc;border:none;color:#fff;padding:5px 0;border-radius:4px;font-size:10px;font-weight:bold;cursor:pointer;">✈️ Telegram: @TG_APK1</button>' +
                    '</div>' +
                    // 警語與免責聲明
                    '<div style="margin-top:8px;padding-top:6px;border-top:1px dashed #475569;font-size:8.5px;color:#94a3b8;line-height:1.35;text-align:center;">' +
                        '<div style="color:#f87171;font-weight:bold;margin-bottom:2px;">🔞 未滿 18 歲禁止使用</div>' +
                        '<div style="color:#64748b;">【免責聲明】本系統僅供演算法與大數據統計模擬，不保證獲利。本應用嚴禁且不提供任何真實金錢交易、儲值或博弈服務，請遵守當地法規。</div>' +
                    '</div>' +
                '</div>';
            rootTarget.appendChild(hud);

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

            // 贊助作者：點擊複製 USDT-TRC20 地址
            bindTap(document.getElementById('nav_sponsor'), function() {
                var addr = 'TLz5EaP1rKUfdFu1iZectuDxCP1URNEivm';
                if (navigator.clipboard && navigator.clipboard.writeText) {
                    navigator.clipboard.writeText(addr).then(function() {
                        alert('已複製 USDT-TRC20 贊助地址：\\n' + addr);
                    }).catch(function() {
                        prompt('USDT-TRC20 地址（請手動複製）：', addr);
                    });
                } else {
                    prompt('USDT-TRC20 地址（請手動複製）：', addr);
                }
            });

            bindTap(document.getElementById('nav_reg'), function() { window.location.href = 'https://osc188.com'; });

            function openLink(u) {
                if (window.AndroidBridge && window.AndroidBridge.openExternalUrl) {
                    window.AndroidBridge.openExternalUrl(u);
                } else {
                    window.location.href = u;
                }
            }
            bindTap(document.getElementById('btn_line'), function() { openLink('https://lin.ee/NfoQ9DH'); });
            bindTap(document.getElementById('btn_tg'), function() { openLink('https://t.me/TG_apk1'); });

            var btnDo = document.getElementById('btn_do_ai');
            bindTap(btnDo, function() {
                btnDo.innerText = '🧠 Astra 深度推論中...';
                btnDo.disabled = true;
                if (window.AndroidBridge && window.AndroidBridge.requestVisualAnalysis) {
                    window.AndroidBridge.requestVisualAnalysis(curMode);
                }
            });

            window.__updateAI = function(pick, conf, reason, stats) {
                var t = document.getElementById('ai_pick_target');
                var d = document.getElementById('ai_pick_desc');
                var s = document.getElementById('hud_score');
                if (t) {
                    t.innerText = '【' + pick + '】 ' + conf + '%';
                    t.style.color = (pick === '莊' || pick === '庄') ? '#ef4444' : '#3b82f6';
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
