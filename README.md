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

Android version based on the following app: [Working With Audio](https://docs.microsoft.com/xamarin/android/app-fundamentals/android-audio). This simple application provides the following functionality:

- Recording audio using the high-level API ([MediaRecorder](https://developer.android.com/reference/android/media/MediaRecorder), [MediaCapture](https://docs.microsoft.com/en-us/uwp/api/windows.media.capture.mediacapture))

- Playing audio using the high-level API ([MediaPlayer](https://developer.android.com/guide/topics/media/mediaplayer), [MediaPlayer](https://docs.microsoft.com/en-us/uwp/api/windows.media.playback.mediaplayer))

- Remixing audio recordings using OGG Opus API ([Concentus](https://github.com/lostromb/concentus.oggfile), [NAudio](https://github.com/naudio/NAudio), [Windows.Media.Transcoding](https://docs.microsoft.com/en-us/uwp/api/windows.media.transcoding))

- Time Pickers using Syncfusion Time Picker for Xamarin ([Syncfusion](https://help.syncfusion.com/xamarin/timepicker/)).
	- **Important** -- You must independently obtain the Syncfusion Xamarin dependencies from Syncfusion under a community, commercial or other license.

Recordings are persisted between sessions using a SQLite database. Once an operation has been started, it must be stopped. No other actions are allowed in the meantime. The best way to demonstrate recording and playback is to record some sound and then play it back again. The best way to remix recordings is to record multiple sounds, and then specify the time intervals and orders that you want them to be remixed in.