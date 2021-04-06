package com.example.swipebuttons.utils;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import java.util.LinkedList;
import java.util.Queue;

public class ListItemTouchHelper extends ItemTouchHelper {

    private static final String TAG = "ListItemTouchHelper";

    private Queue<RecyclerView.ViewHolder> recoverQueue = new LinkedList<RecyclerView.ViewHolder>(){
        @Override
        public boolean add(RecyclerView.ViewHolder o) {
            if (contains(o))
                return false;
            else
                return super.add(o);
        }
    };

    public ListItemTouchHelper(@NonNull Callback callback) {
        super(callback);
    }

    @Override
    public void startDrag(@NonNull RecyclerView.ViewHolder viewHolder) {
        if (!recoverQueue.contains(viewHolder)) {
            Log.i(TAG, "startDrag");
            super.startDrag(viewHolder);
        }
    }

    public void onDrop(RecyclerView.ViewHolder viewHolder) {
        Log.i(TAG, "onDrop");
        recoverQueue.remove(viewHolder);
    }
}
