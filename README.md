# react-native-background-speech

react-native-background-speech is a speech-to-text library without dialog for [React Native](https://facebook.github.io/react-native/) for the Android Platform.

## Install

```shell
npm install --save react-native-background-speech
```
## Usage
### Linking the Library
### Add it to your android project

* In `android/settings.gradle`

```gradle
...
include ':VoiceModule', ':app'
project(':VoiceModule').projectDir = new File(rootProject.projectDir, '../node_modules/react-native-background-speech')
```

* In `android/app/build.gradle`

```gradle
...
dependencies {
    ...
    compile project(':VoiceModule')
}
```
* Register Module (in MainApplication.java)

```java
import com.wmjmc.reactspeech.VoicePackage;  // <--- import

public class MainApplication extends Application implements ReactApplication {
...
    @Override
    protected List<ReactPackage> getPackages() {
      return Arrays.<ReactPackage>asList(
        new MainReactPackage(),
        new VoicePackage()); // <--- added here
    }
}
```
## Example

```javascript
import SpeechAndroid from 'react-native-background-speech';

...
async _buttonClick(){
    try{
        //More Locales will be available upon release.
        var spokenText = await SpeechAndroid.startSpeech("Speak yo", SpeechAndroid.GERMAN);
        ToastAndroid.show(spokenText , ToastAndroid.LONG);
    }catch(error){
        switch(error){
            case SpeechAndroid.E_VOICE_CANCELLED:
                ToastAndroid.show("Voice Recognizer cancelled" , ToastAndroid.LONG);
                break;
            case SpeechAndroid.E_NO_MATCH:
                ToastAndroid.show("No match for what you said" , ToastAndroid.LONG);
                break;
            case SpeechAndroid.E_SERVER_ERROR:
                ToastAndroid.show("Google Server Error" , ToastAndroid.LONG);
                break;
            /*And more errors that will be documented on Docs upon release*/
        }
    }
}
...
```

This will automatically start recognizing and adjusting for the German Language.
On release I'll update these docs with every single Locale available.

## Methods

### startSpeech(prompt, locale)
Initializes the voice recognition activity and returns what you spoke in text.




