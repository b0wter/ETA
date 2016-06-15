package de.roughriders.jf.eta.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.roughriders.jf.eta.R;
import de.roughriders.jf.eta.models.RecentTrip;

/**
 * Created by b0wter on 6/11/16.
 */
public class RecentTripsAdapter extends RecyclerView.Adapter<RecentTripsAdapter.ViewHolder> {

    ArrayList<RecentTrip> trips;

    public RecentTripsAdapter(Context context){
        trips = RecentTrip.getFromSharedPreferences(context);
    }

    public void addItem(RecentTrip trip){
        trips.add(trip);
        notifyDataSetChanged();
    }

    public void addItems(Collection<RecentTrip> trips){
        this.trips.addAll(trips);
        notifyDataSetChanged();
    }

    public void clear(){
        trips.clear();
        notifyDataSetChanged();
    }

    public int size(){
        return trips.size();
    }

    public List<RecentTrip> getItems(){ return trips; }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_recenttrips, parent, false);
        ViewHolder holder = new ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        RecentTrip trip = trips.get(position);
        holder.primaryText.setText(trip.contact.name);
        holder.secondaryText.setText(trip.destination.primaryText);
    }

    @Override
    public int getItemCount(){
        return trips.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public TextView primaryText;
        public TextView secondaryText;

        public ViewHolder(View view){
            super(view);
            primaryText = (TextView)view.findViewById(R.id.viewholder_recenttrips_primarytext);
            secondaryText = (TextView)view.findViewById(R.id.viewholder_recenttrips_secondarytext);
        }
    }
}
