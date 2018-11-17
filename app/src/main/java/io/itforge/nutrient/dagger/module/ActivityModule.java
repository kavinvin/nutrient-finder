package io.itforge.nutrient.dagger.module;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;

import dagger.Module;
import dagger.Provides;
import io.itforge.nutrient.dagger.ActivityScope;
import io.itforge.nutrient.dagger.Qualifiers;

@Module
public class ActivityModule {
    private AppCompatActivity activity;

    public ActivityModule(AppCompatActivity activity) {
        this.activity = activity;
    }

    @Provides
    @Qualifiers.ForActivity
    @ActivityScope
    Context provideActivityContext() {
        return activity;
    }

}
