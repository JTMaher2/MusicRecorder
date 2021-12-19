using SQLite;

namespace MusicRecorderUWP
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
