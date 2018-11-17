package io.itforge.nutrient.views;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;

import butterknife.ButterKnife;
import io.itforge.nutrient.R;
import io.itforge.nutrient.dagger.component.ActivityComponent;
import io.itforge.nutrient.dagger.module.ActivityModule;
import io.itforge.nutrient.utils.LocaleHelper;

public abstract class BaseActivity extends AppCompatActivity {

    private ActivityComponent activityComponent;

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getResources().getBoolean(R.bool.portrait_only)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        activityComponent = OFFApplication.getAppComponent().plusActivityComponent(new ActivityModule(this));
        activityComponent.inject(this);
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        super.setContentView(layoutResID);
        ButterKnife.bind(this);
        LocaleHelper.onCreate(this);
    }

    public ActivityComponent getActivityComponent() {
        return activityComponent;
    }
}