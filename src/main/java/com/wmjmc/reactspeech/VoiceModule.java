package com.wmjmc.reactspeech;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.wmjmc.reactspeech.LocaleConstants;


public class VoiceModule extends ReactContextBaseJavaModule {

    static final int REQUEST_SPEECH_ACTIVITY = 1;

    final ReactApplicationContext reactContext;
    private Promise mVoicepromise;
    private Intent intent;
    private SpeechRecognizer speech = null;
    private String LOG_TAG = "MainActivity";

    public VoiceModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "SpeechAndroid";
    }

    @Override
    public Map<String, Object> getConstants() {
        return Constants.getConstants();
    }

    RecognitionListener recognitionListener = new RecognitionListener() {
        @Override
        public void onBeginningOfSpeech() {
            Log.i(LOG_TAG, "onBeginningOfSpeech");
        }

        @Override
        public void onBufferReceived(byte[] buffer) {
            Log.i(LOG_TAG, "onBufferReceived: " + buffer);
        }

        @Override
        public void onEndOfSpeech() {
            Log.i(LOG_TAG, "onEndOfSpeech");
        }

        @Override
        public void onError(int errorCode) {
            String errorMessage = getErrorText(errorCode);
            Log.d(LOG_TAG, "FAILED " + errorMessage);
            if (errorMessage.equals("No match") || errorMessage.equals("No speech input")) {
                restartSpeech();
            }
        }

        @Override
        public void onEvent(int arg0, Bundle arg1) {
            Log.i(LOG_TAG, "onEvent");
        }

        @Override
        public void onPartialResults(Bundle arg0) {
            Log.i(LOG_TAG, "onPartialResults");
        }

        @Override
        public void onReadyForSpeech(Bundle arg0) {
            Log.i(LOG_TAG, "onReadyForSpeech");
        }

        @Override
        public void onResults(Bundle results) {
            Log.i(LOG_TAG, "onResults");
            ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            String text = "";
            for (String result : matches)
                text += result + "\n";
            Log.d(LOG_TAG, text);
            if (matches.contains("next")) {
                mVoicepromise.resolve("next");
            } else if (matches.contains("back")) {
                mVoicepromise.resolve("back");
            } else if (matches.contains("yes")) {
                mVoicepromise.resolve("yes");
            } else if (matches.contains("no")) {
                mVoicepromise.resolve("no");
            } else if (matches.contains("start")) {
                mVoicepromise.resolve("start");
            } else if (matches.contains("restart")) {
                mVoicepromise.resolve("restart");
            } else if (matches.contains("repeat")) {
                mVoicepromise.resolve("repeat");
            } else if (matches.contains("stop")) {
                mVoicepromise.resolve("stop");
            } else {
//                mVoicepromise.resolve(matches.get(0));
                restartSpeech();
            }
        }

        @Override
        public void onRmsChanged(float rmsdB) {
//            Log.i(LOG_TAG, "onRmsChanged: " + rmsdB);
        }
    };

    @ReactMethod
    public void enableBeep() {
        final Activity currentActivity = getCurrentActivity();

        currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (speech != null) {
                    speech.stopListening();
                }
            }
        });
        
        AudioManager amanager=(AudioManager) currentActivity.getSystemService(Context.AUDIO_SERVICE);
        amanager.setStreamMute(AudioManager.STREAM_MUSIC, false);
    }

    @ReactMethod
    public void startSpeech(String prompt, String locale, final Promise promise) {
        Activity currentActivity = getCurrentActivity();

        if (currentActivity == null) {
            promise.reject(ErrorConstants.E_ACTIVITY_DOES_NOT_EXIST);
            return;
        }

        mVoicepromise = promise;
        
        startSpeechWithoutBeep();
    }

    public void startSpeechWithoutBeep() {
        final Activity currentActivity = getCurrentActivity();

        currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                speech = SpeechRecognizer.createSpeechRecognizer(reactContext);
                speech.setRecognitionListener(recognitionListener);
                intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,"en");
                intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,reactContext.getPackageName());
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
                intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 5000);
                intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);

                AudioManager amanager=(AudioManager) currentActivity.getSystemService(Context.AUDIO_SERVICE);
                amanager.setStreamMute(AudioManager.STREAM_MUSIC, true);
                
                speech.startListening(intent);
            }
        });

    }

    public void restartSpeech() {
        speech.stopListening();
        speech.startListening(intent);
    }

    public static String getErrorText(int errorCode) {
        String message;
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                message = "Audio recording error";
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                message = "Client side error";
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                message = "Insufficient permissions";
                break;
            case SpeechRecognizer.ERROR_NETWORK:
                message = "Network error";
                break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                message = "Network timeout";
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                message = "No match";
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                message = "RecognitionService busy";
                break;
            case SpeechRecognizer.ERROR_SERVER:
                message = "error from server";
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                message = "No speech input";
                break;
            default:
                message = "Didn't understand, please try again.";
                break;
        }
        return message;
    }

}
