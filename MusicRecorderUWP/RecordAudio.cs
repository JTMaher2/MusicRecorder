using System;
using Io.Github.Jtmaher2.MusicRecorder.Services;
using Xamarin.Essentials;
using System.Threading.Tasks;
using Windows.Media.Transcoding;
using Windows.Media.Capture;
using Windows.Storage;
using Windows.Media.MediaProperties;
using Windows.UI.Popups;
using Windows.Media.Core;
using Windows.Media.Playback;
using Windows.UI.Xaml;
using System.Linq;
using System.IO;
using System.Collections.Generic;
using Windows.Media.Editing;

namespace MusicRecorderUWP
{
    //
    // Shows how to use the MediaRecorder to record audio.
    //
    public class RecordAudio : IRecordAudio
	{
		bool mCompleted = false; // has the playback completed?
		private MediaCapture mediaCapture;
		private LowLagMediaRecording _mediaRecording;
		private MediaPlayer PlayMusic;
		private DispatcherTimer mDT;
		private static readonly int TICK_MULTIPLIER = 10000;

		public async Task<PermissionStatus> CheckAndRequestMicPermission()
		{
			var status = await Permissions.CheckStatusAsync<Permissions.Microphone>();

			if (status == PermissionStatus.Granted)
				return status;

			if (Permissions.ShouldShowRationale<Permissions.Microphone>())
			{
				// Prompt the user with additional information as to why the permission is needed
				var dialog = new MessageDialog("Please enable the microphone permission in order to record audio.");
				await dialog.ShowAsync();
			}

			status = await Permissions.RequestAsync<Permissions.Microphone>();

			return status;
		}

		public async Task<PermissionStatus> CheckAndRequestMediaPermission()
		{
			var status = await Permissions.CheckStatusAsync<Permissions.Media>();

			if (status == PermissionStatus.Granted)
				return status;

			if (Permissions.ShouldShowRationale<Permissions.Media>())
			{
				// Prompt the user with additional information as to why the permission is needed
				var dialog = new MessageDialog("Please enable the media permission in order to record audio.");
				await dialog.ShowAsync();
			}

			status = await Permissions.RequestAsync<Permissions.StorageWrite>();

			return status;
		}

		public async void Start (string fileName)
		{
			await CheckAndRequestMicPermission();
			await CheckAndRequestMediaPermission();

			// create file in folder
			mediaCapture = new MediaCapture();
			await mediaCapture.InitializeAsync(new MediaCaptureInitializationSettings
			{
				MediaCategory = MediaCategory.Media,
				StreamingCaptureMode = StreamingCaptureMode.Audio
			});

			_mediaRecording = await mediaCapture.PrepareLowLagRecordToStorageFileAsync(
					MediaEncodingProfile.CreateFlac(AudioEncodingQuality.High), await ApplicationData.Current.LocalFolder.CreateFileAsync(fileName + ".flac", CreationCollisionOption.GenerateUniqueName));
			await _mediaRecording.StartAsync();
		}

		public async void Stop ()
		{
			await _mediaRecording.StopAsync();
			await _mediaRecording.FinishAsync();
			mediaCapture.Dispose();
		}

		public async void PreviewRecording(string fileName, long seekToMS, long stopAtMS)
        {
			PlayMusic = new MediaPlayer();
			mCompleted = false; // the playback is in progress

			var Folder = ApplicationData.Current.LocalFolder;
			IOrderedEnumerable<StorageFile> sf = (await Folder.GetFilesAsync()).OrderBy(p => p.DateCreated);

			StorageFile foundFile = null;

			// if there are multiple recordings with same name, get most recent one
			for (int f = 0; f < sf.Count(); f++)
            {
				if (sf.Skip(f).First().Name == fileName)
                {
					foundFile = sf.Skip(f).First();
					break;
                }
            }

            long diff = stopAtMS - seekToMS;

            if (diff > 0)
            {
				mDT = new DispatcherTimer();
				mDT.Tick += Dt_Tick;
                mDT.Interval = new TimeSpan((stopAtMS - seekToMS) * TICK_MULTIPLIER);
                mDT.Start();
            }

            PlayMusic.Source = MediaSource.CreateFromStorageFile(foundFile);
			PlayMusic.PlaybackSession.Position = new TimeSpan(seekToMS * TICK_MULTIPLIER);
            PlayMusic.MediaEnded += PlayMusic_MediaEnded;
            PlayMusic.Play();
        }

        private void Dt_Tick(object sender, object e)
        {
            StopPreviewRecording();
        }

        private void PlayMusic_MediaEnded(MediaPlayer sender, object args)
        {
            mCompleted = true; // the playback has completed
        }

        public void StopPreviewRecording()
        {

			if (PlayMusic != null)
			{
				try { PlayMusic.Pause(); } catch (Exception) { }
				PlayMusic.Dispose();
			}
			mCompleted = true;
			if (mDT != null)
			{
				mDT.Stop();
			}
        }

        public bool IsCompleted()
        {
            return mCompleted;
        }

        public async void EncodeFlac(string source, string dest)
        {
            PrepareTranscodeResult res = await new MediaTranscoder().PrepareFileTranscodeAsync(await StorageFile.GetFileFromPathAsync(source),
                await (await StorageFolder.GetFolderFromPathAsync(dest.Substring(0, dest.LastIndexOf('\\')))).CreateFileAsync(dest.Substring(dest.LastIndexOf('\\') + 1)),
                MediaEncodingProfile.CreateFlac(AudioEncodingQuality.High));

            if (res.CanTranscode)
            {
                await res.TranscodeAsync();
            }

            File.Delete(source);
        }

        public async void WriteFlacRemix(List<string> sources, List<TimeSpan> startTimes, List<TimeSpan> stopTimes, string dest)
        {
            MediaComposition composition = new MediaComposition();

            for (int s = 0; s < sources.Count; s++)
            {
                StorageFolder folderPath = await StorageFolder.GetFolderFromPathAsync(sources[s].Substring(0, sources[s].LastIndexOf('\\')));
                MediaClip mc = await MediaClip.CreateFromFileAsync(await folderPath.GetFileAsync(sources[s].Substring(sources[s].LastIndexOf('\\') + 1)));
                mc.TrimTimeFromStart = startTimes[s];
				long l = (long)((mc.OriginalDuration.TotalMilliseconds - stopTimes[s].TotalMilliseconds) * TICK_MULTIPLIER);
				if (l > 0)
				{
					mc.TrimTimeFromEnd = new TimeSpan(l);
				}
                composition.Clips.Add(mc);
            }
            StorageFolder sf = await StorageFolder.GetFolderFromPathAsync(dest.Substring(0, dest.LastIndexOf('\\')));
            await composition.RenderToFileAsync(await sf.CreateFileAsync(dest.Substring(dest.LastIndexOf('\\') + 1)), MediaTrimmingPreference.Precise, MediaEncodingProfile.CreateFlac(AudioEncodingQuality.High));
        }

		public void WriteFile(string fileName, string origFileName)
        {
            File.WriteAllBytes(ApplicationData.Current.LocalFolder.Path + "\\" + fileName + ".flac", File.ReadAllBytes(ApplicationData.Current.LocalFolder.Path + "\\" + origFileName + ".flac"));
            File.Delete(ApplicationData.Current.LocalFolder.Path + "\\" + origFileName + ".flac");
        }
	}
}