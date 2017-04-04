package com.udacity.stockhawk.sync;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import timber.log.Timber;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.HistoricalQuote;
import yahoofinance.histquotes.Interval;
import yahoofinance.quotes.stock.StockQuote;

public final class QuoteSyncJob {

    private static final int ONE_OFF_ID = 2;
    public static final String ACTION_DATA_UPDATED = "com.udacity.stockhawk.ACTION_DATA_UPDATED";
    private static final int PERIOD = 300000;
    private static final int INITIAL_BACKOFF = 10000;
    private static final int PERIODIC_ID = 1;

    private static final int ONE_YEAR_OF_HISTORY = 1;
    private static final int SIX_MONTHS_OF_HISTORY = 6;
    private static final int THREE_MONTHS_OF_HISTORY = 3;
    private static final int ONE_MONTH_OF_HISTORY = 1;
    private static final int ONE_WEEK_OF_HISTORY = 1;

    private static final int TOTAL_NUMBER_OF_HISTORY_VIEWS = 5;

    private QuoteSyncJob() {
    }

    static void getQuotes(Context context) {

        Timber.d("Running sync job");


        try {

            Set<String> stockPref = Utils.getStocks(context);
            Set<String> stockCopy = new HashSet<>();
            stockCopy.addAll(stockPref);
            String[] stockArray = stockPref.toArray(new String[stockPref.size()]);

            Timber.d(stockCopy.toString());

            if (stockArray.length == 0) {
                return;
            }

            // YahooFinance API to get stocks from web
            Map<String, Stock> quotes = YahooFinance.get(stockArray);
            Iterator<String> iterator = stockCopy.iterator();

            Timber.d(quotes.toString());

            ArrayList<ContentValues> quoteCVs = new ArrayList<>();

            while (iterator.hasNext()) {
                String symbol = iterator.next();

                Stock stock = quotes.get(symbol);
                if (null != stock) {
                    StockQuote quote = stock.getQuote();
                    if (null != quote && null != quote.getPrice()) {
                        float price = quote.getPrice().floatValue();
                        float change = quote.getChange().floatValue();
                        float percentChange = quote.getChangeInPercent().floatValue();
                        String stockName = stock.getName();

                        List<StringBuilder> historyStringBuilders = getHistoryStringBuilders(stock);


                        ContentValues quoteCV = new ContentValues();
                        quoteCV.put(Contract.Quote.COLUMN_SYMBOL, symbol);
                        quoteCV.put(Contract.Quote.COLUMN_PRICE, price);
                        quoteCV.put(Contract.Quote.COLUMN_PERCENTAGE_CHANGE, percentChange);
                        quoteCV.put(Contract.Quote.COLUMN_ABSOLUTE_CHANGE, change);

                        quoteCV.put(Contract.Quote.COLUMN_HISTORY_ONE_YEAR,
                                historyStringBuilders.get(Contract.Quote.POSITION_HISTORY_ONE_YEAR - TOTAL_NUMBER_OF_HISTORY_VIEWS).toString());
                        quoteCV.put(Contract.Quote.COLUMN_HISTORY_SIX_MONTHS,
                                historyStringBuilders.get(Contract.Quote.POSITION_HISTORY_SIX_MONTHS - TOTAL_NUMBER_OF_HISTORY_VIEWS).toString());
                        quoteCV.put(Contract.Quote.COLUMN_HISTORY_THREE_MONTHS,
                                historyStringBuilders.get(Contract.Quote.POSITION_HISTORY_THREE_MONTHS - TOTAL_NUMBER_OF_HISTORY_VIEWS).toString());
                        quoteCV.put(Contract.Quote.COLUMN_HISTORY_ONE_MONTH,
                                historyStringBuilders.get(Contract.Quote.POSITION_HISTORY_ONE_MONTH - TOTAL_NUMBER_OF_HISTORY_VIEWS).toString());
                        quoteCV.put(Contract.Quote.COLUMN_HISTORY_ONE_WEEK,
                                historyStringBuilders.get(Contract.Quote.POSITION_HISTORY_ONE_WEEK - TOTAL_NUMBER_OF_HISTORY_VIEWS).toString());

                        quoteCV.put(Contract.Quote.COLUMN_NAME, stockName);

                        quoteCVs.add(quoteCV);
                    } else {
                        invalidStock(context, symbol);
                        return;
                    }
                } else {
                    invalidStock(context, symbol);
                    return;
                }

            }

            context.getContentResolver()
                    .bulkInsert(
                            Contract.Quote.URI,
                            quoteCVs.toArray(new ContentValues[quoteCVs.size()]));

            Intent dataUpdatedIntent = new Intent(ACTION_DATA_UPDATED);
            context.sendBroadcast(dataUpdatedIntent);

            // set date in shared preferences for last update in database.
            Utils.setDateLastUpdated(context, Calendar.getInstance().getTimeInMillis());


        } catch (IOException exception) {
            Timber.e(exception, "Error fetching stock quotes");
        }
    }

    private static void invalidStock(Context context, String symbol) {
        // delete the invalid symbol from shared preferences.
        Utils.removeStock(context, symbol);
        // send broadcast to notify UI.
        Intent badInputIntent = new Intent(Utils.ACTION_BAD_INPUT);
        badInputIntent.putExtra(Utils.KEY_INVALID_STOCK, symbol);
        context.sendBroadcast(badInputIntent);
    }

    private static List<StringBuilder> getHistoryStringBuilders(Stock stock) {
        final Calendar to = Calendar.getInstance();
        // date for 1 year of historical data
        final Calendar fromOneYear = Calendar.getInstance();
        fromOneYear.add(Calendar.YEAR, -ONE_YEAR_OF_HISTORY);

        // date for 6 months of historical data
        final Calendar fromSixMonths = Calendar.getInstance();
        fromSixMonths.add(Calendar.MONTH, -SIX_MONTHS_OF_HISTORY);

        // date for 3 months of historical data
        final Calendar fromThreeMonths = Calendar.getInstance();
        fromThreeMonths.add(Calendar.MONTH, -THREE_MONTHS_OF_HISTORY);

        // date for 1 month of historical data
        final Calendar fromOneMonth = Calendar.getInstance();
        fromOneMonth.add(Calendar.MONTH, -ONE_MONTH_OF_HISTORY);

        // date for 1 week of historical data
        final Calendar fromOneWeek = Calendar.getInstance();
        fromOneWeek.add(Calendar.WEEK_OF_MONTH, -ONE_WEEK_OF_HISTORY);

        List<StringBuilder> stringBuilders = new ArrayList<StringBuilder>(TOTAL_NUMBER_OF_HISTORY_VIEWS);

        stringBuilders.add(getCalendarHistory(stock, fromOneYear, to, Interval.WEEKLY));
        stringBuilders.add(getCalendarHistory(stock, fromSixMonths, to, Interval.WEEKLY));
        stringBuilders.add(getCalendarHistory(stock, fromThreeMonths, to, Interval.WEEKLY));
        stringBuilders.add(getCalendarHistory(stock, fromOneMonth, to, Interval.DAILY));
        stringBuilders.add(getCalendarHistory(stock, fromOneWeek, to, Interval.DAILY));

        return stringBuilders;

    }

    private static StringBuilder getCalendarHistory(Stock stock, final Calendar from, final Calendar to, Interval interval) {

        // WARNING! Don't request historical data for a stock that doesn't exist!
        // The request will hang forever X_x
        List<HistoricalQuote> history = null;
        try {
            history = stock.getHistory(from, to, interval);
        } catch (IOException e) {
            Timber.e(e, "Error while while extracting historical data of stock ", stock.getSymbol());
            e.printStackTrace();
        }

        StringBuilder historyBuilder = new StringBuilder();

        if (null != history) {
            for (HistoricalQuote it : history) {
                historyBuilder.append(it.getDate().getTimeInMillis());
                historyBuilder.append(", ");
                historyBuilder.append(it.getClose());
                historyBuilder.append("\n");
            }
        }
        return historyBuilder;
    }

    private static void schedulePeriodic(Context context) {
        Timber.d("Scheduling a periodic task");


        JobInfo.Builder builder = new JobInfo.Builder(PERIODIC_ID, new ComponentName(context, QuoteJobService.class));


        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(PERIOD)
                .setBackoffCriteria(INITIAL_BACKOFF, JobInfo.BACKOFF_POLICY_EXPONENTIAL);


        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        scheduler.schedule(builder.build());
    }


    public static synchronized void initialize(final Context context) {

        schedulePeriodic(context);
        syncImmediately(context);

    }

    public static synchronized void syncImmediately(Context context) {

        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnectedOrConnecting()) {
            Intent nowIntent = new Intent(context, QuoteIntentService.class);
            context.startService(nowIntent);
        } else {

            JobInfo.Builder builder = new JobInfo.Builder(ONE_OFF_ID, new ComponentName(context, QuoteJobService.class));


            builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setBackoffCriteria(INITIAL_BACKOFF, JobInfo.BACKOFF_POLICY_EXPONENTIAL);


            JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

            scheduler.schedule(builder.build());


        }
    }


}
