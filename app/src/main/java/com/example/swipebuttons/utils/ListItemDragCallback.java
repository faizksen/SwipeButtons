package com.example.swipebuttons.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import static java.lang.Math.abs;
import static java.lang.Math.max;

public abstract class ListItemDragCallback extends ItemTouchHelper.Callback {

    private static final String TAG = "ListDragCallback";

    public static final int BUTTON_WIDTH = 200;
    public static final int FIX_OFFSET_IF_ONE_BUTTON = 20;

    private RecyclerView recyclerView;
    private int dragDirs;
    private DropListener dropListener;

    List<UnderlayButton> leftButtons = new ArrayList<>();
    List<UnderlayButton> rightButtons = new ArrayList<>();
    private List<UnderlayButton> buttons;
    private int currentPosition = -1;
    private Queue<Integer> recoverQueue = new LinkedList<Integer>(){
        @Override
        public boolean add(Integer o) {
            if (contains(o))
                return false;
            else
                return super.add(o);
        }
    };
    private float currentDir = 0;//для определения какие кнопки сейчас использовать левые или правые
    private float dragOffset = 0;
    private Boolean isDragToRestoreState = null;
    private boolean isButtonsFixed = false;
    private boolean isPrevActive = false;

    private GestureDetector.SimpleOnGestureListener gestureListener = new GestureDetector.SimpleOnGestureListener(){
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            for (UnderlayButton button : buttons){
                if (button.onClick(e.getX(), e.getY()))
                    break;
            }
            recoverSwipedItem();

            return true;
        }
    };

    private GestureDetector gestureDetector;

    private View.OnTouchListener onTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent e) {
            if (currentPosition > -1) {
                Point point = new Point((int) e.getRawX(), (int) e.getRawY());

                RecyclerView.ViewHolder swipedViewHolder = recyclerView.findViewHolderForAdapterPosition(currentPosition);
                if (swipedViewHolder != null) {
                    View swipedItem = swipedViewHolder.itemView;
                    Rect rect = new Rect();
                    swipedItem.getGlobalVisibleRect(rect);

                    if (e.getAction() == MotionEvent.ACTION_DOWN || e.getAction() == MotionEvent.ACTION_UP) {// ||e.getAction() == MotionEvent.ACTION_MOVE
                        if (rect.top < point.y && rect.bottom > point.y) {
                            Log.i(TAG, "onTouch gestureDetector event = " + e.getAction());
                            gestureDetector.onTouchEvent(e);
                        } else {
                            recoverQueue.add(currentPosition);
                        }
                    }
                }
            }

            return false;
        }
    };

    public ListItemDragCallback(Context context, RecyclerView recyclerView, int dragDirs, DropListener dropListener) {
        gestureDetector = new GestureDetector(context, gestureListener);
        this.recyclerView = recyclerView;
        this.dragDirs = dragDirs;
        this.dropListener = dropListener;
        recyclerView.setOnTouchListener(onTouchListener);
    }

    public abstract void instantiateUnderlayButton(RecyclerView.ViewHolder viewHolder, int position, List<UnderlayButton> leftButtons, List<UnderlayButton> rightButtons);

    private synchronized void recoverSwipedItem() {
        Log.i(TAG,
                "recoverSwipedItem "
                        + "recoverQueue first = " + recoverQueue.peek()
                        + "; currentPosition = " + currentPosition
                        + "; currentDir = " + currentDir
                        + "; isDragToRestoreState = " + isDragToRestoreState
                        + "; isButtonsFixed = " + isButtonsFixed
                        + "; dragOffset = " + dragOffset
        );
        currentPosition = -1;
        currentDir = 0;
        isDragToRestoreState = null;
        isButtonsFixed = false;
        dragOffset = 0;
        RecyclerView.Adapter adapter = recyclerView.getAdapter();
        if (adapter != null) {
            while (!recoverQueue.isEmpty()){
                int pos = recoverQueue.poll();
                RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(pos);
                if (pos > -1 && viewHolder != null) {
                    clearView(recyclerView, viewHolder);
                    adapter.notifyItemChanged(currentPosition);
                }
            }
        }
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        int pos = viewHolder.getAdapterPosition();
        Log.i(TAG,
                "getMovementFlags for pos = " + pos
                        + "; currentPosition = " + currentPosition
        );
        if (pos != currentPosition && currentPosition > -1) {
            Log.i(TAG,
                    "getMovementFlags recoverSwipedItem"
            );
            recoverQueue.add(currentPosition);
            recoverSwipedItem();
        }
        return makeMovementFlags(dragDirs, 0);
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return false;
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) { }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {

        Log.i(TAG,
                "onChildDraw state drag = " + (actionState == ItemTouchHelper.ACTION_STATE_DRAG)
                        + ";dX = " + dX
                        + ";isCurrentlyActive = " + isCurrentlyActive
                        + ";isPrevActive = " + isPrevActive
        );
        int pos = viewHolder.getAdapterPosition();
        float translationX = dX;
        View itemView = viewHolder.itemView;

        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
            float delta = abs(dX);
            float oldDir = currentDir;

            if (dX == 0 || dX < 0 && oldDir > 0 || dX > 0 && oldDir < 0) {
                updateButtons(viewHolder, pos);
            }

            if (dX != 0) {
                currentDir = dX;
            }
            buttons = currentDir > 0 ? leftButtons : rightButtons;

            Log.i(TAG,
                    "onChildDraw buffer.size() = " + buttons.size()
            );
            float buttonsWidth = buttons.size() * BUTTON_WIDTH;
            Log.i(TAG,
                    "onChildDraw delta = " + delta
                            + ";oldDragOffset = " + dragOffset
                            + ";buttonsWidth = " + buttonsWidth
                            + ";currentPosition = " + currentPosition
                            + ";currentDir = " + currentDir
            );

            if (!isCurrentlyActive && isPrevActive) {
                dropListener.onDrop(viewHolder);
            }

            if (isCurrentlyActive) {
                if (!isPrevActive) {
                    dragOffset = 0;
                }
                isDragToRestoreState = max(0, abs(dragOffset) - FIX_OFFSET_IF_ONE_BUTTON) > delta;
                if (delta != 0) {
                    dragOffset = dX;
                }
            }

            boolean overDragButtonWidth = abs(dragOffset) >= (buttonsWidth - FIX_OFFSET_IF_ONE_BUTTON);
            Log.i(TAG,
                    "onChildDraw isDragToRestoreState = " + isDragToRestoreState
                            + "; dragOffset = " + dragOffset
                            + "; overDragButtonWidth = " + overDragButtonWidth
            );
            if (overDragButtonWidth && !isDragToRestoreState) {
                isButtonsFixed = true;
            } else if (overDragButtonWidth) {
                isButtonsFixed = false;
            }
            if (isButtonsFixed && currentPosition != pos) {
                currentPosition = pos;
            }
            Log.i(TAG,
                    "onChildDraw isButtonsFixed = " + isButtonsFixed
            );

            if (isButtonsFixed) {
                translationX = currentDir < 0 ? -buttonsWidth : buttonsWidth;
            }
            Log.i(TAG,
                    "onChildDraw translationX = " + translationX
            );

            if (translationX > 0) {
                translationX = Math.min(buttonsWidth, translationX);
            } else if (translationX < 0) {
                translationX = Math.max(-buttonsWidth, translationX);
            }

            Log.i(TAG,
                    "onChildDraw translationX = " + translationX
            );
            drawButtons(c, itemView, buttons, pos, translationX);
        }

        isPrevActive = isCurrentlyActive;
        super.onChildDraw(c, recyclerView, viewHolder, translationX, dY, actionState, isCurrentlyActive);
    }

    private void updateButtons(RecyclerView.ViewHolder viewHolder, int pos) {
        leftButtons.clear();
        rightButtons.clear();
        instantiateUnderlayButton(viewHolder, pos, leftButtons, rightButtons);
    }

    private void drawButtons(Canvas c, View itemView, List<UnderlayButton> buffer, int pos, float dX){
        float right = itemView.getRight();
        float left = itemView.getLeft();
        float dButtonWidth = (-1) * dX / buffer.size();

        if (dX != 0) {
            for (UnderlayButton button : buffer) {
                if (dX < 0) { //right buttons
                    left = right - dButtonWidth;
                    button.onDraw(
                            c,
                            new RectF(
                                    left,
                                    itemView.getTop(),
                                    right,
                                    itemView.getBottom()
                            ),
                            pos
                    );
                    right = left;
                } else { //left buttons
                    right = left - dButtonWidth;
                    button.onDraw(
                            c,
                            new RectF(
                                    left,
                                    itemView.getTop(),
                                    right,
                                    itemView.getBottom()
                            ),
                            pos
                    );
                    left = right;
                }
            }
        }
    }

    public static class UnderlayButton {
        private String text;
        private int textSize;
        private int color;
        private int pos;
        private RectF clickRegion;
        private UnderlayButtonClickListener clickListener;

        public UnderlayButton(String text, int textSize, int color, UnderlayButtonClickListener clickListener) {
            this.text = text;
            this.textSize = textSize;
            this.color = color;
            this.clickListener = clickListener;
        }

        public boolean onClick(float x, float y){
            if (clickRegion != null && clickRegion.contains(x, y)) {
                clickListener.onClick(pos);
                return true;
            }

            return false;
        }

        public void onDraw(Canvas c, RectF rect, int pos) {
            Paint p = new Paint();

            // Draw background
            p.setColor(color);
            c.drawRect(rect, p);

            // Draw Text
            p.setColor(Color.WHITE);
            p.setTextSize(textSize);

            Rect r = new Rect();
            float cHeight = rect.height();
            float cWidth = rect.width();
            p.setTextAlign(Paint.Align.LEFT);
            p.getTextBounds(text, 0, text.length(), r);
            float x = cWidth / 2f - r.width() / 2f - r.left;
            float y = cHeight / 2f + r.height() / 2f - r.bottom;

            c.drawText(text, rect.left + x, rect.top + y, p);

            clickRegion = rect;
            this.pos = pos;
        }
    }

    public interface DropListener {
        void onDrop(RecyclerView.ViewHolder viewHolder);
    }

    public interface UnderlayButtonClickListener {
        void onClick(int pos);
    }
}
