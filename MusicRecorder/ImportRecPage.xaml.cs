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
                    await Navigation.PopModalAsync();
                }
            }
            catch (Exception ex)
            {
                // The user canceled or something went wrong
            }
        }
    }
}