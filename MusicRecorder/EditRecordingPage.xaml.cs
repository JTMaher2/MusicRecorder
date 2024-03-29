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
using System;
using System.IO;
using System.Threading.Tasks;
using Xamarin.Essentials;
using Xamarin.Forms;
using Xamarin.Forms.Xaml;

namespace Io.Github.Jtmaher2.MusicRecorder
{
    [XamlCompilation(XamlCompilationOptions.Compile)]
    public partial class EditRecordingPage : ContentPage
    {
        private int mId;
        private string mOrigFileName;
        readonly IRecordAudio mAudioRecorderService;

        public EditRecordingPage(int id)
        {
            InitializeComponent();
            Populate(id);
            mAudioRecorderService = DependencyService.Resolve<IRecordAudio>();

        }

        private async void Populate(int id)
        {
            MusicRecording mr = await App.Database.GetItemAsync(id);
            if (mr == null)
            {
                await Navigation.PopModalAsync();
            }
            else
            {
                mId = mr.ID;
                composerEnt.Text = mr.Composer;
                fileNameEnt.Text = mr.RecordingName;
                mOrigFileName = fileNameEnt.Text;
                notesEnt.Text = mr.Notes;
            }
        }

        private async void Button_Clicked(object sender, EventArgs e)
        {
            string fileName = fileNameEnt.Text;
            if (Device.RuntimePlatform == Device.Android)
            {
                // Droid
                if (fileNameEnt.Text != mOrigFileName)
                {
                    // new name is different than old name
                    if (File.Exists(FileSystem.AppDataDirectory + "/" + fileNameEnt.Text + ".opus"))
                    {
                        // file already exists, generate unique name
                        int num = 2;
                        while (File.Exists(FileSystem.AppDataDirectory + "/" + fileNameEnt.Text + " (" + num + ").opus"))
                        {
                            num++;
                        }
                        byte[] readBytes = File.ReadAllBytes(FileSystem.AppDataDirectory + "/" + mOrigFileName + ".opus");
                        File.WriteAllBytes(FileSystem.AppDataDirectory + "/" + fileNameEnt.Text + " (" + num + ").opus", readBytes);
                    }
                    else
                    { // file doesn't already exist, use provided name
                        byte[] readBytes = File.ReadAllBytes(FileSystem.AppDataDirectory + "/" + mOrigFileName + ".opus");
                        File.WriteAllBytes(FileSystem.AppDataDirectory + "/" + fileNameEnt.Text + ".opus", readBytes);
                    }

                    // delete original file
                    File.Delete(FileSystem.AppDataDirectory + "/" + mOrigFileName + ".opus");
                }
            } else
            {
                // UWP
                fileName = mAudioRecorderService.WriteFile(fileNameEnt.Text, mOrigFileName);
            }

            await App.Database.SaveItemAsync(new MusicRecording
            {
                Composer = composerEnt.Text,
                RecordingName = fileName,
                RealRecordingName = mOrigFileName,
                Notes = notesEnt.Text,
                ID = mId
            });

            await Navigation.PopModalAsync();
        }

        private void PreviewRecBtn_Clicked(object sender, EventArgs e)
        {
            if (previewRecBtn.Text == "Preview Recording")
            {
                mAudioRecorderService.PreviewRecording(fileNameEnt.Text + (Device.RuntimePlatform == Device.Android ? ".opus" : ".mp3"), 0, 0);
                previewRecBtn.Text = "Stop";
            }
            else
            {
                mAudioRecorderService.StopPreviewRecording();
                previewRecBtn.Text = "Preview Recording";
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
                    previewRecBtn.Text = "Preview Recording";
                });
            }

            new Task(a).Start();
        }

        protected override bool OnBackButtonPressed()
        {
            mAudioRecorderService.StopPreviewRecording();
            return base.OnBackButtonPressed();
        }
    }
}