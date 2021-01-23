using SQLite;

namespace Io.Github.Jtmaher2.Musicrecorder
{
    public class MusicRemix
    {
        [PrimaryKey, AutoIncrement]
        public int ID { get; set; }
        public string RemixName { get; set; }
    }
}
