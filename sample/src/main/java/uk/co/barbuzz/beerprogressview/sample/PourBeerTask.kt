package uk.co.barbuzz.beerprogressview.sample

import android.os.AsyncTask

import uk.co.barbuzz.beerprogressview.BeerProgressView

/**
 * Old school Async to update progress view gradually
 */
class PourBeerTask(private val mBeerProgressView: BeerProgressView) : AsyncTask<Void, Int, Void>() {

    override fun onProgressUpdate(vararg values: Int?) {
        super.onProgressUpdate(*values)
        mBeerProgressView.beerProgress = values[0]!!
    }

    override fun doInBackground(vararg params: Void): Void? {
        for (i in 0..90) {
            publishProgress(i)
            try {
                if (i > 10) Thread.sleep(SLEEP_TIME.toLong())

            } catch (e: InterruptedException) {

            }

            if (isCancelled) {
                break
            }
        }
        return null
    }

    companion object {

        private val SLEEP_TIME = 70
    }
}
