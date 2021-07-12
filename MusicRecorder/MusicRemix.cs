using SQLite;
using SQLiteNetExtensions.Attributes;
using System.Collections.Generic;

namespace Io.Github.Jtmaher2.MusicRecorder
{
    public class MusicRemix
    {
        [PrimaryKey, AutoIncrement]
        public int ID { get; set; }
        public string RemixName { get; set; }
        [TextBlob("musicRecordingsBlobbed")]
        public List<int> MusicRecordings { get; set; }
    }
}
