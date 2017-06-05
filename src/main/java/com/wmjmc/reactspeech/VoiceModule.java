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
            mVoicepromise.resolve(matches.get(0));
        }

        @Override
        public void onRmsChanged(float rmsdB) {
//            Log.i(LOG_TAG, "onRmsChanged: " + rmsdB);
        }
    };

    @ReactMethod
    public void startSpeech(String prompt, String locale, final Promise promise) {
        Activity currentActivity = getCurrentActivity();

        if (currentActivity == null) {
            promise.reject(ErrorConstants.E_ACTIVITY_DOES_NOT_EXIST);
            return;
        }

        mVoicepromise = promise;
        currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                speech = SpeechRecognizer.createSpeechRecognizer(reactContext);
                speech.setRecognitionListener(recognitionListener);
                intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,"en");
                intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,reactContext.getPackageName());
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
                intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000);
                intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 2);
                speech.startListening(intent);
            }
        });
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
/*
    @Override
    public void onActivityResult(
        Activity activity,
        int requestCode,
        int resultCode,
        Intent data
    ) {
        this.onActivityResult(requestCode, resultCode, data);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mVoicepromise == null) {
            return;
        }

        switch (resultCode){
            case Activity.RESULT_OK:
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                mVoicepromise.resolve(result.get(0));
                mVoicepromise = null;
                break;
            case Activity.RESULT_CANCELED:
                mVoicepromise.reject(ErrorConstants.E_VOICE_CANCELLED);
                mVoicepromise = null;
                break;
            case RecognizerIntent.RESULT_AUDIO_ERROR:
                mVoicepromise.reject(ErrorConstants.E_AUDIO_ERROR);
                mVoicepromise = null;
                break;
            case RecognizerIntent.RESULT_NETWORK_ERROR:
                mVoicepromise.reject(ErrorConstants.E_NETWORK_ERROR);
                mVoicepromise = null;
                break;
            case RecognizerIntent.RESULT_NO_MATCH:
                mVoicepromise.reject(ErrorConstants.E_NO_MATCH);
                mVoicepromise = null;
                break;
            case RecognizerIntent.RESULT_SERVER_ERROR:
                mVoicepromise.reject(ErrorConstants.E_SERVER_ERROR);
                mVoicepromise = null;
                break;
        }
    }

    public void onNewIntent(Intent intent) {
        // no-op
    }

    private String getPrompt(String prompt){
        if(prompt != null && !prompt.equals("")){
            return prompt;
        }

        return "Say something";
    }

    private String getLocale(String locale){
        if(locale != null && !locale.equals("")){
            return locale;
        }

        return Locale.getDefault().toString();
    } */
}
