using Concentus.Enums;
using Concentus.Oggfile;
using Concentus.Structs;
using Io.Github.Jtmaher2.MusicRecorder.Services;
using Io.Github.Jtmaher2.MusicRecorder.ViewModels;
using NAudio.Wave;
using NAudio.Wave.SampleProviders;
using System;
using System.Collections;
using System.Collections.Generic;
using System.IO;
using System.Reflection;
using System.Text;
using System.Threading.Tasks;
using Xamarin.Essentials;
using Xamarin.Forms;
using Xamarin.Forms.Xaml;
using static Xamarin.Forms.Grid;

namespace Io.Github.Jtmaher2.MusicRecorder
{
    [XamlCompilation(XamlCompilationOptions.Compile)]
    public partial class RemixPage : ContentPage
    {
        private static readonly double BYTES_PER_SEC = SAMPLE_RATE * 2 * 2;
        readonly IRecordAudio mAudioRecorderService;
        private Button mSenderBtn;
        private long _startingDT,
            _endingDT,
            _nowMillis;
        private static readonly int SAMPLE_RATE = 48000;
        private static readonly int NUM_CHANNELS = 2;
        private static readonly int BITRATE = 96000;
        private List<int> mMarkedForRemixRecs;

        public RemixPage(List<int> markedForRemixRecs)
        {
            InitializeComponent();

            mAudioRecorderService = DependencyService.Resolve<IRecordAudio>();
            mMarkedForRemixRecs = markedForRemixRecs;
            LoadPage(markedForRemixRecs);
        }

        private async void LoadPage(List<int> markedForRemixRecs)
        {
            List<MusicRecording> allRecs = await App.Database.GetItemsAsync(),
                chosenRecs = new List<MusicRecording>();

            for (int i = 0; i < allRecs.Count; i++) {
                for (int j = 0; j < markedForRemixRecs.Count; j++)
                {
                    if (allRecs[i].ID == markedForRemixRecs[j]) {
                        chosenRecs.Add(allRecs[i]);
                    }
                }
            }

            BindingContext = new MusicRecordingsRemixesViewModel(chosenRecs, null, Navigation, this);
        }

        private void Button_Clicked(object sender, EventArgs e)
        {
            IGridList<View> children = ((Grid)((Button)sender).Parent).Children;

            IEnumerator<View> childrenEnum = children.GetEnumerator();

            int i = 0,
                startingHr = 0,
                startingMin = 0,
                startingSec = 0,
                endingHr = 0,
                endingMin = 0,
                endingSec = 0;

            string name = "";

            while (i < children.Count - 1)
            {
                childrenEnum.MoveNext();

                switch (i)
                {
                    case 0:
                        name = ((Label)childrenEnum.Current).Text;
                        break;
                    case 2:
                        startingHr = int.Parse(((Entry)childrenEnum.Current).Text);
                        break;
                    case 3:
                        startingMin = int.Parse(((Entry)childrenEnum.Current).Text);
                        break;
                    case 4:
                        startingSec = int.Parse(((Entry)childrenEnum.Current).Text);
                        break;
                    case 7:
                        endingHr = int.Parse(((Entry)childrenEnum.Current).Text);
                        break;
                    case 8:
                        endingMin = int.Parse(((Entry)childrenEnum.Current).Text);
                        break;
                    case 9:
                        endingSec = int.Parse(((Entry)childrenEnum.Current).Text);
                        break;
                }
                i++;
            }

            DateTimeOffset dtoNow = DateTimeOffset.Now;

            _startingDT = dtoNow.AddHours(startingHr).AddMinutes(startingMin).AddSeconds(startingSec).ToUnixTimeMilliseconds();
            _endingDT = dtoNow.AddHours(endingHr).AddMinutes(endingMin).AddSeconds(endingSec).ToUnixTimeMilliseconds();
            _nowMillis = dtoNow.ToUnixTimeMilliseconds();

            mSenderBtn = (Button)sender;

            if (mSenderBtn.Text == "Preview")
            {
                mAudioRecorderService.PreviewRecording(name + (Device.RuntimePlatform == Device.Android ? ".opus" : ".flac"),
                    _startingDT - _nowMillis,
                    _endingDT - _nowMillis);
                mSenderBtn.Text = "Stop";
            }
            else
            {
                mAudioRecorderService.StopPreviewRecording();
                System.Threading.Thread.Sleep((int)new TimeSpan(_endingDT - _nowMillis).Subtract(new TimeSpan(_startingDT - _nowMillis)).Ticks); // this is necessary in order to prevent subsequent playbacks from being too short
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

        private void RemixPage_Clicked(object sender, EventArgs e)
        {
            mAudioRecorderService.StopPreviewRecording();
            mSenderBtn.Text = "Preview";
        }

        private async void Button_Clicked_1(object sender, EventArgs e)
        {
            IEnumerable src = MusicRecColView.ItemsSource;

            IList<View> children = ((StackLayout)((Button)sender).Parent).Children;

            IEnumerator<View> childrenEnum = children.GetEnumerator();
            childrenEnum.MoveNext();

            CollectionView cv = (CollectionView)childrenEnum.Current;
            System.Collections.ObjectModel.ReadOnlyCollection<Element> obj = (System.Collections.ObjectModel.ReadOnlyCollection<Element>)cv.GetType().GetProperty("LogicalChildrenInternal", BindingFlags.NonPublic | BindingFlags.Instance).GetValue(cv);
            SortedList<int, object[]> orders = new SortedList<int, object[]>();
            StringBuilder combinedRemNames = new StringBuilder();

            string externalMediaDir = FileSystem.AppDataDirectory;
            for (int i = 0; i < obj.Count; i++)
            {
                int startHr = int.Parse(((Entry)((Grid)obj[i]).Children[2]).Text),
                    startMin = int.Parse(((Entry)((Grid)obj[i]).Children[3]).Text),
                    startSec = int.Parse(((Entry)((Grid)obj[i]).Children[4]).Text),
                    order = int.Parse(((Entry)((Grid)obj[i]).Children[5]).Text),
                    endHr = int.Parse(((Entry)((Grid)obj[i]).Children[7]).Text),
                    endMin = int.Parse(((Entry)((Grid)obj[i]).Children[8]).Text),
                    endSec = int.Parse(((Entry)((Grid)obj[i]).Children[9]).Text);

                IEnumerator ienum = src.GetEnumerator();

                for (int j = 0; j <= i; j++)
                {
                    ienum.MoveNext();
                }

                
                string fileName = null;
                if (Device.RuntimePlatform == Device.Android)
                {
                    fileName = externalMediaDir + "/" + ((MusicRecording)ienum.Current).RecordingName + ".opus";
                } else
                { // UWP
                    fileName = externalMediaDir + "\\" + ((MusicRecording)ienum.Current).RecordingName + ".flac";
                }
                orders.Add(order, new object[3] { fileName, new int[3] { startHr, startMin, startSec }, new int[3] { endHr, endMin, endSec } });

                combinedRemNames.Append(((MusicRecording)ienum.Current).RecordingName);
            }
            string combinedRemNamesStr = combinedRemNames.ToString() + $"-{DateTime.Now.ToFileTime()}";

            await App.Database.SaveRemixItemAsync(new MusicRemix
            {
                ID = 0,
                RemixName = Device.RuntimePlatform == Device.Android ? $"{combinedRemNamesStr}.opus" : $"{combinedRemNamesStr}.flac",
                MusicRecordings = mMarkedForRemixRecs
            });

            List<ISampleProvider> sampleProviders = new List<ISampleProvider>();
            List<MemoryStream> pcmStreams = new List<MemoryStream>();
            List<string> sources = new List<string>();
            List<TimeSpan> startTimes = new List<TimeSpan>(),
                endTimes = new List<TimeSpan>();

            for (int i = 0; i < orders.Count; i++)
            {
                object[] fileOgg = orders[i];

                using (FileStream fileIn = new FileStream($"{(string)fileOgg[0]}", FileMode.Open))
                {

                    MemoryStream pcmStream = new MemoryStream();
                    TimeSpan startTimeSpan = new TimeSpan(((int[])fileOgg[1])[0], ((int[])fileOgg[1])[1], ((int[])fileOgg[1])[2]),
                       endTimeSpan = new TimeSpan(((int[])fileOgg[2])[0], ((int[])fileOgg[2])[1], ((int[])fileOgg[2])[2]);
                    long bufSize = fileIn.Length;

                    double numSecToPlay = (endTimeSpan - startTimeSpan).TotalSeconds,
                        recSecLen = bufSize / BYTES_PER_SEC; // recording total # of seconds

                    long bytesToPlay = (long)Math.Round(numSecToPlay / recSecLen * bufSize); // the number of bytes to read for the specified length; 

                    int numBytesWritten = 0;

                    if (Device.RuntimePlatform == Device.UWP)
                    {
                        // UWP uses FLAC format
                        sources.Add((string)fileOgg[0]);
                        startTimes.Add(startTimeSpan);
                        endTimes.Add(endTimeSpan);
                    }
                    else
                    {
                        // Android uses Opus format
                        OpusOggReadStream oggIn = new OpusOggReadStream(OpusDecoder.Create(SAMPLE_RATE, NUM_CHANNELS), fileIn);

                        if (startTimeSpan.TotalSeconds > 0)
                        {
                            oggIn.SeekTo(startTimeSpan);
                        }

                        while (oggIn.HasNextPacket && numBytesWritten < bytesToPlay)
                        {
                            short[] packet = oggIn.DecodeNextPacket();
                            if (packet != null)
                            {
                                for (int j = 0; j < packet.Length; j++)
                                {
                                    var bytes = BitConverter.GetBytes(packet[j]);
                                    numBytesWritten += bytes.Length;
                                    pcmStream.Write(bytes, 0, bytes.Length);
                                }
                            }
                        }
                    }
                    
                    pcmStream.Position = 0;

                    var wavStream = new RawSourceWaveStream(pcmStream, new WaveFormat(SAMPLE_RATE, NUM_CHANNELS));
                    var sampleProvider = wavStream.ToSampleProvider();

                    sampleProviders.Add(sampleProvider);
                    pcmStreams.Add(pcmStream);
                }
            }

            var playlist = new ConcatenatingSampleProvider(sampleProviders);

            if (Device.RuntimePlatform == Device.Android)
            {
                WaveFileWriter.CreateWaveFile16(externalMediaDir + "/" + combinedRemNamesStr + ".wav", playlist);
                using (FileStream fileOut = new FileStream(externalMediaDir + "/" + combinedRemNamesStr + ".opus", FileMode.Create))
                {
                    OpusEncoder encoder = OpusEncoder.Create(SAMPLE_RATE, NUM_CHANNELS, OpusApplication.OPUS_APPLICATION_AUDIO);
                    encoder.Bitrate = BITRATE;

                    OpusTags tags = new OpusTags();
                    tags.Fields[OpusTagName.Title] = "combined";
                    tags.Fields[OpusTagName.Artist] = "concentus";
                    OpusOggWriteStream oggOut = new OpusOggWriteStream(encoder, fileOut, tags);

                    byte[] allInput = File.ReadAllBytes(externalMediaDir + "/" + combinedRemNamesStr + ".wav");
                    short[] samples = BytesToShorts(allInput);

                    oggOut.WriteSamples(samples, 0, samples.Length);
                    oggOut.Finish();
                }

                File.Delete(externalMediaDir + "/" + combinedRemNamesStr + ".wav");
            } else
            { // UWP
                mAudioRecorderService.WriteFlacRemix(sources, startTimes, endTimes, externalMediaDir + "\\" + combinedRemNamesStr + ".flac");
            }

            if (Device.RuntimePlatform == Device.Android)
            {
                // if on Android, free up system resources
                for (int i = 0; i < pcmStreams.Count; i++)
                {
                    pcmStreams[i].Close();
                }
            }

            await Navigation.PopModalAsync();
        }

        public static short[] BytesToShorts(byte[] input)
        {
            short[] processedValues = new short[input.Length / 2];
            for (int c = 0; c < processedValues.Length; c++)
            {
                processedValues[c] = (short)(input[(c * 2)] << 0);
                processedValues[c] += (short)(input[(c * 2) + 1] << 8);
            }

            return processedValues;
        }

        protected override bool OnBackButtonPressed()
        {
            mAudioRecorderService.StopPreviewRecording();
            return base.OnBackButtonPressed();
        }
    }
}