package io.itforge.nutrient.dagger.module;

import dagger.Module;
import dagger.Provides;
import io.itforge.nutrient.dagger.FragmentScope;
import io.itforge.nutrient.views.viewmodel.category.CategoryFragmentViewModel;

@Module
public class FragmentModule {
    @FragmentScope
    @Provides
    CategoryFragmentViewModel provideCategoryFragmentViewModel() {
        return new CategoryFragmentViewModel();
    }
}
