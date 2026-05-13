package com.example.laundaryagent;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class HelpCenterActivity extends AppCompatActivity {

    // FAQ question + answer view pairs
    private final int[] faqRootIds    = {R.id.faq_1, R.id.faq_2, R.id.faq_3, R.id.faq_4, R.id.faq_5, R.id.faq_6};
    private final int[] faqAnswerIds  = {R.id.faq_1_answer, R.id.faq_2_answer, R.id.faq_3_answer, R.id.faq_4_answer, R.id.faq_5_answer, R.id.faq_6_answer};
    private final int[] faqArrowIds   = {R.id.faq_1_arrow, R.id.faq_2_arrow, R.id.faq_3_arrow, R.id.faq_4_arrow, R.id.faq_5_arrow, R.id.faq_6_arrow};

    private final boolean[] expanded = new boolean[6];

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_center);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // Call support
        findViewById(R.id.btn_call_support).setOnClickListener(v ->
                startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:18001234567"))));

        // Wire FAQ accordion
        for (int i = 0; i < faqRootIds.length; i++) {
            final int idx = i;
            // The clickable header is the first child LinearLayout of each FAQ root
            LinearLayout faqRoot = findViewById(faqRootIds[i]);
            // First child is the header row
            View headerRow = faqRoot.getChildAt(0);
            headerRow.setOnClickListener(v -> toggleFaq(idx));
        }
    }

    private void toggleFaq(int idx) {
        expanded[idx] = !expanded[idx];

        TextView answer = findViewById(faqAnswerIds[idx]);
        ImageView arrow = findViewById(faqArrowIds[idx]);

        if (expanded[idx]) {
            answer.setVisibility(View.VISIBLE);
            answer.setAlpha(0f);
            answer.animate().alpha(1f).setDuration(200).start();
            arrow.setRotation(90f);
        } else {
            answer.animate().alpha(0f).setDuration(150).withEndAction(() ->
                    answer.setVisibility(View.GONE)).start();
            arrow.setRotation(0f);
        }
    }
}
