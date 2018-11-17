package io.itforge.nutrient.views.product.nutrition;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.customtabs.CustomTabsIntent;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import io.itforge.nutrient.R;
import io.itforge.nutrient.fragments.BaseFragment;
import io.itforge.nutrient.models.NutrientLevelItem;
import io.itforge.nutrient.models.NutrientLevels;
import io.itforge.nutrient.models.NutrimentLevel;
import io.itforge.nutrient.models.Nutriments;
import io.itforge.nutrient.models.Product;
import io.itforge.nutrient.models.State;
import io.itforge.nutrient.utils.Utils;
import io.itforge.nutrient.views.adapters.NutrientLevelListAdapter;
import io.itforge.nutrient.views.customtabs.CustomTabActivityHelper;
import io.itforge.nutrient.views.customtabs.CustomTabsHelper;
import io.itforge.nutrient.views.customtabs.WebViewFallback;

import static io.itforge.nutrient.utils.Utils.bold;
import static io.itforge.nutrient.utils.Utils.getRoundNumber;

public class NutritionProductFragment extends BaseFragment implements CustomTabActivityHelper.ConnectionCallback {

    @BindView(R.id.imageGrade)
    ImageView img;
    @BindView(R.id.listNutrientLevels)
    RecyclerView rv;
    @BindView(R.id.textServingSize)
    TextView serving;
    @BindView(R.id.textCarbonFootprint)
    TextView carbonFootprint;
    @BindView(R.id.textNutrientTxt)
    TextView textNutrientTxt;
    private CustomTabActivityHelper customTabActivityHelper;
    private Uri nutritionScoreUri;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return createView(inflater, container, R.layout.fragment_nutrition_product);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Intent intent = getActivity().getIntent();
        refreshView((State) intent.getExtras().getSerializable("state"));
    }

    @Override
    public void refreshView(State state) {
        super.refreshView(state);
        final Product product = state.getProduct();
        List<NutrientLevelItem> levelItem = new ArrayList<>();

        Nutriments nutriments = product.getNutriments();

        NutrientLevels nutrientLevels = product.getNutrientLevels();
        NutrimentLevel fat = null;
        NutrimentLevel saturatedFat = null;
        NutrimentLevel sugars = null;
        NutrimentLevel salt = null;
        if (nutrientLevels != null) {
            fat = nutrientLevels.getFat();
            saturatedFat = nutrientLevels.getSaturatedFat();
            sugars = nutrientLevels.getSugars();
            salt = nutrientLevels.getSalt();
        }

        if (fat == null && salt == null && saturatedFat == null && sugars == null) {
            textNutrientTxt.setText(" " + getString(R.string.txtNoData));
            levelItem.add(new NutrientLevelItem("", "", "", 0));
        } else {
            // prefetch the uri
            customTabActivityHelper = new CustomTabActivityHelper();
            customTabActivityHelper.setConnectionCallback(this);
            // currently only available in french translations
            nutritionScoreUri = Uri.parse("https://fr.openfoodfacts.org/score-nutritionnel-france");
            customTabActivityHelper.mayLaunchUrl(nutritionScoreUri, null, null);

            Context context = this.getContext();
            Nutriments.Nutriment fatNutriment = nutriments.get(Nutriments.FAT);
            if (fat != null && fatNutriment != null) {
                String fatNutrimentLevel = fat.getLocalize(context);
                String modifier = nutriments.getModifier(Nutriments.FAT);
                levelItem.add(new NutrientLevelItem(getString(R.string.txtFat),
                                                    (modifier == null ? "" : modifier)
                                                            + getRoundNumber(fatNutriment.getFor100g())
                                                            + " " + fatNutriment.getUnit(),
                                                    fatNutrimentLevel,
                                                    fat.getImageLevel()));
            }

            Nutriments.Nutriment saturatedFatNutriment = nutriments.get(Nutriments.SATURATED_FAT);
            if (saturatedFat != null && saturatedFatNutriment != null) {
                String saturatedFatLocalize = saturatedFat.getLocalize(context);
                String saturatedFatValue = getRoundNumber(saturatedFatNutriment.getFor100g()) + " " + saturatedFatNutriment.getUnit();
                String modifier = nutriments.getModifier(Nutriments.SATURATED_FAT);
                levelItem.add(new NutrientLevelItem(getString(R.string.txtSaturatedFat),
                                                    (modifier == null ? "" : modifier) + saturatedFatValue,
                                                    saturatedFatLocalize,
                                                    saturatedFat.getImageLevel()));
            }

            Nutriments.Nutriment sugarsNutriment = nutriments.get(Nutriments.SUGARS);
            if (sugars != null && sugarsNutriment  != null) {
                String sugarsLocalize = sugars.getLocalize(context);
                String sugarsValue = getRoundNumber(sugarsNutriment.getFor100g()) + " " + sugarsNutriment.getUnit();
                String modifier = nutriments.getModifier(Nutriments.SUGARS);
                levelItem.add(new NutrientLevelItem(getString(R.string.txtSugars),
                                                    (modifier == null ? "" : modifier) + sugarsValue,
                                                    sugarsLocalize,
                                                    sugars.getImageLevel()));
            }

            Nutriments.Nutriment saltNutriment = nutriments.get(Nutriments.SALT);
            if (salt != null && saltNutriment != null) {
                String saltLocalize = salt.getLocalize(context);
                String saltValue = getRoundNumber(saltNutriment.getFor100g()) + " " + saltNutriment.getUnit();
                String modifier = nutriments.getModifier(Nutriments.SALT);
                levelItem.add(new NutrientLevelItem(getString(R.string.txtSalt),
                                                    (modifier == null ? "" : modifier) + saltValue,
                                                    saltLocalize,
                                                    salt.getImageLevel()));
            }

            img.setImageDrawable(ContextCompat.getDrawable(context, Utils.getImageGrade(product.getNutritionGradeFr())));
            img.setOnClickListener(view1 -> {
                CustomTabsIntent customTabsIntent = CustomTabsHelper.getCustomTabsIntent(getContext(), customTabActivityHelper.getSession());

                CustomTabActivityHelper.openCustomTab(NutritionProductFragment.this.getActivity(), customTabsIntent, nutritionScoreUri, new WebViewFallback());
            });
        }

        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(new NutrientLevelListAdapter(getContext(), levelItem));

        if (TextUtils.isEmpty(product.getServingSize())) {
            serving.setVisibility(View.GONE);
        } else {
            serving.setText(bold(getString(R.string.txtServingSize)));
            serving.append(" ");
            serving.append(product.getServingSize());
        }
        if (nutriments != null) {
            if (!nutriments.contains(Nutriments.CARBON_FOOTPRINT)) {
                carbonFootprint.setVisibility(View.GONE);
            } else {
                Nutriments.Nutriment carbonFootprintNutriment = nutriments.get(Nutriments.CARBON_FOOTPRINT);
                carbonFootprint.append(bold(getString(R.string.textCarbonFootprint)));
                carbonFootprint.append(carbonFootprintNutriment.getFor100g());
                carbonFootprint.append(carbonFootprintNutriment.getUnit());
            }
        }
    }

    @Override
    public void onCustomTabsConnected() {
        img.setClickable(true);
    }

    @Override
    public void onCustomTabsDisconnected() {
        img.setClickable(false);
    }
}
