﻿using Xamarin.Forms;

namespace Io.Github.Jtmaher2.MusicRecorder
{
    public partial class App : Application
    {
        static MusicRecordingDatabase database;

        public App()
        {
            InitializeComponent();

            MainPage = new MainPage();
        }

        public static MusicRecordingDatabase Database
        {
            get
            {
                if (database == null)
                {
                    database = new MusicRecordingDatabase();
                }
                return database;
            }
        }

        protected override void OnStart()
        {
        }

        protected override void OnSleep()
        {
        }

        protected override void OnResume()
        {
        }
    }
}
