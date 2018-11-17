package io.itforge.nutrient.network;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface WikidataApiService {


    @GET("{code}.json")
    Call<Object> getWikiCategory(@Path("code") String code);
}
