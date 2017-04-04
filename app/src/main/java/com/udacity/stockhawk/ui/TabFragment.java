package com.udacity.stockhawk.ui;

import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Utils;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by vin on 02/04/17.
 */

public class TabFragment extends Fragment {

    private static final String PAGE_NUMBER = "page_number";
    private static final String HISTORICAL_DATA = "historical_data";

    private static final int ANIMATE_X_AXIS_DELAY = 1000;
    private static final int ANIMATE_Y_AXIS_DELAY = 1000;
    private static final float TEXT_SIZE_X_AXIS = 10f;
    private static final float TEXT_SIZE_Y_AXIS = 12f;


    private int mPage;
    private String mHistoricalDataString;
    private ArrayList<Entry> mGraphEntries;
    private Configuration mConfig;
    private DecimalFormat stockPriceFormat;


    @BindView(R.id.line_chart)
    LineChart mLineChart;


    public static TabFragment newInstance(int page, String historicalDataString) {
        Bundle args = new Bundle();
        args.putInt(PAGE_NUMBER, page);
        args.putString(HISTORICAL_DATA, historicalDataString);
        TabFragment fragment = new TabFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mConfig = getResources().getConfiguration();
        mPage = getArguments().getInt(PAGE_NUMBER);
        mHistoricalDataString = getArguments().getString(HISTORICAL_DATA);
        stockPriceFormat = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
        stockPriceFormat.setMaximumFractionDigits(2);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_detail, container, false);
        ButterKnife.bind(this, view);
        //Timber.i("Page # : " + mPage + " history : " + mHistoricalDataString);
        drawLineChart();
        return view;
    }

    private void drawLineChart() {


        // set x-axis properties.
        XAxis xAxis = mLineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextSize(TEXT_SIZE_X_AXIS);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawAxisLine(true);
        xAxis.setDrawGridLines(false);
        xAxis.setValueFormatter(mIAxisValueFormatter);
        xAxis.setLabelCount(4);
        xAxis.setAvoidFirstLastClipping(true);

        // change the graph properties as per the RTL device configuration.
        // set y-axis properties.
        YAxis yAxis;
        if (mConfig.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
            mLineChart.getAxisRight().setDrawLabels(false);
            yAxis = mLineChart.getAxisLeft();
        } else {
            mLineChart.getAxisLeft().setDrawLabels(false);
            yAxis = mLineChart.getAxisRight();
        }
        yAxis.setDrawLabels(true);
        yAxis.setTextColor(Color.WHITE);
        yAxis.setTextSize(TEXT_SIZE_Y_AXIS);

        // set other graph properties.
        Description description = mLineChart.getDescription();
        description.setEnabled(true);
        description.setText(getContext().getString(R.string.label_USD));
        description.setTextSize(TEXT_SIZE_Y_AXIS);
        description.setTextColor(Color.WHITE);

        mLineChart.getLegend().setEnabled(false);
        mLineChart.setTouchEnabled(false);
        mLineChart.setDragEnabled(false);
        mLineChart.setScaleEnabled(false);
        // animate
        mLineChart.animateX(ANIMATE_X_AXIS_DELAY);
        mLineChart.animateY(ANIMATE_Y_AXIS_DELAY);

        //Dataset of entries for the graph.
        mGraphEntries = getGraphEntries();

        if (null != mGraphEntries && mGraphEntries.size() > 0) {

            //create LineDataSet object to be fed into graph.
            LineDataSet lineDataSet = new LineDataSet(mGraphEntries, "Live Stocks");
            lineDataSet.setDrawFilled(true);
            lineDataSet.setFillColor(getContext().getResources().getColor(R.color.material_blue_500));
            lineDataSet.setDrawValues(false);

            LineData lineData = new LineData(lineDataSet);

            // set data in the graph.
            mLineChart.setData(lineData);

            // set content description for graph with first and last date stock prices.
            Entry firstEntry = mGraphEntries.get(0);
            Entry lastEntry = mGraphEntries.get(mGraphEntries.size() - 1);
            String firstDate = Utils.FORMAT_CONTENT_DESC.format(new Date((Long) firstEntry.getData()));
            String lastDate = Utils.FORMAT_CONTENT_DESC.format(new Date((Long) lastEntry.getData()));
            String firstPrice = stockPriceFormat.format(firstEntry.getY());
            String lastPrice = stockPriceFormat.format(lastEntry.getY());

            String contentDesc = String.format(getContext().getString(R.string.description_stock_price_on), firstDate)
                    + firstPrice
                    + String.format(getContext().getString(R.string.description_stock_price_on), lastDate)
                    + lastPrice;

            mLineChart.setContentDescription(contentDesc);
            // refresh the view.
            mLineChart.invalidate();
        }

    }


    private IAxisValueFormatter mIAxisValueFormatter = new IAxisValueFormatter() {
        @Override
        public String getFormattedValue(float value, AxisBase axis) {
            final int page = mPage;
            String xLabel = "";
            Entry e = mGraphEntries.get((int) value);
            Date date = new Date((Long) e.getData());

            switch (page) {
                case 0:
                    xLabel = Utils.FORMAT_HISTORY_ONE_WEEK.format(date);
                    break;
                case 1:
                    xLabel = Utils.FORMAT_HISTORY_ONE_MONTH.format(date);
                    break;
                case 2:
                    xLabel = Utils.FORMAT_HISTORY_THREE_MONTHS.format(date);
                    break;
                case 3:
                    xLabel = Utils.FORMAT_HISTORY_SIX_MONTHS.format(date);
                    break;
                case 4:
                    xLabel = Utils.FORMAT_HISTORY_ONE_YEAR.format(date);
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported Tab.");
            }
            return xLabel;
        }
    };


    private
    @Nullable
    ArrayList<Entry> getGraphEntries() {
        ArrayList<Entry> graphEntries = new ArrayList<Entry>();
        ArrayList<Entry> graphEntriesd;
        String[] stringEntries;
        Long date;
        Float price;
        int count = 0;

        if (mHistoricalDataString.length() != 0) {
            stringEntries = mHistoricalDataString.split("\n");
            // prepare the data entries as per the device's layout direction configuration.

            if (mConfig.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
                count = 0;
                for (String stringEntry : stringEntries) {
                    date = Long.parseLong(stringEntry.split(",")[0]);
                    price = Float.parseFloat(stringEntry.split(",")[1]);
                    graphEntries.add(new Entry(count, price, date));
                    count++;
                }
            } else {
                count = stringEntries.length - 1;
                for (String stringEntry : stringEntries) {
                    date = Long.parseLong(stringEntry.split(",")[0]);
                    price = Float.parseFloat(stringEntry.split(",")[1]);
                    graphEntries.add(new Entry(count, price, date));
                    count--;
                }
                Collections.reverse(graphEntries);
            }
        } else {
            return null;
        }

        return graphEntries;
    }


}
