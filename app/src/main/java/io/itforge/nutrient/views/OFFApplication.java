package io.itforge.nutrient.views;


import android.support.multidex.MultiDexApplication;
import android.support.v7.app.AppCompatDelegate;

import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.query.QueryBuilder;

import io.itforge.nutrient.BuildConfig;
import io.itforge.nutrient.dagger.component.AppComponent;
import io.itforge.nutrient.dagger.module.AppModule;
import io.itforge.nutrient.models.DaoMaster;
import io.itforge.nutrient.models.DaoSession;
import io.itforge.nutrient.models.DatabaseHelper;

public class OFFApplication extends MultiDexApplication {

    public static DaoSession daoSession;
    private boolean DEBUG = false;

    private static OFFApplication application;
    private static AppComponent appComponent;

    public static AppComponent getAppComponent() {
        return appComponent;
    }

    public static OFFApplication getInstance() {
        return application;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        application = this;
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

        // Use only during development: DaoMaster.DevOpenHelper (Drops all table on Upgrade!)
        // Use only during production: DatabaseHelper (see on Upgrade!)
        String nameDB;
        if((BuildConfig.FLAVOR.equals("off"))) {
            nameDB = "open_food_facts";
        } else if ((BuildConfig.FLAVOR.equals("opff"))) {
            nameDB = "open_pet_food_facts";
        } else if ((BuildConfig.FLAVOR.equals("opf"))) {
            nameDB = "open_products_facts";
        }
        else

        {
            nameDB = "open_beauty_facts";
        }
        DatabaseHelper helper = new DatabaseHelper(this, nameDB);
        Database db = helper.getWritableDb();
        daoSession = new DaoMaster(db).newSession();

        // DEBUG
        QueryBuilder.LOG_VALUES = DEBUG;
        QueryBuilder.LOG_SQL = DEBUG;

        appComponent = AppComponent.Initializer.init(new AppModule(this));
        appComponent.inject(this);
    }

    public DaoSession getDaoSession() {
        return daoSession;
    }
}