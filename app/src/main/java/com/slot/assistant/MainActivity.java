package com.slot.assistant;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity {

    // 預設為使用者原創旗艦模型 gpt-6-astra
    private String selectedModel = "gpt-6-astra";
    private boolean isUnlocked = false;

    // UI 元件
    private Button btnModelAstra;
    private Button btnModelSol;
    private Button btnModelLuna;
    private TextView tvModelStatus;

    // 四大即時機率指示器 (超6、龍7、和局、對子)
    private TextView tvProbSuper6;
    private TextView tvProbDragon7;
    private TextView tvProbTie;
    private TextView tvProbPairs;

    private TextView tvPick;
    private TextView tvConf;
    private TextView tvAiResult;

    private EditText etApiKey;
    private EditText etAdminKey;
    private EditText etShoeData;
    private ProgressBar progressBar;
    private Button btnAnalyze;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("AstraConfig", Context.MODE_PRIVATE);
        setContentView(buildRootLayout());

        String savedKey = prefs.getString("openai_key", "");
        if (!savedKey.isEmpty()) {
            etApiKey.setText(savedKey);
        }
    }

    private View buildRootLayout() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(Color.parseColor("#0B1120"));
        scrollView.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(28), dp(16), dp(32));

        // 1. App 標題
        TextView tvTitle = new TextView(this);
        tvTitle.setText("ASTRA ASSISTANT");
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        tvTitle.setTextColor(Color.parseColor("#38BDF8"));
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setGravity(Gravity.CENTER);
        root.addView(tvTitle);

        TextView tvSubtitle = new TextView(this);
        tvSubtitle.setText("AI 視覺精算大師 · 走勢與特殊盤率即時雲端矩陣");
        tvSubtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvSubtitle.setTextColor(Color.parseColor("#94A3B8"));
        tvSubtitle.setGravity(Gravity.CENTER);
        tvSubtitle.setPadding(0, dp(4), 0, dp(16));
        root.addView(tvSubtitle);

        // 2. OpenAI API Key & 管理員授權
        etApiKey = new EditText(this);
        etApiKey.setHint("輸入 OpenAI API Key (sk-...)");
        etApiKey.setHintTextColor(Color.parseColor("#64748B"));
        etApiKey.setTextColor(Color.WHITE);
        etApiKey.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        etApiKey.setBackground(createInputBackground());
        etApiKey.setPadding(dp(12), dp(10), dp(12), dp(10));
        root.addView(etApiKey);

        LinearLayout authRow = new LinearLayout(this);
        authRow.setOrientation(LinearLayout.HORIZONTAL);
        authRow.setPadding(0, dp(6), 0, dp(14));

        etAdminKey = new EditText(this);
        etAdminKey.setHint("管理員金鑰 (OSC-ADMIN-8888)");
        etAdminKey.setHintTextColor(Color.parseColor("#64748B"));
        etAdminKey.setTextColor(Color.WHITE);
        etAdminKey.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        etAdminKey.setBackground(createInputBackground());
        etAdminKey.setPadding(dp(10), dp(8), dp(10), dp(8));
        LinearLayout.LayoutParams adminLp = new LinearLayout.LayoutParams(0, dp(42), 1.8f);
        authRow.addView(etAdminKey, adminLp);

        Button btnUnlock = createStyledButton("驗證解鎖", false);
        LinearLayout.LayoutParams btnUnlockLp = new LinearLayout.LayoutParams(0, dp(42), 1.0f);
        btnUnlockLp.setMargins(dp(6), 0, 0, 0);
        authRow.addView(btnUnlock, btnUnlockLp);
        root.addView(authRow);

        btnUnlock.setOnClickListener(v -> {
            String code = etAdminKey.getText().toString().trim();
            if ("OSC-ADMIN-8888".equals(code)) {
                isUnlocked = true;
                Toast.makeText(this, "管理員模式已啟動！全模型高階運算權限已解鎖", Toast.LENGTH_SHORT).show();
                etAdminKey.setText("已驗證 (OSC-ADMIN)");
                etAdminKey.setEnabled(false);
            } else {
                Toast.makeText(this, "驗證碼無效", Toast.LENGTH_SHORT).show();
            }
        });

        // 3. 三大 AI 核心模型切換 (gpt-6-astra / gpt-6.1-sol / gpt-6-luna)
        TextView tvModelHeader = new TextView(this);
        tvModelHeader.setText("【AI 核心運算引擎切換】");
        tvModelHeader.setTextColor(Color.parseColor("#E2E8F0"));
        tvModelHeader.setTypeface(null, Typeface.BOLD);
        tvModelHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        root.addView(tvModelHeader);

        LinearLayout modelRow = new LinearLayout(this);
        modelRow.setOrientation(LinearLayout.HORIZONTAL);
        modelRow.setPadding(0, dp(6), 0, dp(6));

        btnModelAstra = createStyledButton("ASTRA\n(視覺精算)", true);
        btnModelSol = createStyledButton("SOL\n(深度推理)", false);
        btnModelLuna = createStyledButton("LUNA\n(即時捕捉)", false);

        LinearLayout.LayoutParams btnParamAstra = new LinearLayout.LayoutParams(0, dp(50), 1f);
        btnParamAstra.setMargins(dp(2), 0, dp(2), 0);
        LinearLayout.LayoutParams btnParamSol = new LinearLayout.LayoutParams(0, dp(50), 1f);
        btnParamSol.setMargins(dp(2), 0, dp(2), 0);
        LinearLayout.LayoutParams btnParamLuna = new LinearLayout.LayoutParams(0, dp(50), 1f);
        btnParamLuna.setMargins(dp(2), 0, dp(2), 0);

        modelRow.addView(btnModelAstra, btnParamAstra);
        modelRow.addView(btnModelSol, btnParamSol);
        modelRow.addView(btnModelLuna, btnParamLuna);
        root.addView(modelRow);

        tvModelStatus = new TextView(this);
        tvModelStatus.setText("當前引擎: gpt-6-astra (原創頂尖視覺精算旗艦)");
        tvModelStatus.setTextColor(Color.parseColor("#38BDF8"));
        tvModelStatus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvModelStatus.setPadding(dp(4), dp(2), dp(4), dp(12));
        root.addView(tvModelStatus);

        btnModelAstra.setOnClickListener(v -> selectEngine("gpt-6-astra"));
        btnModelSol.setOnClickListener(v -> selectEngine("gpt-6.1-sol"));
        btnModelLuna.setOnClickListener(v -> selectEngine("gpt-6-luna"));

        // 4. AI 即時落點指標 (下局方向 + 信心度)
        LinearLayout verdictRow = new LinearLayout(this);
        verdictRow.setOrientation(LinearLayout.HORIZONTAL);
        verdictRow.setBackground(createCardBackground());
        verdictRow.setPadding(dp(14), dp(10), dp(14), dp(10));

        LinearLayout vCol1 = new LinearLayout(this);
        vCol1.setOrientation(LinearLayout.VERTICAL);
        TextView tvPickLabel = new TextView(this);
        tvPickLabel.setText("AI 建議落點");
        tvPickLabel.setTextColor(Color.parseColor("#94A3B8"));
        tvPickLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvPick = new TextView(this);
        tvPick.setText("--");
        tvPick.setTextColor(Color.parseColor("#F59E0B"));
        tvPick.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        tvPick.setTypeface(null, Typeface.BOLD);
        vCol1.addView(tvPickLabel);
        vCol1.addView(tvPick);
        verdictRow.addView(vCol1, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout vCol2 = new LinearLayout(this);
        vCol2.setOrientation(LinearLayout.VERTICAL);
        TextView tvConfLabel = new TextView(this);
        tvConfLabel.setText("信心指數");
        tvConfLabel.setTextColor(Color.parseColor("#94A3B8"));
        tvConfLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvConf = new TextView(this);
        tvConf.setText("-- %");
        tvConf.setTextColor(Color.parseColor("#10B981"));
        tvConf.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        tvConf.setTypeface(null, Typeface.BOLD);
        vCol2.addView(tvConfLabel);
        vCol2.addView(tvConf);
        verdictRow.addView(vCol2, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout.LayoutParams verdictParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        verdictParams.setMargins(0, 0, 0, dp(12));
        root.addView(verdictRow, verdictParams);

        // 5. 特殊盤路機率面板 (由 AI 即時精算回傳)
        TextView tvStatsHeader = new TextView(this);
        tvStatsHeader.setText("【AI 盤路特殊機率精算】");
        tvStatsHeader.setTextColor(Color.parseColor("#E2E8F0"));
        tvStatsHeader.setTypeface(null, Typeface.BOLD);
        tvStatsHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        root.addView(tvStatsHeader);

        LinearLayout llStatsContainer = new LinearLayout(this);
        llStatsContainer.setOrientation(LinearLayout.VERTICAL);
        llStatsContainer.setBackground(createCardBackground());
        llStatsContainer.setPadding(dp(14), dp(10), dp(14), dp(10));

        tvProbSuper6 = createMetricRow(llStatsContainer, "超 6 (Super 6)", "等待 AI 精算");
        tvProbDragon7 = createMetricRow(llStatsContainer, "龍 7 (Dragon 7)", "等待 AI 精算");
        tvProbTie = createMetricRow(llStatsContainer, "和 局 (Tie)", "等待 AI 精算");
        tvProbPairs = createMetricRow(llStatsContainer, "對 子 (Pairs)", "等待 AI 精算");

        LinearLayout.LayoutParams statsParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        statsParams.setMargins(0, dp(6), 0, dp(14));
        root.addView(llStatsContainer, statsParams);

        // 6. 輸入靴路與分析指令
        etShoeData = new EditText(this);
        etShoeData.setHint("輸入路單 (如: 莊,莊,閒,莊,和,莊,超6,龍7...)");
        etShoeData.setHintTextColor(Color.parseColor("#64748B"));
        etShoeData.setTextColor(Color.WHITE);
        etShoeData.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        etShoeData.setBackground(createInputBackground());
        etShoeData.setPadding(dp(12), dp(12), dp(12), dp(12));
        root.addView(etShoeData);

        btnAnalyze = createStyledButton("交由 AI 全權雲端運算", true);
        LinearLayout.LayoutParams runParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(48));
        runParams.setMargins(0, dp(8), 0, dp(10));
        root.addView(btnAnalyze, runParams);

        progressBar = new ProgressBar(this);
        progressBar.setVisibility(View.GONE);
        root.addView(progressBar);

        // 7. AI 完整報告
        tvAiResult = new TextView(this);
        tvAiResult.setText("請確認輸入 API Key 與靴路資料，點擊按鈕呼叫 AI 運算。");
        tvAiResult.setTextColor(Color.parseColor("#CBD5E1"));
        tvAiResult.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvAiResult.setBackground(createCardBackground());
        tvAiResult.setPadding(dp(14), dp(14), dp(14), dp(14));
        root.addView(tvAiResult);

        btnAnalyze.setOnClickListener(v -> triggerOpenAiAnalysis());

        scrollView.addView(root);
        return scrollView;
    }

    private void selectEngine(String modelName) {
        this.selectedModel = modelName;
        btnModelAstra.setBackground(modelName.equals("gpt-6-astra") ? createSelectedButtonBackground() : createUnselectedButtonBackground());
        btnModelSol.setBackground(modelName.equals("gpt-6.1-sol") ? createSelectedButtonBackground() : createUnselectedButtonBackground());
        btnModelLuna.setBackground(modelName.equals("gpt-6-luna") ? createSelectedButtonBackground() : createUnselectedButtonBackground());

        if (modelName.equals("gpt-6-astra")) {
            tvModelStatus.setText("當前引擎: gpt-6-astra (原創頂尖視覺精算旗艦)");
        } else if (modelName.equals("gpt-6.1-sol")) {
            tvModelStatus.setText("當前引擎: gpt-6.1-sol (深度長跳邏輯推理)");
        } else {
            tvModelStatus.setText("當前引擎: gpt-6-luna (極速捕捉即時響應)");
        }
        Toast.makeText(this, "已切換為核心引擎: " + modelName, Toast.LENGTH_SHORT).show();
    }

    private void triggerOpenAiAnalysis() {
        String apiKey = etApiKey.getText().toString().trim();
        String shoe = etShoeData.getText().toString().trim();

        if (apiKey.isEmpty()) {
            Toast.makeText(this, "請先輸入 OpenAI API Key", Toast.LENGTH_SHORT).show();
            return;
        }

        if (shoe.isEmpty()) {
            shoe = "莊, 莊, 閒, 莊, 和, 莊, 超6, 閒, 莊, 龍7";
            etShoeData.setText(shoe);
        }

        prefs.edit().putString("openai_key", apiKey).apply();

        btnAnalyze.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        tvAiResult.setText("正在由 OpenAI [" + selectedModel + "] 進行全量雲端走勢推演與特殊機率精算...");

        final String currentShoe = shoe;
        final String currentApiKey = apiKey;
        final String modelToUse = selectedModel;

        new Thread(() -> {
            try {
                String rawResponse = callOpenAIResponses(currentApiKey, modelToUse, currentShoe, "");
                mainHandler.post(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnAnalyze.setEnabled(true);
                    processAiJsonResponse(rawResponse);
                });
            } catch (Exception e) {
                final String err = e.getMessage();
                mainHandler.post(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnAnalyze.setEnabled(true);
                    tvAiResult.setText("AI 運算失敗:\n" + err + "\n\n請確認 API Key 餘額或權限。");
                });
            }
        }).start();
    }

    // 依據原始架構 callOpenAIAstra 升級的標準 /v1/responses API 請求
    private String callOpenAIResponses(String apiKey, String model, String shoe, String base64Image) throws Exception {
        URL url = new URL("https://api.openai.com/v1/responses");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setConnectTimeout(25000);
        conn.setReadTimeout(35000);
        conn.setDoOutput(true);

        JSONObject jsonBody = new JSONObject();
        jsonBody.put("model", model);
        jsonBody.put("service_tier", "default");

        JSONObject reasoningObj = new JSONObject();
        reasoningObj.put("effort", "medium");
        jsonBody.put("reasoning", reasoningObj);

        JSONArray inputArray = new JSONArray();
        JSONObject inputItem = new JSONObject();
        inputItem.put("role", "user");

        JSONArray contentArray = new JSONArray();

        String prompt = "你是頂尖百家樂視覺精算大師。當前模型引擎設定為【" + model.toUpperCase() + "】。\n"
                + "【靴路數據】: " + shoe + "\n\n"
                + "【任務要求】:\n"
                + "請勿套用任何預設死板公式，必須全權依據上述靴路走勢、單跳與長連頻率，即時精算出下一局落點與各項特殊盤機率。\n"
                + "嚴格僅輸出純 JSON 物件，格式如下（不得附加任何 markdown 標籤或額外文字）：\n"
                + "{\n"
                + "  \"pick\": \"莊\",\n"
                + "  \"conf\": 82,\n"
                + "  \"super6\": \"5.8%\",\n"
                + "  \"dragon7\": \"2.4%\",\n"
                + "  \"tie\": \"9.8%\",\n"
                + "  \"pairs\": \"7.6%\",\n"
                + "  \"reason\": \"大路處於單跳形態，下三路齊整，超6臨界點顯著抬升。\",\n"
                + "  \"stats\": \"依據當前靴路形態完成 AI 即時雲端精算\"\n"
                + "}";

        JSONObject textObj = new JSONObject();
        textObj.put("type", "input_text");
        textObj.put("text", prompt);
        contentArray.put(textObj);

        if (base64Image != null && !base64Image.isEmpty()) {
            JSONObject imgObj = new JSONObject();
            imgObj.put("type", "input_image");
            imgObj.put("image_url", "data:image/jpeg;base64," + base64Image);
            contentArray.put(imgObj);
        }

        inputItem.put("content", contentArray);
        inputArray.put(inputItem);
        jsonBody.put("input", inputArray);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.toString().getBytes(StandardCharsets.UTF_8));
            os.flush();
        }

        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();

        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();

        if (code != 200 && code != 201) {
            throw new Exception("HTTP " + code + ": " + sb.toString());
        }

        return sb.toString();
    }

    private void processAiJsonResponse(String rawResponse) {
        try {
            JSONObject root = new JSONObject(rawResponse);
            String content = "";

            if (root.has("output_text")) {
                content = root.getString("output_text");
            } else if (root.has("output")) {
                content = root.getString("output");
            } else if (root.has("choices")) {
                content = root.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
            } else {
                content = rawResponse;
            }

            // 清理可能包含的 markdown 標籤
            content = content.replace("```json", "").replace("```", "").trim();

            JSONObject aiJson = new JSONObject(content);

            String pick = aiJson.optString("pick", "--");
            int conf = aiJson.optInt("conf", 0);
            String super6 = aiJson.optString("super6", "--");
            String dragon7 = aiJson.optString("dragon7", "--");
            String tie = aiJson.optString("tie", "--");
            String pairs = aiJson.optString("pairs", "--");
            String reason = aiJson.optString("reason", "無分析理由");
            String stats = aiJson.optString("stats", "");

            tvPick.setText("【" + pick + "】");
            tvConf.setText(conf + "%");
            tvProbSuper6.setText(super6 + " (AI精算)");
            tvProbDragon7.setText(dragon7 + " (AI精算)");
            tvProbTie.setText(tie + " (AI精算)");
            tvProbPairs.setText(pairs + " (AI精算)");

            tvAiResult.setText("【" + selectedModel.toUpperCase() + " 雲端精算結果】\n"
                    + "• 落點建議: " + pick + " (信心度: " + conf + "%)\n"
                    + "• 分析依據: " + reason + "\n"
                    + "• 狀態資訊: " + stats + "\n"
                    + "• 模式狀態: " + (isUnlocked ? "ADMIN 全功能解鎖" : "訪客預覽模式"));

        } catch (Exception e) {
            tvAiResult.setText("【AI 原始輸出解析】\n" + rawResponse);
        }
    }

    private TextView createMetricRow(LinearLayout parent, String label, String defaultVal) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(4), 0, dp(4));

        TextView tvLabel = new TextView(this);
        tvLabel.setText(label);
        tvLabel.setTextColor(Color.parseColor("#94A3B8"));
        tvLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.2f);
        row.addView(tvLabel, lp1);

        TextView tvValue = new TextView(this);
        tvValue.setText(defaultVal);
        tvValue.setTextColor(Color.parseColor("#38BDF8"));
        tvValue.setTypeface(null, Typeface.BOLD);
        tvValue.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvValue.setGravity(Gravity.END);
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.8f);
        row.addView(tvValue, lp2);

        parent.addView(row);
        return tvValue;
    }

    private Button createStyledButton(String text, boolean isHighlight) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        btn.setTextColor(Color.WHITE);
        btn.setAllCaps(false);
        btn.setBackground(isHighlight ? createSelectedButtonBackground() : createUnselectedButtonBackground());
        return btn;
    }

    private GradientDrawable createSelectedButtonBackground() {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor("#0284C7"));
        gd.setCornerRadius(dp(8));
        return gd;
    }

    private GradientDrawable createUnselectedButtonBackground() {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor("#1E293B"));
        gd.setCornerRadius(dp(8));
        gd.setStroke(dp(1), Color.parseColor("#334155"));
        return gd;
    }

    private GradientDrawable createCardBackground() {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor("#1E293B"));
        gd.setCornerRadius(dp(12));
        gd.setStroke(dp(1), Color.parseColor("#334155"));
        return gd;
    }

    private GradientDrawable createInputBackground() {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor("#0F172A"));
        gd.setCornerRadius(dp(8));
        gd.setStroke(dp(1), Color.parseColor("#334155"));
        return gd;
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }
}
