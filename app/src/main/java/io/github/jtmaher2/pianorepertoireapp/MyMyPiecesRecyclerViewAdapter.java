package io.github.jtmaher2.pianorepertoireapp;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import io.github.jtmaher2.pianorepertoireapp.MyPiecesFragment.OnListFragmentInteractionListener;
import io.github.jtmaher2.pianorepertoireapp.dummy.DummyContent.DummyItem;

/**
 * {@link RecyclerView.Adapter} that can display a {@link DummyItem} and makes a call to the
 * specified {@link OnListFragmentInteractionListener}.
 * TODO: Replace the implementation with code for your data type.
 */
public class MyMyPiecesRecyclerViewAdapter extends RecyclerView.Adapter<MyMyPiecesRecyclerViewAdapter.MyViewHolder> {
    private String[] mDataset;

    static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView mTextView;
        MyViewHolder(TextView v) {
            super(v);
            mTextView = v;
        }
    }

    MyMyPiecesRecyclerViewAdapter(String[] myDataset) {
        mDataset = myDataset;
    }

    @NonNull
    @Override
    public MyMyPiecesRecyclerViewAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LinearLayout v = (LinearLayout)LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_mypieces, parent, false);
        return new MyViewHolder((TextView)v.findViewById(R.id.piece_name_textview));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        holder.mTextView.setText(mDataset[position]);
    }

    @Override
    public int getItemCount() {
        return mDataset.length;
    }
}
