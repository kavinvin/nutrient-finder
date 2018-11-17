
package io.itforge.nutrient.repositories;

import java.util.List;

import io.reactivex.Single;
import io.itforge.nutrient.models.Additive;
import io.itforge.nutrient.models.AdditiveName;
import io.itforge.nutrient.models.Allergen;
import io.itforge.nutrient.models.AllergenName;
import io.itforge.nutrient.models.Category;
import io.itforge.nutrient.models.CategoryName;
import io.itforge.nutrient.models.Country;
import io.itforge.nutrient.models.CountryName;
import io.itforge.nutrient.models.Label;
import io.itforge.nutrient.models.LabelName;
import io.itforge.nutrient.models.Tag;


public interface IProductRepository {

    Single<List<Label>> getLabels(Boolean refresh);

    Single<List<Allergen>> getAllergens(Boolean refresh);

    Single<List<Tag>> getTags(Boolean refresh);

    Single<List<Additive>> getAdditives(Boolean refresh);

    Single<List<Country>> getCountries(Boolean refresh);

    Single<List<Category>> getCategories(Boolean refresh);

    void saveLabels(List<Label> labels);

    void saveTags(List<Tag> tags);

    void saveAdditives(List<Additive> additives);

    void saveCountries(List<Country> countries);

    void saveAllergens(List<Allergen> allergens);

    void saveCategories(List<Category> categories);

    void setAllergenEnabled(String allergenTag, Boolean isEnabled);

    Single<LabelName> getLabelByTagAndLanguageCode(String labelTag, String languageCode);

    Single<LabelName> getLabelByTagAndDefaultLanguageCode(String labelTag);

    Single<CountryName> getCountryByTagAndLanguageCode(String labelTag, String languageCode);

    Single<CountryName> getCountryByTagAndDefaultLanguageCode(String labelTag);

    Single<AdditiveName> getAdditiveByTagAndLanguageCode(String additiveTag, String languageCode);

    Single<AdditiveName> getAdditiveByTagAndDefaultLanguageCode(String additiveTag);

    Single<CategoryName> getCategoryByTagAndLanguageCode(String categoryTag, String languageCode);

    Single<CategoryName> getCategoryByTagAndDefaultLanguageCode(String categoryTag);

    Single<List<CategoryName>> getAllCategoriesByLanguageCode(String languageCode);

    Single<List<CategoryName>> getAllCategoriesByDefaultLanguageCode();

    List<Allergen> getEnabledAllergens();

    Single<List<AllergenName>> getAllergensByEnabledAndLanguageCode(Boolean isEnabled, String languageCode);

    Single<List<AllergenName>> getAllergensByLanguageCode(String languageCode);

    Single<AllergenName> getAllergenByTagAndLanguageCode(String allergenTag, String languageCode);

    Single<AllergenName> getAllergenByTagAndDefaultLanguageCode(String allergenTag);

    Boolean additivesIsEmpty();

}