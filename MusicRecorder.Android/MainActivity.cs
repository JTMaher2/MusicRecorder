using Android.App;
using Android.Content.PM;
using Android.Runtime;
using Android.OS;
using Io.Github.Jtmaher2.MusicRecorder.Services;

namespace Io.Github.Jtmaher2.MusicRecorder.Droid
{
    [Activity(Label = "MusicRecorder", Icon = "@mipmap/icon", Theme = "@style/MainTheme", MainLauncher = true, ConfigurationChanges = ConfigChanges.ScreenSize | ConfigChanges.Orientation | ConfigChanges.UiMode | ConfigChanges.ScreenLayout | ConfigChanges.SmallestScreenSize )]
    public class MainActivity : Xamarin.Forms.Platform.Android.FormsAppCompatActivity
    {
        protected override void OnCreate(Bundle savedInstanceState)
        {
            TabLayoutResource = Resource.Layout.Tabbar;
            ToolbarResource = Resource.Layout.Toolbar;

            base.OnCreate(savedInstanceState);

            Xamarin.Essentials.Platform.Init(this, savedInstanceState);
            Xamarin.Forms.Forms.SetFlags(new string[] { "SwipeView_Experimental" });
            Xamarin.Forms.Forms.Init(this, savedInstanceState);

            MainPage.RegisterType<IRecordAudio, RecordAudio>();
            MainPage.BuildContainer();
            LoadApplication(new App());
        }
        public override void OnRequestPermissionsResult(int requestCode, string[] permissions, [GeneratedEnum] Permission[] grantResults)
        {
            Xamarin.Essentials.Platform.OnRequestPermissionsResult(requestCode, permissions, grantResults);

            base.OnRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}