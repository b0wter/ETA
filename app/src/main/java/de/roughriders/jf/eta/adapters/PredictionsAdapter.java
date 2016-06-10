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

/**
 * Created by b0wter on 6/9/16.
 */
public class PredictionsAdapter extends RecyclerView.Adapter<PredictionsAdapter.ViewHolder> {

    ArrayList<AutocompletePrediction> predictions;

    public PredictionsAdapter(){
        predictions = new ArrayList<>();
    }

    public PredictionsAdapter(ArrayList<AutocompletePrediction> predictions){
        this.predictions = predictions;
    }

    @Override
    public PredictionsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_placeprediction, parent, false);
        ViewHolder holder = new ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(PredictionsAdapter.ViewHolder holder, int position) {
        AutocompletePrediction prediction = predictions.get(position);
        holder.primaryTextView.setText(prediction.getPrimaryText(null).toString());
        holder.secondaryTextView.setText(prediction.getSecondaryText(null).toString());
    }

    public void clear(){
        predictions.clear();
        notifyDataSetChanged();
    }

    public void addItem(AutocompletePrediction prediction){
        predictions.add(prediction);
        notifyDataSetChanged();
    }

    public void addItems(List<AutocompletePrediction> predictions){
        predictions.addAll(predictions);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return predictions.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public TextView primaryTextView;
        public TextView secondaryTextView;

        public ViewHolder(View view){
            super(view);
            primaryTextView = (TextView)view.findViewById(R.id.viewholder_placepredictions_primarytext);
            secondaryTextView = (TextView)view.findViewById(R.id.viewholder_placepredictions_secondarytext);
        }
    }
}
