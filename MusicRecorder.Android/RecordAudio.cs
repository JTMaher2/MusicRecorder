using System;
using System.IO;
using Android.OS;
using Android.Media;
using Io.Github.Jtmaher2.MusicRecorder.Services;
using Xamarin.Essentials;
using System.Threading.Tasks;
using Android.Widget;
using Android.App;
using System.Collections.Generic;

namespace Io.Github.Jtmaher2.MusicRecorder.Droid
{
    class CountDown : CountDownTimer
	{
		private readonly MediaPlayer Player;

		public CountDown(MediaPlayer player, long totalTime, long interval) : base(totalTime, interval)
		{
			Player = player; 
		}

		public override void OnFinish()
		{
			Player.Stop();
		}

		public override void OnTick(long millisUntilFinished)
		{
		}
	}

	//
	// Shows how to use the MediaRecorder to record audio.
	//
	public class RecordAudio : IRecordAudio
	{
		MediaPlayer player = null;
		static string filePath = FileSystem.AppDataDirectory;
		MediaRecorder recorder = null;
		bool mCompleted = false; // has the playback completed?

		public async Task<PermissionStatus> CheckAndRequestMicPermission()
		{
			var status = await Permissions.CheckStatusAsync<Permissions.Microphone>();

			if (status == PermissionStatus.Granted)
				return status;

			if (Permissions.ShouldShowRationale<Permissions.Microphone>())
			{
				// Prompt the user with additional information as to why the permission is needed
				Toast.MakeText(Application.Context, "Please enable the microphone permission in order to record audio.", ToastLength.Long);
			}

			status = await Permissions.RequestAsync<Permissions.Microphone>();

			return status;
		}

		public async Task<PermissionStatus> CheckAndRequestWriteExternalStoragePermission()
		{
			var status = await Permissions.CheckStatusAsync<Permissions.StorageWrite>();

			if (status == PermissionStatus.Granted)
				return status;

			if (Permissions.ShouldShowRationale<Permissions.StorageWrite>())
			{
				// Prompt the user with additional information as to why the permission is needed
				Toast.MakeText(Application.Context, "Please enable the write external storage permission in order to record audio.", ToastLength.Long);
			}

			status = await Permissions.RequestAsync<Permissions.StorageWrite>();

			return status;
		}

		public async Task<PermissionStatus> CheckAndRequestReadExternalStoragePermission()
		{
			var status = await Permissions.CheckStatusAsync<Permissions.StorageRead>();

			if (status == PermissionStatus.Granted)
				return status;

			if (Permissions.ShouldShowRationale<Permissions.StorageRead>())
			{
				// Prompt the user with additional information as to why the permission is needed
				Toast.MakeText(Application.Context, "Please enable the read external storage permission in order to record audio.", ToastLength.Long);
			}

			status = await Permissions.RequestAsync<Permissions.StorageRead>();

			return status;
		}

		public async void Start(string fileName)
		{
			await CheckAndRequestMicPermission();
			await CheckAndRequestWriteExternalStoragePermission();
			await CheckAndRequestReadExternalStoragePermission();

			if (filePath.LastIndexOf('.') > filePath.LastIndexOf('/'))
            {
				filePath = filePath.Substring(0, filePath.LastIndexOf('/') + 1);
            }
			if (!Directory.Exists(filePath))
            {
				Directory.CreateDirectory(filePath);
            }
			filePath += "/" + fileName.Replace('|', '_').Replace('\\', '_').Replace('?', '_').Replace('*', '_').Replace('<', '_').Replace('"', '_').Replace(':', '_').Replace('>', '_').Replace('+', '_').Replace('[', '_').Replace(']', '_').Replace('/', '_').Replace('\'', '_') + ".opus";

			File.Create(filePath);
			if (File.Exists (filePath))
				File.Delete (filePath);

			if (recorder == null)
				recorder = new MediaRecorder(); // Initial state.
			else
				recorder.Reset();

			recorder.SetAudioSource(AudioSource.Mic);
			recorder.SetOutputFormat(OutputFormat.Ogg);
			recorder.SetAudioEncoder(AudioEncoder.Opus); // Initialized state.
			recorder.SetOutputFile(filePath); // DataSourceConfigured state.
			recorder.Prepare(); // Prepared state
			recorder.Start(); // Recording state.
		}

		public void Stop()
		{
			if (recorder != null)
			{
				recorder.Stop();
				recorder.Release();
				recorder = null;
			}
		}

		public async void PreviewRecording(string fileName, long seekToMS, long stopAtMS)
		{
			mCompleted = false; // the playback is in progress
			try
			{
				if (player == null)
				{
					player = new MediaPlayer();
				}
				else
				{
					player.Reset();
				}

				// This method works better than setting the file path in SetDataSource. Don't know why.
				await player.SetDataSourceAsync(new Java.IO.FileInputStream(new Java.IO.File(filePath.Substring(0, filePath.LastIndexOf("/")) + "/" + (fileName.EndsWith(".opus") ? fileName.Substring(0, fileName.LastIndexOf('.')) : fileName) + ".opus")).FD);

				player.Prepare();
				player.SeekTo(seekToMS, MediaPlayerSeekMode.Closest);

				long diff = stopAtMS - seekToMS;

				if (diff > 0)
				{
					new CountDown(player, diff, 1000).Start();
				}

				player.Start();

                player.MediaTimeDiscontinuity += Player_MediaTimeDiscontinuity;
			}
			catch (Exception ex)
			{
				Console.Out.WriteLine(ex.StackTrace);
			}
		}

        private void Player_MediaTimeDiscontinuity(object sender, MediaPlayer.MediaTimeDiscontinuityEventArgs e)
        {
            if (!e.Mp.IsPlaying && !mCompleted)
            {
				mCompleted = true; // the playback has completed
			}
        }

        public void StopPreviewRecording()
		{
			if (player != null)
            {
				player.Stop();
			}
        }

        public bool IsCompleted()
		{
			return mCompleted;
        }

        public void EncodeFlac(string source, string dest)
		{
            throw new NotImplementedException();
        }

        public void WriteFlacRemix(List<string> sources, List<TimeSpan> startTimes, List<TimeSpan> stopTimes, string dest)
		{
            throw new NotImplementedException();
        }

        public void WriteFile(string fileName, string origFileName)
		{
            throw new NotImplementedException();
        }
    }
}