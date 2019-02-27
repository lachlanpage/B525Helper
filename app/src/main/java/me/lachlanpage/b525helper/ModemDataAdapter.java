package me.lachlanpage.b525helper;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class ModemDataAdapter extends ArrayAdapter<ModemData>{

    public ModemDataAdapter(@NonNull Context context, @NonNull List<ModemData> objects) {
        super(context, 0, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        // Get the data item for this position

        ModemData data = getItem(position);

        // Check if an existing view is being reused, otherwise inflate the view

        if (convertView == null) {

            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item, parent, false);
        }

        // Lookup view for data population

        TextView heading = (TextView) convertView.findViewById(R.id.textViewName);
        TextView result = (TextView) convertView.findViewById(R.id.textViewResult);

        // disgusting way to reformat lists and bypass recyclerview reusing formatting
        heading.setTextColor(result.getTextColors().getDefaultColor());
        heading.setTextSize(16);

        // change list view formatting if have header, i.e key = ""
        // Change header to have slightly bigger text size and colour
        if(data.getmDataKey().equals("")) {
            heading.setTextSize(20);
            heading.setTextColor(Color.parseColor("#353839"));
        }

        // Populate the data into the template view using the data object

        heading.setText(data.getmDataName());

        result.setText(data.getmDataKey());

        // Return the completed view to render on screen

        return convertView;

    }
}
