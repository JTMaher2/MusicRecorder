using Autofac;
using Io.Github.Jtmaher2.MusicRecorder.Services;
using Io.Github.Jtmaher2.MusicRecorder.ViewModels;
using System;
using System.Collections.Generic;
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

        public ICommand NavigateCommand { get; private set; }

        private int mID;

        private string mName;

        private readonly List<int> mMarkedForRemixIndexes;

        public MainPage()
        {
            InitializeComponent();
            DependencyResolver.ResolveUsing(type => container.IsRegistered(type) ? container.Resolve(type) : null);

            mAudioRecorderService = DependencyService.Resolve<IRecordAudio>();
            mMarkedForRemixIndexes = new List<int>();
        }

        private async void PopulateRecList()
        {
            mExistingRecs = await App.Database.GetItemsAsync();

            NavigateCommand = new Command<Type>(
            async (Type pageType) =>
            {
                Page page = (Page)Activator.CreateInstance(pageType);
                await Navigation.PushAsync(page);
            });

            BindingContext = new MusicRecordingsViewModel(mExistingRecs, Navigation);

            if (mExistingRecs.Count > 0)
            {
                mID = mExistingRecs[0].ID;

                mName = mExistingRecs[0].RecordingName;
            }
        }

        public static void BuildContainer()
        {
            if (container == null)
            {
                container = builder.Build();
            }
        }

        private void StartRecBtn_Clicked(object sender, EventArgs e)
        {
            mAudioRecorderService.Start(fileNameEnt.Text);
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
            mAudioRecorderService.PreviewRecording(null, 0, 0);
        }

        private async void SaveRecBtn_Clicked(object sender, EventArgs e)
        {
            int id = 0;

            MusicRecording mr = new MusicRecording
            {
                RecordingName = fileNameEnt.Text,
                Notes = notesEnt.Text,
                Composer = composerEnt.Text,
                ID = id
            };

            await App.Database.SaveItemAsync(mr);

            PopulateRecList();
        }

        protected override void OnAppearing()
        {
            PopulateRecList();
        }

        private void Button_Clicked(object sender, EventArgs e)
        {
            if (((Button)sender).Text == "Preview")
            {
                mAudioRecorderService.PreviewRecording(mName, 0, 0);
                ((Button)sender).Text = "Stop";
            }
            else
            {
                mAudioRecorderService.StopPreviewRecording();
                ((Button)sender).Text = "Preview";
            }
        }

        private void CarouselView_CurrentItemChanged(object sender, CurrentItemChangedEventArgs e)
        {
            mID = ((MusicRecording)e.CurrentItem).ID;
            mName = ((MusicRecording)e.CurrentItem).RecordingName;
        }

        private void Button_Clicked_1(object sender, EventArgs e)
        {
            ((Button)sender).Text = "Marked for Remix";
            mMarkedForRemixIndexes.Add(mID);
        }

        private void Button_Clicked_2(object sender, EventArgs e)
        {
            Navigation.PushModalAsync(new RemixPage(mMarkedForRemixIndexes));
        }
    }
}
