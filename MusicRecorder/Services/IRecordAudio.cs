namespace Io.Github.Jtmaher2.MusicRecorder.Services
{
    public interface IRecordAudio
    {
        void Start(string fileName);
        void Stop();
        void PreviewRecording(string fileName, long seekToMS, long stopAtMS);
        void StopPreviewRecording();
        bool IsCompleted();
    }
}
