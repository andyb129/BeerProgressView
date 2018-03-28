package uk.co.barbuzz.beerprogressview.sample

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.roughike.bottombar.BottomBar
import com.roughike.bottombar.OnMenuTabClickListener
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*


class MainActivity : AppCompatActivity() {
    private val beerDepth: Long = 10000

    private val pourBeerTaskNew = object : CountDownTimer(beerDepth, 10) {
        override fun onFinish() {

        }

        override fun onTick(millisUntilFinished: Long) {
            val progress = beerDepth - millisUntilFinished
            val percentage = (progress.toDouble() / beerDepth * 100).toInt()
            when (percentage) {
                in 0..90 -> content_main_beer_progress_view.beerProgress = percentage
                else -> onFinish()
            }
        }
    }
    private val infoDialogue: AlertDialog by lazy {
        AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setMessage(R.string.info_details)
                .setCancelable(true)
                .setPositiveButton("OK", { dialog, _ -> dialog.dismiss() })
                .setNegativeButton("More Info", { dialog, _ ->
                    dialog.dismiss()
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(resources.getString(R.string.github_link))))
                })
                .create()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        initViews(savedInstanceState)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        val itemId = item?.itemId
        when (itemId) {
            R.id.action_github -> {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(resources.getString(R.string.github_link))
                startActivity(intent)
                return true
            }
            R.id.action_info -> {
                infoDialogue()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun infoDialogue() {
        if (infoDialogue.isShowing) {
            return
        } else {
            infoDialogue.show()
        }
    }

    private fun initViews(savedInstanceState: Bundle?) {
        val activity = this
        val bottomBar = BottomBar.attach(this, savedInstanceState)

        bottomBar.setItemsFromMenu(R.menu.menu_bottom_bar, object : OnMenuTabClickListener {
            override fun onMenuTabReSelected(menuItemId: Int) {
            }

            override fun onMenuTabSelected(menuItemId: Int) {
                // The user selected item
                when (menuItemId) {
                    R.id.lager_item -> {
                        pourBeer(ContextCompat.getColor(activity, R.color.lager),
                                ContextCompat.getColor(activity, R.color.lager_bubble))
                    }
                    R.id.ale_item -> {
                        pourBeer(ContextCompat.getColor(activity, R.color.ale),
                                ContextCompat.getColor(activity, R.color.ale_bubble))
                    }
                    R.id.stout_item -> {
                        pourBeer(ContextCompat.getColor(activity, R.color.stout),
                                ContextCompat.getColor(activity, R.color.stout_bubble))
                    }
                }
            }
        })
    }

    fun pourBeer(beerColour: Int, bubbleColour: Int) {
        pourBeerTaskNew.cancel()
        content_main_beer_progress_view.beerProgress = 0
        content_main_beer_progress_view.beerColor = beerColour
        content_main_beer_progress_view.bubbleColor = bubbleColour
        pourBeerTaskNew.start()
    }
}
