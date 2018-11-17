package io.itforge.nutrient.network;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;
import com.afollestad.materialdialogs.MaterialDialog;
import com.fasterxml.jackson.databind.JsonNode;
import com.firebase.jobdispatcher.JobParameters;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import io.itforge.nutrient.BuildConfig;
import io.itforge.nutrient.R;
import io.itforge.nutrient.jobs.SavedProductUploadJob;
import io.itforge.nutrient.models.*;
import io.itforge.nutrient.utils.Utils;
import io.itforge.nutrient.views.AddProductActivity;
import io.itforge.nutrient.views.product.ProductActivity;
import org.apache.commons.lang3.StringUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.*;

import static io.itforge.nutrient.models.ProductImageField.*;
import static io.itforge.nutrient.network.OpenFoodAPIService.PRODUCT_API_COMMENT;

public class OpenFoodAPIClient {

    private AllergenDao mAllergenDao;
    private HistoryProductDao mHistoryProductDao;

    private ToUploadProductDao mToUploadProductDao;
    private OfflineUploadingTask task = new OfflineUploadingTask();
    private static final JacksonConverterFactory jacksonConverterFactory = JacksonConverterFactory.create();
    private DaoSession daoSession;

    private static OkHttpClient httpClient = Utils.HttpClientBuilder();



    private final OpenFoodAPIService apiService;
    private Context mActivity;

    public OpenFoodAPIClient(Activity activity) {
        this(BuildConfig.HOST);
        mAllergenDao = Utils.getAppDaoSession(activity).getAllergenDao();
        mHistoryProductDao = Utils.getAppDaoSession(activity).getHistoryProductDao();
        mToUploadProductDao = Utils.getAppDaoSession(activity).getToUploadProductDao();
        mActivity = activity;
    }

    //used to upload in background
    public OpenFoodAPIClient(Context context) {
        this(BuildConfig.HOST);
        daoSession = Utils.getDaoSession(context);
        mToUploadProductDao = daoSession.getToUploadProductDao();
    }

    public OpenFoodAPIClient(Activity activity, String url) {
        this(url);
        mAllergenDao = Utils.getAppDaoSession(activity).getAllergenDao();
        mHistoryProductDao = Utils.getAppDaoSession(activity).getHistoryProductDao();
        mToUploadProductDao = Utils.getAppDaoSession(activity).getToUploadProductDao();
    }

    private OpenFoodAPIClient(String apiUrl) {
        apiService = new Retrofit.Builder()
                .baseUrl(apiUrl)
                .client(httpClient)
                .addConverterFactory(jacksonConverterFactory)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
                .build()
                .create(OpenFoodAPIService.class);
    }

    /**
     * Open the product activity if the barcode exist.
     * Also add it in the history if the product exist.
     *
     * @param barcode  product barcode
     * @param activity
     */
    public void getProduct(final String barcode, final Activity activity) {

        apiService.getFullProductByBarcode(barcode).enqueue(new Callback<State>() {
            @Override
            public void onResponse(@NonNull Call<State> call, @NonNull Response<State> response) {

                if (activity == null || activity.isFinishing()) {
                    return;
                }

                final State s = response.body();
                if (s.getStatus() == 0) {
                    new MaterialDialog.Builder(activity)
                            .title(R.string.txtDialogsTitle)
                            .content(R.string.txtDialogsContent)
                            .positiveText(R.string.txtYes)
                            .negativeText(R.string.txtNo)
                            .onPositive((dialog, which) -> {
                                if (!activity.isFinishing()) {
                                    Intent intent = new Intent(activity, AddProductActivity.class);
                                    State st = new State();
                                    Product pd = new Product();
                                    pd.setCode(barcode);
                                    st.setProduct(pd);
                                    intent.putExtra("state", st);
                                    activity.startActivity(intent);
                                    activity.finish();
                                }
                            })
                            .show();
                } else {
                    new HistoryTask().doInBackground(s.getProduct());
                    Intent intent = new Intent(activity, ProductActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putSerializable("state", s);
                    intent.putExtras(bundle);
                    activity.startActivity(intent);
                }
            }

            @Override
            public void onFailure(@NonNull Call<State> call, @NonNull Throwable t) {

                if (activity == null || activity.isFinishing()) {
                    return;
                }

                new MaterialDialog.Builder(activity)
                        .title(R.string.txtDialogsTitle)
                        .content(R.string.txtDialogsContent)
                        .positiveText(R.string.txtYes)
                        .negativeText(R.string.txtNo)
                        .onPositive((dialog, which) -> {
                            if (!activity.isFinishing()) {
                                Intent intent = new Intent(activity, AddProductActivity.class);
                                State st = new State();
                                Product pd = new Product();
                                pd.setCode(barcode);
                                st.setProduct(pd);
                                intent.putExtra("state", st);
                                activity.startActivity(intent);
                                activity.finish();
                            }
                        })
                        .show();
            }
        });

    }

    public void searchProduct(final String name, final int page, final Activity activity, final OnProductsCallback productsCallback) {
        apiService.searchProductByName(name, page).enqueue(new Callback<Search>() {
            @Override
            public void onResponse(@NonNull Call<Search> call, @NonNull Response<Search> response) {
                if (!response.isSuccessful()) {
                    productsCallback.onProductsResponse(false, null, -1);
                    return;
                }

                Search s = response.body();
                if (Integer.valueOf(s.getCount()) == 0) {
                    productsCallback.onProductsResponse(false, null, -2);
                } else {
                    productsCallback.onProductsResponse(true, s, Integer.parseInt(s.getCount()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<Search> call, @NonNull Throwable t) {
                Toast.makeText(activity, activity.getString(R.string.errorWeb), Toast.LENGTH_LONG).show();
                productsCallback.onProductsResponse(false, null, -1);
            }
        });
    }


    public void onResponseCallForPostFunction(Call<State> call, Response<State> response, Context activity, final OnProductSentCallback productSentCallback, SendProduct product) {
        if (!response.isSuccessful() || response.body().getStatus() == 0) {
            //lt.error();
            productSentCallback.onProductSentResponse(false);
            return;
        }

        String imguploadFront = product.getImgupload_front();
        if (StringUtils.isNotEmpty(imguploadFront)) {
            ProductImage image = new ProductImage(product.getBarcode(), FRONT, new File(imguploadFront));
            postImg(activity, image);
        }

        String imguploadIngredients = product.getImgupload_ingredients();
        if (StringUtils.isNotEmpty(imguploadIngredients)) {
            postImg(activity, new ProductImage(product.getBarcode(), INGREDIENTS, new File(imguploadIngredients)));
        }

        String imguploadNutrition = product.getImgupload_nutrition();
        if (StringUtils.isNotBlank(imguploadNutrition)) {
            postImg(activity, new ProductImage(product.getBarcode(), NUTRITION, new File(imguploadNutrition)));
        }

        //lt.success();
        productSentCallback.onProductSentResponse(true);
    }


    /**
     * @return This api service gets products of provided brand.
     */
    public OpenFoodAPIService getAPIService() {
        return apiService;
    }

    public void getBrand(final String brand, final int page, final OnBrandCallback onBrandCallback) {

        apiService.getProductByBrands(brand, page).enqueue(new Callback<Search>() {
            @Override
            public void onResponse(Call<Search> call, Response<Search> response) {
                onBrandCallback.onBrandResponse(true, response.body());
            }

            @Override
            public void onFailure(Call<Search> call, Throwable t) {
                onBrandCallback.onBrandResponse(false, null);
            }
        });


    }

    /**
     * This method is used to upload products.
     * Conditional statements in this method ensures that data which is being sent on server is correct
     * and if the product is already present with more information then the server doesn't assume to delete that
     * and write new product's data over that.
     */
    public void post(final Context activity, final SendProduct product, final OnProductSentCallback productSentCallback) {
        // final LoadToast lt = new LoadToast(activity);
        ProgressDialog dialog = new ProgressDialog(activity, ProgressDialog.STYLE_SPINNER);
        dialog.setIndeterminate(true);
        dialog.setMessage(activity.getString(R.string.toastSending));
//        lt.show();

        if (product.getName().equals("") && product.getBrands().equals("") && product.getQuantity() == null) {
            apiService.saveProductWithoutNameBrandsAndQuantity(product.getBarcode(), product.getLang(), product.getUserId(), product.getPassword(), PRODUCT_API_COMMENT).enqueue(new Callback<State>() {
                @Override
                public void onResponse(Call<State> call, Response<State> response) {
                    onResponseCallForPostFunction(call, response, activity, productSentCallback, product);
                    dialog.dismiss();
                }

                @Override
                public void onFailure(Call<State> call, Throwable t) {
                   // lt.error();
                    productSentCallback.onProductSentResponse(false);
                    dialog.dismiss();
                }
            });
        } else if (product.getName().equals("") && product.getBrands().equals("")) {
            apiService.saveProductWithoutNameAndBrands(product.getBarcode(), product.getLang(), product.getQuantity(), product.getUserId(), product.getPassword(), PRODUCT_API_COMMENT).enqueue(new Callback<State>() {
                @Override
                public void onResponse(Call<State> call, Response<State> response) {
                    onResponseCallForPostFunction(call, response, activity, productSentCallback, product);
                    dialog.dismiss();
                }

                @Override
                public void onFailure(Call<State> call, Throwable t) {
                   // lt.error();
                    productSentCallback.onProductSentResponse(false);
                    dialog.dismiss();
                }
            });
        } else if (product.getName().equals("") && product.getQuantity() == null) {
            apiService.saveProductWithoutNameAndQuantity(product.getBarcode(), product.getLang(), product.getBrands(), product.getUserId(), product.getPassword(), PRODUCT_API_COMMENT).enqueue(new Callback<State>() {
                @Override
                public void onResponse(Call<State> call, Response<State> response) {
                    onResponseCallForPostFunction(call, response, activity, productSentCallback, product);
                    dialog.dismiss();
                }

                @Override
                public void onFailure(Call<State> call, Throwable t) {
                    //lt.error();
                    productSentCallback.onProductSentResponse(false);
                    dialog.dismiss();
                }
            });
        } else if (product.getBrands().equals("") && product.getQuantity() == null) {
            apiService.saveProductWithoutBrandsAndQuantity(product.getBarcode(), product.getLang(), product.getName(), product.getUserId(), product.getPassword(), PRODUCT_API_COMMENT).enqueue(new Callback<State>() {
                @Override
                public void onResponse(Call<State> call, Response<State> response) {
                    onResponseCallForPostFunction(call, response, activity, productSentCallback, product);
                    dialog.dismiss();
                }

                @Override
                public void onFailure(Call<State> call, Throwable t) {
                   // lt.error();
                    productSentCallback.onProductSentResponse(false);
                    dialog.dismiss();
                }
            });
        } else {
            apiService.saveProduct(product.getBarcode(), product.getLang(), product.getName(), product.getBrands(), product.getQuantity(), product
                    .getUserId(), product.getPassword(), PRODUCT_API_COMMENT).enqueue(new Callback<State>() {
                @Override
                public void onResponse(Call<State> call, Response<State> response) {
                    onResponseCallForPostFunction(call, response, activity, productSentCallback, product);
                    dialog.dismiss();
                }

                @Override
                public void onFailure(Call<State> call, Throwable t) {
                   // lt.error();
                    productSentCallback.onProductSentResponse(false);
                    dialog.dismiss();
                }
            });
        }

    }

    public void postImg(final Context context, final ProductImage image) {
     /**  final LoadToast lt = new LoadToast(context);
        lt.show();**/

        apiService.saveImage(getUploadableMap(image, context))
                .enqueue(new Callback<JsonNode>() {
                    @Override
                    public void onResponse(@NonNull Call<JsonNode> call, @NonNull Response<JsonNode> response) {
                        Log.d("onResponse", response.toString());
                        if (!response.isSuccessful()) {
                            ToUploadProduct product = new ToUploadProduct(image.getBarcode(), image.getFilePath(), image.getImageField().toString());
                            mToUploadProductDao.insertOrReplace(product);
                            Toast.makeText(context, response.toString(), Toast.LENGTH_LONG).show();
                            //lt.error();
                            return;
                        }

                        JsonNode body = response.body();
                        Log.d("onResponse", body.toString());
                        if (!body.isObject()) {
                            //lt.error();
                        } else if (body.get("status").asText().contains("status not ok")) {
                            Toast.makeText(context, body.get("error").asText(), Toast.LENGTH_LONG).show();
                           // lt.error();
                        } else {
                            //lt.success();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<JsonNode> call, @NonNull Throwable t) {
                        Log.d("onResponse", t.toString());
                        ToUploadProduct product = new ToUploadProduct(image.getBarcode(), image.getFilePath(), image.getImageField().toString());
                        mToUploadProductDao.insertOrReplace(product);
                        Toast.makeText(context, context.getString(R.string.uploadLater), Toast.LENGTH_LONG).show();
                       // lt.error();
                    }
                });
    }

    private Map<String, RequestBody> getUploadableMap(ProductImage image, Context context) {
        String lang = Locale.getDefault().getLanguage();

        Map<String, RequestBody> imgMap = new HashMap<>();
        imgMap.put("code", image.getCode());
        imgMap.put("imagefield", image.getField());
        if (image.getImguploadFront() != null)
            imgMap.put("imgupload_front\"; filename=\"front_" + lang + ".png\"", image.getImguploadFront());
        if (image.getImguploadIngredients() != null)
            imgMap.put("imgupload_ingredients\"; filename=\"ingredients_" + lang + ".png\"", image.getImguploadIngredients());
        if (image.getImguploadNutrition() != null)
            imgMap.put("imgupload_nutrition\"; filename=\"nutrition_" + lang + ".png\"", image.getImguploadNutrition());
        if (image.getImguploadOther() != null)
            imgMap.put("imgupload_other\"; filename=\"other_" + lang + ".png\"", image.getImguploadOther());

        // Attribute the upload to the connected user
        final SharedPreferences settings = context.getSharedPreferences("login", 0);
        final String login = settings.getString("user", "");
        final String password = settings.getString("pass", "");

        if (!login.isEmpty() && !password.isEmpty()) {
            imgMap.put("user_id", RequestBody.create(MediaType.parse("text/plain"), login));
            imgMap.put("password", RequestBody.create(MediaType.parse("text/plain"), password));
        }
        return imgMap;
    }

    public interface OnProductsCallback {

        void onProductsResponse(boolean isOk, Search searchResponse, int countProducts);
    }

    public interface OnAllergensCallback {

        void onAllergensResponse(boolean value, Search allergen);
    }

    public interface OnBrandCallback {

        void onBrandResponse(boolean value, Search brand);
    }

    public interface OnStoreCallback {
        void onStoreResponse(boolean value, Search store);
    }

    public interface OnPackagingCallback {
        void onPackagingResponse(boolean value, Search packaging);
    }


    public interface OnAdditiveCallback {

        void onAdditiveResponse(boolean value, Search brand);
    }

    public interface OnProductSentCallback {
        void onProductSentResponse(boolean value);
    }

    public interface onCountryCallback {
        void onCountryResponse(boolean value, Search country);
    }

    public interface onLabelCallback {
        void onLabelResponse(boolean value, Search label);
    }

    public interface onCategoryCallback {
        void onCategoryResponse(boolean value, Search category);
    }


    public interface onContributorCallback {
        void onContributorResponse(boolean value, Search contributor);
    }


    /**
     * Create an history product asynchronously
     */
    private class HistoryTask extends AsyncTask<Product, Void, Void> {

        @Override
        protected Void doInBackground(Product... products) {
            Product product = products[0];

            List<HistoryProduct> historyProducts = mHistoryProductDao.queryBuilder().where(HistoryProductDao.Properties.Barcode.eq(product.getCode())).list();
            HistoryProduct hp;
            if (historyProducts.size() == 1) {
                hp = historyProducts.get(0);
                hp.setLastSeen(new Date());
            } else {
                hp = new HistoryProduct(product.getProductName(), product.getBrands(), product.getImageSmallUrl(), product.getCode(), product
                        .getQuantity(), product.getNutritionGradeFr());
            }
            mHistoryProductDao.insertOrReplace(hp);

            return null;
        }
    }

    public void uploadOfflineImages(Context context, boolean cancel, JobParameters job, SavedProductUploadJob service) {
        if (!cancel) {
//            Toast.makeText(context, "called function", Toast.LENGTH_SHORT).show();
            task.job = job;
            task.service = new WeakReference<>(service);
            task.execute(context);
        } else {
            task.cancel(true);
        }
    }

    public class OfflineUploadingTask extends AsyncTask<Context, Void, Void> {
        JobParameters job;
        WeakReference<SavedProductUploadJob> service;

        @Override
        protected Void doInBackground(Context... context) {
            List<ToUploadProduct> toUploadProductList = mToUploadProductDao.queryBuilder().where(ToUploadProductDao.Properties.Uploaded.eq(false)
            ).list();
            int totalSize = toUploadProductList.size();
            for (int i = 0; i < totalSize; i++) {
                ToUploadProduct uploadProduct = toUploadProductList.get(i);
                File imageFile;
                try {
                    imageFile = new File(uploadProduct.getImageFilePath());
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }
                ProductImage productImage = new ProductImage(uploadProduct.getBarcode(),
                        uploadProduct.getProductField(), imageFile);

                apiService.saveImage(getUploadableMap(productImage, context[0]))
                        .enqueue(new Callback<JsonNode>() {
                            @Override
                            public void onResponse(@NonNull Call<JsonNode> call, @NonNull Response<JsonNode> response) {
                                if (!response.isSuccessful()) {
                                    Toast.makeText(context[0], response.toString(), Toast.LENGTH_LONG).show();
                                    return;
                                }

                                JsonNode body = response.body();
                                Log.d("onResponse", body.toString());
                                if (!body.isObject()) {

                                } else if (body.get("status").asText().contains("status not ok")) {
                                    mToUploadProductDao.delete(uploadProduct);
                                } else {
                                    mToUploadProductDao.delete(uploadProduct);
                                }
                            }

                            @Override
                            public void onFailure(@NonNull Call<JsonNode> call, @NonNull Throwable t) {

                            }
                        });

            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Log.d("serviceValue", service.get().toString());
            service.get().jobFinished(job, false);

        }
    }

    public void getProductsByBrand(final String brand, final int page, final OnBrandCallback onBrandCallback) {

        apiService.getProductByBrands(brand, page).enqueue(new Callback<Search>() {
            @Override
            public void onResponse(@NonNull Call<Search> call, @NonNull Response<Search> response) {


                if (!response.isSuccessful()) {
                    onBrandCallback.onBrandResponse(false, null);
                    return;
                }

                if (Integer.valueOf(response.body().getCount()) == 0) {
                    onBrandCallback.onBrandResponse(false, null);
                } else {
                    onBrandCallback.onBrandResponse(true, response.body());
                }
            }

            @Override
            public void onFailure(@NonNull Call<Search> call, @NonNull Throwable t) {
                onBrandCallback.onBrandResponse(false, null);
            }
        });

    }


    public void getProductsByPackaging(final String packaging, final int page, final OnPackagingCallback onPackagingCallback) {

        apiService.getProductByPackaging(packaging, page).enqueue(new Callback<Search>() {
            @Override
            public void onResponse(@NonNull Call<Search> call, @NonNull Response<Search> response) {


                if (!response.isSuccessful()) {
                    onPackagingCallback.onPackagingResponse(false, null);
                    return;
                }

                if (Integer.valueOf(response.body().getCount()) == 0) {
                    onPackagingCallback.onPackagingResponse(false, null);
                } else {
                    onPackagingCallback.onPackagingResponse(true, response.body());
                }
            }

            @Override
            public void onFailure(@NonNull Call<Search> call, @NonNull Throwable t) {
                onPackagingCallback.onPackagingResponse(false, null);
            }
        });

    }


    public void syncOldHistory() {
//        Log.d("syncOldHistory", "task ");
        new SyncOldHistoryTask().execute();
    }


    public class SyncOldHistoryTask extends AsyncTask<Void, Void, Void> {
        boolean success = true;

        @Override
        protected Void doInBackground(Void... voids) {
            List<HistoryProduct> historyProducts = mHistoryProductDao.loadAll();
            int size = historyProducts.size();
            for (int i = 0; i < size; i++) {
                HistoryProduct historyProduct = historyProducts.get(i);
                apiService.getShortProductByBarcode(historyProduct.getBarcode()).enqueue(new Callback<State>() {

                    @Override
                    public void onResponse(@NonNull Call<State> call, @NonNull Response<State> response) {
                        final State s = response.body();

                        if (s.getStatus() != 0) {
                            Product product = s.getProduct();
                            HistoryProduct hp = new HistoryProduct(product.getProductName(), product.getBrands(), product.getImageSmallUrl(),
                                    product.getCode(), product.getQuantity(), product.getNutritionGradeFr());
                            Log.d("syncOldHistory", hp.toString());

                            hp.setLastSeen(historyProduct.getLastSeen());
                            mHistoryProductDao.insertOrReplace(hp);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<State> call, @NonNull Throwable t) {
                        success = false;
                    }
                });
            }
            return null;
        }


        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (success) {
                mActivity.getSharedPreferences("prefs", 0).edit().putBoolean("is_old_history_data_synced", true).apply();
            }
        }
    }


    public void getProductsByStore(final String store, final int page, final OnStoreCallback onStoreCallback) {
        apiService.getProductByStores(store, page).enqueue(new Callback<Search>() {
            @Override
            public void onResponse(@NonNull Call<Search> call, @NonNull Response<Search> response) {


                if (!response.isSuccessful()) {
                    onStoreCallback.onStoreResponse(false, null);
                    return;
                }

                if (Integer.valueOf(response.body().getCount()) == 0) {
                    onStoreCallback.onStoreResponse(false, null);
                } else {
                    onStoreCallback.onStoreResponse(true, response.body());
                }
            }

            @Override
            public void onFailure(@NonNull Call<Search> call, @NonNull Throwable t) {
                onStoreCallback.onStoreResponse(false, null);
            }
        });

    }


    public void getProductsByCountry(String country, final int page, final onCountryCallback onCountryCallback) {
        apiService.getProductsByCountry(country, page).enqueue(new Callback<Search>() {

            @Override
            public void onResponse(@NonNull Call<Search> call, @NonNull Response<Search> response) {


                if (!response.isSuccessful()) {
                    onCountryCallback.onCountryResponse(false, null);
                    return;
                }

                if (response.isSuccessful()) {
                    if (Integer.valueOf(response.body().getCount()) == 0) {
                        onCountryCallback.onCountryResponse(false, null);
                    } else {
                        onCountryCallback.onCountryResponse(true, response.body());
                    }
                }


            }

            @Override
            public void onFailure(@NonNull Call<Search> call, @NonNull Throwable t) {

                onCountryCallback.onCountryResponse(false, null);

            }
        });
    }


    public void getProductsByAdditive(final String additive, final int page, final OnAdditiveCallback onAdditiveCallback) {

        apiService.getProductsByAdditive(additive, page).enqueue(new Callback<Search>() {
            @Override
            public void onResponse(@NonNull Call<Search> call, @NonNull Response<Search> response) {
                if (!response.isSuccessful()) {
                    onAdditiveCallback.onAdditiveResponse(false, null);
                    return;
                }

                if (Integer.valueOf(response.body().getCount()) == 0) {
                    onAdditiveCallback.onAdditiveResponse(false, null);
                } else {
                    onAdditiveCallback.onAdditiveResponse(true, response.body());
                }
            }

            @Override
            public void onFailure(@NonNull Call<Search> call, @NonNull Throwable t) {
                onAdditiveCallback.onAdditiveResponse(false, null);
            }
        });

    }

    public void getProductsByAllergen(final String allergen, final int page, final OnAllergensCallback onAllergensCallback) {
        apiService.getProductsByAllergen(allergen, page).enqueue(new Callback<Search>() {
            @Override
            public void onResponse(Call<Search> call, Response<Search> response) {
                if (!response.isSuccessful()) {
                    onAllergensCallback.onAllergensResponse(false, null);
                    return;
                }

                if (Integer.valueOf(response.body().getCount()) == 0) {
                    onAllergensCallback.onAllergensResponse(false, null);
                } else {
                    onAllergensCallback.onAllergensResponse(true, response.body());
                }
            }

            @Override
            public void onFailure(Call<Search> call, Throwable t) {
                onAllergensCallback.onAllergensResponse(false, null);
            }
        });
    }

    public void getProductsByLabel(String label, final int page, final onLabelCallback onLabelCallback) {
        apiService.getProductByLabel(label, page).enqueue(new Callback<Search>() {
            @Override
            public void onResponse(@NonNull Call<Search> call, @NonNull Response<Search> response) {

                if (!response.isSuccessful()) {
                    onLabelCallback.onLabelResponse(false, null);
                    return;
                }

                if (response.isSuccessful()) {

                    if (Integer.valueOf(response.body().getCount()) == 0) {
                        onLabelCallback.onLabelResponse(false, null);
                    } else {
                        onLabelCallback.onLabelResponse(true, response.body());
                    }
                }


            }

            @Override
            public void onFailure(@NonNull Call<Search> call, @NonNull Throwable t) {

                onLabelCallback.onLabelResponse(false, null);

            }
        });
    }


    public void getProductsByCategory(String category, final int page, final onCategoryCallback onCategoryCallback) {
        apiService.getProductByCategory(category, page).enqueue(new Callback<Search>() {
            @Override
            public void onResponse(@NonNull Call<Search> call, @NonNull Response<Search> response) {


                if (!response.isSuccessful()) {
                    onCategoryCallback.onCategoryResponse(false, null);
                    return;
                }

                if (response.isSuccessful()) {
                    onCategoryCallback.onCategoryResponse(true, response.body());
                }


            }

            @Override
            public void onFailure(@NonNull Call<Search> call, @NonNull Throwable t) {

                onCategoryCallback.onCategoryResponse(false, null);

            }
        });
    }


    public void getProductsByContributor(String contributor, final int page, final onContributorCallback onContributorCallback) {
        apiService.searchProductsByContributor(contributor, page).enqueue(new Callback<Search>() {
            @Override
            public void onResponse(@NonNull Call<Search> call, @NonNull Response<Search> response) {


                if (!response.isSuccessful()) {
                    onContributorCallback.onContributorResponse(false, null);
                    return;
                }

                if (response.isSuccessful()) {

                    if (Integer.valueOf(response.body().getCount()) == 0) {
                        onContributorCallback.onContributorResponse(false, null);
                        return;
                    } else {
                        onContributorCallback.onContributorResponse(true, response.body());
                    }
                }


            }

            @Override
            public void onFailure(@NonNull Call<Search> call, @NonNull Throwable t) {

                onContributorCallback.onContributorResponse(false, null);

            }
        });
    }

    public interface OnIncompleteCallback {
        void onIncompleteResponse(boolean value, Search incompleteProducts);
    }

    public void getIncompleteProducts(int page, OnIncompleteCallback onIncompleteCallback) {
        apiService.getIncompleteProducts(page).enqueue(new Callback<Search>() {
            @Override
            public void onResponse(Call<Search> call, Response<Search> response) {
                if (!response.isSuccessful()) {
                    onIncompleteCallback.onIncompleteResponse(false, null);
                    return;
                }

                if (response.isSuccessful()) {

                    if (Integer.valueOf(response.body().getCount()) == 0) {
                        onIncompleteCallback.onIncompleteResponse(false, null);
                        return;
                    } else {
                        onIncompleteCallback.onIncompleteResponse(true, response.body());
                    }
                }
            }

            @Override
            public void onFailure(Call<Search> call, Throwable t) {

                onIncompleteCallback.onIncompleteResponse(false, null);
            }
        });
    }

    public void getToBeCompletedProductsByContributor(String contributor, final int page, final onContributorCallback onContributorCallback) {
        apiService.getToBeCompletedProductsByContributor(contributor, page).enqueue(new Callback<Search>() {
            @Override
            public void onResponse(@NonNull Call<Search> call, @NonNull Response<Search> response) {


                if (!response.isSuccessful()) {
                    onContributorCallback.onContributorResponse(false, null);
                    return;
                }

                if (response.isSuccessful()) {

                    if (Integer.valueOf(response.body().getCount()) == 0) {
                        onContributorCallback.onContributorResponse(false, null);
                        return;
                    } else {
                        onContributorCallback.onContributorResponse(true, response.body());
                    }
                }


            }

            @Override
            public void onFailure(@NonNull Call<Search> call, @NonNull Throwable t) {

                onContributorCallback.onContributorResponse(false, null);

            }
        });
    }

    public void getPicturesContributedProducts(String contributor, final int page, final onContributorCallback onContributorCallback) {
        apiService.getPicturesContributedProducts(contributor, page).enqueue(new Callback<Search>() {
            @Override
            public void onResponse(@NonNull Call<Search> call, @NonNull Response<Search> response) {


                if (!response.isSuccessful()) {
                    onContributorCallback.onContributorResponse(false, null);
                    return;
                }

                if (response.isSuccessful()) {

                    if (Integer.valueOf(response.body().getCount()) == 0) {
                        onContributorCallback.onContributorResponse(false, null);
                        return;
                    } else {
                        onContributorCallback.onContributorResponse(true, response.body());
                    }
                }


            }

            @Override
            public void onFailure(@NonNull Call<Search> call, @NonNull Throwable t) {

                onContributorCallback.onContributorResponse(false, null);

            }
        });
    }

    public void getPicturesContributedIncompleteProducts(String contributor, final int page, final onContributorCallback onContributorCallback) {
        apiService.getPicturesContributedIncompleteProducts(contributor, page).enqueue(new Callback<Search>() {
            @Override
            public void onResponse(@NonNull Call<Search> call, @NonNull Response<Search> response) {


                if (!response.isSuccessful()) {
                    onContributorCallback.onContributorResponse(false, null);
                    return;
                }

                if (response.isSuccessful()) {

                    if (Integer.valueOf(response.body().getCount()) == 0) {
                        onContributorCallback.onContributorResponse(false, null);
                        return;
                    } else {
                        onContributorCallback.onContributorResponse(true, response.body());
                    }
                }


            }

            @Override
            public void onFailure(@NonNull Call<Search> call, @NonNull Throwable t) {

                onContributorCallback.onContributorResponse(false, null);

            }
        });
    }

    public void getInfoAddedProducts(String contributor, final int page, final onContributorCallback onContributorCallback) {
        apiService.getInfoAddedProducts(contributor, page).enqueue(new Callback<Search>() {
            @Override
            public void onResponse(@NonNull Call<Search> call, @NonNull Response<Search> response) {


                if (!response.isSuccessful()) {
                    onContributorCallback.onContributorResponse(false, null);
                    return;
                }

                if (response.isSuccessful()) {

                    if (Integer.valueOf(response.body().getCount()) == 0) {
                        onContributorCallback.onContributorResponse(false, null);
                        return;
                    } else {
                        onContributorCallback.onContributorResponse(true, response.body());
                    }
                }


            }

            @Override
            public void onFailure(@NonNull Call<Search> call, @NonNull Throwable t) {

                onContributorCallback.onContributorResponse(false, null);

            }
        });
    }

    public void getInfoAddedIncompleteProducts(String contributor, final int page, final onContributorCallback onContributorCallback) {
        apiService.getInfoAddedIncompleteProducts(contributor, page).enqueue(new Callback<Search>() {
            @Override
            public void onResponse(@NonNull Call<Search> call, @NonNull Response<Search> response) {


                if (!response.isSuccessful()) {
                    onContributorCallback.onContributorResponse(false, null);
                    return;
                }

                if (response.isSuccessful()) {

                    if (Integer.valueOf(response.body().getCount()) == 0) {
                        onContributorCallback.onContributorResponse(false, null);
                        return;
                    } else {
                        onContributorCallback.onContributorResponse(true, response.body());
                    }
                }


            }

            @Override
            public void onFailure(@NonNull Call<Search> call, @NonNull Throwable t) {

                onContributorCallback.onContributorResponse(false, null);

            }
        });
    }


    public interface onStateCallback {
        void onStateResponse(boolean value, Search state);
    }

    public void getProductsByStates(String state, final int page, final onStateCallback onStateCallback) {
        apiService.getProductsByState(state, page).enqueue(new Callback<Search>() {
            @Override
            public void onResponse(@NonNull Call<Search> call, @NonNull Response<Search> response) {


                if (!response.isSuccessful()) {
                    onStateCallback.onStateResponse(false, null);
                    return;
                }

                if (response.isSuccessful()) {

                    if (Integer.valueOf(response.body().getCount()) == 0) {
                        onStateCallback.onStateResponse(false, null);
                        return;
                    } else {
                        onStateCallback.onStateResponse(true, response.body());
                    }
                }


            }

            @Override
            public void onFailure(@NonNull Call<Search> call, @NonNull Throwable t) {

                onStateCallback.onStateResponse(false, null);

            }
        });
    }

    /**
     * OnResponseCall for uploads through notifications
     */
    public void onResponseCallForNotificationPostFunction(Call<State> call, Response<State> response, Context context, final OnProductSentCallback productSentCallback, SendProduct product) {
        if (!response.isSuccessful() || response.body().getStatus() == 0) {

            productSentCallback.onProductSentResponse(false);
            return;
        }

        String imguploadFront = product.getImgupload_front();
        if (StringUtils.isNotEmpty(imguploadFront)) {
            ProductImage image = new ProductImage(product.getBarcode(), FRONT, new File(imguploadFront));
            postImg(context, image);
        }

        String imguploadIngredients = product.getImgupload_ingredients();
        if (StringUtils.isNotEmpty(imguploadIngredients)) {
            postImg(context, new ProductImage(product.getBarcode(), INGREDIENTS, new File(imguploadIngredients)));
        }

        String imguploadNutrition = product.getImgupload_nutrition();
        if (StringUtils.isNotBlank(imguploadNutrition)) {
            postImg(context, new ProductImage(product.getBarcode(), NUTRITION, new File(imguploadNutrition)));
        }


        productSentCallback.onProductSentResponse(true);
    }


    /**
     * Post method for upload through notification
     */
    public void postForNotification(final Context context, final SendProduct product, final OnProductSentCallback productSentCallback) {

        if (product.getName().equals("") && product.getBrands().equals("") && product.getQuantity() == null) {
            apiService.saveProductWithoutNameBrandsAndQuantity(product.getBarcode(), product.getLang(), product.getUserId(), product.getPassword(), PRODUCT_API_COMMENT).enqueue(new Callback<State>() {
                @Override
                public void onResponse(Call<State> call, Response<State> response) {
                    onResponseCallForNotificationPostFunction(call, response, context, productSentCallback, product);

                }

                @Override
                public void onFailure(Call<State> call, Throwable t) {

                    productSentCallback.onProductSentResponse(false);

                }
            });
        } else if (product.getName().equals("") && product.getBrands().equals("")) {
            apiService.saveProductWithoutNameAndBrands(product.getBarcode(), product.getLang(), product.getQuantity(), product.getUserId(), product.getPassword(), PRODUCT_API_COMMENT).enqueue(new Callback<State>() {
                @Override
                public void onResponse(Call<State> call, Response<State> response) {
                    onResponseCallForNotificationPostFunction(call, response, context, productSentCallback, product);

                }

                @Override
                public void onFailure(Call<State> call, Throwable t) {

                    productSentCallback.onProductSentResponse(false);

                }
            });
        } else if (product.getName().equals("") && product.getQuantity() == null) {
            apiService.saveProductWithoutNameAndQuantity(product.getBarcode(), product.getLang(), product.getBrands(), product.getUserId(), product.getPassword(), PRODUCT_API_COMMENT).enqueue(new Callback<State>() {
                @Override
                public void onResponse(Call<State> call, Response<State> response) {
                    onResponseCallForNotificationPostFunction(call, response, context, productSentCallback, product);

                }

                @Override
                public void onFailure(Call<State> call, Throwable t) {

                    productSentCallback.onProductSentResponse(false);

                }
            });
        } else if (product.getBrands().equals("") && product.getQuantity() == null) {
            apiService.saveProductWithoutBrandsAndQuantity(product.getBarcode(), product.getLang(), product.getName(), product.getUserId(), product.getPassword(), PRODUCT_API_COMMENT).enqueue(new Callback<State>() {
                @Override
                public void onResponse(Call<State> call, Response<State> response) {
                    onResponseCallForNotificationPostFunction(call, response, context, productSentCallback, product);

                }

                @Override
                public void onFailure(Call<State> call, Throwable t) {

                    productSentCallback.onProductSentResponse(false);

                }
            });
        } else {
            apiService.saveProduct(product.getBarcode(), product.getLang(), product.getName(), product.getBrands(), product.getQuantity(), product
                    .getUserId(), product.getPassword(), PRODUCT_API_COMMENT).enqueue(new Callback<State>() {
                @Override
                public void onResponse(Call<State> call, Response<State> response) {
                    onResponseCallForNotificationPostFunction(call, response, context, productSentCallback, product);

                }

                @Override
                public void onFailure(Call<State> call, Throwable t) {

                    productSentCallback.onProductSentResponse(false);

                }
            });
        }

    }
}
