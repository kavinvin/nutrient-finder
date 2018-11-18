package io.itforge.nutrient.fragments;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;

import java.io.IOException;

import butterknife.BindView;
import butterknife.OnClick;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;
import io.itforge.nutrient.R;
import io.itforge.nutrient.models.Search;
import io.itforge.nutrient.network.OpenFoodAPIClient;
import io.itforge.nutrient.network.OpenFoodAPIService;
import io.itforge.nutrient.utils.NavigationDrawerListener.NavigationDrawerType;
import io.itforge.nutrient.utils.Utils;
import io.itforge.nutrient.views.ContinuousScanActivity;
import io.itforge.nutrient.views.MainActivity;
import io.itforge.nutrient.views.OFFApplication;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static io.itforge.nutrient.utils.NavigationDrawerListener.ITEM_HOME;

public class HomeFragment extends NavigationBaseFragment {

    @BindView(R.id.buttonScan)
    FloatingActionButton mButtonScan;

    private OpenFoodAPIService apiClient;
    private SharedPreferences sp;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return createView(inflater, container, R.layout.fragment_home);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        apiClient = new OpenFoodAPIClient(getActivity()).getAPIService();
        checkUserCredentials();
        sp = PreferenceManager.getDefaultSharedPreferences(getContext());
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @OnClick(R.id.buttonScan)
    protected void OnScan() {
        if (Utils.isHardwareCameraInstalled(getContext())) {
            if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.CAMERA)) {
                    new MaterialDialog.Builder(getActivity())
                            .title(R.string.action_about)
                            .content(R.string.permission_camera)
                            .neutralText(R.string.txtOk)
                            .onNeutral((dialog, which) -> ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.CAMERA}, Utils.MY_PERMISSIONS_REQUEST_CAMERA))
                            .show();
                } else {
                    ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.CAMERA}, Utils.MY_PERMISSIONS_REQUEST_CAMERA);
                }
            } else {
                Intent intent = new Intent(getActivity(), ContinuousScanActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        } else {
            if (getContext() instanceof MainActivity) {
                ((MainActivity) getContext()).moveToBarcodeEntry();
            }
        }
    }

    @Override
    @NavigationDrawerType
    public int getNavigationDrawerType() {
        return ITEM_HOME;
    }

    private void checkUserCredentials() {
        final SharedPreferences settings = OFFApplication.getInstance().getSharedPreferences("login", Context.MODE_PRIVATE);
        String login = settings.getString("user", "");
        String password = settings.getString("pass", "");

        if (!login.isEmpty() && !password.isEmpty()) {
            apiClient.signIn(login, password, "Sign-in").enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                    String htmlNoParsed = null;
                    try {
                        htmlNoParsed = response.body().string();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (htmlNoParsed != null && (htmlNoParsed.contains("Incorrect user name or password.")
                            || htmlNoParsed.contains("See you soon!"))) {
                        settings.edit()
                                .putString("user", "")
                                .putString("pass", "")
                                .apply();

                        new MaterialDialog.Builder(getActivity())
                                .title(R.string.alert_dialog_warning_title)
                                .content(R.string.alert_dialog_warning_msg_user)
                                .positiveText(R.string.txtOk)
                                .show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                    Log.e(HomeFragment.class.getName(), "Unable to Sign-in");
                }
            });
        }
    }


    public void onResume() {

        super.onResume();

        String txtHomeOnline = OFFApplication.getInstance().getResources().getString(R.string.txtHomeOnline);
        int productCount = sp.getInt("productCount", 0);
        apiClient.getTotalProductCount()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<Search>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        if (productCount != 0) {
//                            textHome.setText(String.format(txtHomeOnline, productCount));
                        } else {
//                            textHome.setText(R.string.txtHome);
                        }
                    }

                    @Override
                    public void onSuccess(Search search) {
                        SharedPreferences.Editor editor;
                        int totalProductCount = Integer.parseInt(search.getCount());
//                        textHome.setText(String.format(txtHomeOnline, totalProductCount));
                        editor = sp.edit();
                        editor.putInt("productCount", totalProductCount);
                        editor.apply();
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (productCount != 0) {
//                            textHome.setText(String.format(txtHomeOnline, productCount));
                        } else {
//                            textHome.setText(R.string.txtHome);
                        }
                    }
                });

        if (getActivity() instanceof AppCompatActivity) {
            ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
            if (actionBar != null) {
                actionBar.setTitle(R.string.home_drawer);
            }
        }

    }
}