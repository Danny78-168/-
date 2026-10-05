import UIKit
import WebKit

class ViewController: UIViewController, WKNavigationDelegate, UITextFieldViewNil {

    private let workerUrl = "https://openai.zhu90305.workers.dev/"
    
    private var webView: WKWebView!
    private var urlTextField: UITextField!
    private var hudView: UIView!
    private var hudBody: UIView!
    private var hudTitleLabel: UILabel!
    private var collapseButton: UIButton!
    
    private var tvStatusMain: UILabel!
    private var tvBoardCounts: UILabel!
    private var tvSpecialChances: UILabel!
    private var tvStatusSub: UILabel!
    private var tvBalance: UILabel!
    private var btnScanAi: UIButton!
    
    private var panelAi: UIView!
    private var panelService: UIView!
    private var tabAiBtn: UIButton!
    private var tabServiceBtn: UIButton!
    
    private var currentBalance: Double = 0.33
    private var isCollapsed: Bool = false

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = UIColor(hex: "#0B1120")
        
        setupTopBar()
        setupWebView()
        setupHudView()
        
        if let url = URL(string: "https://you888a.com/") {
            webView.load(URLRequest(url: url))
        }
        fetchRealBalance()
    }

    // MARK: - 頂部導航列
    private func setupTopBar() {
        let safeTop = UIApplication.shared.windows.first?.safeAreaInsets.top ?? 44
        let topBar = UIView(frame: CGRect(x: 0, y: 0, width: view.bounds.width, height: safeTop + 50))
        topBar.backgroundColor = UIColor(hex: "#0F172A")
        
        urlTextField = UITextField(frame: CGRect(x: 10, y: safeTop + 6, width: view.bounds.width - 150, height: 38))
        urlTextField.text = "https://you888a.com/"
        urlTextField.textColor = .white
        urlTextField.font = .systemFont(ofSize: 13)
        urlTextField.backgroundColor = UIColor(hex: "#1E293B")
        urlTextField.layer.borderColor = UIColor(hex: "#334155").cgColor
        urlTextField.layer.borderWidth = 1
        urlTextField.layer.cornerRadius = 6
        urlTextField.leftView = UIView(frame: CGRect(x: 0, y: 0, width: 8, height: 38))
        urlTextField.leftViewMode = .always
        topBar.addView(urlTextField)
        
        let btnRefresh = UIButton(frame: CGRect(x: view.bounds.width - 134, y: safeTop + 6, width: 70, height: 38))
        btnRefresh.setTitle("重新整理", for: .normal)
        btnRefresh.setTitleColor(.white, for: .normal)
        btnRefresh.titleLabel?.font = .boldSystemFont(ofSize: 12)
        btnRefresh.backgroundColor = UIColor(hex: "#334155")
        btnRefresh.layer.cornerRadius = 6
        btnRefresh.addTarget(self, action: #selector(handlePageRefresh), for: .touchUpInside)
        topBar.addView(btnRefresh)
        
        let btnGo = UIButton(frame: CGRect(x: view.bounds.width - 58, y: safeTop + 6, width: 48, height: 38))
        btnGo.setTitle("前往", for: .normal)
        btnGo.setTitleColor(.white, for: .normal)
        btnGo.titleLabel?.font = .boldSystemFont(ofSize: 12)
        btnGo.backgroundColor = UIColor(hex: "#2563EB")
        btnGo.layer.cornerRadius = 6
        btnGo.addTarget(self, action: #selector(handlePageGo), for: .touchUpInside)
        topBar.addView(btnGo)
        
        view.addSubview(topBar)
    }

    // MARK: - 遊戲 WKWebView
    private func setupWebView() {
        let safeTop = UIApplication.shared.windows.first?.safeAreaInsets.top ?? 44
        let config = WKWebViewConfiguration()
        config.allowsInlineMediaPlayback = true
        
        let webFrame = CGRect(x: 0, y: safeTop + 50, width: view.bounds.width, height: view.bounds.height - (safeTop + 50))
        webView = WKWebView(frame: webFrame, configuration: config)
        webView.navigationDelegate = self
        webView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        view.addSubview(webView)
    }

    // MARK: - 原生懸浮面板 (HUD)
    private func setupHudView() {
        let hudWidth: CGFloat = 260
        hudView = UIView(frame: CGRect(x: view.bounds.width - hudWidth - 12, y: 120, width: hudWidth, height: 380))
        hudView.backgroundColor = UIColor(hex: "#0B1120")
        hudView.layer.borderColor = UIColor(hex: "#38BDF8").cgColor
        hudView.layer.borderWidth = 1.5
        hudView.layer.cornerRadius = 10
        hudView.clipsToBounds = true
        
        // 拖曳手勢
        let panGesture = UIPanGestureRecognizer(target: self, action: #selector(handleHudPan(_:)))
        hudView.addGestureRecognizer(panGesture)
        
        // 頂部標題列
        let header = UIView(frame: CGRect(x: 0, y: 0, width: hudWidth, height: 36))
        header.backgroundColor = UIColor(hex: "#1E293B")
        
        hudTitleLabel = UILabel(frame: CGRect(x: 10, y: 0, width: hudWidth - 50, height: 36))
        hudTitleLabel.text = "👁 Astra 深度推論"
        hudTitleLabel.textColor = UIColor(hex: "#38BDF8")
        hudTitleLabel.font = .boldSystemFont(ofSize: 12)
        header.addSubview(hudTitleLabel)
        
        collapseButton = UIButton(frame: CGRect(x: hudWidth - 40, y: 0, width: 36, height: 36))
        collapseButton.setTitle("[收]", for: .normal)
        collapseButton.setTitleColor(UIColor(hex: "#38BDF8"), for: .normal)
        collapseButton.titleLabel?.font = .boldSystemFont(ofSize: 12)
        collapseButton.addTarget(self, action: #selector(toggleHudCollapse), for: .touchUpInside)
        header.addSubview(collapseButton)
        hudView.addSubview(header)
        
        // 內容容器
        hudBody = UIView(frame: CGRect(x: 0, y: 36, width: hudWidth, height: 344))
        
        // 分頁按鈕
        let tabBar = UIView(frame: CGRect(x: 0, y: 0, width: hudWidth, height: 32))
        tabBar.backgroundColor = UIColor(hex: "#0F172A")
        
        tabAiBtn = UIButton(frame: CGRect(x: 0, y: 0, width: hudWidth / 2, height: 32))
        tabAiBtn.setTitle("🎯 AI 精算", for: .normal)
        tabAiBtn.setTitleColor(UIColor(hex: "#38BDF8"), for: .normal)
        tabAiBtn.titleLabel?.font = .boldSystemFont(ofSize: 11)
        tabAiBtn.addTarget(self, action: #selector(switchTabToAi), for: .touchUpInside)
        tabBar.addSubview(tabAiBtn)
        
        tabServiceBtn = UIButton(frame: CGRect(x: hudWidth / 2, y: 0, width: hudWidth / 2, height: 32))
        tabServiceBtn.setTitle("💬 反饋客服", for: .normal)
        tabServiceBtn.setTitleColor(UIColor(hex: "#94A3B8"), for: .normal)
        tabServiceBtn.titleLabel?.font = .boldSystemFont(ofSize: 11)
        tabServiceBtn.addTarget(self, action: #selector(switchTabToService), for: .touchUpInside)
        tabBar.addSubview(tabServiceBtn)
        hudBody.addSubview(tabBar)
        
        // --- AI 面板 ---
        panelAi = UIView(frame: CGRect(x: 0, y: 32, width: hudWidth, height: 312))
        
        // 建議展示框
        let adviceBox = UIView(frame: CGRect(x: 8, y: 8, width: hudWidth - 16, height: 110))
        adviceBox.backgroundColor = UIColor(hex: "#0F172A")
        adviceBox.layer.borderColor = UIColor(hex: "#3B82F6").cgColor
        adviceBox.layer.borderWidth = 1
        adviceBox.layer.cornerRadius = 6
        
        tvStatusMain = UILabel(frame: CGRect(x: 0, y: 6, width: adviceBox.bounds.width, height: 26))
        tvStatusMain.text = "待命中"
        tvStatusMain.textAlignment = .center
        tvStatusMain.textColor = UIColor(hex: "#38BDF8")
        tvStatusMain.font = .boldSystemFont(ofSize: 17)
        adviceBox.addSubview(tvStatusMain)
        
        tvBoardCounts = UILabel(frame: CGRect(x: 0, y: 34, width: adviceBox.bounds.width, height: 16))
        tvBoardCounts.text = "盤面統計: 莊 -- | 閒 -- | 和 -- | 超6 --"
        tvBoardCounts.textAlignment = .center
        tvBoardCounts.textColor = UIColor(hex: "#E2E8F0")
        tvBoardCounts.font = .systemFont(ofSize: 9.5)
        adviceBox.addSubview(tvBoardCounts)
        
        tvSpecialChances = UILabel(frame: CGRect(x: 0, y: 52, width: adviceBox.bounds.width, height: 16))
        tvSpecialChances.text = "對子機會: -- | 超6: -- | 超和: --"
        tvSpecialChances.textAlignment = .center
        tvSpecialChances.textColor = UIColor(hex: "#FBBF24")
        tvSpecialChances.font = .boldSystemFont(ofSize: 9.5)
        adviceBox.addSubview(tvSpecialChances)
        
        tvStatusSub = UILabel(frame: CGRect(x: 4, y: 70, width: adviceBox.bounds.width - 8, height: 34))
        tvStatusSub.text = "進入牌桌後點擊下方按鈕由 Astra 推算"
        tvStatusSub.textAlignment = .center
        tvStatusSub.textColor = UIColor(hex: "#94A3B8")
        tvStatusSub.font = .systemFont(ofSize: 9.5)
        tvStatusSub.numberOfLines = 2
        adviceBox.addSubview(tvStatusSub)
        panelAi.addSubview(adviceBox)
        
        // 費用與餘額框
        let costBox = UIView(frame: CGRect(x: 8, y: 124, width: hudWidth - 16, height: 74))
        costBox.backgroundColor = UIColor(hex: "#080D1A")
        costBox.layer.borderColor = UIColor(hex: "#1E293B").cgColor
        costBox.layer.borderWidth = 1
        costBox.layer.cornerRadius = 6
        
        let tokenLabel = UILabel(frame: CGRect(x: 8, y: 4, width: 140, height: 16))
        tokenLabel.text = "Token: In 1,480 / Out 120"
        tokenLabel.textColor = UIColor(hex: "#38BDF8")
        tokenLabel.font = .systemFont(ofSize: 9.5)
        costBox.addSubview(tokenLabel)
        
        let costTag = UILabel(frame: CGRect(x: costBox.bounds.width - 80, y: 4, width: 72, height: 16))
        costTag.text = "本次: $0.0086"
        costTag.textAlignment = .right
        costTag.textColor = UIColor(hex: "#F59E0B")
        costTag.font = .boldSystemFont(ofSize: 9.5)
        costBox.addSubview(costTag)
        
        let balTitle = UILabel(frame: CGRect(x: 8, y: 26, width: 55, height: 18))
        balTitle.text = "真實餘額: "
        balTitle.textColor = UIColor(hex: "#94A3B8")
        balTitle.font = .systemFont(ofSize: 10)
        costBox.addSubview(balTitle)
        
        tvBalance = UILabel(frame: CGRect(x: 63, y: 26, width: 75, height: 18))
        tvBalance.text = "$0.33 USD"
        tvBalance.textColor = UIColor(hex: "#22C55E")
        tvBalance.font = .boldSystemFont(ofSize: 10)
        costBox.addSubview(tvBalance)
        
        // 餘額刷新按鈕 [🔄]
        let btnRefreshBal = UIButton(frame: CGRect(x: 138, y: 24, width: 26, height: 22))
        btnRefreshBal.setTitle("🔄", for: .normal)
        btnRefreshBal.titleLabel?.font = .systemFont(ofSize: 11)
        btnRefreshBal.addTarget(self, action: #selector(handleRefreshBalanceClick), for: .touchUpInside)
        
        let longPress = UILongPressGestureRecognizer(target: self, action: #selector(handleBalanceLongPress(_:)))
        btnRefreshBal.addGestureRecognizer(longPress)
        costBox.addSubview(btnRefreshBal)
        
        let minTag = UILabel(frame: CGRect(x: costBox.bounds.width - 80, y: 26, width: 72, height: 18))
        minTag.text = "(最低需 $0.01)"
        minTag.textAlignment = .right
        minTag.textColor = UIColor(hex: "#64748B")
        minTag.font = .systemFont(ofSize: 9)
        costBox.addSubview(minTag)
        
        let statsLine = UILabel(frame: CGRect(x: 0, y: 50, width: costBox.bounds.width, height: 16))
        statsLine.text = "累計調用: 3,466 次 | 視覺推演模式已就緒"
        statsLine.textAlignment = .center
        statsLine.textColor = UIColor(hex: "#64748B")
        statsLine.font = .systemFont(ofSize: 9)
        costBox.addSubview(statsLine)
        panelAi.addSubview(costBox)
        
        // 辨識按鈕
        btnScanAi = UIButton(frame: CGRect(x: 8, y: 206, width: hudWidth - 16, height: 38))
        btnScanAi.setTitle("📸 截圖畫面並由 AI 辨識", for: .normal)
        btnScanAi.setTitleColor(.white, for: .normal)
        btnScanAi.titleLabel?.font = .boldSystemFont(ofSize: 11)
        btnScanAi.backgroundColor = UIColor(hex: "#2563EB")
        btnScanAi.layer.cornerRadius = 6
        btnScanAi.addTarget(self, action: #selector(handleScanAiClick), for: .touchUpInside)
        panelAi.addSubview(btnScanAi)
        
        hudBody.addSubview(panelAi)
        
        // --- 客服面板 ---
        panelService = UIView(frame: CGRect(x: 0, y: 32, width: hudWidth, height: 312))
        panelService.isHidden = true
        
        let btnTg = UIButton(frame: CGRect(x: 8, y: 12, width: hudWidth - 16, height: 36))
        btnTg.setTitle("✈ Telegram: @TG_APK1", for: .normal)
        btnTg.backgroundColor = UIColor(hex: "#0284C7")
        btnTg.titleLabel?.font = .boldSystemFont(ofSize: 11)
        btnTg.layer.cornerRadius = 4
        btnTg.addTarget(self, action: #selector(openTg), for: .touchUpInside)
        panelService.addSubview(btnTg)
        
        let btnLine = UIButton(frame: CGRect(x: 8, y: 56, width: hudWidth - 16, height: 36))
        btnLine.setTitle("💬 LINE 客服: @OSC168", for: .normal)
        btnLine.backgroundColor = UIColor(hex: "#16A34A")
        btnLine.titleLabel?.font = .boldSystemFont(ofSize: 11)
        btnLine.layer.cornerRadius = 4
        btnLine.addTarget(self, action: #selector(openLine), for: .touchUpInside)
        panelService.addSubview(btnLine)
        hudBody.addSubview(panelService)
        
        hudView.addSubview(hudBody)
        view.addSubview(hudView)
    }

    // MARK: - 核心動作：截圖 ➔ 720px 縮放 ➔ 上傳推論
    @objc private func handleScanAiClick() {
        guard currentBalance >= 0.0086 else {
            tvStatusMain.text = "餘額不足"
            tvStatusMain.textColor = UIColor(hex: "#EF4444")
            tvStatusSub.text = "API 餘額低於 $0.01，請點擊 🔄 刷新或長按儲值"
            return
        }
        
        btnScanAi.setTitle("📸 擷取畫面中...", for: .normal)
        btnScanAi.isEnabled = false
        hudView.isHidden = true
        
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.05) {
            let config = WKSnapshotConfiguration()
            config.snapshotWidth = self.webView.bounds.width as NSNumber
            
            self.webView.takeSnapshot(with: config) { [weak self] (image, error) in
                guard let self = self else { return }
                self.hudView.isHidden = false
                
                guard let originalImage = image, error == nil else {
                    self.btnScanAi.setTitle("📸 截圖畫面並由 AI 辨識", for: .normal)
                    self.btnScanAi.isEnabled = true
                    self.tvStatusMain.text = "截圖失敗"
                    self.tvStatusSub.text = "WebView 截圖錯誤，請重試"
                    return
                }
                
                self.btnScanAi.setTitle("Astra 推演中...", for: .normal)
                
                // 等比縮放至 720px 寬度，降低延遲
                let targetWidth: CGFloat = 720.0
                let scale = targetWidth / originalImage.size.width
                let targetHeight = originalImage.size.height * scale
                let targetSize = CGSize(width: targetWidth, height: targetHeight)
                
                UIGraphicsBeginImageContextWithOptions(targetSize, false, 1.0)
                originalImage.draw(in: CGRect(origin: .zero, size: targetSize))
                let scaledImage = UIGraphicsGetImageFromCurrentImageContext()
                UIGraphicsEndImageContext()
                
                guard let finalImg = scaledImage,
                      let jpegData = finalImg.jpegData(compressionQuality: 0.7) else { return }
                
                let base64String = jpegData.base64EncodedString()
                self.sendScreenshotToWorker(base64Image: base64String)
            }
        }
    }

    private func sendScreenshotToWorker(base64Image: String) {
        guard let url = URL(string: workerUrl) else { return }
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.timeoutInterval = 10.0 // 10秒硬性逾時
        
        let json: [String: Any] = ["image": base64Image, "user": "default_user"]
        request.httpBody = try? JSONSerialization.data(withJSONObject: json)
        
        URLSession.shared.dataTask(with: request) { [weak self] data, response, error in
            guard let self = self else { return }
            
            DispatchQueue.main.async {
                self.btnScanAi.setTitle("📸 截圖畫面並由 AI 辨識", for: .normal)
                self.btnScanAi.isEnabled = true
                
                if let error = error {
                    self.tvStatusMain.text = "連線逾時"
                    self.tvStatusMain.textColor = UIColor(hex: "#EF4444")
                    self.tvStatusSub.text = "已達連線限制: \(error.localizedDescription)"
                    return
                }
                
                guard let data = data,
                      let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else { return }
                
                if let status = json["status"] as? String, status == "success" {
                    let analysis = json["analysis"] as? String ?? ""
                    let bal = json["balance"] as? Double ?? self.currentBalance
                    self.currentBalance = bal
                    
                    let lines = analysis.components(separatedBy: "\n")
                    if lines.count > 0 {
                        let p1 = lines[0].replacingOccurrences(of: "【", with: "").replacingOccurrences(of: "】", with: "").trimmingCharacters(in: .whitespaces)
                        self.tvStatusMain.text = p1
                        self.tvStatusMain.textColor = p1.contains("莊") ? UIColor(hex: "#EF4444") : UIColor(hex: "#38BDF8")
                    }
                    if lines.count > 1 { self.tvBoardCounts.text = lines[1].trimmingCharacters(in: .whitespaces) }
                    if lines.count > 2 { self.tvSpecialChances.text = lines[2].trimmingCharacters(in: .whitespaces) }
                    if lines.count > 3 { self.tvStatusSub.text = lines[3].trimmingCharacters(in: .whitespaces) }
                    
                    self.tvBalance.text = String(format: "$%.4f USD", bal)
                    self.tvBalance.textColor = bal >= 0.01 ? UIColor(hex: "#22C55E") : UIColor(hex: "#EF4444")
                } else {
                    let msg = json["message"] as? String ?? "推演失敗"
                    self.tvStatusMain.text = "推演失敗"
                    self.tvStatusMain.textColor = UIColor(hex: "#EF4444")
                    self.tvStatusSub.text = msg
                    if let bal = json["balance"] as? Double {
                        self.currentBalance = bal
                        self.tvBalance.text = String(format: "$%.4f USD", bal)
                        self.tvBalance.textColor = UIColor(hex: "#EF4444")
                    }
                }
            }
        }.resume()
    }

    // MARK: - 餘額查詢與強制刷新
    @objc private func handleRefreshBalanceClick() {
        tvBalance.text = "更新中..."
        fetchRealBalance()
    }
    
    @objc private func handleBalanceLongPress(_ gesture: UILongPressGestureRecognizer) {
        guard gesture.state == .began else { return }
        
        let alert = UIAlertController(title: "手動校正真實餘額", message: "請輸入最新儲值金額 (例如: 5.00)", preferredStyle: .alert)
        alert.addTextField { tf in
            tf.placeholder = "5.00"
            tf.keyboardType = .decimalPad
        }
        alert.addAction(UIAlertAction(title: "取消", style: .cancel))
        alert.addAction(UIAlertAction(title: "更新", style: .default, handler: { [weak self] _ in
            guard let self = self, let text = alert.textFields?.first?.text, !text.isEmpty else { return }
            if let url = URL(string: "\(self.workerUrl)?set=\(text)") {
                URLSession.shared.dataTask(with: url) { _, _, _ in
                    self.fetchRealBalance()
                }.resume()
            }
        }))
        present(alert, animated: true)
    }

    private func fetchRealBalance() {
        guard let url = URL(string: workerUrl) else { return }
        var request = URLRequest(url: url)
        request.timeoutInterval = 5.0
        
        URLSession.shared.dataTask(with: request) { [weak self] data, _, _ in
            guard let self = self, let data = data,
                  let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
                  let bal = json["balance"] as? Double else { return }
            
            DispatchQueue.main.async {
                self.currentBalance = bal
                self.tvBalance.text = String(format: "$%.4f USD", bal)
                self.tvBalance.textColor = bal >= 0.01 ? UIColor(hex: "#22C55E") : UIColor(hex: "#EF4444")
            }
        }.resume()
    }

    // MARK: - 輔助互動
    @objc private func toggleHudCollapse() {
        isCollapsed.toggle()
        if isCollapsed {
            hudBody.isHidden = true
            hudTitleLabel.text = "👁"
            collapseButton.setTitle("[展]", for: .normal)
            hudView.frame.size = CGSize(width: 75, height: 36)
        } else {
            hudBody.isHidden = false
            hudTitleLabel.text = "👁 Astra 深度推論"
            collapseButton.setTitle("[收]", for: .normal)
            hudView.frame.size = CGSize(width: 260, height: 380)
        }
    }

    @objc private func switchTabToAi() {
        tabAiBtn.setTitleColor(UIColor(hex: "#38BDF8"), for: .normal)
        tabServiceBtn.setTitleColor(UIColor(hex: "#94A3B8"), for: .normal)
        panelAi.isHidden = false
        panelService.isHidden = true
    }

    @objc private func switchTabToService() {
        tabServiceBtn.setTitleColor(UIColor(hex: "#38BDF8"), for: .normal)
        tabAiBtn.setTitleColor(UIColor(hex: "#94A3B8"), for: .normal)
        panelAi.isHidden = true
        panelService.isHidden = false
    }

    @objc private func handleHudPan(_ gesture: UIPanGestureRecognizer) {
        let translation = gesture.translation(in: view)
        var newCenter = CGPoint(x: hudView.center.x + translation.x, y: hudView.center.y + translation.y)
        
        let minX = hudView.bounds.width / 2
        let maxX = view.bounds.width - minX
        let minY = (hudView.bounds.height / 2) + 50
        let maxY = view.bounds.height - (hudView.bounds.height / 2)
        
        newCenter.x = max(minX, min(newCenter.x, maxX))
        newCenter.y = max(minY, min(newCenter.y, maxY))
        
        hudView.center = newCenter
        gesture.setTranslation(.zero, in: view)
    }

    @objc private func handlePageRefresh() { webView.reload(); fetchRealBalance() }
    @objc private func handlePageGo() {
        guard var text = urlTextField.text?.trimmingCharacters(in: .whitespaces), !text.isEmpty else { return }
        if !text.hasPrefix("http://") && !text.hasPrefix("https://") { text = "https://" + text }
        if let url = URL(string: text) { webView.load(URLRequest(url: url)) }
    }
    
    @objc private func openTg() {
        if let url = URL(string: "https://t.me/TG_APK1") { UIApplication.shared.open(url) }
    }
    @objc private func openLine() {
        if let url = URL(string: "https://lin.ee/NfoQ9DH") { UIApplication.shared.open(url) }
    }
}

// MARK: - Hex 顏色擴充
extension UIColor {
    convenience init(hex: String) {
        var cString = hex.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
        if cString.hasPrefix("#") { cString.remove(at: cString.startIndex) }
        var rgbValue: UInt64 = 0
        Scanner(string: cString).scanHexInt64(&rgbValue)
        self.init(
            red: CGFloat((rgbValue & 0xFF0000) >> 16) / 255.0,
            green: CGFloat((rgbValue & 0x00FF00) >> 8) / 255.0,
            blue: CGFloat(rgbValue & 0x0000FF) / 255.0,
            alpha: 1.0
        )
    }
}
