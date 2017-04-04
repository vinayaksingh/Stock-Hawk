package com.udacity.stockhawk.ui;

/**
 * Created by vin on 02/04/17.
 */

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;

/**
 * Created by aitorvs on 20/08/15.
 */
public class TabPageAdapter extends FragmentPagerAdapter{
    final int PAGE_COUNT = 5;
    private String tabTitles[];
    private Context mContext;
    private Uri symbolUri;
    final private String [] HISTORY_PROJECTION = {
            Contract.Quote.COLUMN_HISTORY_ONE_WEEK,
            Contract.Quote.COLUMN_HISTORY_ONE_MONTH,
            Contract.Quote.COLUMN_HISTORY_THREE_MONTHS,
            Contract.Quote.COLUMN_HISTORY_SIX_MONTHS,
            Contract.Quote.COLUMN_HISTORY_ONE_YEAR,
    };

    public TabPageAdapter(FragmentManager fm, Context context, Uri symbolUri) {
        super(fm);
        mContext = context;
        tabTitles = mContext.getResources().getStringArray(R.array.tab_titles);
        this.symbolUri = symbolUri;
    }


    @Override
    public int getCount() {
        return PAGE_COUNT;
    }

    @Override
    public Fragment getItem(int position) {

        Cursor cursor = mContext.getContentResolver().query(symbolUri, HISTORY_PROJECTION, null, null, null, null);
        if(null != cursor && cursor.getCount() > 0) {
            cursor.moveToFirst();
        }
        return TabFragment.newInstance(position, cursor.getString(position));
    }

    @Override
    public CharSequence getPageTitle(int position) {
        // Generate title based on item position
        return tabTitles[position];
    }


}
