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
using System.Collections.Generic;
using System.Threading.Tasks;
//using Windows.Media.Editing;

namespace Io.Github.Jtmaher2.MusicRecorder.Services
{
    public interface IRecordAudio
    {
        Task<string> Start(string fileName);
        void Stop();
        void PreviewRecording(string fileName, long seekToMS, long stopAtMS);
        void StopPreviewRecording();
        bool IsCompleted();
        void EncodeMP3(string source, string dest);
        void WriteMp3Remix(List<string> sources, List<TimeSpan> startTimes, List<TimeSpan> stopTimes, string dest);
        string WriteFile(string fileName, string origFileName);
        Task<string> Import(string filePath, string notes);
    }
}
