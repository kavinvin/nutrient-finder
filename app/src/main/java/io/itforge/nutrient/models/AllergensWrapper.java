package io.itforge.nutrient.models;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.ArrayList;
import java.util.List;

import io.itforge.nutrient.network.deserializers.AllergensWrapperDeserializer;


@JsonDeserialize(using = AllergensWrapperDeserializer.class)
public class AllergensWrapper {

    private List<AllergenResponse> allergens;

    /**
     * @return A list of Allergen objects
     */
    public List<Allergen> map() {
        List<Allergen> entityAllergens = new ArrayList<>();
        for (AllergenResponse allergen : allergens) {
            entityAllergens.add(allergen.map());
        }

        return entityAllergens;
    }

    public void setAllergens(List<AllergenResponse> allergens) {
        this.allergens = allergens;
    }

    public List<AllergenResponse> getAllergens() {
        return allergens;
    }
}
