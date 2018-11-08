package io.github.jtmaher2.pianorepertoireapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

class CustomAdapter extends BaseAdapter {
    private final String[] countryNames;
    private final LayoutInflater inflater;

    CustomAdapter(Context applicationContext, String[] countryNames) {
        this.countryNames = countryNames;
        inflater = (LayoutInflater.from(applicationContext));
    }

    @Override
    public int getCount() {
        return countryNames.length;
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = inflater.inflate(R.layout.custom_spinner_items, viewGroup, false);
        }
        TextView names = view.findViewById(R.id.textView);
        names.setText(countryNames[i]);
        return view;
    }
}
