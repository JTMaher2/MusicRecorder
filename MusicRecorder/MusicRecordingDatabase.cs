using AsyncAwaitBestPractices;
using Io.Github.Jtmaher2.Musicrecorder;
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
                if (!Database.TableMappings.Any(m => m.MappedType.Name == typeof(MusicRemix).Name))
                {
                    await Database.CreateTablesAsync(CreateFlags.None, typeof(MusicRemix)).ConfigureAwait(false);
                }
                initialized = true;
            }
        }

        public async Task<List<MusicRecording>> GetItemsAsync()
        {
            return await Database.Table<MusicRecording>().ToListAsync();
        }

        public async Task<List<MusicRemix>> GetRemixItemsAsync()
        {
            return await Database.Table<MusicRemix>().ToListAsync();
        }

        public async Task<MusicRecording> GetItemAsync(int id)
        {
            return await Database.Table<MusicRecording>().Where(i => i.ID == id).FirstOrDefaultAsync();
        }

        public async Task<MusicRemix> GetRemixItemAsync(int id)
        {
            return await Database.Table<MusicRemix>().Where(i => i.ID == id).FirstOrDefaultAsync();
        }

        public async Task<int> SaveItemAsync(MusicRecording item)
        {
            if (item.ID != 0)
            {
                return await Database.UpdateAsync(item);
            }
            else
            {
                return await Database.InsertAsync(item);
            }
        }

        public async Task<int> SaveRemixItemAsync(MusicRemix item)
        {
            if (item.ID != 0)
            {
                return await Database.UpdateAsync(item);
            }
            else
            {
                return await Database.InsertAsync(item);
            }
        }

        public async Task<int> DeleteItemAsync(MusicRecording item)
        {
            return await Database.DeleteAsync(item);
        }

        public async Task<int> DeleteRemixItemAsync(MusicRemix item)
        {
            return await Database.DeleteAsync(item);
        }
    }
}
