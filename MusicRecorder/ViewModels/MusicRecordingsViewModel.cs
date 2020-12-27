using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.ComponentModel;
using System.Linq;
using System.Runtime.CompilerServices;
using System.Windows.Input;
using Xamarin.Forms;

namespace Io.Github.Jtmaher2.MusicRecorder.ViewModels
{
    public class MusicRecordingsViewModel : INotifyPropertyChanged
    {
        readonly IList<MusicRecording> musicRecSource;

        public ObservableCollection<MusicRecording> MusicRecordings { get; private set; }

        public IList<MusicRecording> EmptyMusicRecordings { get; private set; }

        public MusicRecording PreviousMusicRecording { get; set; }
        public MusicRecording CurrentMusicRecording { get; set; }
        public MusicRecording CurrentItem { get; set; }
        public int PreviousPosition { get; set; }
        public int CurrentPosition { get; set; }
        public int Position { get; set; }

        public ICommand FilterCommand => new Command<string>(FilterItems);
        public ICommand ItemChangedCommand => new Command<MusicRecording>(ItemChanged);
        public ICommand PositionChangedCommand => new Command<int>(PositionChanged);
        public ICommand DeleteCommand => new Command<MusicRecording>(RemoveMusicRecording);
        public ICommand EditCommand => new Command<MusicRecording>(EditMusicRecording);

        private INavigation mNavigation;

        public MusicRecordingsViewModel(List<MusicRecording> existingRecs, INavigation navigation)
        {
            musicRecSource = new List<MusicRecording>();

            mNavigation = navigation;
            CreateMusicRecordingCollection(existingRecs);

            CurrentItem = MusicRecordings.Skip(0).FirstOrDefault();
            OnPropertyChanged("CurrentItem");
            Position = 0;
            OnPropertyChanged("Position");
        }

        void CreateMusicRecordingCollection(List<MusicRecording> existingRecs)
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

            MusicRecordings = new ObservableCollection<MusicRecording>(musicRecSource);
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

        void ItemChanged(MusicRecording item)
        {
            PreviousMusicRecording = CurrentMusicRecording;
            CurrentMusicRecording = item;
            OnPropertyChanged("PreviousMusicRecording");
            OnPropertyChanged("CurrentMusicRecording");
        }

        void PositionChanged(int position)
        {
            PreviousPosition = CurrentPosition;
            CurrentPosition = position;
            OnPropertyChanged("PreviousPosition");
            OnPropertyChanged("CurrentPosition");
        }

        void RemoveMusicRecording(MusicRecording recording)
        {
            if (MusicRecordings.Contains(recording))
            {
                MusicRecordings.Remove(recording);
                App.Database.DeleteItemAsync(recording);
                OnPropertyChanged("source");
            }
        }

        void EditMusicRecording(MusicRecording recordingNav)
        {
            mNavigation.PushModalAsync(new EditRecordingPage(recordingNav.ID));
        }

        #region INotifyPropertyChanged
        public event PropertyChangedEventHandler PropertyChanged;

        void OnPropertyChanged([CallerMemberName] string propertyName = null)
        {
            PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(propertyName));
        }
        #endregion
    }
}
