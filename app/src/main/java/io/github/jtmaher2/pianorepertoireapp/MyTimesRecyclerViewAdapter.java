package io.github.jtmaher2.pianorepertoireapp;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

public class MyTimesRecyclerViewAdapter extends RecyclerView.Adapter<MyTimesRecyclerViewAdapter.MyViewHolder> {
    private final String[] mDataset;
    private final Context mContext;
    private static final int DIALOG_REQUEST_CODE = 9001;
    private static final int NUM_BYTES_PER_READ = 100;
    private static final int BYTES_PER_SEC = 44100 * 2 * 2;
    private int[] mStartTimes, mEndTimes;

    // keys for storing a piece's/recording's Uri in a Bundle passed to the activity
    private static final String PIECE_URI = "piece_uri",
            RECORDING_URIS = "recording_uris",
            PIECE_ID = "piece_id",
            TIME_PICKER_TYPE = "time_picker_type",
            CHILD_IDX = "child_index";

    class MyViewHolder extends RecyclerView.ViewHolder {
        final LinearLayout mLinearLayout;

        MyViewHolder(LinearLayout l) {
            super(l);
            mLinearLayout = l;
        }
    }

    MyTimesRecyclerViewAdapter(String[] myDataset, Context context, int[] startTimes, int[] endTimes) {
        mDataset = myDataset;
        mContext = context;
        mStartTimes = startTimes;
        mEndTimes = endTimes;
    }

    @NonNull
    @Override
    public MyTimesRecyclerViewAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder((LinearLayout)LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.fragment_remix_pieces, parent, false));
    }

    // play a selected recording
    private void playRec(int pos, String recName)
    {
        byte[] byteData = null;
        File file = null;
        file = new File(Environment.getExternalStorageDirectory().getAbsoluteFile() + "/PianoRepertoire/" + recName);

        // for ex. path= "/sdcard/samplesound.pcm" or "/sdcard/samplesound.wav"

        AudioTrack at = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(44100)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build())
                .setBufferSizeInBytes(android.media.AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_OUT_STEREO,
                        AudioFormat.ENCODING_PCM_8BIT))
                .build();

        int i = 0;
        int bufSize = (int) file.length();
        byteData = new byte[bufSize];

        try
        {
            FileInputStream in = new FileInputStream( file );
            BufferedInputStream bis = new BufferedInputStream(in, 8000);
            DataInputStream dis = new DataInputStream(bis);
            dis.skipBytes(BYTES_PER_SEC * mStartTimes[pos]); // skip the first N seconds, where N is the start time for this rec
            at.play();
            int numSecToPlay = mEndTimes[pos] - mStartTimes[pos];
            double recSecLen = bufSize / BYTES_PER_SEC; // recording total # of seconds
            double bytesToPlay = (numSecToPlay / recSecLen) * bufSize; // the number of bytes to read for the specified length

            int numBytesWritten = 1;
            do {
                i = dis.read(byteData, 0, NUM_BYTES_PER_READ/*bufSize*/);
                at.write(byteData, 0, i);
                numBytesWritten += NUM_BYTES_PER_READ;
            } while (i > -1 && numBytesWritten < bytesToPlay);

             /*{

                numBytesWritten++;
            }*/
            at.stop();
            at.release();
            dis.close();
            bis.close();
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBindViewHolder(@NonNull final MyViewHolder holder, final int position) {
        final int pos = holder.getAdapterPosition();
        ((TextView)holder.mLinearLayout.findViewById(R.id.rec_start_label)).setText(String.format(mContext.getString(R.string.recording_start), mDataset[pos]));

        ((TextView)holder.mLinearLayout.findViewById(R.id.rec_end_label)).setText(String.format(mContext.getString(R.string.recording_end), mDataset[pos]));

        Button pickStartBtn = holder.mLinearLayout.findViewById(R.id.pick_start_btn);
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

        Button pickEndBtn = holder.mLinearLayout.findViewById(R.id.pick_end_btn);
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

        Button previewBtn = holder.mLinearLayout.findViewById(R.id.preview_button);
        previewBtn.setOnClickListener(view -> {
            playRec(pos, mDataset[pos]);
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
