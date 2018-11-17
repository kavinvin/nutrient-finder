package io.itforge.nutrient.utils;

import android.support.annotation.StringDef;
import io.itforge.nutrient.BuildConfig;

import java.lang.annotation.Retention;
import java.util.HashMap;

import static java.lang.annotation.RetentionPolicy.SOURCE;
import static io.itforge.nutrient.utils.SearchType.*;


@Retention(SOURCE)
@StringDef({
        ADDITIVE,
        ALLERGEN,
        BRAND,
        CATEGORY,
        COUNTRY,
        EMB,
        LABEL,
        PACKAGING,
        SEARCH,
        STORE,
        TRACE,
        CONTRIBUTOR,
        STATE,
        INCOMPLETE_PRODUCT
})
public @interface SearchType {

    String ADDITIVE = "additive";
    String ALLERGEN = "allergen";
    String BRAND = "brand";
    String CATEGORY = "category";
    String COUNTRY = "country";
    String EMB = "emb";
    String LABEL = "label";
    String PACKAGING = "packaging";
    String SEARCH = "search";
    String STORE = "store";
    String TRACE = "trace";
    String CONTRIBUTOR = "contributor";
    String INCOMPLETE_PRODUCT = "incomplete_product";
    String STATE = "state";


    HashMap<String, String> URLS = new HashMap<String, String>() {{
        put(ALLERGEN, BuildConfig.OFWEBSITE + "allergens/");
        put(EMB, BuildConfig.OFWEBSITE + "packager-code/");
        put(TRACE, BuildConfig.OFWEBSITE + "trace/");
    }};

}
