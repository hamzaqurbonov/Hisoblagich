package com.example.hisob;

public class ItemModel {

    private String  name;
    private int  amount;
    private String  amountPlus;
    public ItemModel(String name, int amount) {
        this.name = name;
        this.amount = amount;
    }

    public String getName() {
        return name;
    }

    public int getAmount() {
        return amount;
    }
}