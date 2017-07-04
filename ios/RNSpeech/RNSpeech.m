//
//  RNSpeech.m
//  RNSpeech
//
//  Created by Alex on 6/5/17.
//  Copyright © 2017 Alex. All rights reserved.
//

#import "RNSpeech.h"
#import <React/RCTUtils.h>
#import <React/RCTBridge.h>
#import <React/RCTEventDispatcher.h>

@interface RNSpeech () {
    SFSpeechRecognizer *speechRecognizer;
    SFSpeechAudioBufferRecognitionRequest *recognitionRequest;
    SFSpeechRecognitionTask *recognitionTask;
    AVAudioEngine *audioEngine;
    bool isResult;
    NSTimer *timer;
    bool isFirst;
}

@property (nonatomic, weak, readwrite) RCTBridge *bridge;

@end

@implementation RNSpeech

RCT_EXPORT_MODULE()

- (NSArray<NSString *> *)supportedEvents
{
    return @[@"RNSpeech"];
}


RCT_EXPORT_METHOD(startSpeech) {
    [self callSpeech];
}

RCT_EXPORT_METHOD(enableBeep) {
    [self setAudioCategoryForPlayback];
}

-(void)setAudioCategoryForPlayback {
    NSError *error;
    AVAudioSession *audioSession = [AVAudioSession sharedInstance];
    [audioSession setCategory:AVAudioSessionCategoryPlayback error:&error];// withOptions:AVAudioSessionCategoryOptionMixWithOthers
    [audioSession setActive:YES error:&error];
}

-(void)callSpeech {
    if ([timer isValid]) {
        [timer invalidate];
        timer = nil;
    }
    
    isFirst = false;
    
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        timer = [NSTimer scheduledTimerWithTimeInterval:60 target:self selector:@selector(restartSpeech) userInfo:nil repeats:YES];
        [[NSRunLoop currentRunLoop] addTimer:timer forMode:UITrackingRunLoopMode];
        [[NSRunLoop currentRunLoop] run];
    });
    
    speechRecognizer = [[SFSpeechRecognizer alloc] initWithLocale:[[NSLocale alloc] initWithLocaleIdentifier:@"en_US"]];
    
    speechRecognizer.delegate = self;
    
    [SFSpeechRecognizer requestAuthorization:^(SFSpeechRecognizerAuthorizationStatus status) {
        switch (status) {
            case SFSpeechRecognizerAuthorizationStatusAuthorized:
                NSLog(@"Authorized");
                break;
            case SFSpeechRecognizerAuthorizationStatusDenied:
                NSLog(@"Denied");
                break;
            case SFSpeechRecognizerAuthorizationStatusNotDetermined:
                NSLog(@"Not Determined");
                break;
            case SFSpeechRecognizerAuthorizationStatusRestricted:
                NSLog(@"Restricted");
                break;
            default:
                break;
        }
    }];
    
    [self restartListening];
}

-(void)restartSpeech:(NSTimer *)theTimer {
    NSLog(@"----- Restarted Speech -----");
    if (!isFirst) {
        isFirst = true;
    } else {
        [self restartListening];
    }
}

-(void)restartListening {
    if (audioEngine.isRunning) {
        [audioEngine stop];
        [recognitionRequest endAudio];
        [self startListening];
    } else {
        [self startListening];
    }
}

- (void)startListening {
    isResult = false;
    
    // Initialize the AVAudioEngine
    audioEngine = [[AVAudioEngine alloc] init];
    // Make sure there's not a recognition task already running
    if (recognitionTask) {
        [recognitionTask cancel];
        recognitionTask = nil;
    }
    
    // Starts an AVAudio Session
    NSError *error;
    AVAudioSession *audioSession = [AVAudioSession sharedInstance];
    [audioSession setCategory:AVAudioSessionCategoryRecord withOptions:AVAudioSessionCategoryOptionAllowBluetooth error:&error];
    if (error != nil) {
        return;
    }
    // [audioSession setMode:AVAudioSessionModeMeasurement error:&error];
    if (error != nil) {
        return;
    }
    
    [audioSession setActive:YES error:&error];
    if (error != nil) {
        return;
    }
    
    // Starts a recognition process, in the block it logs the input or stops the audio
    // process if there's an error.
    recognitionRequest = [[SFSpeechAudioBufferRecognitionRequest alloc] init];
    if (recognitionRequest == nil){
        return;
    }
    if (audioEngine == nil) {
        audioEngine = [[AVAudioEngine alloc] init];
    }
    AVAudioInputNode *inputNode = audioEngine.inputNode;
    if (inputNode == nil) {
        return;
    }
    
    recognitionRequest.shouldReportPartialResults = YES;
    
    recognitionTask = [speechRecognizer recognitionTaskWithRequest:recognitionRequest resultHandler:^(SFSpeechRecognitionResult * _Nullable result, NSError * _Nullable error) {
        BOOL isFinal = NO;
        
        if (error != nil) {
            return;
        }
        
        if (result) {
            
            // Whatever you say in the microphone after pressing the button should be being logged
            // in the console.
            NSLog(@"RESULT:%@",result.bestTranscription.formattedString);
            isFinal = !result.isFinal;
            NSString *spokenText = [result.bestTranscription.formattedString lowercaseString];
            if ([spokenText containsString:@"next"] || [spokenText containsString:@"xt"] || [spokenText containsString:@"st"]) {
                spokenText = @"next";
                [self stopSpeech:spokenText];
            } else if ([spokenText containsString:@"back"]) {
                spokenText = @"back";
                [self stopSpeech:spokenText];
            } else if ([spokenText containsString:@"yes"]) {
                spokenText = @"yes";
                [self stopSpeech:spokenText];
            } else if ([spokenText containsString:@"no"]) {
                spokenText = @"no";
                [self stopSpeech:spokenText];
            } else if ([spokenText containsString:@"repeat"]) {
                spokenText = @"repeat";
                [self stopSpeech:spokenText];
            } else if ([spokenText containsString:@"start"]) {
                spokenText = @"start";
                [self stopSpeech:spokenText];
            } else if ([spokenText containsString:@"restart"]) {
                spokenText = @"restart";
                [self stopSpeech:spokenText];
            } else if ([spokenText containsString:@"stop"]) {
                spokenText = @"stop";
                [self stopSpeech:spokenText];
            } else {
                NSLog(@"---Not matched word--");
                spokenText = @"nomatch";
                [self stopSpeech:spokenText];
            }
        }
        
        if (error) {
            NSLog(@"---error---");
            [audioEngine stop];
            [recognitionRequest endAudio];
            [inputNode removeTapOnBus:0];
            recognitionRequest = nil;
            recognitionTask = nil;
        }
    }];
    
    // Sets the recording format
    AVAudioFormat *recordingFormat = [inputNode outputFormatForBus:0];
    [inputNode installTapOnBus:0 bufferSize:1024 format:recordingFormat block:^(AVAudioPCMBuffer * _Nonnull buffer, AVAudioTime * _Nonnull when) {
        if (recognitionRequest != nil) {
            [recognitionRequest appendAudioPCMBuffer:buffer];
        }
    }];
    // Starts the audio engine, i.e. it starts listening.
    [audioEngine prepare];
    [audioEngine startAndReturnError:&error];
    if (error != nil) {
        return;
    }
    
    NSLog(@"Say Something, I'm listening");
}

-(void)stopSpeech:(NSString *)txt {
    NSLog(@"stopSpeech");
    [audioEngine stop];
    [recognitionRequest endAudio];
    recognitionRequest = nil;
    [recognitionTask cancel];
    recognitionTask = nil;
    
    if ([timer isValid]) {
        [timer invalidate];
        timer = nil;
    }
    
    if (!isResult) {
        isResult = true;
        NSLog(@"Send Speech Result");
        [self.bridge.eventDispatcher sendAppEventWithName:@"RNSpeech" body:txt];
        //        [self sendEventWithName:@"RNSpeech" body:txt];
    }
    
}

#pragma mark - SFSpeechRecognizerDelegate Delegate Methods

- (void)speechRecognizer:(SFSpeechRecognizer *)speechRecognizer availabilityDidChange:(BOOL)available {
    NSLog(@"Availability:%d",available);
}

@end
