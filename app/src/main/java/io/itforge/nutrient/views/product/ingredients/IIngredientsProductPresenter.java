package io.itforge.nutrient.views.product.ingredients;

import java.util.List;

import io.itforge.nutrient.models.AdditiveName;
import io.itforge.nutrient.models.AllergenName;
import io.itforge.nutrient.utils.ProductInfoState;

/**
 * Created by Lobster on 17.03.18.
 */

public interface IIngredientsProductPresenter {

    interface Actions {
        void loadAdditives();
        void loadAllergens();
        void dispose();
    }

    interface View {
        void showAdditives(List<AdditiveName> additives);
        void showAdditivesState(@ProductInfoState String state);

        void showAllergens(List<AllergenName> allergens);
        void showAllergensState(@ProductInfoState String state);
    }

}