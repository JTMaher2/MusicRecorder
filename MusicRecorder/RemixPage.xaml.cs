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
using Xamarin.Forms;
using Xamarin.Forms.Xaml;
using static Xamarin.Forms.Grid;

namespace Io.Github.Jtmaher2.MusicRecorder
{
    [XamlCompilation(XamlCompilationOptions.Compile)]
    public partial class RemixPage : ContentPage
    {
        private static readonly double BYTES_PER_SEC = 48000 * 2 * 2;
        readonly IRecordAudio mAudioRecorderService;

        public RemixPage(List<int> markedForRemixRecs)
        {
            InitializeComponent();

            mAudioRecorderService = DependencyService.Resolve<IRecordAudio>();

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

            BindingContext = new MusicRecordingsViewModel(chosenRecs, Navigation);
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

            long startingDT = dtoNow.AddHours(startingHr).AddMinutes(startingMin).AddSeconds(startingSec).ToUnixTimeMilliseconds(),
                endingDT = dtoNow.AddHours(endingHr).AddMinutes(endingMin).AddSeconds(endingSec).ToUnixTimeMilliseconds(),
                nowMillis = dtoNow.ToUnixTimeMilliseconds();

            mAudioRecorderService.PreviewRecording(name, 
                startingDT - nowMillis,
                endingDT - nowMillis);
        }

        private void Button_Clicked_1(object sender, EventArgs e)
        {
            IEnumerable src = MusicRecColView.ItemsSource;

            IList<View> children = ((StackLayout)((Button)sender).Parent).Children;

            IEnumerator<View> childrenEnum = children.GetEnumerator();
            childrenEnum.MoveNext();

            CollectionView cv = (CollectionView)childrenEnum.Current;
            System.Collections.ObjectModel.ReadOnlyCollection<Element> obj = (System.Collections.ObjectModel.ReadOnlyCollection<Element>)cv.GetType().GetProperty("LogicalChildrenInternal", BindingFlags.NonPublic | BindingFlags.Instance).GetValue(cv);
            SortedList<int, object[]> orders = new SortedList<int, object[]>();

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

                string fileName = "/storage/emulated/0/Android/media/io.github.jtmaher2.musicrecorder/" + ((MusicRecording)ienum.Current).RecordingName + ".ogg";

                orders.Add(order, new object[3] { fileName, new int[3] { startHr, startMin, startSec }, new int[3] { endHr, endMin, endSec } });
            }

            List<ISampleProvider> sampleProviders = new List<ISampleProvider>();
            List<MemoryStream> pcmStreams = new List<MemoryStream>();

            for (int i = 1; i <= orders.Count; i++)
            {
                object[] fileOgg = orders[i];

                using FileStream fileIn = new FileStream($"{(string)fileOgg[0]}", FileMode.Open);

                MemoryStream pcmStream = new MemoryStream();

                OpusOggReadStream oggIn = new OpusOggReadStream(OpusDecoder.Create(48000, 2), fileIn);

                TimeSpan startTimeSpan = new TimeSpan(((int[])fileOgg[1])[0], ((int[])fileOgg[1])[1], ((int[])fileOgg[1])[2]),
                    endTimeSpan = new TimeSpan(((int[])fileOgg[2])[0], ((int[])fileOgg[2])[1], ((int[])fileOgg[2])[2]);

                if (startTimeSpan.TotalSeconds > 0)
                {
                    oggIn.SeekTo(startTimeSpan);
                }

                long bufSize = fileIn.Length;

                double numSecToPlay = (endTimeSpan - startTimeSpan).TotalSeconds,
                    recSecLen = bufSize / BYTES_PER_SEC; // recording total # of seconds

                long bytesToPlay = (long)Math.Round((numSecToPlay / recSecLen) * bufSize); // the number of bytes to read for the specified length; 
                
                int numBytesWritten = 0;

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
                pcmStream.Position = 0;

                var wavStream = new RawSourceWaveStream(pcmStream, new WaveFormat(48000, 2));
                var sampleProvider = wavStream.ToSampleProvider();

                sampleProviders.Add(sampleProvider);
                pcmStreams.Add(pcmStream);
            }

            var playlist = new ConcatenatingSampleProvider(sampleProviders);
            
            WaveFileWriter.CreateWaveFile16("/storage/emulated/0/Android/media/io.github.jtmaher2.musicrecorder/combined.wav", playlist);

            using (FileStream fileOut = new FileStream("/storage/emulated/0/Android/media/io.github.jtmaher2.musicrecorder/combined.ogg", FileMode.Create))
            {
                OpusEncoder encoder = OpusEncoder.Create(48000, 2, OpusApplication.OPUS_APPLICATION_AUDIO);
                encoder.Bitrate = 96000;

                OpusTags tags = new OpusTags();
                tags.Fields[OpusTagName.Title] = "combined";
                tags.Fields[OpusTagName.Artist] = "concentus";
                OpusOggWriteStream oggOut = new OpusOggWriteStream(encoder, fileOut, tags);

                byte[] allInput = File.ReadAllBytes("/storage/emulated/0/Android/media/io.github.jtmaher2.musicrecorder/combined.wav");
                short[] samples = BytesToShorts(allInput);

                oggOut.WriteSamples(samples, 0, samples.Length);
                oggOut.Finish();
            }

            File.Delete("/storage/emulated/0/Android/media/io.github.jtmaher2.musicrecorder/combined.wav");

            // free up system resources
            for (int i = 0; i < pcmStreams.Count; i++)
            {
                pcmStreams[i].Close();
            }
        }

        public static short[] BytesToShorts(byte[] input)
        {
            return BytesToShorts(input, 0, input.Length);
        }

        public static short[] BytesToShorts(byte[] input, int offset, int length)
        {
            short[] processedValues = new short[length / 2];
            for (int c = 0; c < processedValues.Length; c++)
            {
                processedValues[c] = (short)(((int)input[(c * 2) + offset]) << 0);
                processedValues[c] += (short)(((int)input[(c * 2) + 1 + offset]) << 8);
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