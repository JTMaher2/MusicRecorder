using Io.Github.Jtmaher2.MusicRecorder;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.ComponentModel;
using System.Linq;
using System.Runtime.CompilerServices;
using System.Windows.Input;
using Xamarin.Forms;

namespace Io.Github.Jtmaher2.MusicRecorder.ViewModels
{
    public class MusicRecordingsRemixesViewModel : INotifyPropertyChanged
    {
        readonly IList<MusicRecording> musicRecSource;
        readonly IList<MusicRemix> musicRemSource;

        public ObservableCollection<MusicRecording> MusicRecordings { get; private set; }
        public ObservableCollection<MusicRemix> MusicRemixes { get; private set; }

        public IList<MusicRecording> EmptyMusicRecordings { get; private set; }
        public IList<MusicRemix> EmptyMusicRemixes { get; private set; }

        public MusicRecording PreviousMusicRecording { get; set; }
        public MusicRemix PreviousMusicRemix { get; set; }

        public MusicRecording CurrentMusicRecording { get; set; }
        public MusicRemix CurrentMusicRemix { get; set; }

        public MusicRecording CurrentItem { get; set; }
        public MusicRemix CurrentRemixItem { get; set; }

        public int PreviousPosition { get; set; }
        public int PreviousRemixPosition { get; set; }

        public int CurrentPosition { get; set; }
        public int CurrentRemixPosition { get; set; }

        public int Position { get; set; }
        public int RemixPosition { get; set; }

        public ICommand FilterCommand => new Command<string>(FilterItems);
        public ICommand FilterRemixCommand => new Command<string>(FilterRemixItems);

        public ICommand ItemChangedCommand => new Command<MusicRecording>(ItemChanged);
        public ICommand ItemRemixChangedCommand => new Command<MusicRemix>(ItemRemixChanged);

        public ICommand PositionChangedCommand => new Command<int>(PositionChanged);
        public ICommand PositionRemixChangedCommand => new Command<int>(RemixPositionChanged);

        public ICommand DeleteCommand => new Command<MusicRecording>(RemoveMusicRecording);
        public ICommand DeleteRemixCommand => new Command<MusicRemix>(RemoveMusicRemix);

        public ICommand EditCommand => new Command<MusicRecording>(EditMusicRecording);
        public ICommand EditRemixCommand => new Command<MusicRemix>(EditMusicRemix);

        private readonly INavigation mNavigation;

        private readonly Page mPage;

        public MusicRecordingsRemixesViewModel(List<MusicRecording> existingRecs, List<MusicRemix> existingRems, INavigation navigation, Page page)
        {
            musicRecSource = new List<MusicRecording>();
            musicRemSource = new List<MusicRemix>();

            mNavigation = navigation;
            CreateMusicRecordingCollection(existingRecs);
            CreateMusicRemixCollection(existingRems);

            CurrentItem = MusicRecordings.Skip(0).FirstOrDefault();
            CurrentRemixItem = MusicRemixes.Skip(0).FirstOrDefault();

            mPage = page;

            OnPropertyChanged("CurrentItem");
            Position = 0;
            OnPropertyChanged("Position");
        }

        void CreateMusicRecordingCollection(List<MusicRecording> existingRecs)
        {
            if (existingRecs.Count == 0)
            {
                musicRecSource.Add(new MusicRecording
                {
                    ID = -1,
                    RecordingName = "",
                    Composer = "",
                    Notes = ""
                });
            } else
            {
                for (int i = 0; i < existingRecs.Count; i++)
                {
                    musicRecSource.Add(new MusicRecording
                    {
                        ID = existingRecs[i].ID,
                        RecordingName = existingRecs[i].RecordingName,
                        Composer = existingRecs[i].Composer,
                        Notes = existingRecs[i].Notes
                    });
                }
            }

            MusicRecordings = new ObservableCollection<MusicRecording>(musicRecSource);
        }

        void CreateMusicRemixCollection(List<MusicRemix> existingRems)
        {
            if (existingRems != null) {
                if (existingRems.Count == 0)
                {
                    musicRemSource.Add(new MusicRemix
                    {
                        ID = -1,
                        RemixName = ""
                    });
                }
                else
                {
                    for (int i = 0; i < existingRems.Count; i++)
                    {
                        musicRemSource.Add(new MusicRemix
                        {
                            ID = existingRems[i].ID,
                            RemixName = existingRems[i].RemixName
                        });
                    }
                }
            }
            
            MusicRemixes = new ObservableCollection<MusicRemix>(musicRemSource);
        }

        void FilterItems(string filter)
        {
            var filteredItems = musicRecSource.Where(recording => recording.RecordingName.ToLower().Contains(filter.ToLower())).ToList();
            foreach (var recording in musicRecSource)
            {
                if (!filteredItems.Contains(recording))
                {
                    MusicRecordings.Remove(recording);
                }
                else
                {
                    if (!MusicRecordings.Contains(recording))
                    {
                        MusicRecordings.Add(recording);
                    }
                }
            }
        }

        void FilterRemixItems(string filter)
        {
            var filteredItems = musicRemSource.Where(remix => remix.RemixName.ToLower().Contains(filter.ToLower())).ToList();
            foreach (var remix in musicRemSource)
            {
                if (!filteredItems.Contains(remix))
                {
                    MusicRemixes.Remove(remix);
                }
                else
                {
                    if (!MusicRemixes.Contains(remix))
                    {
                        MusicRemixes.Add(remix);
                    }
                }
            }
        }

        void ItemChanged(MusicRecording item)
        {
            PreviousMusicRecording = CurrentMusicRecording;
            CurrentMusicRecording = item;
            OnPropertyChanged("PreviousMusicRecording");
            OnPropertyChanged("CurrentMusicRecording");
        }

        void ItemRemixChanged(MusicRemix item)
        {
            PreviousMusicRemix = CurrentMusicRemix;
            CurrentMusicRemix = item;
            OnPropertyChanged("PreviousMusicRemix");
            OnPropertyChanged("CurrentMusicRemix");
        }

        void PositionChanged(int position)
        {
            PreviousPosition = CurrentPosition;
            CurrentPosition = position;
            OnPropertyChanged("PreviousPosition");
            OnPropertyChanged("CurrentPosition");
        }

        void RemixPositionChanged(int position)
        {
            PreviousRemixPosition = CurrentRemixPosition;
            CurrentRemixPosition = position;
            OnPropertyChanged("PreviousRemixPosition");
            OnPropertyChanged("CurrentRemixPosition");
        }

        async void RemoveMusicRecording(MusicRecording recording)
        {
            if (MusicRecordings.Contains(recording))
            {
                await mPage.DisplayAlert("Delete", $"Are you sure you want to delete {recording.RecordingName}?", "Yes", "No");
                MusicRecordings.Remove(recording);
                await App.Database.DeleteItemAsync(recording);
                OnPropertyChanged("source");
            }
        }

        async void RemoveMusicRemix(MusicRemix remix)
        {
            if (MusicRemixes.Contains(remix))
            {
                await mPage.DisplayAlert("Delete", $"Are you sure you want to delete {remix.RemixName}?", "Yes", "No");
                MusicRemixes.Remove(remix);
                await App.Database.DeleteRemixItemAsync(remix);
                OnPropertyChanged("source");
            }
        }

        async void EditMusicRecording(MusicRecording recordingNav)
        {
            await mNavigation.PushModalAsync(new EditRecordingPage(recordingNav.ID));
        }

        async void EditMusicRemix(MusicRemix remixNav)
        {
            await mNavigation.PushModalAsync(new EditRemixPage(remixNav.ID));
        }

        #region INotifyPropertyChanged
        public event PropertyChangedEventHandler PropertyChanged;

        void OnPropertyChanged([CallerMemberName] string propertyName = null)
        {
            PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(propertyName));
        }

        protected bool SetProperty<T>(ref T field, T newValue, [CallerMemberName] string propertyName = null)
        {
            if (!Equals(field, newValue))
            {
                field = newValue;
                PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(propertyName));
                return true;
            }

            return false;
        }

        
        #endregion
    }
}
