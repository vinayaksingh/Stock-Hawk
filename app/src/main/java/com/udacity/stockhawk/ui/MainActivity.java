package com.udacity.stockhawk.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.Utils;
import com.udacity.stockhawk.sync.QuoteSyncJob;

import butterknife.BindView;
import butterknife.ButterKnife;
import es.dmoral.toasty.Toasty;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>,
        SwipeRefreshLayout.OnRefreshListener,
        StockAdapter.StockAdapterOnClickHandler {

    private static final int STOCK_LOADER = 0;
    private static final String SELECTOR_KEY = "selector_key";
    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.recycler_view)
    RecyclerView stockRecyclerView;
    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.swipe_refresh)
    SwipeRefreshLayout swipeRefreshLayout;
    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.error)
    TextView error;

    private StockAdapter adapter;
    private Context mContext;
    private int mPosition = RecyclerView.NO_POSITION;

    @Override
    public void onClick(String symbol, int position) {
        Timber.d("Symbol clicked: %s", symbol);
        Uri symbolSelectedUri = Contract.Quote.makeUriForStock(symbol);
        mPosition = position;

        Intent startDetailActivity = new Intent(this, DetailActivity.class);
        startDetailActivity.setData(symbolSelectedUri);
        startActivity(startDetailActivity);
    }


    @Override
    protected void onPause() {

        if (mPosition != RecyclerView.NO_POSITION) {
            Timber.d("POSITION onPause " + mPosition);
            Bundle outBundle =  new Bundle();
            outBundle.putInt(SELECTOR_KEY, mPosition);
            getIntent().putExtras(outBundle);
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        Bundle inBundle =  getIntent().getExtras();
        if (inBundle != null && inBundle.containsKey(SELECTOR_KEY)) {
            mPosition = inBundle.getInt(SELECTOR_KEY);
            Timber.d("POSITION onResume " + mPosition);
        }
        super.onResume();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        mContext = this;

        registerReceiver(WrongStockSymbolBroadcastReceiver, new IntentFilter(Utils.ACTION_BAD_INPUT));

        adapter = new StockAdapter(this, this);
        stockRecyclerView.setAdapter(adapter);
        stockRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        swipeRefreshLayout.setOnRefreshListener(this);
        setRefresh(true);
        onRefresh();

        QuoteSyncJob.initialize(this);
        getSupportLoaderManager().initLoader(STOCK_LOADER, null, this);

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                String symbol = adapter.getSymbolAtPosition(viewHolder.getAdapterPosition());
                Utils.removeStock(MainActivity.this, symbol);
                getContentResolver().delete(Contract.Quote.makeUriForStock(symbol), null, null);
                updateWidgets();
            }
        }).attachToRecyclerView(stockRecyclerView);


    }

    private void updateWidgets() {

        // Setting the package ensures that only components in our app will receive the broadcast
        Intent dataUpdatedIntent = new Intent(QuoteSyncJob.ACTION_DATA_UPDATED)
                .setPackage(getPackageName());
        sendBroadcast(dataUpdatedIntent);
    }

    private BroadcastReceiver WrongStockSymbolBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equalsIgnoreCase(Utils.ACTION_BAD_INPUT)) {
                createErrorToast(intent.getStringExtra(Utils.KEY_INVALID_STOCK));
            }
        }
    };

    private void createErrorToast(final String invalidStockSymbol) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toasty.error(mContext, String.format(getString(R.string.toast_bad_input), invalidStockSymbol)).show();
            }
        });
        setRefresh(false);
    }

    @Override
    protected void onDestroy() {
        if (WrongStockSymbolBroadcastReceiver != null) {
            unregisterReceiver(WrongStockSymbolBroadcastReceiver);
        }
        super.onDestroy();
    }

    private boolean networkUp() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnectedOrConnecting();
    }

    @Override
    public void onRefresh() {

        QuoteSyncJob.syncImmediately(this);

        if (!networkUp() && adapter.getItemCount() == 0) {
            setRefresh(false);
            error.setText(getString(R.string.error_no_network));
            error.setContentDescription(getString(R.string.error_no_network));
            error.setVisibility(View.VISIBLE);
        } else if (!networkUp()) {
            setRefresh(false);
            if (null != Utils.getDateLastUpdated(mContext)) {
                // show the time for last database update.
                Toasty.info(this,
                        String.format(getString(R.string.toast_no_connectivity_with_date), Utils.getDateLastUpdated(mContext)),
                        Toast.LENGTH_LONG).show();
            } else {
                Toasty.info(this, getString(R.string.toast_no_connectivity),
                        Toast.LENGTH_LONG).show();
            }

        } else if (Utils.getStocks(this).size() == 0) {
            setRefresh(false);
            error.setText(getString(R.string.error_no_stocks));
            error.setContentDescription(getString(R.string.error_no_stocks));
            error.setVisibility(View.VISIBLE);
        } else {
            error.setVisibility(View.GONE);
        }
    }

    private void setRefresh(final boolean start) {
        swipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                swipeRefreshLayout.setRefreshing(start);
            }
        });
    }

    public void button(@SuppressWarnings("UnusedParameters") View view) {
        new AddStockDialog().show(getFragmentManager(), "StockDialogFragment");
    }

    void addStock(String symbol) {
        if (symbol != null && !symbol.isEmpty()) {

            if (networkUp()) {
                setRefresh(true);
            } else {
                String message = getString(R.string.toast_stock_added_no_connectivity, symbol);
                Toasty.info(this, message, Toast.LENGTH_LONG).show();
            }

            Utils.addStock(this, symbol);
            QuoteSyncJob.syncImmediately(this);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this,
                Contract.Quote.URI,
                Contract.Quote.QUOTE_COLUMNS.toArray(new String[]{}),
                null, null, Contract.Quote.COLUMN_SYMBOL);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data.getCount() != 0) {
            error.setVisibility(View.GONE);
        }
        adapter.setCursor(data);
        setRefresh(false);
        Timber.d("POSITION onLoadFinished " + mPosition);
        if (mPosition != RecyclerView.NO_POSITION && mPosition <= adapter.getItemCount()) {
            stockRecyclerView.scrollToPosition(mPosition);
        }
    }


    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.setCursor(null);
        setRefresh(false);
    }


    private void setDisplayModeMenuItemIcon(MenuItem item) {
        if (Utils.getDisplayMode(this)
                .equals(getString(R.string.pref_display_mode_absolute_key))) {
            item.setIcon(R.drawable.ic_percentage);
            item.setTitle(getString(R.string.description_change_menu_to) + getString(R.string.description_in_percentage));
        } else {
            item.setIcon(R.drawable.ic_dollar);
            item.setTitle(getString(R.string.description_change_menu_to) + getString(R.string.description_in_dollars));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity_settings, menu);
        MenuItem item = menu.findItem(R.id.action_change_units);
        setDisplayModeMenuItemIcon(item);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_change_units) {
            Utils.toggleDisplayMode(this);
            setDisplayModeMenuItemIcon(item);
            adapter.notifyDataSetChanged();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
