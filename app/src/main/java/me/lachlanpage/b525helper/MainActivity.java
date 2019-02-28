package me.lachlanpage.b525helper;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.Toast;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private Modem mB525;
    private ModemDataAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mB525 = new Modem(this, MainActivity.this);

        ListView lv = (ListView) findViewById(R.id.dataListView);
        List<ModemData> modemData = mB525.getModemData();
        mAdapter = new ModemDataAdapter(this, modemData);
        lv.setAdapter(mAdapter);

        Timer modemTimer = new Timer();
        modemTimer.scheduleAtFixedRate(new ModemController(mB525), 0,2000);
    }

    private void updateAdapter(final String errorText) {

        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                // Update the listview with new data
                mAdapter.clear();
                mAdapter.addAll(mB525.getModemData());
                mAdapter.notifyDataSetChanged();

                if(errorText != null)
                    Toast.makeText(MainActivity.this, errorText , Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void SetTitle(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setTitle(text);
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId())
        {
            case R.id.action_settings:
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private class ModemController extends TimerTask {
        private Modem mB525;
        public ModemController(Modem _b525) {
            mB525 = _b525;
        }

        public void run() {
            Thread modemThread = new Thread(mB525);
            modemThread.start();
            String errorCode = mB525.getErrorCode();
            updateAdapter(errorCode);
        }
    }
}
