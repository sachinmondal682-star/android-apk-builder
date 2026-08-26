package com.example.sampleapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class DashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        val fabRefer = findViewById<FloatingActionButton>(R.id.fab_refer)

        // Default screen
        replaceFragment(HomeFragment())

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    replaceFragment(HomeFragment())
                    true
                }
                R.id.nav_offers -> {
                    replaceFragment(OffersFragment())
                    true
                }
                R.id.nav_my_offers -> {
                    replaceFragment(MyOffersFragment())
                    true
                }
                R.id.nav_profile -> {
                    replaceFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }

        fabRefer.setOnClickListener {
            replaceFragment(ReferFragment())
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}

// Basic Fragment Classes
class HomeFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val tv = TextView(context)
        tv.text = "Home Screen"
        tv.textSize = 20f
        return tv
    }
}

class OffersFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val tv = TextView(context)
        tv.text = "All Offers"
        tv.textSize = 20f
        return tv
    }
}

class MyOffersFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val tv = TextView(context)
        tv.text = "My Offers Tracker"
        tv.textSize = 20f
        return tv
    }
}

class ProfileFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val tv = TextView(context)
        tv.text = "Profile & Settings"
        tv.textSize = 20f
        return tv
    }
}

class ReferFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val tv = TextView(context)
        tv.text = "Refer & Earn"
        tv.textSize = 20f
        return tv
    }
}
