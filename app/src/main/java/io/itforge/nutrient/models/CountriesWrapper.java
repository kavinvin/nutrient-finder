package io.itforge.nutrient.models;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.ArrayList;
import java.util.List;

import io.itforge.nutrient.network.deserializers.CountriesWrapperDeserializer;



@JsonDeserialize(using = CountriesWrapperDeserializer.class)
public class CountriesWrapper {

    private List<CountryResponse> countries;

    public List<Country> map() {
        List<Country> entityCountries = new ArrayList<>();
        for (CountryResponse country : countries) {
            entityCountries.add(country.map());
        }

        return entityCountries;
    }

    public List<CountryResponse> getCountries() {
        return countries;
    }

    public void setCountries(List<CountryResponse> countries) {
        this.countries = countries;
    }
}
