
package io.itforge.nutrient.views.product.summary;

import java.util.List;

import io.itforge.nutrient.models.AllergenName;
import io.itforge.nutrient.models.CategoryName;
import io.itforge.nutrient.models.CountryName;
import io.itforge.nutrient.models.LabelName;
import io.itforge.nutrient.utils.ProductInfoState;

/**
 * Created by Lobster on 17.03.18.
 */

public interface ISummaryProductPresenter {

    interface Actions {
        void loadAllergens();

        void loadCategories();

        void loadLabels();

        void loadCountries();

        void dispose();
    }

    interface View {
        void showAllergens(List<AllergenName> allergens);

        void showCategories(List<CategoryName> categories);

        void showLabels(List<LabelName> labels);

        void showCountries(List<CountryName> countries);

        void showCategoriesState(@ProductInfoState String state);

        void showLabelsState(@ProductInfoState String state);

        void showCountriesState(@ProductInfoState String state);
    }

}