package org.a5calls.android.a5calls.view;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

public class GridItemDecoration extends RecyclerView.ItemDecoration {

    private int gridSpacingPx;
    private int gridSize;

    public GridItemDecoration(int gridSpacingPx, int gridSize) {
        this.gridSpacingPx = gridSpacingPx;
        this.gridSize = gridSize;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        int itemPosition = ((RecyclerView.LayoutParams) view.getLayoutParams()).getViewAdapterPosition();

        if (itemPosition < gridSize) {
            outRect.top = 0;
        } else {
            outRect.top = gridSpacingPx / 2;
        }

        if (itemPosition % gridSize == 0) {
            outRect.left = 0;
            outRect.right = gridSpacingPx / 2;
        } else if ((itemPosition + 1) % gridSize == 0) {
            outRect.right = 0;
            outRect.left = gridSpacingPx / 2;
        } else {
            outRect.left = gridSpacingPx / 2;
            outRect.right = gridSpacingPx / 2;
        }

        outRect.bottom = gridSpacingPx / 2;
    }
}
