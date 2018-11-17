package io.itforge.nutrient.jobs;

import android.widget.Toast;

import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;

import io.itforge.nutrient.network.OpenFoodAPIClient;

public class SavedProductUploadJob extends JobService {
    OpenFoodAPIClient apiClient;

    @Override
    public boolean onStartJob(JobParameters job) {
        apiClient = new OpenFoodAPIClient(this);
        apiClient.uploadOfflineImages(this, false, job, this);
        return true;
    }


    @Override
    public boolean onStopJob(JobParameters job) {
        apiClient.uploadOfflineImages(this, true, job, this);
        return true;
    }

}
