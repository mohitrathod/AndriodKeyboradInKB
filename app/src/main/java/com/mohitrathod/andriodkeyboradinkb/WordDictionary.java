package com.mohitrathod.andriodkeyboradinkb;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WordDictionary {
    private static final String TAG = "WordDictionary";
    private static final String ASSET_FILE = "words.txt";
    private static final int MAX_CANDIDATES_SCAN = 500;
    private static final int MIN_WORD_LENGTH = 4;

    private String[] words;
    private int[] ranks;
    private volatile boolean ready = false;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface LoadCallback {
        void onLoaded();
    }

    public void loadAsync(Context context, LoadCallback callback) {
        executor.execute(() -> {
            try (InputStream is = context.getAssets().open(ASSET_FILE);
                 BufferedReader br = new BufferedReader(new InputStreamReader(is))) {

                List<String> wordList = new ArrayList<>();
                List<Integer> rankList = new ArrayList<>();
                String line;
                int rank = 0;
                while ((line = br.readLine()) != null && rank < 50000) {
                    String[] parts = line.split("\t");
                    if (parts.length >= 1) {
                        String word = parts[0].toLowerCase();
                        if (word.length() >= MIN_WORD_LENGTH && word.matches("[a-z']+")) {
                            wordList.add(word);
                            rankList.add(rank++);
                        }
                    }
                }

                int n = wordList.size();
                String[] tempWords = new String[n];
                int[] tempRanks = new int[n];
                for (int i = 0; i < n; i++) {
                    tempWords[i] = wordList.get(i);
                    tempRanks[i] = rankList.get(i);
                }

                Integer[] idx = new Integer[n];
                for (int i = 0; i < n; i++) idx[i] = i;
                Arrays.sort(idx, (a, b) -> tempWords[a].compareTo(tempWords[b]));

                this.words = new String[n];
                this.ranks = new int[n];
                for (int i = 0; i < n; i++) {
                    this.words[i] = tempWords[idx[i]];
                    this.ranks[i] = tempRanks[idx[i]];
                }

                this.ready = true;
                Log.d(TAG, "Loaded " + n + " words");

            } catch (IOException e) {
                Log.e(TAG, "Failed to load dictionary", e);
            }

            if (callback != null) {
                mainHandler.post(callback::onLoaded);
            }
        });
    }

    public boolean isReady() {
        return ready;
    }

    public boolean isKnown(String word) {
        if (!ready || word == null) return false;
        int idx = Arrays.binarySearch(words, word.toLowerCase());
        return idx >= 0;
    }

    public List<String> suggest(String prefix, int max) {
        List<String> result = new ArrayList<>();
        if (!ready || prefix == null || prefix.length() < MIN_WORD_LENGTH) return result;

        String p = prefix.toLowerCase();
        int start = Arrays.binarySearch(words, p);
        if (start < 0) start = -start - 1;

        List<Integer> candidateIndices = new ArrayList<>();
        int scanned = 0;
        for (int i = start; i < words.length && scanned < MAX_CANDIDATES_SCAN; i++) {
            String w = words[i];
            if (!w.startsWith(p)) break;
            if (w.length() > p.length()) {
                candidateIndices.add(i);
            }
            scanned++;
        }

        candidateIndices.sort((a, b) -> Integer.compare(ranks[a], ranks[b]));
        int count = Math.min(max, candidateIndices.size());
        for (int i = 0; i < count; i++) {
            result.add(words[candidateIndices.get(i)]);
        }
        return result;
    }

    public void shutdown() {
        executor.shutdown();
    }
}