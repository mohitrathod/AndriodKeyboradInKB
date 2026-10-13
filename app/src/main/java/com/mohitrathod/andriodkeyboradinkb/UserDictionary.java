package com.mohitrathod.andriodkeyboradinkb;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UserDictionary {
    private static final String TAG = "UserDictionary";
    private static final String FILE_NAME = "user_words.txt";
    private static final int MAX_ENTRIES = 5000;

    private static volatile UserDictionary instance;

    private static class Entry {
        int count;
        boolean manual;

        Entry(int count, boolean manual) {
            this.count = count;
            this.manual = manual;
        }

        boolean isActive() {
            return manual || count >= 2;
        }
    }

    private final Context context;
    private final File file;
    private final Map<String, Entry> entries = new HashMap<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final List<Runnable> pendingCallbacks = new ArrayList<>();

    private boolean loadStarted = false;
    private volatile boolean loaded = false;

    private UserDictionary(Context context) {
        this.context = context.getApplicationContext();
        this.file = new File(context.getFilesDir(), FILE_NAME);
    }

    public static UserDictionary get(Context context) {
        if (instance == null) {
            synchronized (UserDictionary.class) {
                if (instance == null) {
                    instance = new UserDictionary(context);
                }
            }
        }
        return instance;
    }

    public void ensureLoaded(Runnable callback) {
        boolean startLoad = false;
        synchronized (this) {
            if (loaded) {
                if (callback != null) callback.run();
                return;
            }
            if (callback != null) pendingCallbacks.add(callback);
            if (!loadStarted) {
                loadStarted = true;
                startLoad = true;
            }
        }
        if (startLoad) {
            executor.execute(this::doLoad);
        }
    }

    private void doLoad() {
        Map<String, Entry> loadedEntries = new HashMap<>();
        if (file.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split("\t");
                    if (parts.length >= 2) {
                        String word = parts[0].toLowerCase();
                        int count = Integer.parseInt(parts[1]);
                        boolean manual = parts.length >= 3 && "manual".equals(parts[2]);
                        loadedEntries.put(word, new Entry(count, manual));
                    }
                }
            } catch (IOException | NumberFormatException e) {
                Log.w(TAG, "Failed to load user dictionary", e);
            }
        }

        List<Runnable> callbacks;
        synchronized (this) {
            for (Map.Entry<String, Entry> e : loadedEntries.entrySet()) {
                if (!entries.containsKey(e.getKey())) {
                    entries.put(e.getKey(), e.getValue());
                }
            }
            loaded = true;
            callbacks = new ArrayList<>(pendingCallbacks);
            pendingCallbacks.clear();
        }
        Log.d(TAG, "Loaded " + entries.size() + " user words");
        for (Runnable r : callbacks) {
            r.run();
        }
    }

    public boolean isLoaded() {
        return loaded;
    }

    public synchronized boolean contains(String word) {
        if (word == null) return false;
        Entry e = entries.get(word.toLowerCase());
        return e != null && e.isActive();
    }

    public synchronized void recordUse(String word) {
        if (word == null || word.length() < 2 || !word.matches("[a-z']+")) return;
        String w = word.toLowerCase();
        Entry e = entries.get(w);
        if (e == null) {
            entries.put(w, new Entry(1, false));
        } else if (!e.manual) {
            e.count++;
        }
        persistAsync();
    }

    public synchronized void addManual(String word) {
        if (word == null || word.length() < 2 || !word.matches("[a-z']+")) return;
        String w = word.toLowerCase();
        Entry e = entries.get(w);
        if (e == null) {
            entries.put(w, new Entry(2, true));
        } else {
            e.count = Math.max(e.count, 2);
            e.manual = true;
        }
        persistAsync();
    }

    public synchronized void removeWord(String word) {
        if (word == null) return;
        entries.remove(word.toLowerCase());
        persistAsync();
    }

    public synchronized void clear() {
        entries.clear();
        persistAsync();
    }

    public synchronized int size() {
        int active = 0;
        for (Entry e : entries.values()) {
            if (e.isActive()) active++;
        }
        return active;
    }

    public synchronized List<String> suggest(String prefix, int max) {
        List<String> result = new ArrayList<>();
        if (prefix == null || prefix.length() < 1) return result;
        String p = prefix.toLowerCase();
        for (Map.Entry<String, Entry> entry : entries.entrySet()) {
            String word = entry.getKey();
            if (entry.getValue().isActive() && word.startsWith(p) && word.length() > p.length()) {
                result.add(word);
            }
        }
        result.sort(Comparator.comparingInt((String w) -> -entries.get(w).count));
        if (result.size() > max) result = new ArrayList<>(result.subList(0, max));
        return result;
    }

    private void persistAsync() {
        final Map<String, Entry> snapshot;
        synchronized (this) {
            snapshot = new HashMap<>(entries);
        }
        executor.execute(() -> {
            File temp = new File(context.getFilesDir(), FILE_NAME + ".tmp");
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(temp))) {
                List<Map.Entry<String, Entry>> list = new ArrayList<>(snapshot.entrySet());
                list.sort((a, b) -> {
                    Entry ea = a.getValue();
                    Entry eb = b.getValue();
                    boolean aa = ea.isActive();
                    boolean ab = eb.isActive();
                    if (aa != ab) return aa ? -1 : 1;
                    if (ea.manual != eb.manual) return ea.manual ? -1 : 1;
                    return Integer.compare(eb.count, ea.count);
                });
                if (list.size() > MAX_ENTRIES) {
                    list = list.subList(0, MAX_ENTRIES);
                }
                for (Map.Entry<String, Entry> e : list) {
                    bw.write(e.getKey() + "\t" + e.getValue().count + "\t" + (e.getValue().manual ? "manual" : "auto"));
                    bw.newLine();
                }
                bw.flush();
                if (!temp.renameTo(file)) {
                    Log.w(TAG, "Failed to rename temp file");
                }
            } catch (IOException ex) {
                Log.w(TAG, "Failed to persist user dictionary", ex);
            }
        });
    }
}