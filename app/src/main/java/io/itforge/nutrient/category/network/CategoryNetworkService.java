package io.itforge.nutrient.category.network;

import io.reactivex.Single;
import retrofit2.http.GET;

@FunctionalInterface
public interface CategoryNetworkService {

    @GET("categories.json")
    Single<CategoryResponse> getCategories();
}
