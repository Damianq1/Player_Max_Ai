package com.damianq1.playermaxai;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;
import androidx.appcompat.app.AppCompatActivity;
import com.damianq1.playermaxai.parser.M3UParser;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.util.VLCVideoLayout;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements MediaPlayer.EventListener {

    private static final String PREFS_NAME = "PlayerPrefs";
    private static final String KEY_LAST_URL = "lastUrl";
    private static final String KEY_LAST_CHANNEL = "lastChannel";
    private static final String KEY_BRIGHTNESS = "brightness";
    private static final String KEY_CONTRAST = "contrast";
    private static final String KEY_SATURATION = "saturation";
    private static final String KEY_VOLUME = "volume";
    private static final String KEY_CURRENT_INDEX = "currentIndex";

    private LibVLC libVLC;
    private MediaPlayer mediaPlayer;
    private VLCVideoLayout videoLayout;
    private EditText urlInput;
    private Button scanButton, btnAspect, btnRec, btnSleep, btnBackup;
    private ToggleButton togglePowerSaving, toggleSort, toggleTheme;
    private LinearLayout channelList, favoritesList, recentList, groupsContainer;
    private ProgressBar loading;
    private TextView osdChannel, diagLog;
    private SeekBar seekBrightness, seekContrast, seekSaturation, seekVolume;
    private List<M3UParser.Channel> channels = new ArrayList<>();
    private SharedPreferences prefs;
    private String lastPlayedUrl = "";
    private String lastPlayedName = "";
    private int currentChannelIndex = -1;
    private float brightness = 1.0f, contrast = 1.0f, saturation = 1.0f;
    private int volume = 100;
    private boolean mutedOnError = false;
    private long lastProgressTime = 0;
    private long lastPosition = -1;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable reconnectRunnable, freezeCheckRunnable, autoHideMenuRunnable, batteryCheckRunnable, unmuteRunnable, sleepRunnable;
    private BroadcastReceiver networkReceiver;
    private Vibrator vibrator;
    private final String[] userAgents = {
        "VLC/3.0.21 LibVLC/3.0.21", "Lavf/58.76.100", "Kodi/21.0 (Linux; Android)",
        "IPTV Smarters/3.1.5.1", "MXPlayer/1.9.0", "ExoPlayer/2.19.1",
        "VLC/3.6.0", "GSE Smart IPTV", "Perfect Player/3.9"
    };
    private Random random = new Random();
    private int uaRotateCounter = 0;
    private boolean lowPowerMode = false;
    private boolean powerSaving = false;
    private StringBuilder logBuilder = new StringBuilder();
    private Set<String> favorites = new HashSet<>();
    private LinkedList<String> recentUrls = new LinkedList<>();
    private static final int MAX_RECENT = 8;
    private Map<String, LinearLayout> groupLayouts = new HashMap<>();
    private List<TextView> channelButtons = new ArrayList<>();
    private boolean isRecording = false;
    private String recPath;
    private int osdTheme = 0;
    private int aspectMode = 0;
    private int sleepMinutes = 0;
    private boolean sortByFav = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        videoLayout = findViewById(R.id.video_layout);
        urlInput = findViewById(R.id.url_input);
        scanButton = findViewById(R.id.scan_button);
        channelList = findViewById(R.id.channel_list);
        favoritesList = findViewById(R.id.favorites_list);
        recentList = findViewById(R.id.recent_list);
        groupsContainer = findViewById(R.id.groups_container);
        loading = findViewById(R.id.loading);
        osdChannel = findViewById(R.id.osd_channel);
        diagLog = findViewById(R.id.diag_log);

        seekBrightness = findViewById(R.id.seek_brightness);
        seekContrast = findViewById(R.id.seek_contrast);
        seekSaturation = findViewById(R.id.seek_saturation);
        seekVolume = findViewById(R.id.seek_volume);

        btnAspect = findViewById(R.id.btn_aspect);
        btnRec = findViewById(R.id.btn_rec);
        btnSleep = findViewById(R.id.btn_sleep);
        btnBackup = findViewById(R.id.btn_backup);
        togglePowerSaving = findViewById(R.id.toggle_power_saving);
        toggleSort = findViewById(R.id.toggle_sort);
        toggleTheme = findViewById(R.id.toggle_theme);

        brightness = prefs.getFloat(KEY_BRIGHTNESS, 1.0f);
        contrast = prefs.getFloat(KEY_CONTRAST, 1.0f);
        saturation = prefs.getFloat(KEY_SATURATION, 1.0f);
        volume = prefs.getInt(KEY_VOLUME, 100);
        currentChannelIndex = prefs.getInt(KEY_CURRENT_INDEX, -1);

        seekBrightness.setProgress((int)(brightness * 100));
        seekContrast.setProgress((int)(contrast * 100));
        seekSaturation.setProgress((int)(saturation * 100));
        seekVolume.setProgress(volume);

        libVLC = new LibVLC(this);
        mediaPlayer = new MediaPlayer(libVLC);
        mediaPlayer.attachViews(videoLayout, null, false, false);
        mediaPlayer.setEventListener(this);

        favorites = prefs.getStringSet("favorites", new HashSet<>());
        String recentStr = prefs.getString("recent_urls", "");
        if (!recentStr.isEmpty()) {
            Collections.addAll(recentUrls, recentStr.split("\\|"));
            rebuildRecentList();
        }

        String lastUrl = prefs.getString(KEY_LAST_URL, "");
        if (!lastUrl.isEmpty()) {
            urlInput.setText(lastUrl);
            scanAndPopulate(lastUrl);
        }

        scanButton.setOnClickListener(v -> {
            vibrate(30);
            String m3uUrl = urlInput.getText().toString().trim();
            if (!m3uUrl.isEmpty()) {
                prefs.edit().putString(KEY_LAST_URL, m3uUrl).apply();
                scanAndPopulate(m3uUrl);
            }
        });

        videoLayout.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN && event.getX() < 120) {
                toggleMenu();
                resetAutoHide();
                vibrate(20);
                return true;
            }
            resetAutoHide();
            return false;
        });

        seekBrightness.setOnSeekBarChangeListener(createFilterListener(KEY_BRIGHTNESS, p -> brightness = p / 100f));
        seekContrast.setOnSeekBarChangeListener(createFilterListener(KEY_CONTRAST, p -> contrast = p / 100f));
        seekSaturation.setOnSeekBarChangeListener(createFilterListener(KEY_SATURATION, p -> saturation = p / 100f));
        seekVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean f) {
                volume = p; prefs.edit().putInt(KEY_VOLUME, volume).apply();
                mediaPlayer.setVolume(mutedOnError ? 0 : volume);
                vibrate(10);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });

        togglePowerSaving.setOnCheckedChangeListener((b, isChecked) -> {
            powerSaving = isChecked;
            vibrate(20);
            if (powerSaving) {
                brightness = 0.7f; contrast = 0.8f; saturation = 0.75f;
                seekBrightness.setProgress(70);
                seekContrast.setProgress(80);
                seekSaturation.setProgress(75);
                showOsd("Tryb oszczędzania AKTYWNY");
            } else {
                brightness = 1.0f; contrast = 1.0f; saturation = 1.0f;
                seekBrightness.setProgress(100);
                seekContrast.setProgress(100);
                seekSaturation.setProgress(100);
                showOsd("Tryb oszczędzania WYŁĄCZONY");
            }
            restartPlayback();
        });

        toggleSort.setOnCheckedChangeListener((b, checked) -> {
            sortByFav = checked;
            vibrate(20);
            scanAndPopulate(urlInput.getText().toString().trim());
            showOsd(checked ? "Sort: Ulubione na górze" : "Sort: Standard");
        });

        toggleTheme.setOnCheckedChangeListener((b, isChecked) -> {
            osdTheme = isChecked ? 1 : 0;
            applyOsdTheme();
            vibrate(20);
        });

        btnAspect.setOnClickListener(v -> {
            vibrate(30);
            aspectMode = (aspectMode + 1) % 5;
            String[] modes = {"Auto", "16:9", "4:3", "Fit", "Zoom 1.2×"};
            btnAspect.setText("Aspect: " + modes[aspectMode]);
            applyAspectRatio();
            showOsd("Aspect: " + modes[aspectMode]);
        });

        btnRec.setOnClickListener(v -> {
            vibrate(50);
            isRecording = !isRecording;
            btnRec.setText(isRecording ? "STOP" : "REC");
            showOsd(isRecording ? "Nagrywanie rozpoczęte" : "Nagrywanie zatrzymane");
            restartPlayback();
        });

        btnSleep.setOnClickListener(v -> {
            vibrate(30);
            sleepMinutes = (sleepMinutes + 30) % 150;
            if (sleepMinutes == 0) {
                btnSleep.setText("Sleep Timer: OFF");
                handler.removeCallbacks(sleepRunnable);
                showOsd("Sleep wyłączony");
            } else {
                btnSleep.setText("Sleep: " + sleepMinutes + " min");
                handler.removeCallbacks(sleepRunnable);
                sleepRunnable = () -> {
                    showOsd("Sleep Timer – zatrzymuję odtwarzanie");
                    mediaPlayer.pause();
                };
                handler.postDelayed(sleepRunnable, sleepMinutes * 60L * 1000);
                showOsd("Ustawiono sleep: " + sleepMinutes + " min");
            }
        });

        btnBackup.setOnClickListener(v -> showBackupDialog());

        networkReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context c, Intent i) {
                if (!isConnected()) loading.setVisibility(View.VISIBLE);
                else loading.setVisibility(View.GONE);
            }
        };
        registerReceiver(networkReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        reconnectRunnable = () -> {
            if (!mediaPlayer.isPlaying() && !lastPlayedUrl.isEmpty()) {
                attemptReconnectWithDelay(10000);
            }
            handler.postDelayed(reconnectRunnable, 20000);
        };
        handler.postDelayed(reconnectRunnable, 20000);

        freezeCheckRunnable = () -> {
            long pos = mediaPlayer.getTime();
            if (pos == lastPosition && pos > 0 && mediaPlayer.isPlaying()) {
                if (System.currentTimeMillis() - lastProgressTime > 15000) {
                    attemptReconnectWithDelay(0);
                }
            } else {
                lastProgressTime = System.currentTimeMillis();
                lastPosition = pos;
            }
            handler.postDelayed(freezeCheckRunnable, 5000);
        };
        handler.postDelayed(freezeCheckRunnable, 5000);

        batteryCheckRunnable = () -> {
            BatteryManager bm = (BatteryManager) getSystemService(BATTERY_SERVICE);
            int level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
            if (level < 20 && !lowPowerMode) {
                lowPowerMode = true;
                showOsd("Niski poziom baterii – tryb oszczędzania");
                brightness = 0.8f; contrast = 0.9f; saturation = 0.85f;
                seekBrightness.setProgress(80);
                seekContrast.setProgress(90);
                seekSaturation.setProgress(85);
                restartPlayback();
            } else if (level >= 30 && lowPowerMode) {
                lowPowerMode = false;
                showOsd("Bateria OK – normalny tryb");
            }
            handler.postDelayed(batteryCheckRunnable, 60000);
        };
        handler.postDelayed(batteryCheckRunnable, 10000);

        autoHideMenuRunnable = () -> toggleMenu(); // ukryj jeśli otwarte
        resetAutoHide();

        recPath = Environment.getExternalStorageDirectory() + "/PlayerMaxAi/rec/";
        new File(recPath).mkdirs();

        applyOsdTheme();

        // Obsługa Intent
        Intent intent = getIntent();
        if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
            String url = intent.getDataString();
            if (url != null && (url.startsWith("http") || url.startsWith("https"))) {
                scanAndPopulate(url);
            }
        }
    }

    private void applyOsdTheme() {
        if (osdTheme == 1) {
            osdChannel.setBackgroundColor(0xD0FFFFFF);
            osdChannel.setTextColor(0xFF000000);
        } else {
            osdChannel.setBackgroundColor(0xC0000000);
            osdChannel.setTextColor(0xFFFFFFFF);
        }
    }

    private void toggleMenu() {
        int vis = sideMenu.getVisibility();
        sideMenu.setVisibility(vis == View.VISIBLE ? View.GONE : View.VISIBLE);
        resetAutoHide();
    }

    private void resetAutoHide() {
        handler.removeCallbacks(autoHideMenuRunnable);
        handler.postDelayed(autoHideMenuRunnable, 5000);
    }

    private SeekBar.OnSeekBarChangeListener createFilterListener(String key, java.util.function.Consumer<Integer> setter) {
        return new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean f) {
                setter.accept(p);
                prefs.edit().putFloat(key, p / 100f).apply();
                restartPlayback();
                vibrate(15);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        };
    }

    private boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnectedOrConnecting();
    }

    private void attemptReconnectWithDelay(long delayMs) {
        handler.postDelayed(() -> {
            if (!lastPlayedUrl.isEmpty()) {
                playSmartFromFreeze(lastPlayedUrl, lastPlayedName);
            }
        }, delayMs);
    }

    private void playSmartFromFreeze(String url, String name) {
        lastPlayedUrl = url;
        lastPlayedName = name;
        mediaPlayer.stop();
        mutedOnError = false;
        lastPosition = -1;
        lastProgressTime = System.currentTimeMillis();

        mediaPlayer.detachViews();
        mediaPlayer.attachViews(videoLayout, null, false, false);

        Media media = new Media(libVLC, url);
        media.addOption(":hardware-decoder=auto");
        media.addOption(":avcodec-hw=any");
        media.addOption(powerSaving ? ":network-caching=2000" : ":network-caching=4500");
        media.addOption(":rtsp-tcp");
        media.addOption(":http-continuous");
        media.addOption(powerSaving ? ":deinterlace=0" : ":deinterlace-mode=blend");
        media.addOption(":http-keep-alive");
        media.addOption(":low-delay");
        media.addOption(":clock-jitter=0");
        media.addOption(":preferred-resolution=-1");
        media.addOption(":http-user-agent=" + getNextUserAgent());
        media.addOption(String.format(":video-filter=adjust{brightness=%.2f,contrast=%.2f,saturation=%.2f}", brightness, contrast, saturation));

        if (isRecording) {
            String filename = recPath + "rec_" + System.currentTimeMillis() + ".ts";
            media.addOption(":sout=#file{dst=" + filename + "}");
            addToLog("Nagrywanie do: " + filename);
        }

        mediaPlayer.setMedia(media);
        mediaPlayer.setVolume(mutedOnError ? 0 : volume);
        mediaPlayer.play();
        media.release();

        String extra = "";
        // Tutaj można dodać EPG z cache jeśli jest
        runOnUiThread(() -> {
            osdChannel.setText(name + extra);
            osdChannel.setVisibility(View.VISIBLE);
            osdChannel.postDelayed(() -> osdChannel.setVisibility(View.GONE), 5000);
        });
    }

    private String getNextUserAgent() {
        uaRotateCounter = (uaRotateCounter + 1) % userAgents.length;
        if (uaRotateCounter % 4 == 0) {
            return userAgents[random.nextInt(userAgents.length)];
        }
        return userAgents[uaRotateCounter];
    }

    private void vibrate(long ms) {
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(ms);
            }
        }
    }

    private void showOsd(String msg) {
        runOnUiThread(() -> {
            osdChannel.setText(msg);
            osdChannel.setVisibility(View.VISIBLE);
            osdChannel.postDelayed(() -> osdChannel.setVisibility(View.GONE), 4000);
        });
    }

    private void addToLog(String msg) {
        logBuilder.append(msg).append("\n");
        if (logBuilder.length() > 400) logBuilder.delete(0, logBuilder.length() - 400);
        runOnUiThread(() -> diagLog.setText(logBuilder.toString()));
    }

    private void scanAndPopulate(String m3uUrl) {
        if (m3uUrl.isEmpty()) return;

        loading.setVisibility(View.VISIBLE);
        groupsContainer.removeAllViews();
        groupLayouts.clear();
        channelButtons.clear();

        new Thread(() -> {
            try {
                channels = M3UParser.parse(m3uUrl);

                runOnUiThread(() -> {
                    loading.setVisibility(View.GONE);
                    addToLog("Załadowano " + channels.size() + " kanałów");

                    // Grupowanie po group-title
                    Map<String, List<M3UParser.Channel>> grouped = new HashMap<>();
                    for (M3UParser.Channel ch : channels) {
                        grouped.computeIfAbsent(ch.group, k -> new ArrayList<>()).add(ch);
                    }

                    List<String> sortedGroups = new ArrayList<>(grouped.keySet());
                    Collections.sort(sortedGroups);

                    for (String groupName : sortedGroups) {
                        List<M3UParser.Channel> groupCh = grouped.get(groupName);

                        TextView header = new TextView(this);
                        header.setText(groupName + "  (" + groupCh.size() + ")");
                        header.setTextColor(0xFFFFAA00);
                        header.setTextSize(18f);
                        header.setPadding(16, 24, 16, 12);
                        header.setBackgroundColor(0x33000000);

                        LinearLayout groupContent = new LinearLayout(this);
                        groupContent.setOrientation(LinearLayout.VERTICAL);
                        groupContent.setVisibility(View.GONE);

                        header.setOnClickListener(v -> {
                            boolean isVisible = groupContent.getVisibility() == View.VISIBLE;
                            groupContent.setVisibility(isVisible ? View.GONE : View.VISIBLE);
                            vibrate(20);
                        });

                        for (M3UParser.Channel ch : groupCh) {
                            TextView btn = new TextView(this);
                            btn.setText(ch.name);
                            btn.setTextColor(0xFFFFFFFF);
                            btn.setPadding(48, 20, 16, 20);
                            btn.setBackgroundColor(0x11000000);

                            final int idx = channels.indexOf(ch);

                            btn.setOnClickListener(v -> {
                                vibrate(40);
                                playSmartFromFreeze(ch.url, ch.name);
                                currentChannelIndex = idx;
                                prefs.edit().putInt(KEY_CURRENT_INDEX, idx).apply();
                                focusCurrentChannel();

                                String extra = (ch.epgInfo != null && !ch.epgInfo.isEmpty()) ? "\n" + ch.epgInfo : "";
                                showOsd(ch.name + extra);
                            });

                            groupContent.addView(btn);
                            channelButtons.add(btn);
                        }

                        groupsContainer.addView(header);
                        groupsContainer.addView(groupContent);
                        groupLayouts.put(groupName, groupContent);
                    }

                    if (currentChannelIndex >= 0 && currentChannelIndex < channels.size()) {
                        M3UParser.Channel lastCh = channels.get(currentChannelIndex);
                        playSmartFromFreeze(lastCh.url, lastCh.name);
                        focusCurrentChannel();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    loading.setVisibility(View.GONE);
                    addToLog("Błąd parsowania: " + e.getMessage());
                });
            }
        }).start();
    }

    private void focusCurrentChannel() {
        for (int i = 0; i < channelButtons.size(); i++) {
            TextView tv = channelButtons.get(i);
            tv.setBackgroundColor(i == currentChannelIndex ? 0x44FFFF00 : 0x00000000);
        }
    }

    private void applyAspectRatio() {
        // VLC aspect ratio w runtime jest trudne – restartujemy z opcją
        restartPlayback();
    }

    private void showBackupDialog() {
        String[] options = {"Backup", "Restore"};
        new AlertDialog.Builder(this)
            .setTitle("Backup / Restore")
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    try {
                        File file = new File(Environment.getExternalStorageDirectory(), "PlayerMaxAi_backup.json");
                        FileWriter writer = new FileWriter(file);
                        writer.write("{\"favorites\":" + favorites + ", \"recent\":" + recentUrls + "}");
                        writer.close();
                        showOsd("Backup zapisany: " + file.getPath());
                    } catch (Exception e) {
                        showOsd("Błąd backupu");
                    }
                } else {
                    showOsd("Restore – wklej plik ręcznie (TODO)");
                }
            }).show();
    }

    private void rebuildRecentList() {
        recentList.removeAllViews();
        for (String entry : recentUrls) {
            String[] parts = entry.split("\\|", 2);
            if (parts.length < 2) continue;
            TextView tv = new TextView(this);
            tv.setText(parts[1]);
            tv.setTextColor(0xFFFFFFFF);
            tv.setPadding(16, 20, 16, 20);
            tv.setBackgroundColor(0x22000000);
            tv.setOnClickListener(v -> {
                vibrate(40);
                playSmartFromFreeze(parts[0], parts[1]);
            });
            recentList.addView(tv);
        }
    }

    @Override
    public void onEvent(MediaPlayer.Event event) {
        switch (event.type) {
            case MediaPlayer.Event.EncounteredError:
                mutedOnError = true;
                mediaPlayer.setVolume(0);
                loading.setVisibility(View.VISIBLE);
                attemptReconnectWithDelay(3000);
                handler.removeCallbacks(unmuteRunnable);
                unmuteRunnable = () -> {
                    mutedOnError = false;
                    mediaPlayer.setVolume(volume);
                };
                handler.postDelayed(unmuteRunnable, 8000);
                break;
            case MediaPlayer.Event.Buffering:
                loading.setVisibility(View.VISIBLE);
                break;
            case MediaPlayer.Event.Playing:
                loading.setVisibility(View.GONE);
                if (!mutedOnError) mediaPlayer.setVolume(volume);
                break;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (sideMenu.getVisibility() == View.VISIBLE) {
                sideMenu.setVisibility(View.GONE);
                vibrate(30);
                return true;
            } else {
                new AlertDialog.Builder(this)
                        .setMessage("Czy na pewno chcesz wyjść?")
                        .setPositiveButton("Tak", (d, w) -> finish())
                        .setNegativeButton("Nie", null)
                        .show();
                return true;
            }
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_CHANNEL_DOWN) {
            // prevChannel();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == KeyEvent.KEYCODE_CHANNEL_UP) {
            // nextChannel();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_MENU) {
            toggleMenu();
            resetAutoHide();
            vibrate(40);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(reconnectRunnable);
        handler.removeCallbacks(freezeCheckRunnable);
        handler.removeCallbacks(autoHideMenuRunnable);
        handler.removeCallbacks(batteryCheckRunnable);
        handler.removeCallbacks(unmuteRunnable);
        handler.removeCallbacks(sleepRunnable);
        unregisterReceiver(networkReceiver);
        mediaPlayer.setEventListener(null);
        mediaPlayer.detachViews();
        mediaPlayer.release();
        libVLC.release();
        prefs.edit().putStringSet("favorites", favorites).apply();
        prefs.edit().putString("recent_urls", String.join("|", recentUrls)).apply();
        super.onDestroy();
    }
}
