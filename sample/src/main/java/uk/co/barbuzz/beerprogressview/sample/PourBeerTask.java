package uk.co.barbuzz.beerprogressview.sample;

import android.os.AsyncTask;

import uk.co.barbuzz.beerprogressview.BeerProgressView;

/**
 * Old school Async to update progress view gradually
 */
public class PourBeerTask extends AsyncTask<Void, Integer, Void> {

    private static final int SLEEP_TIME = 70;
    private final BeerProgressView mBeerProgressView;

    public PourBeerTask(BeerProgressView beerProgressView) {
        mBeerProgressView = beerProgressView;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        mBeerProgressView.setBeerProgress(values[0]);
    }

    @Override
    protected Void doInBackground(Void... params) {
        for (int i = 0; i < 90; i++) {
            publishProgress(i);
            try {
                if (i > 10) Thread.sleep(SLEEP_TIME);

            } catch (InterruptedException e) {

            }
            if (isCancelled()) {
                break;
            }
        }
        return null;
    }
}
