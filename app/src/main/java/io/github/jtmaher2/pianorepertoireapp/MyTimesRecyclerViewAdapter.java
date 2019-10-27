package io.github.jtmaher2.pianorepertoireapp;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Environment;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class MyTimesRecyclerViewAdapter extends RecyclerView.Adapter<MyTimesRecyclerViewAdapter.MyViewHolder> {
    private final String[] mDataset;
    private final int mPieceId;
    private final Context mContext;
    private static final int DIALOG_REQUEST_CODE = 9001;
    private static final int NUM_BYTES_PER_READ = 100;
    private static final int BYTES_PER_SEC = 44100 * 2 * 2;
    private static final int SAMPLE_RATE_SIZE = 44100;
    private static final int BUFFERED_INPUT_STREAM_SIZE = 8000;
    private int[] mStartTimes, mEndTimes;

    // keys for storing a piece's/recording's Uri in a Bundle passed to the activity
    private static final String PIECE_URI = "piece_uri",
            RECORDING_URIS = "recording_uris",
            PIECE_ID = "piece_id",
            TIME_PICKER_TYPE = "time_picker_type",
            CHILD_IDX = "child_index",
            TAG = "MyTimesRecyclerVAdapter";

    private AudioTrack mAt;
    private DataInputStream mDis;
    private byte[] mByteData;
    private int mBufSize;
    private BufferedInputStream mBis;
    private FileInputStream mFin;
    private double mBytesToPlay;

    class MyViewHolder extends RecyclerView.ViewHolder {
        final ScrollView mScrollView;

        MyViewHolder(ScrollView s) {
            super(s);
            mScrollView = s;
        }
    }

    MyTimesRecyclerViewAdapter(String[] myDataset, int pieceId, Context context, int[] startTimes, int[] endTimes) {
        mDataset = myDataset;
        mPieceId = pieceId;
        mContext = context;
        mStartTimes = startTimes;
        mEndTimes = endTimes;
    }

    @NonNull
    @Override
    public MyTimesRecyclerViewAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder((ScrollView)LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.fragment_remix_pieces, parent, false));
    }

    // stop playing a preview
    private void stopPlaying(int pos, Button previewBtn)
    {
        //mAudioTrack.flush();
        //mAudioTrack.stop();
        //mAudioTrack.release();
        mAt.pause();
        mAt.flush();

        previewBtn.setText("Preview");
        previewBtn.setOnClickListener((view) -> playRec(pos, previewBtn));
    }

    // play a selected recording
    private void playRec(int pos, Button previewBtn)
    {
        previewBtn.setText("Stop");
        previewBtn.setOnClickListener((view)-> stopPlaying(pos, previewBtn));

        byte[] byteData = null;
        File file = null;
        file = new File(Environment.getExternalStorageDirectory().getAbsoluteFile() + "/PianoRepertoire/" + mPieceId  + "/" + mDataset[pos]);

        // for ex. path= "/sdcard/samplesound.ogg" or "/sdcard/samplesound.webm"

        mAt = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE_SIZE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build())
                .setBufferSizeInBytes(android.media.AudioTrack.getMinBufferSize(SAMPLE_RATE_SIZE, AudioFormat.CHANNEL_OUT_STEREO,
                        AudioFormat.ENCODING_PCM_8BIT))
                .build();

        int i = 0;
        int bufSize = (int) file.length();
        mByteData = new byte[bufSize];

        try
        {
            mFin = new FileInputStream( file );
            mBis = new BufferedInputStream(mFin, BUFFERED_INPUT_STREAM_SIZE);
            mDis = new DataInputStream(mBis);
            mDis.skipBytes(BYTES_PER_SEC * mStartTimes[pos]); // skip the first N seconds, where N is the start time for this rec
            mAt.play();
            int numSecToPlay = mEndTimes[pos] - mStartTimes[pos];
            double recSecLen = bufSize / BYTES_PER_SEC; // recording total # of seconds
            mBytesToPlay = (numSecToPlay / recSecLen) * bufSize; // the number of bytes to read for the specified length

            class PlayAudioInBackground implements Runnable {
                private PlayAudioInBackground() {}
                public void run() {
                    Thread.currentThread().setPriority(Thread.MIN_PRIORITY);

                    int i;
                    int numBytesWritten = 1;
                    try {
                        do {
                            i = mDis.read(mByteData, 0, NUM_BYTES_PER_READ/*bufSize*/);
                            mAt.write(mByteData, 0, i);
                            numBytesWritten += NUM_BYTES_PER_READ;
                        } while (i > -1 && numBytesWritten < mBytesToPlay);
                    } catch (IOException e) {
                        Log.e(TAG, e.getMessage());
                    }

                    mAt.stop(); // stop after last buffer is played
                    mAt.release();
                    try {
                        mDis.close();
                        mBis.close();
                        mFin.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    ((AppCompatActivity)mContext).runOnUiThread(new Runnable() {

                        @Override
                        public void run() {

                            previewBtn.setText("Preview");
                            previewBtn.setOnClickListener((view) -> playRec(pos, previewBtn));

                        }
                    });
                }
            }

            Thread m_playThread = new Thread(new PlayAudioInBackground());
            m_playThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBindViewHolder(@NonNull final MyViewHolder holder, final int position) {
        int pos = holder.getAdapterPosition();
        ((TextView)holder.mScrollView.findViewById(R.id.rec_start_label)).setText(String.format(mContext.getString(R.string.recording_start), mDataset[pos]));

        ((TextView)holder.mScrollView.findViewById(R.id.rec_end_label)).setText(String.format(mContext.getString(R.string.recording_end), mDataset[pos]));

        Button pickStartBtn = holder.mScrollView.findViewById(R.id.pick_start_btn);
        pickStartBtn.setOnClickListener(view -> {
            /*Intent i = new Intent(mContext, TimePickerFragment.class);
            i.putExtra(TIME_PICKER_TYPE, "start");
            mContext.startActivity(i);*/
            DialogFragment newFragment = new TimePickerFragment();
            FragmentActivity fragActivity = (FragmentActivity)mContext;
            Bundle b = new Bundle();
            b.putString(TIME_PICKER_TYPE, "start");
            b.putInt(CHILD_IDX, pos);
            newFragment.setArguments(b);
            newFragment.show(fragActivity.getSupportFragmentManager(), "");
        });

        Button pickEndBtn = holder.mScrollView.findViewById(R.id.pick_end_btn);
        pickEndBtn.setOnClickListener(view -> {
            /*Intent i = new Intent(mContext, TimePickerFragment.class);
            i.putExtra(TIME_PICKER_TYPE, "end");
            mContext.startActivity(i);*/
            DialogFragment newFragment = new TimePickerFragment();
            FragmentActivity fragActivity = (FragmentActivity)mContext;
            Bundle b = new Bundle();
            b.putString(TIME_PICKER_TYPE, "end");
            b.putInt(CHILD_IDX, pos);
            newFragment.setArguments(b);
            newFragment.show(fragActivity.getSupportFragmentManager(), "");
        });

        Button previewBtn = holder.mScrollView.findViewById(R.id.preview_button);
        previewBtn.setOnClickListener(view -> {
            playRec(pos, previewBtn);
        });

    }

    @Override
    public int getItemCount() {
        if (mDataset != null)
            return mDataset.length;
        else
            return 0;
    }
}
