# NotificationReader Android App

NotificationReader is an Android application that reads notifications from various apps, analyzes the language of the messages, and reads them aloud. Currently, the app supports reading notifications from WhatsApp, but it is designed to be easily extendable to support other apps in the future.

## Features

- **Multilingual Notification Reader**: The app identifies the language of incoming notification messages and reads them aloud using Android Text-to-Speech functionality.

- **Google ML Kit Integration**: NotificationReader leverages Google ML Kit for Language Identification, enabling accurate and efficient language recognition.

- **NotificationService**: The app utilizes the Android NotificationService to listen for and process incoming notifications from supported apps.

## How to Use

1. Clone the repository to your local machine:

   ```
   git clone https://github.com/nikhiljainlive/NotificationReader.git
   ```

2. Open the project in Android Studio.

3. Build and run the app on your Android device or emulator.

4. Grant the necessary notification access permission for the app to read notifications.

5. Start/Stop Reading Service: Use the "Start Reading Service" button to initiate the foreground service, which listens to incoming notifications from WhatsApp. To stop the foreground service, click on "Stop Reading Service".

6. Receive notifications from WhatsApp, and the app will read them aloud based on the detected language.

## Dependencies

- Android SDK
- Google ML Kit
- Android Text-to-Speech API

## License

This project is licensed under the [MIT License](https://opensource.org/licenses/MIT).
