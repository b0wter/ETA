package de.roughriders.jf.eta.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.roughriders.jf.eta.R;
import de.roughriders.jf.eta.models.RecentDestination;

/**
 * Created by b0wter on 6/10/16.
 */
public class RecentDestinationsAdapter extends RecyclerView.Adapter<RecentDestinationsAdapter.ViewHolder> {

    ArrayList<RecentDestination> recentDestinations;

    public RecentDestinationsAdapter(Context context){
        recentDestinations = RecentDestination.getFromSharedPreferences(context);
    }

    public void addItem(RecentDestination destination){
        recentDestinations.add(destination);
        notifyDataSetChanged();
    }

    public void addItems(Collection<RecentDestination> items){
        recentDestinations.addAll(items);
        notifyDataSetChanged();
    }

    public int size(){
        return recentDestinations.size();
    }

    public List<RecentDestination> getItems(){
        return recentDestinations;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_recentdestinations, parent, false);
        ViewHolder holder = new ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        RecentDestination destination = recentDestinations.get(position);
        holder.primaryTextView.setText(destination.primaryText);
        holder.secondaryTextView.setText(destination.secondaryText);
    }

    @Override
    public int getItemCount() {
        return recentDestinations.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public TextView primaryTextView;
        public TextView secondaryTextView;

        public ViewHolder(View view){
            super(view);
            primaryTextView = (TextView)view.findViewById(R.id.viewholder_recentdestinations_primarytext);
            secondaryTextView = (TextView)view.findViewById(R.id.viewholder_recentdestinations_secondarytext);
        }
    }
}
