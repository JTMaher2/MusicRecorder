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

using SQLite;
using Xamarin.CommunityToolkit.ObjectModel;

namespace Io.Github.Jtmaher2.MusicRecorder
{
    public class MusicRecording : ObservableObject
    {
        [PrimaryKey, AutoIncrement]
        public int ID { get; set; }
        public string RecordingName { get; set; }
        public string RealRecordingName { get; set; }
        public string Composer { get; set; }
        public string Notes { get; set; }

        [Ignore]
        public bool m_bIsBeingDragged { get; set; } // this property is only used for visual purposes and should not be stored in DB
    }
}
