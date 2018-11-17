package io.itforge.nutrient.views.viewmodel;

import android.support.annotation.NonNull;

import io.reactivex.disposables.CompositeDisposable;

public abstract class ViewModel {

    protected CompositeDisposable subscriptions;

    public ViewModel() {
    }

    public void bind() {
        unbind();
        subscriptions = new CompositeDisposable();
        subscribe(subscriptions);
    }

    public void unbind() {
        if (subscriptions != null) {
            subscriptions.clear();
            subscriptions = null;
        }
    }

    protected abstract void subscribe(@NonNull final CompositeDisposable subscriptions);


}
