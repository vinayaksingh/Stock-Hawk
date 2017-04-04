package com.udacity.stockhawk.data;


import android.net.Uri;
import android.provider.BaseColumns;

import com.google.common.collect.ImmutableList;

public final class Contract {

    static final String AUTHORITY = "com.udacity.stockhawk";
    static final String PATH_QUOTE = "quote";
    static final String PATH_QUOTE_WITH_SYMBOL = "quote/*";
    private static final Uri BASE_URI = Uri.parse("content://" + AUTHORITY);

    private Contract() {
    }

    @SuppressWarnings("unused")
    public static final class Quote implements BaseColumns {

        public static final Uri URI = BASE_URI.buildUpon().appendPath(PATH_QUOTE).build();
        public static final String COLUMN_SYMBOL = "symbol";
        public static final String COLUMN_PRICE = "price";
        public static final String COLUMN_ABSOLUTE_CHANGE = "absolute_change";
        public static final String COLUMN_PERCENTAGE_CHANGE = "percentage_change";
        public static final String COLUMN_HISTORY_ONE_YEAR = "history_one_year";
        public static final String COLUMN_HISTORY_SIX_MONTHS = "history_six_months";
        public static final String COLUMN_HISTORY_THREE_MONTHS = "history_three_months";
        public static final String COLUMN_HISTORY_ONE_MONTH = "history_one_month";
        public static final String COLUMN_HISTORY_ONE_WEEK = "history_one_week";
        public static final String COLUMN_NAME = "name";

        public static final int POSITION_ID = 0;
        public static final int POSITION_SYMBOL = 1;
        public static final int POSITION_PRICE = 2;
        public static final int POSITION_ABSOLUTE_CHANGE = 3;
        public static final int POSITION_PERCENTAGE_CHANGE = 4;
        public static final int POSITION_HISTORY_ONE_YEAR = 5;
        public static final int POSITION_HISTORY_SIX_MONTHS = 6;
        public static final int POSITION_HISTORY_THREE_MONTHS  = 7;
        public static final int POSITION_HISTORY_ONE_MONTH = 8;
        public static final int POSITION_HISTORY_ONE_WEEK = 9;
        public static final int POSITION_NAME = 10;

        public static final ImmutableList<String> QUOTE_COLUMNS = ImmutableList.of(
                _ID,
                COLUMN_SYMBOL,
                COLUMN_PRICE,
                COLUMN_ABSOLUTE_CHANGE,
                COLUMN_PERCENTAGE_CHANGE,
                COLUMN_HISTORY_ONE_YEAR,
                COLUMN_HISTORY_SIX_MONTHS,
                COLUMN_HISTORY_THREE_MONTHS,
                COLUMN_HISTORY_ONE_MONTH,
                COLUMN_HISTORY_ONE_WEEK,
                COLUMN_NAME
        );
        static final String TABLE_NAME = "quotes";

        public static Uri makeUriForStock(String symbol) {
            return URI.buildUpon().appendPath(symbol).build();
        }

        public static String getStockFromUri(Uri queryUri) {
            return queryUri.getLastPathSegment();
        }


    }

}