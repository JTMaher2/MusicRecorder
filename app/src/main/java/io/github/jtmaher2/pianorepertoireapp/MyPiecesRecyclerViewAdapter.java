package io.github.jtmaher2.pianorepertoireapp;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

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
            SQLiteDatabase db = new PianoRepertoireDatabaseHelper(mContext).getReadableDatabase();
            ArrayList<Uri> recordingUris = DatabaseDescription.Recording.buildRecordingUris(db, holder.getAdapterPosition() + 1);
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
