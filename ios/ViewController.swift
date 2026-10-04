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

    private let OPENAI_KEY_PART1 = "sk-proj-dwQyYlJrpRoJqtP9ZCcPjQzDUtQXJi1MT1sd6OfsMdW7RF[span_0](start_span)"[span_0](end_span)
    private let OPENAI_KEY_PART2 = "OIOwKJ1JSgi2Satw9WoTaiC8WHPxT3BlbkFJhhCPi2LwrFZ3k7mbJ_LSvLLm65LHzcjTbnqkvKEyKsBgbRlmJzX8X0pGNyrvgH-vPN9sAcwiwA[span_1](start_span)"[span_1](end_span)

    private var openAIKey: String {
        return (OPENAI_KEY_PART1 + OPENAI_KEY_PART2).trimmingCharacters(in: .whitespacesAndNewlines)[span_2](start_span)[span_2](end_span)
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = UIColor(red: 11/255, green: 17/255, blue: 32/255, alpha: 1)[span_3](start_span)[span_3](end_span)
        setupWebView()[span_4](start_span)[span_4](end_span)
        setupTopBar()[span_5](start_span)[span_5](end_span)
    }

    private func setupWebView() {
        let contentController = WKUserContentController()[span_6](start_span)[span_6](end_span)
        contentController.add(self, name: "requestVisualAnalysis")[span_7](start_span)[span_7](end_span)
        contentController.add(self, name: "openExternalUrl")[span_8](start_span)[span_8](end_span)

        let bridgePolyfill = """
        window.AndroidBridge = {
            requestVisualAnalysis: function(mode) {
                window.webkit.messageHandlers.requestVisualAnalysis.postMessage(mode || 'auto');
            },
            openExternalUrl: function(url) {
                window.webkit.messageHandlers.openExternalUrl.postMessage(url);
            }
        };
        ""[span_9](start_span)"[span_9](end_span)
        contentController.addUserScript(WKUserScript(source: bridgePolyfill, injectionTime: .atDocumentStart, forMainFrameOnly: false))[span_10](start_span)[span_10](end_span)

        let config = WKWebViewConfiguration()[span_11](start_span)[span_11](end_span)
        config.userContentController = contentController[span_12](start_span)[span_12](end_span)
        config.allowsInlineMediaPlayback = true[span_13](start_span)[span_13](end_span)
        config.mediaTypesRequiringUserActionForPlayback = [][span_14](start_span)[span_14](end_span)

        webView = WKWebView(frame: .zero, configuration: config)[span_15](start_span)[span_15](end_span)
        webView.translatesAutoresizingMaskIntoConstraints = false[span_16](start_span)[span_16](end_span)
        webView.navigationDelegate = self[span_17](start_span)[span_17](end_span)
        webView.customUserAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1[span_18](start_span)"[span_18](end_span)
        view.addSubview(webView)[span_19](start_span)[span_19](end_span)
    }

    private func setupTopBar() {
        topBar.translatesAutoresizingMaskIntoConstraints = false[span_20](start_span)[span_20](end_span)
        topBar.backgroundColor = UIColor(red: 15/255, green: 23/255, blue: 42/255, alpha: 0.95)[span_21](start_span)[span_21](end_span)
        view.addSubview(topBar)[span_22](start_span)[span_22](end_span)

        backButton.setTitle("◀", for: .normal)[span_23](start_span)[span_23](end_span)
        backButton.setTitleColor(.white, for: .normal)[span_24](start_span)[span_24](end_span)
        backButton.titleLabel?.font = .systemFont(ofSize: 14, weight: .bold)[span_25](start_span)[span_25](end_span)
        backButton.translatesAutoresizingMaskIntoConstraints = false[span_26](start_span)[span_26](end_span)
        backButton.addTarget(self, action: #selector(handleBack), for: .touchUpInside)[span_27](start_span)[span_27](end_span)
        topBar.addSubview(backButton)[span_28](start_span)[span_28](end_span)

        urlTextField.translatesAutoresizingMaskIntoConstraints = false[span_29](start_span)[span_29](end_span)
        urlTextField.backgroundColor = UIColor(red: 30/255, green: 41/255, blue: 59/255, alpha: 1)[span_30](start_span)[span_30](end_span)
        urlTextField.textColor = UIColor(red: 56/255, green: 189/255, blue: 248/255, alpha: 1)[span_31](start_span)[span_31](end_span)
        urlTextField.font = .systemFont(ofSize: 12)[span_32](start_span)[span_32](end_span)
        urlTextField.placeholder = "請輸入或貼上任何遊戲網址...[span_33](start_span)"[span_33](end_span)
        urlTextField.keyboardType = .URL[span_34](start_span)[span_34](end_span)
        urlTextField.autocapitalizationType = .none[span_35](start_span)[span_35](end_span)
        urlTextField.autocorrectionType = .no[span_36](start_span)[span_36](end_span)
        urlTextField.layer.cornerRadius = 6[span_37](start_span)[span_37](end_span)
        urlTextField.layer.borderWidth = 1[span_38](start_span)[span_38](end_span)
        urlTextField.layer.borderColor = UIColor(red: 71/255, green: 85/255, blue: 105/255, alpha: 1).cgColor[span_39](start_span)[span_39](end_span)
        urlTextField.leftView = UIView(frame: CGRect(x: 0, y: 0, width: 8, height: 28))[span_40](start_span)[span_40](end_span)
        urlTextField.leftViewMode = .always[span_41](start_span)[span_41](end_span)
        urlTextField.returnKeyType = .go[span_42](start_span)[span_42](end_span)
        urlTextField.delegate = self[span_43](start_span)[span_43](end_span)
        topBar.addSubview(urlTextField)[span_44](start_span)[span_44](end_span)

        goButton.setTitle("前往", for: .normal)[span_45](start_span)[span_45](end_span)
        goButton.backgroundColor = UIColor(red: 37/255, green: 99/255, blue: 235/255, alpha: 1)[span_46](start_span)[span_46](end_span)
        goButton.setTitleColor(.white, for: .normal)[span_47](start_span)[span_47](end_span)
        goButton.titleLabel?.font = .systemFont(ofSize: 12, weight: .bold)[span_48](start_span)[span_48](end_span)
        goButton.layer.cornerRadius = 6[span_49](start_span)[span_49](end_span)
        goButton.translatesAutoresizingMaskIntoConstraints = false[span_50](start_span)[span_50](end_span)
        goButton.addTarget(self, action: #selector(handleGo), for: .touchUpInside)[span_51](start_span)[span_51](end_span)
        topBar.addSubview(goButton)[span_52](start_span)[span_52](end_span)

        toggleBarButton.setTitle("網址列", for: .normal)[span_53](start_span)[span_53](end_span)
        toggleBarButton.setTitleColor(.white, for: .normal)[span_54](start_span)[span_54](end_span)
        toggleBarButton.titleLabel?.font = .systemFont(ofSize: 10, weight: .bold)[span_55](start_span)[span_55](end_span)
        toggleBarButton.backgroundColor = UIColor(red: 30/255, green: 41/255, blue: 59/255, alpha: 0.9)[span_56](start_span)[span_56](end_span)
        toggleBarButton.layer.cornerRadius = 4[span_57](start_span)[span_57](end_span)
        toggleBarButton.layer.borderWidth = 1[span_58](start_span)[span_58](end_span)
        toggleBarButton.layer.borderColor = UIColor(red: 56/255, green: 189/255, blue: 248/255, alpha: 0.5).cgColor[span_59](start_span)[span_59](end_span)
        toggleBarButton.translatesAutoresizingMaskIntoConstraints = false[span_60](start_span)[span_60](end_span)
        toggleBarButton.addTarget(self, action: #selector(handleToggleBar), for: .touchUpInside)[span_61](start_span)[span_61](end_span)
        view.addSubview(toggleBarButton)[span_62](start_span)[span_62](end_span)

        topBarHeightConstraint = topBar.heightAnchor.constraint(equalToConstant: 44)[span_63](start_span)[span_63](end_span)

        NSLayoutConstraint.activate([
            topBar.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),[span_64](start_span)[span_64](end_span)
            topBar.leadingAnchor.constraint(equalTo: view.leadingAnchor),[span_65](start_span)[span_65](end_span)
            topBar.trailingAnchor.constraint(equalTo: view.trailingAnchor),[span_66](start_span)[span_66](end_span)
            topBarHeightConstraint!,[span_67](start_span)[span_67](end_span)

            backButton.leadingAnchor.constraint(equalTo: topBar.leadingAnchor, constant: 8),[span_68](start_span)[span_68](end_span)
            backButton.centerYAnchor.constraint(equalTo: topBar.centerYAnchor),[span_69](start_span)[span_69](end_span)
            backButton.widthAnchor.constraint(equalToConstant: 30),[span_70](start_span)[span_70](end_span)

            urlTextField.leadingAnchor.constraint(equalTo: backButton.trailingAnchor, constant: 6),[span_71](start_span)[span_71](end_span)
            urlTextField.centerYAnchor.constraint(equalTo: topBar.centerYAnchor),[span_72](start_span)[span_72](end_span)
            urlTextField.heightAnchor.constraint(equalToConstant: 32),[span_73](start_span)[span_73](end_span)

            goButton.leadingAnchor.constraint(equalTo: urlTextField.trailingAnchor, constant: 6),[span_74](start_span)[span_74](end_span)
            goButton.trailingAnchor.constraint(equalTo: topBar.trailingAnchor, constant: -8),[span_75](start_span)[span_75](end_span)
            goButton.centerYAnchor.constraint(equalTo: topBar.centerYAnchor),[span_76](start_span)[span_76](end_span)
            goButton.widthAnchor.constraint(equalToConstant: 48),[span_77](start_span)[span_77](end_span)
            goButton.heightAnchor.constraint(equalToConstant: 32),[span_78](start_span)[span_78](end_span)

            webView.topAnchor.constraint(equalTo: topBar.bottomAnchor),[span_79](start_span)[span_79](end_span)
            webView.leadingAnchor.constraint(equalTo: view.leadingAnchor),[span_80](start_span)[span_80](end_span)
            webView.trailingAnchor.constraint(equalTo: view.trailingAnchor),[span_81](start_span)[span_81](end_span)
            webView.bottomAnchor.constraint(equalTo: view.bottomAnchor),[span_82](start_span)[span_82](end_span)

            toggleBarButton.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 4),[span_83](start_span)[span_83](end_span)
            toggleBarButton.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 8),[span_84](start_span)[span_84](end_span)
            toggleBarButton.widthAnchor.constraint(equalToConstant: 50),[span_85](start_span)[span_85](end_span)
            toggleBarButton.heightAnchor.constraint(equalToConstant: 24)[span_86](start_span)[span_86](end_span)
        ])
        toggleBarButton.isHidden = true[span_87](start_span)[span_87](end_span)
    }

    @objc private func handleBack() {
        if webView.canGoBack { webView.goBack() }[span_88](start_span)[span_88](end_span)
    }

    @objc private func handleGo() {
        urlTextField.resignFirstResponder()[span_89](start_span)[span_89](end_span)
        guard var text = urlTextField.text?.trimmingCharacters(in: .whitespacesAndNewlines), !text.isEmpty else { return }[span_90](start_span)[span_90](end_span)
        if !text.lowercased().hasPrefix("http://") && !text.lowercased().hasPrefix("https://") {[span_91](start_span)[span_91](end_span)
            text = "https://" + text[span_92](start_span)[span_92](end_span)
        }
        if let url = URL(string: text) {[span_93](start_span)[span_93](end_span)
            webView.load(URLRequest(url: url))[span_94](start_span)[span_94](end_span)
        }
    }

    @objc private func handleToggleBar() {
        isBarHidden.toggle()[span_95](start_span)[span_95](end_span)
        topBarHeightConstraint?.constant = isBarHidden ? 0 : 44[span_96](start_span)[span_96](end_span)
        topBar.isHidden = isBarHidden[span_97](start_span)[span_97](end_span)
        toggleBarButton.isHidden = !isBarHidden[span_98](start_span)[span_98](end_span)
        UIView.animate(withDuration: 0.25) { self.view.layoutIfNeeded() }[span_99](start_span)[span_99](end_span)
    }

    func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        handleGo()[span_100](start_span)[span_100](end_span)
        return true[span_101](start_span)[span_101](end_span)
    }

    func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
        if message.name == "requestVisualAnalysis" {[span_102](start_span)[span_102](end_span)
            let mode = (message.body as? String) ?? "auto[span_103](start_span)"[span_103](end_span)
            captureAndAnalyze(mode: mode)[span_104](start_span)[span_104](end_span)
        } else if message.name == "openExternalUrl", let urlStr = message.body as? String, let url = URL(string: urlStr) {[span_105](start_span)[span_105](end_span)
            UIApplication.shared.open(url)[span_106](start_span)[span_106](end_span)
        }
    }

    func webView(_ webView: WKWebView, decidePolicyFor navigationAction: WKNavigationAction, decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) {
        if let url = navigationAction.request.url {[span_107](start_span)[span_107](end_span)
            let host = url.host?.lowercased() ?? "[span_108](start_span)"[span_108](end_span)
            if host == "lin.ee" || host == "t.me" || url.scheme == "line" || url.scheme == "tg" {[span_109](start_span)[span_109](end_span)
                UIApplication.shared.open(url)[span_110](start_span)[span_110](end_span)
                decisionHandler(.cancel)[span_111](start_span)[span_111](end_span)
                return[span_112](start_span)[span_112](end_span)
            }
        }
        decisionHandler(.allow)[span_113](start_span)[span_113](end_span)
    }

    private func captureAndAnalyze(mode: String) {
        guard !isAnalyzing else { return }[span_114](start_span)[span_114](end_span)
        isAnalyzing = true[span_115](start_span)[span_115](end_span)

        let snapConfig = WKSnapshotConfiguration()[span_116](start_span)[span_116](end_span)
        snapConfig.rect = webView.bounds[span_117](start_span)[span_117](end_span)

        webView.takeSnapshot(with: snapConfig) { [weak self] image, error in[span_118](start_span)[span_118](end_span)
            guard let self = self, let snapshot = image else {[span_119](start_span)[span_119](end_span)
                self?.isAnalyzing = false[span_120](start_span)[span_120](end_span)
                self?.updateHUDWithError("畫面截圖失敗")[span_121](start_span)[span_121](end_span)
                return[span_122](start_span)[span_122](end_span)
            }

            let targetWidth: CGFloat = 720.0[span_123](start_span)[span_123](end_span)
            let scale = targetWidth / snapshot.size.width[span_124](start_span)[span_124](end_span)
            let targetSize = CGSize(width: targetWidth, height: snapshot.size.height * scale)[span_125](start_span)[span_125](end_span)

            UIGraphicsBeginImageContextWithOptions(targetSize, false, 1.0)[span_126](start_span)[span_126](end_span)
            snapshot.draw(in: CGRect(origin: .zero, size: targetSize))[span_127](start_span)[span_127](end_span)
            let resizedImage = UIGraphicsGetImageFromCurrentImageContext()[span_128](start_span)[span_128](end_span)
            UIGraphicsEndImageContext()[span_129](start_span)[span_129](end_span)

            guard let jpegData = resizedImage?.jpegData(compressionQuality: 0.85) else {[span_130](start_span)[span_130](end_span)
                self.isAnalyzing = false[span_131](start_span)[span_131](end_span)
                self.updateHUDWithError("影像壓縮失敗")[span_132](start_span)[span_132](end_span)
                return[span_133](start_span)[span_133](end_span)
            }

            self.callOpenAIAstra(base64Image: jpegData.base64EncodedString(), mode: mode)[span_134](start_span)[span_134](end_span)
        }
    }

    private func callOpenAIAstra(base64Image: String, mode: String) {
        guard let url = URL(string: "https://api.openai.com/v1/responses") else { return }[span_135](start_span)[span_135](end_span)

        var request = URLRequest(url: url)[span_136](start_span)[span_136](end_span)
        request.httpMethod = "POST[span_137](start_span)"[span_137](end_span)
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")[span_138](start_span)[span_138](end_span)
        request.setValue("Bearer \(openAIKey)", forHTTPHeaderField: "Authorization")[span_139](start_span)[span_139](end_span)
        request.timeoutInterval = 25[span_140](start_span)[span_140](end_span)

        let prompt = """
        你是頂尖百家樂視覺精算大師。當前模式設定為【\(mode.uppercased())】。
        【通用視覺辨識核心指引】：
        1. 觀察畫面中的路單走勢（大路、珠盤路、下三路）與即時比分。
        2. 嚴禁回答『畫面仍在載入』或『數據未知』！底欄數字清晰可見，務必識別真實比分填入 stats！
        3. 嚴禁輸出觀望！必須強制在【莊】與【閒】中二選一，信心度評估於 68%~92% 之間。
        嚴格僅輸出純 JSON 物件：
        {"pick":"莊","conf":80,"reason":"大路單跳形態，下三路齊整轉紅","stats":"庄16 闲16 和7 (39局)"}
        ""[span_141](start_span)"[span_141](end_span)

        let body: [String: Any] = [
            "model": "gpt-6-astra",[span_142](start_span)[span_142](end_span)
            "service_tier": "default",[span_143](start_span)[span_143](end_span)
            "reasoning": ["effort": "medium"],[span_144](start_span)[span_144](end_span)
            "input": [
                [
                    "role": "user",[span_145](start_span)[span_145](end_span)
                    "content": [
                        ["type": "input_text", "text": prompt],[span_146](start_span)[span_146](end_span)
                        ["type": "input_image", "image_url": "data:image/jpeg;base64,\(base64Image)"][span_147](start_span)[span_147](end_span)
                    ]
                ]
            ]
        ]

        request.httpBody = try? JSONSerialization.data(withJSONObject: body)[span_148](start_span)[span_148](end_span)

        URLSession.shared.dataTask(with: request) { [weak self] data, response, error in[span_149](start_span)[span_149](end_span)
            guard let self = self else { return }[span_150](start_span)[span_150](end_span)
            self.isAnalyzing = false[span_151](start_span)[span_151](end_span)

            if let error = error {[span_152](start_span)[span_152](end_span)
                self.updateHUDWithError(error.localizedDescription)[span_153](start_span)[span_153](end_span)
                return[span_154](start_span)[span_154](end_span)
            }

            guard let data = data, let rawStr = String(data: data, encoding: .utf8) else {[span_155](start_span)[span_155](end_span)
                self.updateHUDWithError("無回傳數據")[span_156](start_span)[span_156](end_span)
                return[span_157](start_span)[span_157](end_span)
            }

            let clean = self.extractJson(from: rawStr)[span_158](start_span)[span_158](end_span)
            self.updateHUDWithResult(clean)[span_159](start_span)[span_159](end_span)
        }.resume()[span_160](start_span)[span_160](end_span)
    }

    private func extractJson(from text: String) -> String {
        guard let start = text.firstIndex(of: "{"),[span_161](start_span)[span_161](end_span)
              let end = text.lastIndex(of: "}") else {[span_162](start_span)[span_162](end_span)
            return "{}[span_163](start_span)"[span_163](end_span)
        }
        return String(text[start...end])[span_164](start_span)[span_164](end_span)
    }

    private func updateHUDWithResult(_ jsonStr: String) {
        DispatchQueue.main.async { [weak self] in[span_165](start_span)[span_165](end_span)
            guard let data = jsonStr.data(using: .utf8),[span_166](start_span)[span_166](end_span)
                  let obj = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {[span_167](start_span)[span_167](end_span)
                self?.updateHUDWithError("解析格式異常")[span_168](start_span)[span_168](end_span)
                return[span_169](start_span)[span_169](start_span)[span_169](end_span)
            }

            let pick = obj["pick"] as? String ?? "莊[span_170](start_span)"[span_170](end_span)
            let conf = obj["conf"] as? Int ?? 65[span_171](start_span)[span_171](end_span)
            let reason = obj["reason"] as? String ?? "走勢推論[span_172](start_span)"[span_172](end_span)
            let stats = obj["stats"] as? String ?? "統計更新[span_173](start_span)"[span_173](end_span)

            let js = "window.__updateAI && window.__updateAI('\(pick)', \(conf), '\(reason)', '\(stats)');[span_174](start_span)"[span_174](end_span)
            self?.webView.evaluateJavaScript(js, completionHandler: nil)[span_175](start_span)[span_175](end_span)
        }
    }

    private func updateHUDWithError(_ msg: String) {
        DispatchQueue.main.async { [weak self] in[span_176](start_span)[span_176](end_span)
            let js = "window.__updateAIError && window.__updateAIError('\(msg)');[span_177](start_span)"[span_177](end_span)
            self?.webView.evaluateJavaScript(js, completionHandler: nil)[span_178](start_span)[span_178](end_span)
        }
    }

    func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
        if let currentUrl = webView.url?.absoluteString {[span_179](start_span)[span_179](end_span)
            urlTextField.text = currentUrl[span_180](start_span)[span_180](end_span)
        }
        injectAssistantScript()[span_181](start_span)[span_181](end_span)
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
            var defaultW = 215;
            var defaultH = 340;

            var hud = document.createElement('div');
            hud.id = 'slot-assistant-hud';
            hud.style.cssText = 'position:fixed;top:70px;right:10px;width:' + defaultW + 'px;height:' + defaultH + 'px;min-width:160px;max-width:380px;min-height:160px;max-height:85vh;background:rgba(11,17,32,0.96);border:1.5px solid #38bdf8;border-radius:10px;z-index:2147483647;color:#f1f5f9;font-size:11px;box-shadow:0 8px 30px rgba(0,0,0,0.9);font-family:sans-serif;user-select:none;-webkit-user-select:none;backdrop-filter:blur(8px);display:flex;flex-direction:column;box-sizing:border-box;overflow:hidden;';
            hud.innerHTML =
                '<div id="hud_header" style="padding:7px 8px;background:#1e293b;border-radius:9px 9px 0 0;font-weight:bold;color:#38bdf8;display:flex;justify-content:space-between;align-items:center;border-bottom:1px solid #334155;cursor:move;touch-action:none;flex-shrink:0;">' +
                    '<span id="hud_title_txt">👁 Astra 深度推論</span>' +
                    '<div style="display:flex;align-items:center;gap:3px;">' +
                        '<span id="hud_scale_m" style="cursor:pointer;color:#94a3b8;font-size:11px;font-weight:bold;padding:0 3px;">[-]</span>' +
                        '<span id="hud_scale_p" style="cursor:pointer;color:#94a3b8;font-size:11px;font-weight:bold;padding:0 3px;">[+]</span>' +
                        '<span id="hud_tog" style="cursor:pointer;color:#38bdf8;font-size:11px;font-weight:bold;padding:0 2px;">[收]</span>' +
                    '</div>' +
                '</div>' +
                '<div id="hud_content" style="padding:8px 8px 16px 8px;flex:1;min-height:0;overflow-y:auto;-webkit-overflow-scrolling:touch;">' +
                    '<div style="margin-bottom:6px;">' +
                        '<button id="nav_sponsor" style="width:100%;background:#0f172a;border:1px solid #f59e0b;color:#f59e0b;padding:5px 0;border-radius:4px;font-size:10px;font-weight:bold;cursor:pointer;">🪙 贊助作者</button>' +
                    '</div>' +
                    '<div style="background:rgba(15,23,42,0.85);border:1px solid #3b82f6;border-radius:6px;padding:8px 4px;text-align:center;margin-bottom:6px;">' +
                        '<div style="font-size:10px;color:#94a3b8;">🎯 深度路單精算建議</div>' +
                        '<div id=\"ai_pick_target\" style="font-size:18px;font-weight:900;color:#ef4444;margin:2px 0;">待命中</div>' +
                        '<div id=\"ai_pick_desc\" style="font-size:10px;color:#38bdf8;">點擊下方進行大路與下三路分析</div>' +
                    '</div>' +
                    '<button id="btn_do_ai" style="width:100%;background:#2563eb;color:#fff;border:none;padding:8px 0;border-radius:4px;font-weight:bold;margin-bottom:6px;font-size:11px;cursor:pointer;">📸 截圖畫面並由 AI 辨識</button>' +
                    '<div style="display:flex;justify-content:space-between;font-size:10px;color:#94a3b8;">' +
                        '<span id="hud_score">未掃描</span>' +
                        '<span id="hud_sync_dot" style="color:#4ade80;">● 連線</span>' +
                    '</div>' +
                    '<div style="margin-top:8px;padding-top:6px;border-top:1px solid #334155;display:flex;flex-direction:column;gap:5px;">' +
                        '<button id="btn_line" style="width:100%;background:#06c755;border:none;color:#fff;padding:5px 0;border-radius:4px;font-size:10px;font-weight:bold;cursor:pointer;">💬 LINE: @OSC168</button>' +
                        '<button id="btn_tg" style="width:100%;background:#0088cc;border:none;color:#fff;padding:5px 0;border-radius:4px;font-size:10px;font-weight:bold;cursor:pointer;">✈ Telegram: @TG_APK1</button>' +
                    '</div>' +
                    '<div style="margin-top:8px;padding-top:6px;border-top:1px dashed #475569;font-size:8.5px;color:#94a3b8;line-height:1.35;text-align:center;">' +
                        '<div style="color:#f87171;font-weight:bold;margin-bottom:2px;">🔞 未滿 18 歲禁止使用</div>' +
                        '<div style="color:#64748b;">【免責聲明】本系統僅供演算法模擬，不保證獲利。本應用嚴禁真實金錢交易。</div>' +
                    '</div>' +
                '</div>' +
                '<div id="hud_resize_handle" style="position:absolute;right:0;bottom:0;width:22px;height:22px;cursor:se-resize;display:flex;align-items:flex-end;justify-content:flex-end;padding:0 3px 2px 0;color:#38bdf8;font-size:12px;touch-action:none;opacity:0.9;z-index:99;">◢</div>';
            rootTarget.appendChild(hud);

            var header = document.getElementById('hud_header');
            var isDrag = false, sX, sY, iL, iT;
            header.addEventListener('touchstart', function(e) {
                if (e.target.closest('span')) { isDrag = false; return; }
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

            // 右下角雙軸（寬度 + 高度）同步觸控拉伸
            var handle = document.getElementById('hud_resize_handle');
            var isResizing = false, rStartX, rStartY, rStartW, rStartH;
            handle.addEventListener('touchstart', function(e) {
                e.stopPropagation(); e.preventDefault();
                isResizing = true;
                rStartX = e.touches[0].clientX; rStartY = e.touches[0].clientY;
                rStartW = hud.offsetWidth; rStartH = hud.offsetHeight;
            }, { passive: false });
            window.addEventListener('touchmove', function(e) {
                if (!isResizing) return;
                var deltaX = e.touches[0].clientX - rStartX;
                var deltaY = e.touches[0].clientY - rStartY;
                var targetW = Math.max(160, Math.min(window.innerWidth - 10, rStartW + deltaX));
                var targetH = Math.max(160, Math.min(window.innerHeight - 30, rStartH + deltaY));
                hud.style.width = targetW + 'px';
                hud.style.height = targetH + 'px';
                defaultW = targetW; defaultH = targetH;
            }, { passive: false });
            window.addEventListener('touchend', function() { isResizing = false; });

            bindTap(document.getElementById('hud_scale_m'), function() {
                defaultW = Math.max(160, hud.offsetWidth - 25);
                defaultH = Math.max(160, hud.offsetHeight - 40);
                hud.style.width = defaultW + 'px';
                hud.style.height = defaultH + 'px';
            });
            bindTap(document.getElementById('hud_scale_p'), function() {
                defaultW = Math.min(window.innerWidth - 10, hud.offsetWidth + 25);
                defaultH = Math.min(window.innerHeight - 30, hud.offsetHeight + 40);
                hud.style.width = defaultW + 'px';
                hud.style.height = defaultH + 'px';
            });

            var tog = document.getElementById('hud_tog');
            var cnt = document.getElementById('hud_content');
            var titleTxt = document.getElementById('hud_title_txt');
            bindTap(tog, function() {
                if (cnt.style.display === 'none') {
                    cnt.style.display = 'block'; handle.style.display = 'flex';
                    hud.style.width = defaultW + 'px'; hud.style.height = defaultH + 'px';
                    titleTxt.innerText = '👁 Astra 深度推論';
                    tog.innerText = '[收]';
                } else {
                    cnt.style.display = 'none'; handle.style.display = 'none';
                    hud.style.width = '76px'; hud.style.height = 'auto';
                    titleTxt.innerText = '👁';
                    tog.innerText = '[展]';
                }
            });

            bindTap(document.getElementById('nav_sponsor'), function() {
                var addr = 'TLz5EaP1rKUfdFu1iZectuDxCP1URNEivm';[span_182](start_span)[span_182](end_span)
                if (navigator.clipboard && navigator.clipboard.writeText) {
                    navigator.clipboard.writeText(addr).then(function() {
                        alert('已複製 USDT-TRC20 贊助地址：\\n' + addr);
                    }).catch(function() { prompt('USDT-TRC20 地址：', addr); });
                } else { prompt('USDT-TRC20 地址：', addr); }
            });

            function openLink(u) {
                if (window.AndroidBridge && window.AndroidBridge.openExternalUrl) {
                    window.AndroidBridge.openExternalUrl(u);
                } else { window.location.href = u; }
            }
            bindTap(document.getElementById('btn_line'), function() { openLink('https://lin.ee/NfoQ9DH'); });[span_183](start_span)[span_183](end_span)
            bindTap(document.getElementById('btn_tg'), function() { openLink('https://t.me/TG_apk1'); });[span_184](start_span)[span_184](end_span)

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
        ""[span_185](start_span)"[span_185](end_span)
        webView.evaluateJavaScript(js, completionHandler: nil)[span_186](start_span)[span_186](end_span)
    }
}
