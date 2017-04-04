package com.udacity.stockhawk.widget;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.udacity.stockhawk.data.Contract;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Utils;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Created by vin on 03/04/17.


 StockWidgetRemoteViewService controlling the data for remote adapter.
 */
public class StockWidgetRemoteViewService extends RemoteViewsService {
    private DecimalFormat dollarFormatWithPlus;
    private DecimalFormat dollarFormat;
    private DecimalFormat percentageFormat;

    public StockWidgetRemoteViewService(){
        dollarFormat = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
        dollarFormatWithPlus = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
        dollarFormatWithPlus.setPositivePrefix("+$");
        percentageFormat = (DecimalFormat) NumberFormat.getPercentInstance(Locale.getDefault());
        percentageFormat.setMaximumFractionDigits(2);
        percentageFormat.setMinimumFractionDigits(2);
        percentageFormat.setPositivePrefix("+");
    }


    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {


        return new RemoteViewsFactory(){

            private Cursor stockData = null;

            @Override
            public void onCreate() {
                // doing nothing here.
            }

            @Override
            public void onDataSetChanged() {
                if (stockData != null) {
                    stockData.close();
                }

                // Our content provider is not visible to Launcher app. Thus have to restore token.
                final long identityToken = Binder.clearCallingIdentity();

                stockData = getContentResolver().query(
                        Contract.Quote.URI,
                        Contract.Quote.QUOTE_COLUMNS.toArray(new String[]{}),
                        null,
                        null,
                        Contract.Quote.COLUMN_SYMBOL);

                Binder.restoreCallingIdentity(identityToken);

            }

            @Override
            public void onDestroy() {
                if (stockData != null) {
                    stockData.close();
                    stockData = null;
                }
            }

            @Override
            public int getCount() {
                return stockData == null ? 0 : stockData.getCount();
            }

            @Override
            public RemoteViews getViewAt(int position) {
                if (position == AdapterView.INVALID_POSITION ||
                        stockData == null || !stockData.moveToPosition(position)) {
                    return null;
                }

                // the single item/row we would populating/inflating at a time as part of adapter.
                RemoteViews views = new RemoteViews(getPackageName(),
                        R.layout.list_item_quote);
                // start setting up row items.
                String price = dollarFormat.format(stockData.getFloat(Contract.Quote.POSITION_PRICE));
                String contentDescriptionChange;

                views.setTextViewText(R.id.symbol, stockData.getString(Contract.Quote.POSITION_SYMBOL));
                views.setContentDescription(R.id.symbol, stockData.getString(Contract.Quote.POSITION_NAME));

                views.setTextViewText(R.id.price, price);
                views.setContentDescription(R.id.price, price);


                float rawAbsoluteChange = stockData.getFloat(Contract.Quote.POSITION_ABSOLUTE_CHANGE);
                float percentageChange = stockData.getFloat(Contract.Quote.POSITION_PERCENTAGE_CHANGE);

                if (rawAbsoluteChange > 0) {
                    views.setInt(R.id.change, "setBackgroundColor",
                            getResources().getColor(R.color.material_green_700));
                    //holder.change.setBackgroundResource(R.drawable.percent_change_pill_green);
                    contentDescriptionChange = getString(R.string.description_price_up_by);
                } else {
                    views.setInt(R.id.change, "setBackgroundColor",
                            getResources().getColor(R.color.material_red_700));
                    contentDescriptionChange = getString(R.string.description_price_down_by);
                }

                String change = dollarFormatWithPlus.format(rawAbsoluteChange);
                String percentage = percentageFormat.format(percentageChange / 100);

                if (Utils.getDisplayMode(getApplicationContext())
                        .equals(getString(R.string.pref_display_mode_absolute_key))) {
                    views.setTextViewText(R.id.change, change);
                    contentDescriptionChange += change;
                    views.setContentDescription(R.id.change, contentDescriptionChange);
                } else {
                    views.setTextViewText(R.id.change, percentage);
                    contentDescriptionChange += percentage;
                    views.setContentDescription(R.id.change, contentDescriptionChange);
                }

                final Intent fillInIntent = new Intent();
                Uri stockUri = Contract.Quote.makeUriForStock(stockData.getString(Contract.Quote.POSITION_SYMBOL));
                fillInIntent.setData(stockUri);
                // distinguish between items via fillintent and setting unique Uri for each item/row.
                views.setOnClickFillInIntent(R.id.list_item, fillInIntent);

                return views;
            }

            @Override
            public RemoteViews getLoadingView() {
                return new RemoteViews(getPackageName(), R.layout.list_item_quote);
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public long getItemId(int position) {
                if (stockData.moveToPosition(position))
                    return stockData.getLong(0);
                return position;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }
        };
    }
}
