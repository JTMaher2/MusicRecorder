using System;
using System.IO;
using Android.OS;
using Android.Media;
using Io.Github.Jtmaher2.MusicRecorder.Services;
using Xamarin.Essentials;
using System.Threading.Tasks;
using Android.Widget;
using Android.App;

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
		static string filePath = "/storage/emulated/0/Android/media/io.github.jtmaher2.musicrecorder/";
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

		public async void Start (string fileName)
		{
			await CheckAndRequestMicPermission();

			if (filePath.LastIndexOf('.') > filePath.LastIndexOf('/'))
            {
				filePath = filePath.Substring(0, filePath.LastIndexOf('/'));
            }
			try {
				if (!Directory.Exists(filePath))
                {
					Directory.CreateDirectory(filePath);
                }
				filePath += fileName.Replace('|', '_').Replace('\\', '_').Replace('?', '_').Replace('*', '_').Replace('<', '_').Replace('"', '_').Replace(':', '_').Replace('>', '_').Replace('+', '_').Replace('[', '_').Replace(']', '_').Replace('/', '_').Replace('\'', '_') + ".ogg";

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

			} catch (Exception ex) {
				Console.Out.WriteLine (ex.StackTrace);
			}
		}

		public void Stop ()
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
				Java.IO.File file;
				if (string.IsNullOrWhiteSpace(fileName))
				{
					// new recording
					file = new Java.IO.File(filePath);
				} else
                {
					// existing recording
					file = new Java.IO.File(filePath.Substring(0, filePath.LastIndexOf("/")) + "/" + (fileName.EndsWith(".ogg") ? fileName.Substring(0, fileName.LastIndexOf('.')) : fileName) + ".ogg");
				}
				Java.IO.FileInputStream fis = new Java.IO.FileInputStream(file);
				await player.SetDataSourceAsync(fis.FD);

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
    }
}