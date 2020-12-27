---
name: Music Recorder
description: 'App for recording and remixing music'
page_type: sample
languages:
- csharp
products:
- xamarin
urlFragment: musicrecorder
---
# Music Recorder

Based on the following app: [Working With Audio](https://docs.microsoft.com/xamarin/android/app-fundamentals/android-audio). This simple application provides the following functionality:

- Recording audio using the high-level API ([MediaRecorder](https://developer.android.com/reference/android/media/MediaRecorder))
- Playing audio using the high-level API ([MediaPlayer](https://developer.android.com/guide/topics/media/mediaplayer))
- Remixing audio recordings using OGG Opus API ([Concentus](https://github.com/lostromb/concentus.oggfile), [NAudio](https://github.com/naudio/NAudio))

Recordings are persisted between sessions using a SQLite database. Once an operation has been started, it must be stopped. No other actions are allowed in the meantime. The best way to demonstrate recording and playback is to record some sound and then play it back again. The best way to remix recordings is to record multiple sounds, and then specify the time intervals and orders that you want them to be remixed in.

This app is licensed under the Microsoft Public License.