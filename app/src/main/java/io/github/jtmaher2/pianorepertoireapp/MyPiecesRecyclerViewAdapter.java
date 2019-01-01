package io.github.jtmaher2.pianorepertoireapp;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import io.github.jtmaher2.pianorepertoireapp.data.DatabaseDescription;
import io.github.jtmaher2.pianorepertoireapp.data.PianoRepertoireDatabaseHelper;

public class MyPiecesRecyclerViewAdapter extends RecyclerView.Adapter<MyPiecesRecyclerViewAdapter.MyViewHolder> {
    private final String[] mDataset;
    private final Context mContext;

    // keys for storing a piece's/recording's Uri in a Bundle passed to the activity
    private static final String PIECE_URI = "piece_uri",
            RECORDING_URIS = "recording_uris",
            PIECE_ID = "piece_id",
            REMIX_URIS = "remix_uris";

    class MyViewHolder extends RecyclerView.ViewHolder {
        final LinearLayout mLinearLayout;

        MyViewHolder(LinearLayout l) {
            super(l);
            mLinearLayout = l;
        }
    }

    MyPiecesRecyclerViewAdapter(String[] myDataset, Context context) {
        mDataset = myDataset;
        mContext = context;
    }

    @NonNull
    @Override
    public MyPiecesRecyclerViewAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder((LinearLayout)LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.fragment_myrecs, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull final MyViewHolder holder, final int position) {
        ((TextView)holder.mLinearLayout.findViewById(R.id.piece_name_textview_list)).setText(mDataset[position]);
        holder.mLinearLayout.findViewById(R.id.piece_details_btn).setOnClickListener(view -> {
            Intent i = new Intent(mContext, DetailsActivity.class);
            i.putExtra(PIECE_URI, DatabaseDescription.Piece.buildPieceUri(holder.getAdapterPosition() + 1));
            ArrayList<Uri> recordingUris = DatabaseDescription.Recording.buildRecordingUris(new PianoRepertoireDatabaseHelper(mContext).getReadableDatabase(), holder.getAdapterPosition() + 1);
            ArrayList<Uri> remixUris = DatabaseDescription.Remix.buildRemixUris(new PianoRepertoireDatabaseHelper(mContext).getReadableDatabase(), holder.getAdapterPosition() + 1);

            i.putParcelableArrayListExtra(RECORDING_URIS, recordingUris);
            i.putParcelableArrayListExtra(REMIX_URIS, remixUris);
            i.putExtra(PIECE_ID, holder.getAdapterPosition() + 1);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(i);
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
