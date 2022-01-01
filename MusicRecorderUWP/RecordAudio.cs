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

		public async Task<PermissionStatus> CheckAndRequestStorageReadPermission()
		{
			var status = await Permissions.CheckStatusAsync<Permissions.StorageRead>();

			if (status == PermissionStatus.Granted)
				return status;

			if (Permissions.ShouldShowRationale<Permissions.StorageRead>())
			{
				// Prompt the user with additional information as to why the permission is needed
				var dialog = new MessageDialog("Please enable the storage read permission in order to import media.");
				await dialog.ShowAsync();
			}

			status = await Permissions.RequestAsync<Permissions.StorageRead>();

			return status;
		}

		public async Task<PermissionStatus> CheckAndRequestStorageWritePermission()
		{
			var status = await Permissions.CheckStatusAsync<Permissions.StorageWrite>();

			if (status == PermissionStatus.Granted)
				return status;

			if (Permissions.ShouldShowRationale<Permissions.StorageWrite>())
			{
				// Prompt the user with additional information as to why the permission is needed
				var dialog = new MessageDialog("Please enable the storage write permission in order to save recordings.");
				await dialog.ShowAsync();
			}

			status = await Permissions.RequestAsync<Permissions.StorageWrite>();

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
				var dialog = new MessageDialog("Please enable the media permission in order to import audio.");
				await dialog.ShowAsync();
			}

			status = await Permissions.RequestAsync<Permissions.Media>();

			return status;
		}

		public async Task<string> Start (string fileName)
		{
			await CheckAndRequestMicPermission();
			await CheckAndRequestStorageWritePermission();

			// create file in folder
			mediaCapture = new MediaCapture();
			await mediaCapture.InitializeAsync(new MediaCaptureInitializationSettings
			{
				MediaCategory = MediaCategory.Media,
				StreamingCaptureMode = StreamingCaptureMode.Audio
			});

			StorageFile file = await ApplicationData.Current.LocalFolder.CreateFileAsync(fileName + ".mp3", CreationCollisionOption.GenerateUniqueName);

			_mediaRecording = await mediaCapture.PrepareLowLagRecordToStorageFileAsync(
				MediaEncodingProfile.CreateMp3(AudioEncodingQuality.High), file);
			
			await _mediaRecording.StartAsync();

			return file.DisplayName;
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

			for (int f = 0; f < sf.Count(); f++)
            {
				var thisFile = sf.Skip(f).First();
				if (thisFile.Name == fileName)
                {
					foundFile = thisFile;
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

        public async void EncodeMP3(string source, string dest)
        {
            PrepareTranscodeResult res = await new MediaTranscoder().PrepareFileTranscodeAsync(await StorageFile.GetFileFromPathAsync(source),
                await (await StorageFolder.GetFolderFromPathAsync(dest.Substring(0, dest.LastIndexOf('\\')))).CreateFileAsync(dest.Substring(dest.LastIndexOf('\\') + 1)),
                MediaEncodingProfile.CreateMp3(AudioEncodingQuality.High));

            if (res.CanTranscode)
            {
                await res.TranscodeAsync();
            }

            File.Delete(source);
        }

        public async void WriteMp3Remix(List<string> sources, List<TimeSpan> startTimes, List<TimeSpan> stopTimes, string dest)
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
            await composition.RenderToFileAsync(await sf.CreateFileAsync(dest.Substring(dest.LastIndexOf('\\') + 1)), MediaTrimmingPreference.Precise, MediaEncodingProfile.CreateMp3(AudioEncodingQuality.High));
        }

		public string WriteFile(string fileName, string origFileName)
        {
			if (fileName != origFileName) {
				// new name is different than old name
				if (File.Exists(ApplicationData.Current.LocalFolder.Path + "\\" + fileName))
				{
					// file already exists, generate unique name
					int num = 2;
					while (File.Exists(ApplicationData.Current.LocalFolder.Path + "\\" + fileName.TrimEnd('c').TrimEnd('a').TrimEnd('l').TrimEnd('f').TrimEnd('.') + " (" + num + ").mp3"))
					{
						num++;
					}
					fileName = fileName.TrimEnd('c').TrimEnd('a').TrimEnd('l').TrimEnd('f').TrimEnd('.') + " (" + num + ")";
					using (FileStream fs = File.Create(ApplicationData.Current.LocalFolder.Path + "\\" + fileName + ".mp3"))
					{
						byte[] bytes = File.ReadAllBytes(ApplicationData.Current.LocalFolder.Path + "\\" + origFileName);
						fs.Write(bytes, 0, bytes.Length);
					}
				}
				else
				{ // file doesn't already exist, use provided name
					using (FileStream fs = File.Create(ApplicationData.Current.LocalFolder.Path + "\\" + fileName + (fileName.EndsWith(".mp3") ? "" : ".mp3")))
					{
						byte[] bytes = File.ReadAllBytes(ApplicationData.Current.LocalFolder.Path + "\\" + origFileName + (origFileName.EndsWith(".mp3") ? "" : ".mp3"));
						fs.Write(bytes, 0, bytes.Length);
					}
				}

				// delete original file
				File.Delete(ApplicationData.Current.LocalFolder.Path + "\\" + origFileName);
			}

			return fileName;
        }

		public async Task<string> Import(string filePathEntry, string notes)
        {
			string fileName = filePathEntry.Substring(filePathEntry.LastIndexOf('\\') + 1);

			Windows.Storage.FileProperties.MusicProperties musicProperties =
				await (await ApplicationData.Current.LocalFolder.GetFileAsync(fileName)).Properties.GetMusicPropertiesAsync();

			// copy to local directory
			await FileIO.WriteBufferAsync(await ApplicationData.Current.LocalFolder.CreateFileAsync(musicProperties.Title + ".mp3",
				CreationCollisionOption.ReplaceExisting), await FileIO.ReadBufferAsync(await KnownFolders.MusicLibrary.GetFileAsync(fileName)));

			// add to library
			return System.Text.Json.JsonSerializer.Serialize(new MusicRecording
			{
				Composer = string.Join(',', musicProperties.Composers),
				RecordingName = musicProperties.Title,
				Notes = notes
			});
		}
    }
}