using SQLite;

namespace Io.Github.Jtmaher2.MusicRecorder
{
    public class MusicRecording
    {
        [PrimaryKey, AutoIncrement]
        public int ID { get; set; }
        public string RecordingName { get; set; }
        public string Composer { get; set; }
        public string Notes { get; set; }
    }
}
