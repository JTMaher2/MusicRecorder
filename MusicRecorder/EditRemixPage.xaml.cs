﻿/*
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

using Io.Github.Jtmaher2.MusicRecorder.Services;
using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.IO;
using System.Threading.Tasks;
using Xamarin.Essentials;
using Xamarin.Forms;
using Xamarin.Forms.Xaml;

namespace Io.Github.Jtmaher2.MusicRecorder
{
    [XamlCompilation(XamlCompilationOptions.Compile)]
    public partial class EditRemixPage : ContentPage
    {
        private int mId;
        private string mOrigFileName;
        readonly IRecordAudio mAudioRecorderService;
        private List<int> mMusicRecordings;

        public EditRemixPage(int id)
        {
            InitializeComponent();
            Populate(id);
            mAudioRecorderService = DependencyService.Resolve<IRecordAudio>();
        }

        private async void Populate(int id)
        {
            MusicRemix mr = await App.Database.GetRemixItemAsync(id);

            if (mr == null)
            {
                await Navigation.PopModalAsync();
            }
            else
            {
                mId = mr.ID;
                remixNameEnt.Text = mr.RemixName;
                mMusicRecordings = string.IsNullOrWhiteSpace(mr.MusicRecordings) ? new List<int>() : JsonConvert.DeserializeObject<List<int>>(mr.MusicRecordings);
                mOrigFileName = remixNameEnt.Text;
            }
        }

        private async void Button_Clicked(object sender, EventArgs e)
        {
            await App.Database.SaveRemixItemAsync(new MusicRemix
            {
                RemixName = remixNameEnt.Text,
                ID = mId,
                MusicRecordings = JsonConvert.SerializeObject(mMusicRecordings)
            });

            if (Device.RuntimePlatform == Device.Android)
            {
                // new name is different than old name
                if (File.Exists(FileSystem.AppDataDirectory + "/" + remixNameEnt.Text + ".opus"))
                {
                    // file already exists, generate unique name
                    int num = 2;
                    while (File.Exists(FileSystem.AppDataDirectory + "/" + remixNameEnt.Text + " (" + num + ").opus"))
                    {
                        num++;
                    }
                    byte[] readBytes = File.ReadAllBytes(FileSystem.AppDataDirectory + "/" + mOrigFileName);
                    File.WriteAllBytes(FileSystem.AppDataDirectory + "/" + remixNameEnt.Text + " (" + num + ").opus", readBytes);
                }
                else
                { // file doesn't already exist, use provided name
                    byte[] readBytes = File.ReadAllBytes(FileSystem.AppDataDirectory + "/" + mOrigFileName);
                    File.WriteAllBytes(FileSystem.AppDataDirectory + "/" + remixNameEnt.Text + ".opus", readBytes);
                }

                // delete original file
                File.Delete(FileSystem.AppDataDirectory + "/" + mOrigFileName + ".opus");

                File.WriteAllBytes(FileSystem.AppDataDirectory + "/" + remixNameEnt.Text, File.ReadAllBytes(FileSystem.AppDataDirectory + "/" + mOrigFileName));
                File.Delete(FileSystem.AppDataDirectory + "/" + mOrigFileName);
            }
            else
            {
                mAudioRecorderService.WriteFile(remixNameEnt.Text, mOrigFileName);
            }

            await Navigation.PopModalAsync();
        }

        private void PreviewRemBtn_Clicked(object sender, EventArgs e)
        {
            if (previewRemBtn.Text == "Preview Remix")
            {
                mAudioRecorderService.PreviewRecording(remixNameEnt.Text, 0, 0);
                previewRemBtn.Text = "Stop";
            }
            else
            {
                mAudioRecorderService.StopPreviewRecording();
                previewRemBtn.Text = "Preview Remix";
            }

            // monitor when playback completes, so that button text can be changed
            void a()
            {
                while (!mAudioRecorderService.IsCompleted())
                {
                    System.Threading.Thread.Sleep(1000);
                }

                Device.BeginInvokeOnMainThread(() =>
                {
                    previewRemBtn.Text = "Preview Remix";
                });
            }

            new Task(a).Start();
        }

        protected override bool OnBackButtonPressed()
        {
            mAudioRecorderService.StopPreviewRecording();
            return base.OnBackButtonPressed();
        }

        private async void StartRemBtn_Clicked(object sender, EventArgs e)
        {

            await Navigation.PushModalAsync(new RemixPage(mMusicRecordings, mId));
        }
    }
}