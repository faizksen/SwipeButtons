package com.example.swipebuttons;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.swipebuttons.utils.ListItemDragCallback;
import com.example.swipebuttons.utils.ListItemTouchHelper;
import com.example.swipebuttons.utils.OnStartDragListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RecyclerAdapter adapter;
    private ListItemTouchHelper itemTouchHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        List<ListItem> items = Arrays.asList(
                new ListItem("Kate Winslet", "02.04.2021"),
                new ListItem("Kate Blanchett", "02.04.2021"),
                new ListItem("Kate Beckinsale", "02.04.2021"),
                new ListItem("Kate Barell", "02.04.2021"),
                new ListItem("Kate Hudson", "02.04.2021"),
                new ListItem("Kate Winslet", "02.04.2021"),
                new ListItem("Kate Winslet", "02.04.2021"),
                new ListItem("Kate Winslet", "02.04.2021"),
                new ListItem("Kate Winslet", "02.04.2021"),
                new ListItem("Kate Winslet", "02.04.2021"),
                new ListItem("Kate Winslet", "02.04.2021")
        );
        recyclerView = findViewById(R.id.recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        ListItemDragCallback.DropListener dropListener = new ListItemDragCallback.DropListener() {
            @Override
            public void onDrop(RecyclerView.ViewHolder viewHolder) {
                if (itemTouchHelper != null) {
                    itemTouchHelper.onDrop(viewHolder);
                }
            }
        };
        itemTouchHelper = new ListItemTouchHelper(new ListItemDragCallback(this, recyclerView, ItemTouchHelper.START, dropListener) {
            @Override
            public void instantiateUnderlayButton(RecyclerView.ViewHolder viewHolder, int position, List<ListItemDragCallback.UnderlayButton> leftButtons, List<UnderlayButton> rightButtons) {
                rightButtons.addAll(getButtons(position));
            }
        });
        itemTouchHelper.attachToRecyclerView(recyclerView);

        adapter = new RecyclerAdapter(new ItemTouchListener() {
            @Override
            public void onItemClicked() {
                Toast.makeText(MainActivity.this, "ITEM pressed", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
                itemTouchHelper.startDrag(viewHolder);
            }
        });
        recyclerView.setAdapter(adapter);

        adapter.setItems(items);
    }

    private Collection<? extends ListItemDragCallback.UnderlayButton> getButtons(int position) {
        ArrayList<ListItemDragCallback.UnderlayButton> list = new ArrayList<>();
        list.add(getUnreadButton());
        list.add(getHideButton());
        return list;
    }

    private ListItemDragCallback.UnderlayButton getHideButton() {
        return new ListItemDragCallback.UnderlayButton(
                "Hide",
                getResources().getDimensionPixelSize(R.dimen.underlay_button_text_size),
                ContextCompat.getColor(this, R.color.design_default_color_secondary),
                this::onChatHidePressed
        );
    }

    private void onChatHidePressed(int i) {
        Toast.makeText(this, "HIDE pressed", Toast.LENGTH_SHORT).show();
    }

    private ListItemDragCallback.UnderlayButton getUnreadButton() {
        return new ListItemDragCallback.UnderlayButton(
                "Unread",
                getResources().getDimensionPixelSize(R.dimen.underlay_button_text_size),
                ContextCompat.getColor(this, R.color.purple_200),
                this::onChatUnreadPressed
        );
    }

    private void onChatUnreadPressed(int i) {
        Toast.makeText(this, "UNREAD pressed", Toast.LENGTH_SHORT).show();
    }

    static class ListItem {
        public String title;
        public String date;

        public ListItem (String title, String date) {
            this.title = title;
            this.date = date;
        }
    }

    class RecyclerAdapter extends RecyclerView.Adapter<RecyclerViewHolder> {

        private static final String TAG = "RecyclerAdapter";

        private final ItemTouchListener touchListener;

        private ArrayList<ListItem> items;

        public RecyclerAdapter(ItemTouchListener touchListener) {
            this.touchListener = touchListener;
        }

        public void setItems(List<ListItem> items) {
            if (items == null) {
                items = new ArrayList<>();
            }
            this.items = new ArrayList<>(items);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public RecyclerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new RecyclerViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerViewHolder holder, int position) {
            Log.i(TAG, "onBindViewHolder position = " + position);
            holder.itemView.setTranslationX(0);

            GestureDetector gd = new GestureDetector(holder.itemView.getContext(), new GestureDetector.SimpleOnGestureListener() {

                @Override
                public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                    Log.i(TAG, "onScroll distance = " + (e1.getX()-e2.getX()));
                    if (touchListener != null) {
                        touchListener.onStartDrag(holder);
                        return true;
                    }
                    return super.onScroll(e1, e2, distanceX, distanceY);
                }
            });

            holder.root.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return gd.onTouchEvent(event);
                }
            });

            holder.root.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (touchListener != null) {
                        touchListener.onItemClicked();
                    }
                }
            });

            ListItem item = items.get(position);
            holder.title.setText(item.title);
            holder.date.setText(item.date);
        }

        @Override
        public int getItemCount() {
            return items == null ? 0 : items.size();
        }
    }

    interface ItemTouchListener extends OnStartDragListener {
        void onItemClicked();
    }

    class RecyclerViewHolder extends RecyclerView.ViewHolder {

        public View root;
        public TextView title, date;

        public RecyclerViewHolder(@NonNull View itemView) {
            super(itemView);

            root = itemView;
            title = itemView.findViewById(R.id.title);
            date = itemView.findViewById(R.id.date);
        }
    }
}