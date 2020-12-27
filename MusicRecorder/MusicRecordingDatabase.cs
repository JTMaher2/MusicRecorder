using AsyncAwaitBestPractices;
using SQLite;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;

namespace Io.Github.Jtmaher2.MusicRecorder
{
    public class MusicRecordingDatabase
    {
        static readonly Lazy<SQLiteAsyncConnection> lazyInitializer = new Lazy<SQLiteAsyncConnection>(() =>
        {
            return new SQLiteAsyncConnection(Constants.DatabasePath, Constants.Flags);
        });

        static SQLiteAsyncConnection Database => lazyInitializer.Value;
        static bool initialized = false;

        public MusicRecordingDatabase()
        {
            Task t = InitializeAsync();
            t.SafeFireAndForget(null, false);
        }

        async Task InitializeAsync()
        {
            if (!initialized)
            {
                if (!Database.TableMappings.Any(m => m.MappedType.Name == typeof(MusicRecording).Name))
                {
                    await Database.CreateTablesAsync(CreateFlags.None, typeof(MusicRecording)).ConfigureAwait(false);
                }
                initialized = true;
            }
        }

        public Task<List<MusicRecording>> GetItemsAsync()
        {
            return Database.Table<MusicRecording>().ToListAsync();
        }

        public Task<MusicRecording> GetItemAsync(int id)
        {
            return Database.Table<MusicRecording>().Where(i => i.ID == id).FirstOrDefaultAsync();
        }

        public Task<int> SaveItemAsync(MusicRecording item)
        {
            if (item.ID != 0)
            {
                return Database.UpdateAsync(item);
            }
            else
            {
                return Database.InsertAsync(item);
            }
        }

        public Task<int> DeleteItemAsync(MusicRecording item)
        {
            return Database.DeleteAsync(item);
        }
    }
}
