package com.example.hisob;

public class ItemModel {

    private String  name, amount, amountplus;
    private int  id ;
    private int temporaryAmount = 0;
//    private String  amountPlus;
    public ItemModel(int id, String name, String amount,String amountplus) {
        this.id = id;
        this.name = name;
        this.amount = amount;
        this.amountplus = amountplus;
    }
    public int getId() {
        return id;
    }
    public String getName() {
        return name;
    }

    public String getAmount() {
        return amount;
    }

    public String getAmountplus() {
        return amountplus;
    }

    public int getTemporaryAmount() {
        return temporaryAmount;
    }

    public void setTemporaryAmount(int temporaryAmount) {
        this.temporaryAmount = temporaryAmount;
    }
}