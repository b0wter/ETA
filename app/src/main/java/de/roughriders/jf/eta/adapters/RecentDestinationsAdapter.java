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
import de.roughriders.jf.eta.helpers.IRecyclerIndexedViewItemClicked;
import de.roughriders.jf.eta.helpers.IRecyclerViewItemClicked;
import de.roughriders.jf.eta.models.RecentDestination;

/**
 * Created by b0wter on 6/10/16.
 */
public class RecentDestinationsAdapter extends RecyclerView.Adapter<RecentDestinationsAdapter.ViewHolder> implements IRecyclerIndexedViewItemClicked {

    ArrayList<RecentDestination> recentDestinations;
    List<IRecyclerViewItemClicked<RecentDestination>> itemClickedListeners = new ArrayList<>();

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
        ViewHolder holder = new ViewHolder(view, this);
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

    public void addOnItemclickedListener(IRecyclerViewItemClicked<RecentDestination> listener){
        itemClickedListeners.add(listener);
    }

    public void removeOnItemClickedListener(IRecyclerViewItemClicked<RecentDestination> listener){
        itemClickedListeners.remove(listener);
    }

    @Override
    public void onItemClicked(int position) {
        for(IRecyclerViewItemClicked<RecentDestination> listener : itemClickedListeners)
            listener.onItemclicked(recentDestinations.get(position));
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public TextView primaryTextView;
        public TextView secondaryTextView;
        private IRecyclerIndexedViewItemClicked itemClickedListener;

        public ViewHolder(View view, IRecyclerIndexedViewItemClicked onItemClickedListener){
            super(view);
            view.setOnClickListener(this);
            primaryTextView = (TextView)view.findViewById(R.id.viewholder_recentdestinations_primarytext);
            secondaryTextView = (TextView)view.findViewById(R.id.viewholder_recentdestinations_secondarytext);
            itemClickedListener = onItemClickedListener;
        }

        @Override
        public void onClick(View view) {
            if(itemClickedListener != null)
                itemClickedListener.onItemClicked(getAdapterPosition());
        }
    }
}
