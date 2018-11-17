package io.itforge.nutrient.dagger.component;

import javax.inject.Singleton;

import dagger.Component;
import io.itforge.nutrient.dagger.module.ActivityModule;
import io.itforge.nutrient.dagger.module.AppModule;
import io.itforge.nutrient.views.OFFApplication;

@Component(modules = {AppModule.class})
@Singleton
public interface AppComponent {

    ActivityComponent plusActivityComponent(ActivityModule activityModule);

    void inject(OFFApplication application);

    void inject(io.itforge.nutrient.views.ContinuousScanActivity activity);

    void inject(io.itforge.nutrient.views.AddProductActivity activity);

    final class Initializer {

        private Initializer() {
            //empty
        }

        public static synchronized AppComponent init(AppModule appModule) {
            return DaggerAppComponent.builder()
                    .appModule(appModule)
                    .build();
        }
    }
}
