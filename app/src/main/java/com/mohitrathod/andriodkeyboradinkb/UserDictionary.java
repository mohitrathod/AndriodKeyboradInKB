package com.mohitrathod.andriodkeyboradinkb;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
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
    private static final int MIN_WORD_LENGTH = 4;

    private static volatile UserDictionary instance;

    public interface ResultCallback {
        void onResult(boolean success, int count);
    }

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
                        if (word.length() < MIN_WORD_LENGTH) continue;
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
        if (word == null || word.length() < MIN_WORD_LENGTH || !word.matches("[a-z']+")) return;
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
        if (word == null || word.length() < MIN_WORD_LENGTH || !word.matches("[a-z']+")) return;
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
        if (prefix == null || prefix.length() < MIN_WORD_LENGTH) return result;
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
        executor.execute(() -> persistNow(snapshot));
    }

    private void persistNow(Map<String, Entry> snapshot) {
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
    }

    public synchronized String exportText() {
        List<Map.Entry<String, Entry>> list = new ArrayList<>(entries.entrySet());
        list.sort(Comparator.comparing(entry -> entry.getKey()));
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Entry> entry : list) {
            Entry value = entry.getValue();
            sb.append(entry.getKey()).append('\t')
              .append(value.count).append('\t')
              .append(value.manual ? "manual" : "auto").append('\n');
        }
        return sb.toString();
    }

    public void exportAsync(final Uri uri, final ResultCallback callback) {
        executor.execute(() -> {
            boolean ok = false;
            int count;
            synchronized (UserDictionary.this) {
                count = entries.size();
            }
            String data = exportText();
            try (OutputStream os = context.getContentResolver().openOutputStream(uri, "wt")) {
                if (os != null) {
                    os.write(data.getBytes(StandardCharsets.UTF_8));
                    os.flush();
                    ok = true;
                }
            } catch (IOException e) {
                Log.w(TAG, "Failed to export user dictionary", e);
            }
            if (callback != null) callback.onResult(ok, count);
        });
    }

    public void importAsync(final Uri uri, final ResultCallback callback) {
        executor.execute(() -> {
            boolean ok = false;
            int added = 0;
            try (InputStream is = context.getContentResolver().openInputStream(uri)) {
                if (is != null) {
                    added = importTsv(readAll(is));
                    final Map<String, Entry> snapshot;
                    synchronized (UserDictionary.this) {
                        snapshot = new HashMap<>(entries);
                    }
                    persistNow(snapshot);
                    ok = true;
                }
            } catch (IOException e) {
                Log.w(TAG, "Failed to import user dictionary", e);
            }
            if (callback != null) callback.onResult(ok, added);
        });
    }

    private int importTsv(String text) {
        int added = 0;
        synchronized (this) {
            for (String line : text.split("\n")) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split("\t");
                String word = parts[0].trim().toLowerCase();
                if (word.length() < MIN_WORD_LENGTH || !word.matches("[a-z']+")) continue;
                if (entries.containsKey(word)) continue;
                if (entries.size() >= MAX_ENTRIES) break;
                int count = 1;
                if (parts.length >= 2) {
                    try {
                        count = Integer.parseInt(parts[1].trim());
                    } catch (NumberFormatException ignored) {
                        count = 1;
                    }
                }
                if (count < 1) count = 1;
                boolean manual = parts.length >= 3 && "manual".equals(parts[2].trim());
                entries.put(word, new Entry(count, manual));
                added++;
            }
        }
        return added;
    }

    private static String readAll(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int read;
        while ((read = is.read(chunk)) != -1) {
            buffer.write(chunk, 0, read);
        }
        return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
    }
}