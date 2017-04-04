package com.udacity.stockhawk.ui;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.Utils;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;


/**
 * Created by vin on 02/04/17.
 */

public class DetailActivity extends AppCompatActivity {

    @BindView(R.id.viewpager)
    ViewPager mViewPager;
    @BindView(R.id.tabs)
    TabLayout mTabLayout;
    @Nullable @BindView(R.id.detail_symbol)
    TextView mSymbol;
    @Nullable @BindView(R.id.detail_name)
    TextView mName;
    @Nullable @BindView(R.id.detail_price)
    TextView mPrice;
    @Nullable @BindView(R.id.detail_up_down_arrow)
    ImageView mArrow;
    @Nullable @BindView(R.id.detail_price_delta)
    TextView mDelta;

    private DecimalFormat dollarFormatWithPlus;
    private DecimalFormat dollarFormat;
    private DecimalFormat percentageFormat;

    private String[] TAB_CONTENT_DESC;


    @Override
    public Intent getParentActivityIntent() {
        return super.getParentActivityIntent().addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        ButterKnife.bind(this);

        dollarFormat = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
        dollarFormatWithPlus = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
        dollarFormatWithPlus.setPositivePrefix("$");
        percentageFormat = (DecimalFormat) NumberFormat.getPercentInstance(Locale.getDefault());
        percentageFormat.setMaximumFractionDigits(2);
        percentageFormat.setMinimumFractionDigits(2);

        TAB_CONTENT_DESC = new String[]{
                getString(R.string.description_one_week),
                getString(R.string.description_one_month),
                getString(R.string.description_three_months),
                getString(R.string.description_six_months),
                getString(R.string.description_one_year),
        };

        // get Uri for filling data in detail view.
        Uri symbolUri = getIntent().getData();

        // setup the view pager with the page adapter
        mViewPager.setAdapter(new TabPageAdapter(getSupportFragmentManager(), this, symbolUri));
        // set the view pager created to the tab layout.
        mTabLayout.setupWithViewPager(mViewPager);
        // set content description for accessibility.

        for (int i = 0; i < TAB_CONTENT_DESC.length; i++) {
            mTabLayout.getTabAt(i).setContentDescription(TAB_CONTENT_DESC[i]);
        }

        // to check whether the layout inflated is land or port.
        if (null != findViewById(R.id.detail_symbol)) {
            // Portrait mode.

            Cursor query = getContentResolver().query(symbolUri, null, null, null, null);
            if (query != null && query.getCount() > 0) {
                query.moveToFirst();

                String price = dollarFormat.format(query.getFloat(Contract.Quote.POSITION_PRICE));

                mSymbol.setText(query.getString(Contract.Quote.POSITION_SYMBOL));
                mSymbol.setContentDescription(query.getString(Contract.Quote.POSITION_SYMBOL));
                mName.setText(query.getString(Contract.Quote.POSITION_NAME));
                mName.setContentDescription(query.getString(Contract.Quote.POSITION_NAME));
                mPrice.setText(price);
                mPrice.setContentDescription(price);


                float rawAbsoluteChange = query.getFloat(Contract.Quote.POSITION_ABSOLUTE_CHANGE);
                float percentageChange = query.getFloat(Contract.Quote.POSITION_PERCENTAGE_CHANGE);

                int color;
                if (rawAbsoluteChange > 0) {
                    color = Color.GREEN;
                    mArrow.setImageDrawable(getDrawable(R.drawable.up));
                    mArrow.setContentDescription(getString(R.string.description_price_up_by));
                } else {
                    color = Color.RED;
                    mArrow.setImageDrawable(getDrawable(R.drawable.down));
                    mArrow.setContentDescription(getString(R.string.description_price_down_by));
                }

                String change = dollarFormatWithPlus.format(Math.abs(rawAbsoluteChange));
                String percentage = percentageFormat.format(Math.abs(percentageChange / 100));

                if (Utils.getDisplayMode(this)
                        .equals(getString(R.string.pref_display_mode_absolute_key))) {
                    mDelta.setText(change);
                    mDelta.setContentDescription(change);
                } else {
                    mDelta.setText(percentage);
                    mDelta.setContentDescription(percentage);
                }
                mDelta.setTextColor(color);

                query.close();
            }
        } else {
            // Landscape mode.
            ActionBar actionBar = getSupportActionBar();
            if (null != actionBar) {
                getSupportActionBar().setElevation(0f);
                getSupportActionBar().setTitle(Contract.Quote.getStockFromUri(symbolUri));
            }
        }

    }
}
