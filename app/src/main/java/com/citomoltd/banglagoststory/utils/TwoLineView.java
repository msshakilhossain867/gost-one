package com.citomoltd.banglagoststory.utils;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class TwoLineView {

    private Context context;  // Declare a Context variable

    // Constructor to initialize the context
    public TwoLineView(Context context) {
        this.context = context;  // Assign the passed context to the class variable
    }

    public String readFirstTwoLinesFromFile(String fileName) {
        AssetManager assetManager = context.getAssets();
        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(assetManager.open(fileName)))) {
            String line;
            int lineCount = 0;
            while ((line = reader.readLine()) != null && lineCount < 2) {
                result.append(line).append("\n");
                lineCount++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result.toString().trim();
    }
}
