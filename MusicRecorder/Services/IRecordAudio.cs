using System;
using System.Collections.Generic;
using System.Threading.Tasks;
//using Windows.Media.Editing;

namespace Io.Github.Jtmaher2.MusicRecorder.Services
{
    public interface IRecordAudio
    {
        void Start(string fileName);
        void Stop();
        void PreviewRecording(string fileName, long seekToMS, long stopAtMS);
        void StopPreviewRecording();
        bool IsCompleted();
        void EncodeFlac(string source, string dest);
        void WriteFlacRemix(List<string> sources, List<TimeSpan> startTimes, List<TimeSpan> stopTimes, string dest);
        void WriteFile(string fileName, string origFileName);
    }
}
