using Io.Github.Jtmaher2.MusicRecorder.Services;
using System;
using System.IO;
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
            mId = mr.ID;
            composerEnt.Text = mr.Composer;
            fileNameEnt.Text = mr.RecordingName;
            mOrigFileName = fileNameEnt.Text;
            notesEnt.Text = mr.Notes;
        }

        private async void Button_Clicked(object sender, EventArgs e)
        {
            await App.Database.SaveItemAsync(new MusicRecording
            {
                Composer = composerEnt.Text,
                RecordingName = fileNameEnt.Text,
                Notes = notesEnt.Text,
                ID = mId
            });

            File.WriteAllBytes("/storage/emulated/0/Android/media/io.github.jtmaher2.musicrecorder/" + fileNameEnt.Text + ".ogg", File.ReadAllBytes("/storage/emulated/0/Android/media/io.github.jtmaher2.musicrecorder/" + mOrigFileName + ".ogg"));

            File.Delete("/storage/emulated/0/Android/media/io.github.jtmaher2.musicrecorder/" + mOrigFileName + ".ogg");

            await Navigation.PopModalAsync();
        }

        private void PreviewRecBtn_Clicked(object sender, EventArgs e)
        {
            mAudioRecorderService.PreviewRecording(fileNameEnt.Text, 0, 0);
        }

        protected override bool OnBackButtonPressed()
        {
            mAudioRecorderService.StopPreviewRecording();
            return base.OnBackButtonPressed();
        }
    }
}