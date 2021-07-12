using Io.Github.Jtmaher2.MusicRecorder.Services;
using System;
using System.Collections.Generic;
using System.IO;
using System.Threading.Tasks;
using Xamarin.Essentials;
//using Windows.Storage;
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
            mId = mr.ID;
            remixNameEnt.Text = mr.RemixName;
            mMusicRecordings = mr.MusicRecordings;
            mOrigFileName = remixNameEnt.Text;
        }

        private async void Button_Clicked(object sender, EventArgs e)
        {
            await App.Database.SaveRemixItemAsync(new MusicRemix
            {
                RemixName = remixNameEnt.Text,
                ID = mId
            });

            if (Device.RuntimePlatform == Device.Android)
            {
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

        private async void startRemBtn_Clicked(object sender, EventArgs e)
        {

            await Navigation.PushModalAsync(new RemixPage(mMusicRecordings));
        }

        private void stopRemBtn_Clicked(object sender, EventArgs e)
        {

        }
    }
}