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
import de.roughriders.jf.eta.helpers.IRecyclerIndexedViewItemClicked;
import de.roughriders.jf.eta.helpers.IRecyclerViewItemClicked;
import de.roughriders.jf.eta.models.RecentDestination;
import de.roughriders.jf.eta.models.RecentTrip;

/**
 * Created by b0wter on 6/11/16.
 */
public class RecentTripsAdapter extends RecyclerView.Adapter<RecentTripsAdapter.ViewHolder> implements IRecyclerIndexedViewItemClicked {

    ArrayList<RecentTrip> trips;
    List<IRecyclerViewItemClicked<RecentTrip>> itemClickedListeners = new ArrayList<>();

    public RecentTripsAdapter(Context context){
        trips = RecentTrip.getFromSharedPreferences(context);
    }

    public void addItem(RecentTrip trip){
        if(!alreadyContainsTrip(trip)) {
            trips.add(trip);
            notifyDataSetChanged();
        }
    }

    public void updateFromSharedPreferences(Context context){
        trips.clear();
        trips = RecentTrip.getFromSharedPreferences(context);
        notifyDataSetChanged();
    }

    public void clearAll(){
        trips.clear();
        notifyDataSetChanged();
    }

    private boolean alreadyContainsTrip(RecentTrip trip){
        for(RecentTrip t : trips){
            if(     trip.contact.name.equals(t.contact.name) &&
                    trip.contact.phone.equals(t.contact.phone) &&
                    trip.destination.primaryText.equals(t.destination.primaryText) &&
                    trip.destination.secondaryText.equals(t.destination.secondaryText))
                return true;
        }
        return false;
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
        ViewHolder holder = new ViewHolder(view, this);
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

    public void addOnItemclickedListener(IRecyclerViewItemClicked<RecentTrip> listener){
        itemClickedListeners.add(listener);
    }

    public void removeOnItemClickedListener(IRecyclerViewItemClicked<RecentTrip> listener){
        itemClickedListeners.remove(listener);
    }

    @Override
    public void onItemClicked(int position) {
        for(IRecyclerViewItemClicked<RecentTrip> listener : itemClickedListeners)
            listener.onItemclicked(trips.get(position));
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public TextView primaryText;
        public TextView secondaryText;
        private IRecyclerIndexedViewItemClicked itemClickedListener;

        public ViewHolder(View view, IRecyclerIndexedViewItemClicked onItemClickedListener){
            super(view);
            view.setOnClickListener(this);
            itemView.setOnClickListener(this);
            primaryText = (TextView)view.findViewById(R.id.viewholder_recenttrips_primarytext);
            secondaryText = (TextView)view.findViewById(R.id.viewholder_recenttrips_secondarytext);
            itemClickedListener = onItemClickedListener;
        }

        @Override
        public void onClick(View view) {
            if(itemClickedListener != null)
                itemClickedListener.onItemClicked(getAdapterPosition());
        }
    }
}
