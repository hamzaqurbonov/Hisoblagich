package com.example.hisob;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;

public class DbSQL extends SQLiteOpenHelper {

    private static final String DB_NAME = "DBMain";
    private static final int DB_VERSION = 1;
    private static final String TABLE_NAME = "mycourses";
    private static final String ID_COL = "id";
    private static final String NAME = "name";
    private static final String AMOUNT = "amount";
    private static final String AMOUNTPLUS = "amountplus";

    // creating a constructor for our database handler.
    public DbSQL(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String query = "CREATE TABLE " + TABLE_NAME + " (" + ID_COL + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + NAME + " TEXT,"
                + AMOUNT + " TEXT,"
                + AMOUNTPLUS + " TEXT)";
        db.execSQL(query);
    }
    public void addNewCourse(String name, String amount, String amountplus) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
//        values.put(ID_COL, id);
        values.put(NAME, name);
        values.put(AMOUNT, amount);
        values.put(AMOUNTPLUS, amountplus);
        db.insert(TABLE_NAME, null, values);
        db.close();
    }

    public void updateNull(String id, String newAmount, String newAmountPlus) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
//        values.put(NAME, newName); // Янги сўзни қўшиш
        values.put(AMOUNT, newAmount); // Янги таржимани қўшиш
        values.put(AMOUNTPLUS, newAmountPlus); // Янги таржимани қўшиш

        // Мавжуд сўзни фильтр сифатида ишлатиш
        String selection = ID_COL + " = ?";
        String[] selectionArgs = { String.valueOf(id) };

        // Маълумотни янгилаш
        db.update(TABLE_NAME, values, selection, selectionArgs);
        db.close();
    }

    public void updateCourse(String id, int addedAmount) {
        SQLiteDatabase db = this.getWritableDatabase();

        // Ҳозирги `Amount` ва `AmountPlus` қийматларини олиш
        Cursor cursor = db.rawQuery("SELECT " + AMOUNT + ", " + AMOUNTPLUS + " FROM " + TABLE_NAME + " WHERE " + ID_COL + " = ?", new String[]{id});
        if (cursor.moveToFirst()) {
            int currentAmount = cursor.getInt(cursor.getColumnIndexOrThrow(AMOUNT));
            String currentAmountPlus = cursor.getString(cursor.getColumnIndexOrThrow(AMOUNTPLUS));

            // Янги қийматларни ҳисоблаш
            int newAmount = currentAmount + addedAmount;
            String newAmountPlus;

            // Тенгликни киритиш логикаси
            if (currentAmountPlus == null || currentAmountPlus.isEmpty()) {
                newAmountPlus = String.valueOf(addedAmount); // Биринчи қиймат
            } else {
                // Ҳозирги изоҳга янги қиймат қўшиш
                int equalIndex = currentAmountPlus.lastIndexOf("=");
                if (equalIndex != -1) {
                    newAmountPlus = currentAmountPlus.substring(0, equalIndex).trim() + "+" + addedAmount; // Тенгликни олиб ташлаш
                } else {
                    newAmountPlus = currentAmountPlus + "+" + addedAmount; // Агар тенглик йўқ бўлса
                }
            }

            // Янгиланган қийматларни якунлаш
            newAmountPlus += "=" + newAmount;

            // Янгиланган қийматларни сақлаш
            ContentValues values = new ContentValues();
            values.put(AMOUNT, newAmount); // Янги умумий қиймат
            values.put(AMOUNTPLUS, newAmountPlus); // Арифметик изоҳ

            String selection = ID_COL + " = ?";
            String[] selectionArgs = {id};

            db.update(TABLE_NAME, values, selection, selectionArgs);
        }
        cursor.close();
        db.close();
    }




    public ArrayList<ItemModel> readCourses() {


        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursorCourses = db.rawQuery("SELECT * FROM " + TABLE_NAME, null);

        ArrayList<ItemModel> courseModalArrayList = new ArrayList<>();

        if (cursorCourses.moveToFirst()) {
            do {
                int id = Integer.parseInt(cursorCourses.getString(0));
                String name = cursorCourses.getString(1);
                String amount = cursorCourses.getString(2);
                String amountplus = cursorCourses.getString(3);

                courseModalArrayList.add(new ItemModel(id, name, amount, amountplus));
            } while (cursorCourses.moveToNext());
        }
        cursorCourses.close();

        courseModalArrayList.sort((item1, item2) -> {
            // `amount`ни сонга айлантириб муқоиса қиламиз
            int amount1 = Integer.parseInt(item1.getAmount());
            int amount2 = Integer.parseInt(item2.getAmount());
            return Integer.compare(amount2, amount1); // Каттадан кичикка
        });


        return courseModalArrayList;
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public  void deleteAllData(){
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_NAME);
        db.execSQL("DELETE FROM sqlite_sequence WHERE name='" + TABLE_NAME + "'");

    }


    public void deleteSelect(String id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_NAME, ID_COL + " = ?", new String[]{id});
        db.close();
    }


//    public ArrayList<ItemModel> searchCourses(String query) {
//        ArrayList<ItemModel> searchList = new ArrayList<>();
//        SQLiteDatabase db = this.getReadableDatabase();
//
//        // Мос келадиган қидириш учун аниқ мослик
//        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE " + NAME + " LIKE ?", new String[]{ query.trim() + "%"});   // query.trim() пробелни йўқ қилади. "%" ўхшашларини топади
////        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE " + WORD + " LIKE ?", new String[]{query.trim()});
//
//        if (cursor.moveToFirst()) {
//            do {
//                @SuppressLint("Range") int Id = cursor.getInt(cursor.getColumnIndex(ID_COL));
//                @SuppressLint("Range") String word = cursor.getString(cursor.getColumnIndex(NAME));
//                @SuppressLint("Range") String translate = cursor.getString(cursor.getColumnIndex(AMOUNT));
//
//                searchList.add(new ItemModel(Id, word, translate));
//            } while (cursor.moveToNext());
//        }
//        Log.d("demo45", "Қидирув сўрови: " + query);
//        Log.d("demo45", "Натижалар сони: " + searchList.size());
//        cursor.close();
//        return searchList;
//    }
}