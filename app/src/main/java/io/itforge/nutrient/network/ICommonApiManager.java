package io.itforge.nutrient.network;


public interface ICommonApiManager {

    ProductApiService getProductApiService();

    OpenFoodAPIService getOpenFoodApiService();

}
