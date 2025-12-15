package com.example.newsai.tts;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.example.newsai.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TextToSpeechManager {
    private static final String TAG = "TextToSpeechManager";
    private static final String CACHE_DIR = "tts_cache";
    private static final int VOICE_ID = 1; // Default voice ID
    
    private final Context context;
    private MediaPlayer mediaPlayer;
    private String currentPlayingText;
    private boolean isPlaying;
    private boolean isLoading;
    private OnPlayStateChangeListener listener;
    
    public interface OnPlayStateChangeListener {
        void onPlayStateChanged(boolean isPlaying, String text);
        void onLoadingStateChanged(boolean isLoading, String text);
        void onPlaybackCompleted(String text);
        void onError(String text, String error);
    }

    public TextToSpeechManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public void setOnPlayStateChangeListener(OnPlayStateChangeListener listener) {
        this.listener = listener;
    }

    public void playText(String text, Integer voiceId) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        
        // Stop current playback if any
        stop();
        
        currentPlayingText = text.trim();
        Integer finalVoiceId = voiceId != null ? voiceId : VOICE_ID;
        
        // Check cache first
        File cachedFile = getCachedFile(currentPlayingText, finalVoiceId);
        if (cachedFile != null && cachedFile.exists()) {
            playAudioFile(cachedFile.getAbsolutePath());
            return;
        }
        
        // Notify loading started
        isLoading = true;
        if (listener != null) {
            new Handler(Looper.getMainLooper()).post(() -> 
                listener.onLoadingStateChanged(true, currentPlayingText));
        }
        
        // Download and play
        TTSApiService api = TTSApiClient.getService();
        Call<ResponseBody> call = api.synthesizeText(currentPlayingText, finalVoiceId);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        File audioFile = saveAudioFile(response.body(), currentPlayingText, finalVoiceId);
                        isLoading = false;
                        if (listener != null) {
                            new Handler(Looper.getMainLooper()).post(() -> 
                                listener.onLoadingStateChanged(false, currentPlayingText));
                        }
                        if (audioFile != null) {
                            playAudioFile(audioFile.getAbsolutePath());
                        } else {
                            notifyError(currentPlayingText, "Không thể lưu file audio");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error saving audio", e);
                        isLoading = false;
                        if (listener != null) {
                            new Handler(Looper.getMainLooper()).post(() -> 
                                listener.onLoadingStateChanged(false, currentPlayingText));
                        }
                        notifyError(currentPlayingText, "Lỗi khi xử lý audio: " + e.getMessage());
                    }
                } else {
                    isLoading = false;
                    if (listener != null) {
                        new Handler(Looper.getMainLooper()).post(() -> 
                            listener.onLoadingStateChanged(false, currentPlayingText));
                    }
                    notifyError(currentPlayingText, "API trả về lỗi: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "API call failed", t);
                isLoading = false;
                if (listener != null) {
                    new Handler(Looper.getMainLooper()).post(() -> 
                        listener.onLoadingStateChanged(false, currentPlayingText));
                }
                notifyError(currentPlayingText, "Lỗi kết nối: " + t.getMessage());
            }
        });
    }

    private File saveAudioFile(ResponseBody body, String text, Integer voiceId) throws IOException {
        File cacheDir = new File(context.getCacheDir(), CACHE_DIR);
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        
        String fileName = getCacheFileName(text, voiceId);
        File audioFile = new File(cacheDir, fileName);
        
        InputStream inputStream = null;
        FileOutputStream outputStream = null;
        try {
            byte[] buffer = new byte[4096];
            inputStream = body.byteStream();
            outputStream = new FileOutputStream(audioFile);
            
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
            return audioFile;
        } finally {
            if (inputStream != null) inputStream.close();
            if (outputStream != null) outputStream.close();
        }
    }

    private void playAudioFile(String filePath) {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.release();
            }
            
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(filePath);
            mediaPlayer.prepare();
            
            mediaPlayer.setOnCompletionListener(mp -> {
                isPlaying = false;
                String completedText = currentPlayingText;
                if (listener != null) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        listener.onPlayStateChanged(false, completedText);
                        listener.onPlaybackCompleted(completedText);
                    });
                }
            });
            
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                isPlaying = false;
                notifyError(currentPlayingText, "Lỗi phát audio");
                return true;
            });
            
            mediaPlayer.start();
            isPlaying = true;
            
            if (listener != null) {
                new Handler(Looper.getMainLooper()).post(() -> 
                    listener.onPlayStateChanged(true, currentPlayingText));
            }
            
        } catch (IOException e) {
            Log.e(TAG, "Error playing audio", e);
            notifyError(currentPlayingText, "Lỗi phát audio: " + e.getMessage());
        }
    }

    public void stop() {
        if (mediaPlayer != null && isPlaying) {
            try {
                mediaPlayer.stop();
                mediaPlayer.release();
            } catch (Exception e) {
                Log.e(TAG, "Error stopping player", e);
            }
            mediaPlayer = null;
        }
        isPlaying = false;
        isLoading = false;
        if (listener != null && currentPlayingText != null) {
            new Handler(Looper.getMainLooper()).post(() -> {
                listener.onPlayStateChanged(false, currentPlayingText);
                listener.onLoadingStateChanged(false, currentPlayingText);
            });
        }
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public boolean isLoading() {
        return isLoading;
    }

    public boolean isPlayingText(String text) {
        return isPlaying && currentPlayingText != null && currentPlayingText.equals(text.trim());
    }

    private File getCachedFile(String text, Integer voiceId) {
        File cacheDir = new File(context.getCacheDir(), CACHE_DIR);
        if (!cacheDir.exists()) return null;
        
        String fileName = getCacheFileName(text, voiceId);
        File file = new File(cacheDir, fileName);
        return file.exists() ? file : null;
    }

    private String getCacheFileName(String text, Integer voiceId) {
        int hash = text.hashCode();
        return "tts_" + voiceId + "_" + Math.abs(hash) + ".wav";
    }

    private void notifyError(String text, String error) {
        Log.e(TAG, error + " for text: " + text);
        if (listener != null) {
            new Handler(Looper.getMainLooper()).post(() -> 
                listener.onError(text, error));
        }
        new Handler(Looper.getMainLooper()).post(() -> 
            Toast.makeText(context, "Lỗi đọc: " + error, Toast.LENGTH_SHORT).show());
    }

    public void release() {
        stop();
        currentPlayingText = null;
    }
}

