package com.sails.example;

public class RecyclerData {
    String buildingName;
    String buildingAddress;

    public RecyclerData(String buildingName, String buildingAddress){
        this.buildingName = buildingName;
        this.buildingAddress = buildingAddress;
    }

    public String getBuildingName(){
        return buildingName;
    }

    public String getBuildingAddress(){
        return buildingAddress;
    }
}
