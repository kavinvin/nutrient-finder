package io.itforge.nutrient.category.mapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import io.itforge.nutrient.category.model.Category;
import io.itforge.nutrient.category.network.CategoryResponse;

public class CategoryMapper {

    @Inject
    public CategoryMapper() {
    }

    public List<Category> fromNetwork(List<CategoryResponse.Tag> tags) {
        List<Category> categories = new ArrayList<>(tags.size());
        for (CategoryResponse.Tag tag : tags) {
            categories.add(new Category(tag.getId(),
                    tag.getName(),
                    tag.getUrl(),
                    tag.getProducts()));
        }
        Collections.sort(categories, (first, second) -> first.getName().compareTo(second.getName()));
        return categories;
    }
}