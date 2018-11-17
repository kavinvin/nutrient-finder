package io.itforge.nutrient.views.category.adapter;

import android.support.v7.widget.RecyclerView;

import io.itforge.nutrient.databinding.CategoryRecyclerItemBinding;
import io.itforge.nutrient.models.CategoryName;
import io.itforge.nutrient.utils.SearchType;
import io.itforge.nutrient.views.ProductBrowsingListActivity;

/**
 * Created by Abdelali Eramli on 27/12/2017.
 */

public class CategoryViewHolder extends RecyclerView.ViewHolder {
    private final CategoryRecyclerItemBinding binding;

    public CategoryViewHolder(CategoryRecyclerItemBinding binding) {
        super(binding.getRoot());
        this.binding = binding;
    }

    public void bind(CategoryName category) {
        binding.setCategory(category);
        binding.getRoot().setOnClickListener(v -> ProductBrowsingListActivity.startActivity(v.getContext(), category.getName(), SearchType.CATEGORY));
        binding.executePendingBindings();
    }
}
