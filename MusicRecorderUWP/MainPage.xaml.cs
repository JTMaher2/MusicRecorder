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

using Io.Github.Jtmaher2.MusicRecorder.Services;
using Windows.ApplicationModel.Resources;

// The Blank Page item template is documented at https://go.microsoft.com/fwlink/?LinkId=402352&clcid=0x409

namespace MusicRecorderUWP
{
    /// <summary>
    /// An empty page that can be used on its own or navigated to within a Frame.
    /// </summary>
    public sealed partial class MainPage
    {
        public MainPage()
        {
            Syncfusion.Licensing.SyncfusionLicenseProvider.RegisterLicense(ResourceLoader.GetForCurrentView().GetString("SyncfusionLicense"));
            InitializeComponent();
            Io.Github.Jtmaher2.MusicRecorder.MainPage.RegisterType<IRecordAudio, RecordAudio>();
            LoadApplication(new Io.Github.Jtmaher2.MusicRecorder.App());
        
        }
    }
}
