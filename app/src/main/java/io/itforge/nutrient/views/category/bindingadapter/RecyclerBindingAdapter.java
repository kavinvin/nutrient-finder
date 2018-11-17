package io.itforge.nutrient.views.category.bindingadapter;

import android.databinding.BindingAdapter;
import android.support.v7.widget.RecyclerView;

import java.util.List;

import io.itforge.nutrient.models.CategoryName;
import io.itforge.nutrient.views.category.adapter.CategoryListRecyclerAdapter;

public class RecyclerBindingAdapter {
    @BindingAdapter({"categories"})
    public static void setStations(RecyclerView recyclerView, List<CategoryName> categoryList) {
        if (recyclerView != null && categoryList != null) {
            recyclerView.setAdapter(new CategoryListRecyclerAdapter(categoryList));
        }
    }
}