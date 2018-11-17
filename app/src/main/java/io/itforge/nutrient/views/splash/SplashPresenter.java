package io.itforge.nutrient.views.splash;

import android.content.SharedPreferences;

import java.util.Arrays;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.itforge.nutrient.BuildConfig;
import io.itforge.nutrient.repositories.IProductRepository;
import io.itforge.nutrient.repositories.ProductRepository;
import io.itforge.nutrient.utils.Utils;

public class SplashPresenter implements ISplashPresenter.Actions {

    private final Long REFRESH_PERIOD = 6 * 30 * 24 * 60 * 60 * 1000L;

    private ISplashPresenter.View view;
    private SharedPreferences settings;
    private IProductRepository productRepository;

    public SplashPresenter(SharedPreferences settings, ISplashPresenter.View view) {
        this.view = view;
        this.settings = settings;
        productRepository = ProductRepository.getInstance();
    }

    @Override
    public void refreshData() {
        if (BuildConfig.FLAVOR.equals("off")) {
            boolean firstRun = settings.getBoolean("firstRun", true);
            if (firstRun) {
                settings.edit()
                        .putBoolean("firstRun", false)
                        .apply();
            }

            if (isNeedToRefresh()) { //true if data was refreshed more than 1 day ago
                Single.zip(
                        productRepository.getLabels(true),
                        productRepository.getTags(true),
                        productRepository.getAllergens(true),
                        productRepository.getCountries(true),
                        productRepository.getAdditives(true),
                        productRepository.getCategories(true), (labels, tags, allergens, countries, additives, categories) -> {
                            Completable.merge(
                                    Arrays.asList(
                                            Completable.fromAction(() -> productRepository.saveLabels(labels)),
                                            Completable.fromAction(() -> productRepository.saveTags(tags)),
                                            Completable.fromAction(() -> productRepository.saveAllergens(allergens)),
                                            Completable.fromAction(() -> productRepository.saveCountries(countries)),
                                            Completable.fromAction(() -> productRepository.saveAdditives(additives)),
                                            Completable.fromAction(() -> productRepository.saveCategories(categories))
                                    )
                            ).subscribeOn(Schedulers.computation())
                                    .subscribe(() -> {
                                        settings.edit().putLong(Utils.LAST_REFRESH_DATE, System.currentTimeMillis()).apply();
                                    }, Throwable::printStackTrace);

                            return true;
                        })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .toCompletable()
                        //.doOnSubscribe(d -> view.showLoading())
                        .subscribe(() -> {
                            //view.hideLoading(false);
                            view.navigateToMainActivity();
                        }, e -> {
                            e.printStackTrace();
                            //view.hideLoading(true);
                            view.navigateToMainActivity();
                        });
            } else {
                view.navigateToMainActivity();
            }
        } else {
            view.navigateToMainActivity();
        }
    }

    /*
    * This method checks if data was refreshed more than 1 day ago
     */
    private Boolean isNeedToRefresh() {
        return System.currentTimeMillis() - settings.getLong(Utils.LAST_REFRESH_DATE, 0) > REFRESH_PERIOD;
    }
}
