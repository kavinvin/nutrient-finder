package io.itforge.nutrient.dagger.component;

import dagger.Subcomponent;
import io.itforge.nutrient.dagger.ActivityScope;
import io.itforge.nutrient.dagger.module.ActivityModule;
import io.itforge.nutrient.views.BaseActivity;

@Subcomponent(modules = {ActivityModule.class})
@ActivityScope
public interface ActivityComponent {

    FragmentComponent plusFragmentComponent();

    void inject(BaseActivity baseActivity);
}
