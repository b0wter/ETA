package de.roughriders.jf.eta.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.location.places.AutocompletePrediction;

import java.util.ArrayList;
import java.util.List;

import de.roughriders.jf.eta.R;
import de.roughriders.jf.eta.helpers.IRecyclerIndexedViewItemClicked;
import de.roughriders.jf.eta.helpers.IRecyclerViewItemClicked;
import de.roughriders.jf.eta.models.RecentDestination;

/**
 * Created by b0wter on 6/9/16.
 */
public class PredictionsAdapter extends RecyclerView.Adapter<PredictionsAdapter.ViewHolder> implements IRecyclerIndexedViewItemClicked {

    ArrayList<RecentDestination> destinations;
    List<IRecyclerViewItemClicked<RecentDestination>> itemClickedListeners = new ArrayList<>();

    public PredictionsAdapter(){
        destinations = new ArrayList<>();
    }

    @Override
    public PredictionsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_placeprediction, parent, false);
        ViewHolder holder = new ViewHolder(view, this);
        return holder;
    }

    @Override
    public void onBindViewHolder(PredictionsAdapter.ViewHolder holder, int position) {
        RecentDestination prediction = destinations.get(position);
        holder.primaryTextView.setText(prediction.primaryText);
        holder.secondaryTextView.setText(prediction.secondaryText);
    }

    public void clear(){
        destinations.clear();
        notifyDataSetChanged();
    }

    public void addItem(AutocompletePrediction prediction){
        destinations.add(RecentDestination.fromPrediction(prediction));
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return destinations.size();
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
            listener.onItemclicked(destinations.get(position));
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        public TextView primaryTextView;
        public TextView secondaryTextView;
        private IRecyclerIndexedViewItemClicked itemClickedListener;

        public ViewHolder(View view, IRecyclerIndexedViewItemClicked onItemClickedListener){
            super(view);
            view.setOnClickListener(this);
            primaryTextView = (TextView)view.findViewById(R.id.viewholder_placepredictions_primarytext);
            secondaryTextView = (TextView)view.findViewById(R.id.viewholder_placepredictions_secondarytext);
            itemClickedListener = onItemClickedListener;
        }

        @Override
        public void onClick(View view) {
            if(itemClickedListener != null)
                itemClickedListener.onItemClicked(getAdapterPosition());
        }
    }
}
