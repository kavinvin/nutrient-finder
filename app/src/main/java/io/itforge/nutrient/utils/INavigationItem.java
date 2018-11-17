package io.itforge.nutrient.utils;

import io.itforge.nutrient.utils.NavigationDrawerListener.NavigationDrawerType;

public interface INavigationItem {

    NavigationDrawerListener getNavigationDrawerListener();

    @NavigationDrawerType
    int getNavigationDrawerType();

}
