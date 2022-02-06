/*
 * Copyright 2022 James Maher

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

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
		static string mFilePath = FileSystem.AppDataDirectory;
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

		public async Task<string> Start(string fileName)
		{
			await CheckAndRequestMicPermission();
			await CheckAndRequestWriteExternalStoragePermission();
			await CheckAndRequestReadExternalStoragePermission();

			if (mFilePath.LastIndexOf('.') > mFilePath.LastIndexOf('/'))
            {
				mFilePath = mFilePath[..(mFilePath.LastIndexOf('/') + 1)];
            }
			string origFilePath = mFilePath;
			if (!Directory.Exists(mFilePath))
            {
				Directory.CreateDirectory(mFilePath);
            }
			string safeFileName = fileName.Replace('|', '_').Replace('\\', '_').Replace('?', '_').Replace('*', '_').Replace('<', '_').Replace('"', '_').Replace(':', '_').Replace('>', '_').Replace('+', '_').Replace('[', '_').Replace(']', '_').Replace('/', '_').Replace('\'', '_');
			mFilePath += "/" + safeFileName + ".opus";

			if (File.Exists (mFilePath))
            {
				// file already exists, generate unique name
				int num = 2;
				while (File.Exists(origFilePath + "/" + safeFileName + " (" + num + ").opus"))
				{
					num++;
				}
				mFilePath = origFilePath + "/" + safeFileName + " (" + num + ").opus";
            }

			using (FileStream fs = File.Create(mFilePath))
			{
				if (recorder == null)
					recorder = new MediaRecorder(Application.Context); // Initial state.
				else
					recorder.Reset();

				recorder.SetAudioSource(AudioSource.Mic);
				recorder.SetOutputFormat(OutputFormat.Ogg);
				recorder.SetAudioEncoder(AudioEncoder.Opus); // Initialized state.
				recorder.SetOutputFile(mFilePath); // DataSourceConfigured state.
				recorder.Prepare(); // Prepared state
				recorder.Start(); // Recording state.
			}

			return mFilePath[(origFilePath + "/").Length..].TrimEnd('s').TrimEnd('u').TrimEnd('p').TrimEnd('o').TrimEnd('.');
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
				await player.SetDataSourceAsync(new Java.IO.FileInputStream(new Java.IO.File(mFilePath[..mFilePath.LastIndexOf("/")] + "/files/" + (fileName.EndsWith(".opus") ? fileName[..fileName.LastIndexOf('.')] : fileName) + ".opus")).FD);

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

        public void EncodeMP3(string source, string dest)
		{
            throw new NotImplementedException();
        }

        public void WriteMp3Remix(List<string> sources, List<TimeSpan> startTimes, List<TimeSpan> stopTimes, string dest)
		{
            throw new NotImplementedException();
        }

        public string WriteFile(string fileName, string origFileName)
		{
            throw new NotImplementedException();
        }

        Task<string> IRecordAudio.Import(string filePathEntry, string notes)
        {
			MediaMetadataRetriever musicProperties = new MediaMetadataRetriever();
			musicProperties.SetDataSource(filePathEntry);

			// copy to local directory
			File.WriteAllBytes(FileSystem.AppDataDirectory + "/" + filePathEntry[(filePathEntry.LastIndexOf('/') + 1)..], File.ReadAllBytes(filePathEntry));
			
			// add to library
			return Task.FromResult(System.Text.Json.JsonSerializer.Serialize(new MusicRecording
			{
				Composer = string.Join(',', musicProperties.ExtractMetadata(MetadataKey.Composer)),
				RealRecordingName = filePathEntry[(filePathEntry.LastIndexOf("/") + 1)..],
				RecordingName = musicProperties.ExtractMetadata(MetadataKey.Title),
				Notes = notes
			}));
		}
    }
}