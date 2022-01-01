using Autofac;
using Io.Github.Jtmaher2.MusicRecorder.Services;
using Io.Github.Jtmaher2.MusicRecorder.ViewModels;
using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using System.Windows.Input;
using Xamarin.Forms;
using Xamarin.Forms.Internals;

namespace Io.Github.Jtmaher2.MusicRecorder
{
    public partial class MainPage : ContentPage
    {
        static readonly ContainerBuilder builder = new ContainerBuilder();
        readonly IRecordAudio mAudioRecorderService;

        static IContainer container;

        List<MusicRecording> mExistingRecs;

        List<MusicRemix> mExistingRems;

        public ICommand NavigateCommand { get; private set; }

        private int mID;

        private string mName;
        private string mRemixName;
        private string mRecName;

        Button mSenderBtn; // the button that gets clicked when previewing a recording or remix

        private List<int> mMarkedForRemixIndexes;

        public MainPage()
        {
            InitializeComponent();
            BuildContainer();
            DependencyResolver.ResolveUsing(type => container.IsRegistered(type) ? container.Resolve(type) : null);

            mAudioRecorderService = DependencyService.Resolve<IRecordAudio>();            
        }

        private async void PopulateRecAndRemList()
        {
            mExistingRecs = await App.Database.GetItemsAsync();
            mExistingRems = await App.Database.GetRemixItemsAsync();

            NavigateCommand = new Command<Type>(
            async (Type pageType) =>
            {
                Page page = (Page)Activator.CreateInstance(pageType);
                await Navigation.PushAsync(page);
            });

            MusicRecordingsRemixesViewModel mrrvm = new MusicRecordingsRemixesViewModel(mExistingRecs, mExistingRems, Navigation, this);
            BindingContext = mrrvm;

            if (mExistingRecs.Count > 0)
            {
                mID = mExistingRecs[0].ID;

                mName = mExistingRecs[0].RecordingName;
            }

            if (mExistingRems.Count > 0)
            {
                mRemixName = mExistingRems[0].RemixName;
            }

            mMarkedForRemixIndexes = new List<int>();
        }

        public static void BuildContainer()
        {
            if (container == null)
            {
                container = builder.Build();
            }
        }

        private async void StartRecBtn_Clicked(object sender, EventArgs e)
        {
            mRecName = await mAudioRecorderService.Start(fileNameEnt.Text);
        }

        public static void RegisterType<T>() where T : class
        {
            builder.RegisterType<T>();
        }

        public static void RegisterType<TInterface, T>() where TInterface : class where T : class, TInterface
        {
            builder.RegisterType<T>().As<TInterface>();
        }

        private void StopRecBtn_Clicked(object sender, EventArgs e)
        {
            mAudioRecorderService.Stop();
            previewRecBtn.IsEnabled = true;
        }

        private void PreviewRecBtn_Clicked(object sender, EventArgs e)
        {
            mAudioRecorderService.PreviewRecording(fileNameEnt.Text + (Device.RuntimePlatform == Device.Android ? ".opus" : ".mp3"), 0, 0);
        }

        private async void SaveRecBtn_Clicked(object sender, EventArgs e)
        {
            int id = 0;

            MusicRecording mr = new MusicRecording
            {
                RecordingName = mRecName,
                Notes = notesEnt.Text,
                RealRecordingName = fileNameEnt.Text,
                Composer = composerEnt.Text,
                ID = id
            };

            await App.Database.SaveItemAsync(mr);

            PopulateRecAndRemList();
        }

        protected override void OnAppearing()
        {
            PopulateRecAndRemList();
        }

        private void Button_Clicked(object sender, EventArgs e)
        {
            mSenderBtn = (Button)sender;

            if (mSenderBtn.Text == "Preview")
            {
                mAudioRecorderService.PreviewRecording(mName + (Device.RuntimePlatform == Device.Android ? ".opus" : ".mp3"), 0, 0);
                mSenderBtn.Text = "Stop";
            }
            else
            {
                mAudioRecorderService.StopPreviewRecording();
                mSenderBtn.Text = "Preview";
            }

            // monitor when playback completes, so that button text can be changed
            void a()
            {
                while (!mAudioRecorderService.IsCompleted())
                {
                    System.Threading.Thread.Sleep(1000);
                }

                Device.BeginInvokeOnMainThread(() =>
                {
                    mSenderBtn.Text = "Preview";
                });
            }

            new Task(a).Start();
        }

        private void Button_RemixClicked(object sender, EventArgs e)
        {
            mSenderBtn = (Button)sender;

            if (mSenderBtn.Text == "Preview")
            {
                mAudioRecorderService.PreviewRecording(mRemixName, 0, 0);
                mSenderBtn.Text = "Stop";
            }
            else
            {
                mAudioRecorderService.StopPreviewRecording();
                mSenderBtn.Text = "Preview";
            }

            // monitor when playback completes, so that button text can be changed
            void a()
            {
                while (!mAudioRecorderService.IsCompleted())
                {
                    System.Threading.Thread.Sleep(1000);
                }

                Device.BeginInvokeOnMainThread(() =>
                {
                    mSenderBtn.Text = "Preview";
                });
            }

            new Task(a).Start();
        }

        private void CarouselView_CurrentItemChanged(object sender, CurrentItemChangedEventArgs e)
        {
            if (e.CurrentItem != null)
            {
                mID = ((MusicRecording)e.CurrentItem).ID;
                mName = ((MusicRecording)e.CurrentItem).RecordingName;
            }
        }
        
        private void CarouselView_RemixCurrentItemChanged(object sender, CurrentItemChangedEventArgs e)
        {
            if (e.CurrentItem != null)
            {
                mRemixName = ((MusicRemix)e.CurrentItem).RemixName;
            }
        }

        private void Button_Clicked_1(object sender, EventArgs e)
        {
            if (((Button)sender).Text == "Mark for Remix")
            {
                ((Button)sender).Text = "Marked for Remix";
                mMarkedForRemixIndexes.Add(mID);
            } else
            {
                ((Button)sender).Text = "Mark for Remix";
                mMarkedForRemixIndexes.Remove(mID);
            }
        }

        private async void Button_Clicked_2(object sender, EventArgs e)
        {
            await Navigation.PushModalAsync(new RemixPage(mMarkedForRemixIndexes));
        }

        private async void ImportRecBtn_Clicked(object sender, EventArgs e)
        {
            await Navigation.PushModalAsync(new ImportRecPage());
        }
    }
}
