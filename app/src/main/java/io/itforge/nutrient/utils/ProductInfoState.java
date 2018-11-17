
package io.itforge.nutrient.utils;

import android.support.annotation.StringDef;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.SOURCE;
import static io.itforge.nutrient.utils.ProductInfoState.EMPTY;
import static io.itforge.nutrient.utils.ProductInfoState.LOADING;

@Retention(SOURCE)
@StringDef({
        LOADING,
        EMPTY
})
public @interface ProductInfoState {
    String LOADING = "loading";
    String EMPTY = "empty";
}