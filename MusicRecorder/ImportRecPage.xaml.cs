using System;
using Xamarin.Essentials;
using Xamarin.Forms;
using Xamarin.Forms.Xaml;

namespace Io.Github.Jtmaher2.MusicRecorder
{
    [XamlCompilation(XamlCompilationOptions.Compile)]
    public partial class ImportRecPage : ContentPage
    {
        readonly Services.IRecordAudio mAudioRecorderService;

        public ImportRecPage()
        {
            InitializeComponent();

            mAudioRecorderService = DependencyService.Resolve<Services.IRecordAudio>();
        }

        private async void Button_Clicked(object sender, EventArgs e)
        {
            try
            {
                var result = await FilePicker.PickAsync();

                if (result != null)
                {
                    await App.Database.SaveItemAsync(System.Text.Json.JsonSerializer.Deserialize<MusicRecording>(await mAudioRecorderService.Import(result.FullPath, notes.Text)));
                }
            }
            catch (Exception ex)
            {
                // The user canceled or something went wrong
            }
        }
    }
}