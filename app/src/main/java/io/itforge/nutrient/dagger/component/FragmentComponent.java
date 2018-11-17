package io.itforge.nutrient.dagger.component;

import dagger.Subcomponent;
import io.itforge.nutrient.dagger.FragmentScope;
import io.itforge.nutrient.dagger.module.FragmentModule;
import io.itforge.nutrient.views.category.fragment.CategoryListFragment;

@Subcomponent(modules = {FragmentModule.class})
@FragmentScope
public interface FragmentComponent {
    void inject(CategoryListFragment categoryListFragment);
}