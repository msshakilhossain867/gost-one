package com.citomoltd.banglagoststory.utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;
import androidx.core.content.res.ResourcesCompat;

import com.citomoltd.banglagoststory.R;

import java.io.File;
import java.io.FileOutputStream;

public class StoryQuoteCardGenerator {

    private static final String TAG = "QuoteCardGenerator";

    public static void shareQuoteCard(Context context, String title, String excerpt) {
        try {
            int width = 1080;
            int height = 1350; // Standard 4:5 social media card ratio

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);

            // 1. Dark atmospheric gradient background (deep midnight noir to dark crimson)
            Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            bgPaint.setShader(new LinearGradient(
                    0, 0, width, height,
                    new int[]{Color.parseColor("#090D16"), Color.parseColor("#150B14"), Color.parseColor("#1F0A10")},
                    new float[]{0.0f, 0.6f, 1.0f},
                    Shader.TileMode.CLAMP
            ));
            canvas.drawRect(0, 0, width, height, bgPaint);

            // 2. Decorative inner stroke
            Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            strokePaint.setColor(Color.parseColor("#33E11D48"));
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setStrokeWidth(6);
            RectF innerRect = new RectF(50, 50, width - 50, height - 50);
            canvas.drawRoundRect(innerRect, 32, 32, strokePaint);

            // 3. Spooky glowing corner accents
            Paint cornerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            cornerPaint.setColor(Color.parseColor("#E11D48"));
            cornerPaint.setStyle(Paint.Style.STROKE);
            cornerPaint.setStrokeWidth(12);
            canvas.drawLine(70, 70, 160, 70, cornerPaint);
            canvas.drawLine(70, 70, 70, 160, cornerPaint);

            canvas.drawLine(width - 160, 70, width - 70, 70, cornerPaint);
            canvas.drawLine(width - 70, 70, width - 70, 160, cornerPaint);

            canvas.drawLine(70, height - 70, 160, height - 70, cornerPaint);
            canvas.drawLine(70, height - 160, 70, height - 70, cornerPaint);

            canvas.drawLine(width - 160, height - 70, width - 70, height - 70, cornerPaint);
            canvas.drawLine(width - 70, height - 160, width - 70, height - 70, cornerPaint);

            Typeface kalpurush = null;
            try {
                kalpurush = ResourcesCompat.getFont(context, R.font.kalpurush);
            } catch (Exception ignored) {}
            if (kalpurush == null) {
                kalpurush = Typeface.DEFAULT_BOLD;
            }

            // 4. Header: App Badge & Tagline
            TextPaint badgePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            badgePaint.setColor(Color.parseColor("#F59E0B"));
            badgePaint.setTextSize(44);
            badgePaint.setTypeface(kalpurush);
            badgePaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("⚡ বাংলা ভৌতিক ও রহস্য গল্প ⚡", width / 2f, 150, badgePaint);

            // 5. Story Title in Crimson Glow
            TextPaint titlePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            titlePaint.setColor(Color.parseColor("#FFFFFF"));
            titlePaint.setTextSize(64);
            titlePaint.setTypeface(Typeface.create(kalpurush, Typeface.BOLD));
            titlePaint.setTextAlign(Paint.Align.CENTER);

            // Title divider line
            Paint divPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            divPaint.setColor(Color.parseColor("#E11D48"));
            divPaint.setStrokeWidth(4);
            canvas.drawLine(width / 2f - 200, 290, width / 2f + 200, 290, divPaint);

            StaticLayout titleLayout = StaticLayout.Builder.obtain(
                    "\"" + title + "\"", 0, ("\"" + title + "\"").length(),
                    titlePaint, width - 200
            ).setAlignment(Layout.Alignment.ALIGN_CENTER).build();

            canvas.save();
            canvas.translate(width / 2f - titleLayout.getWidth() / 2f, 190);
            titleLayout.draw(canvas);
            canvas.restore();

            // 6. Excerpt / Story Preview
            String cleanExcerpt = excerpt.replaceAll("\\s+", " ").trim();
            if (cleanExcerpt.length() > 320) {
                cleanExcerpt = cleanExcerpt.substring(0, 320) + "...";
            }

            TextPaint quotePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            quotePaint.setColor(Color.parseColor("#E2E8F0"));
            quotePaint.setTextSize(46);
            quotePaint.setTypeface(kalpurush);

            StaticLayout quoteLayout = StaticLayout.Builder.obtain(
                    cleanExcerpt, 0, cleanExcerpt.length(),
                    quotePaint, width - 220
            ).setAlignment(Layout.Alignment.ALIGN_NORMAL).setLineSpacing(16, 1.25f).build();

            canvas.save();
            canvas.translate(110, 360);
            quoteLayout.draw(canvas);
            canvas.restore();

            // 7. Footer: Download CTA & Watermark
            Paint footerBg = new Paint(Paint.ANTI_ALIAS_FLAG);
            footerBg.setColor(Color.parseColor("#1F1322"));
            RectF footerRect = new RectF(100, height - 200, width - 100, height - 100);
            canvas.drawRoundRect(footerRect, 20, 20, footerBg);

            TextPaint footerPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            footerPaint.setColor(Color.parseColor("#CBD5E1"));
            footerPaint.setTextSize(36);
            footerPaint.setTypeface(kalpurush);
            footerPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("📱 সম্পূর্ণ গল্পটি পড়তে ডাউনলোড করুন গুগল প্লে স্টোর থেকে", width / 2f, height - 135, footerPaint);

            // Save bitmap to cache directory
            File cachePath = new File(context.getCacheDir(), "images");
            if (!cachePath.exists()) cachePath.mkdirs();
            File imageFile = new File(cachePath, "story_quote_" + System.currentTimeMillis() + ".png");
            FileOutputStream stream = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 95, stream);
            stream.close();

            // Share via FileProvider
            Uri contentUri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", imageFile);
            if (contentUri != null) {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                shareIntent.setDataAndType(contentUri, context.getContentResolver().getType(contentUri));
                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                shareIntent.putExtra(Intent.EXTRA_TEXT, "📖 " + title + "\n\nগল্পটি পড়তে ডাউনলোড করুন: https://play.google.com/store/apps/details?id=" + context.getPackageName());
                shareIntent.setType("image/png");
                context.startActivity(Intent.createChooser(shareIntent, "গল্পের ফটো কার্ড শেয়ার করুন"));
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed to generate quote card", e);
            Toast.makeText(context, "কার্ড তৈরিতে সমস্যা হয়েছে। টেক্সট শেয়ার করা হচ্ছে...", Toast.LENGTH_SHORT).show();
        }
    }
}
