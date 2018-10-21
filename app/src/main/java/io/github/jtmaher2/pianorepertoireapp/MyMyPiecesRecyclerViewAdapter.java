package io.github.jtmaher2.pianorepertoireapp;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MyMyPiecesRecyclerViewAdapter extends RecyclerView.Adapter<MyMyPiecesRecyclerViewAdapter.MyViewHolder> {
    private String[] mDataset;
    private Context mContext;
    static class MyViewHolder extends RecyclerView.ViewHolder {
        LinearLayout mLinearLayout;

        MyViewHolder(LinearLayout l) {
            super(l);
            mLinearLayout = l;
        }
    }

    MyMyPiecesRecyclerViewAdapter(String[] myDataset, Context context) {
        mDataset = myDataset;
        mContext = context;
    }

    @NonNull
    @Override
    public MyMyPiecesRecyclerViewAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder((LinearLayout)LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.fragment_mypieces, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        ((TextView)holder.mLinearLayout.findViewById(R.id.piece_name_textview_list)).setText(mDataset[position]);
        holder.mLinearLayout.findViewById(R.id.piece_details_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(mContext, DetailsActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(i);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mDataset.length;
    }
}
