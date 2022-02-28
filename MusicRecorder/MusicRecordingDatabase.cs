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
