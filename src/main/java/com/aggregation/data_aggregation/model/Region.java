package com.aggregation.data_aggregation.model;

import jakarta.persistence.Embeddable;

@Embeddable
public class Region {

    private String country = "IN";
    private String state;
    private String city;

    public Region() {}

    public Region(String state, String city) {
        this.state = state;
        this.city = city;
    }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
}
