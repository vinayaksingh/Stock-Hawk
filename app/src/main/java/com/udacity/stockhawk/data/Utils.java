package com.udacity.stockhawk.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

import com.udacity.stockhawk.R;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class Utils {

    public static final String ACTION_BAD_INPUT = "com.udacity.stockhawk.ACTION_BAD_INPUT";

    public static final String KEY_LAST_UPDATED = "last_updated";
    public static final String KEY_INVALID_STOCK = "invalid_stock";

    public static final SimpleDateFormat FORMAT_HISTORY_ONE_WEEK = new SimpleDateFormat("EEE, dd MMM", Locale.ENGLISH);
    public static final SimpleDateFormat FORMAT_HISTORY_ONE_MONTH = new SimpleDateFormat("dd MMM yy", Locale.ENGLISH);
    public static final SimpleDateFormat FORMAT_HISTORY_THREE_MONTHS = new SimpleDateFormat("dd MMM yy", Locale.ENGLISH);
    public static final SimpleDateFormat FORMAT_HISTORY_SIX_MONTHS = new SimpleDateFormat("dd MMM yy", Locale.ENGLISH);
    public static final SimpleDateFormat FORMAT_HISTORY_ONE_YEAR = new SimpleDateFormat("MMM, yy", Locale.ENGLISH);

    private static final SimpleDateFormat FORMAT_TOAST_DATE = new SimpleDateFormat("hh:mm a, dd-MMM-yyyy", Locale.ENGLISH);

    public static final SimpleDateFormat FORMAT_CONTENT_DESC = new SimpleDateFormat("dd MMMM yyyy", Locale.ENGLISH);

    private Utils() {

    }

    public static Set<String> getStocks(Context context) {
        String stocksKey = context.getString(R.string.pref_stocks_key);
        String initializedKey = context.getString(R.string.pref_stocks_initialized_key);
        String[] defaultStocksList = context.getResources().getStringArray(R.array.default_stocks);

        HashSet<String> defaultStocks = new HashSet<>(Arrays.asList(defaultStocksList));
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);


        boolean initialized = prefs.getBoolean(initializedKey, false);

        if (!initialized) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(initializedKey, true);
            editor.putStringSet(stocksKey, defaultStocks);
            editor.apply();
            return defaultStocks;
        }
        return prefs.getStringSet(stocksKey, new HashSet<String>());

    }

    public static void setDateLastUpdated (Context context, long date){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(KEY_LAST_UPDATED, date);
        editor.apply();
    }

    public static @Nullable String getDateLastUpdated (Context context){

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        long longDate = prefs.getLong(KEY_LAST_UPDATED, 0);
        if(longDate != 0){
            return FORMAT_TOAST_DATE.format(new Date(longDate));
        }
        return null;
    }

    private static void editStockPref(Context context, String symbol, Boolean add) {
        String key = context.getString(R.string.pref_stocks_key);
        Set<String> stocks = getStocks(context);

        if (add) {
            stocks.add(symbol);
        } else {
            stocks.remove(symbol);
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet(key, stocks);
        editor.apply();
    }

    public static void addStock(Context context, String symbol) {
        editStockPref(context, symbol, true);
    }

    public static void removeStock(Context context, String symbol) {
        editStockPref(context, symbol, false);
    }

    public static String getDisplayMode(Context context) {
        String key = context.getString(R.string.pref_display_mode_key);
        String defaultValue = context.getString(R.string.pref_display_mode_default);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(key, defaultValue);
    }

    public static void toggleDisplayMode(Context context) {
        String key = context.getString(R.string.pref_display_mode_key);
        String absoluteKey = context.getString(R.string.pref_display_mode_absolute_key);
        String percentageKey = context.getString(R.string.pref_display_mode_percentage_key);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        String displayMode = getDisplayMode(context);

        SharedPreferences.Editor editor = prefs.edit();

        if (displayMode.equals(absoluteKey)) {
            editor.putString(key, percentageKey);
        } else {
            editor.putString(key, absoluteKey);
        }

        editor.apply();
    }

}
