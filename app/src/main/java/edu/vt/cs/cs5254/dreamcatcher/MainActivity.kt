package edu.vt.cs.cs5254.dreamcatcher

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import java.util.*

private const val TAG ="MainActivity"

//implement the callback object from listFragment
class MainActivity : AppCompatActivity(), DreamListFragment.Callbacks {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val currentFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (currentFragment == null) {
            val fragment = DreamListFragment.newInstance()
            supportFragmentManager
                .beginTransaction()
                .add(R.id.fragment_container, fragment)
                .commit()
        }
    }

    override fun onDreamSelected(dreamId: UUID) {
        Log.d(TAG, "MainActivity.onDreamSelected: $dreamId")
        val fragment = DreamDetailFragment.newInstance(dreamId)
        //(dreamId) put the parameter in newInstance
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
        // .addToBackStack => avoid abort the app, instead, return last fragment
    }
}