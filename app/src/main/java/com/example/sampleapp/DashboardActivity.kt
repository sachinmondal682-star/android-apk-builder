package com.example.sampleapp

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class DashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        val fabRefer = findViewById<FloatingActionButton>(R.id.fab_refer)

        replaceFragment(HomeFragment())

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { replaceFragment(HomeFragment()); true }
                R.id.nav_offers -> { replaceFragment(OffersFragment()); true }
                R.id.nav_my_offers -> { replaceFragment(MyOffersFragment()); true }
                R.id.nav_profile -> { replaceFragment(ProfileFragment()); true }
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

    fun showWithdrawDialog(token: String) {
        val input = EditText(this).apply {
            hint = "Enter UPI ID (e.g. mobile@upi)"
            setPadding(40, 30, 40, 30)
        }

        AlertDialog.Builder(this)
            .setTitle("Redeem Coins (UPI)")
            .setMessage("100 Coins = ₹10.00 Direct Payout\nMinimum: 100 Coins")
            .setView(input)
            .setPositiveButton("Redeem") { _, _ ->
                val upi = input.text.toString().trim()
                if (upi.isNotEmpty()) {
                    processWithdraw(token, 10.0, upi)
                } else {
                    Toast.makeText(this, "Enter valid UPI ID", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun processWithdraw(token: String, amount: Double, upiId: String) {
        val json = JSONObject().apply {
            put("token", token)
            put("amount", amount)
            put("upi_id", upiId)
        }
        val body = json.toString().toRequestBody("application/json".toMediaType())
        val req = Request.Builder().url("https://paisaloots.site/api/withdraw.php").post(body).build()

        OkHttpClient().newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { Toast.makeText(applicationContext, "Network error", Toast.LENGTH_SHORT).show() }
            }
            override fun onResponse(call: Call, response: Response) {
                val res = JSONObject(response.body?.string() ?: "{}")
                runOnUiThread {
                    Toast.makeText(applicationContext, res.optString("message", "Done"), Toast.LENGTH_LONG).show()
                }
            }
        })
    }
}

// 1. Home Fragment
class HomeFragment : Fragment() {
    private lateinit var sessionManager: SessionManager
    private val httpClient = OkHttpClient()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        sessionManager = SessionManager(requireContext())

        val tvWelcome = view.findViewById<TextView>(R.id.tv_welcome_name)
        val tvBalance = view.findViewById<TextView>(R.id.tv_wallet_coins)
        val tvTotalEarned = view.findViewById<TextView>(R.id.tv_total_earned_badge)
        val offersContainer = view.findViewById<LinearLayout>(R.id.offers_list_container)
        val btnWithdraw = view.findViewById<Button>(R.id.btn_withdraw_quick)

        tvWelcome.text = "Hey, ${sessionManager.getUserName()} 👋"

        btnWithdraw.setOnClickListener {
            sessionManager.getAuthToken()?.let { (activity as? DashboardActivity)?.showWithdrawDialog(it) }
        }

        fetchDashboardData(tvBalance, tvTotalEarned)
        fetchTrendingOffers(inflater, offersContainer)

        return view
    }

    private fun fetchDashboardData(tvBalance: TextView, tvTotalEarned: TextView) {
        val token = sessionManager.getAuthToken() ?: return
        val body = JSONObject().apply { put("token", token) }.toString().toRequestBody("application/json".toMediaType())
        val req = Request.Builder().url("https://paisaloots.site/api/get_dashboard.php").post(body).build()

        httpClient.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                try {
                    val res = JSONObject(response.body?.string() ?: "{}")
                    if (res.optBoolean("success")) {
                        val user = res.getJSONObject("user")
                        activity?.runOnUiThread {
                            tvBalance.text = user.optString("balance", "0.00")
                            tvTotalEarned.text = "Total Earned: ₹${user.optString("total_earned", "0.00")}"
                        }
                    }
                } catch (e: Exception) {}
            }
        })
    }

    private fun fetchTrendingOffers(inflater: LayoutInflater, container: LinearLayout) {
        val req = Request.Builder().url("https://paisaloots.site/api/get_offers.php").get().build()
        httpClient.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                try {
                    val res = JSONObject(response.body?.string() ?: "{}")
                    if (res.optBoolean("success")) {
                        val offers = res.getJSONArray("offers")
                        activity?.runOnUiThread {
                            container.removeAllViews()
                            for (i in 0 until offers.length()) {
                                val item = offers.getJSONObject(i)
                                val card = inflater.inflate(R.layout.item_offer_card, container, false)
                                card.findViewById<TextView>(R.id.tv_offer_title).text = item.optString("title")
                                card.findViewById<TextView>(R.id.tv_offer_desc).text = item.optString("tagline")
                                card.findViewById<TextView>(R.id.tv_offer_reward).text = "+ ${item.optString("payout_coins")}"
                                card.setOnClickListener {
                                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.optString("offer_url"))))
                                }
                                container.addView(card)
                            }
                        }
                    }
                } catch (e: Exception) {}
            }
        })
    }
}

// 2. Offers Fragment
class OffersFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        val offersContainer = view.findViewById<LinearLayout>(R.id.offers_list_container)

        val req = Request.Builder().url("https://paisaloots.site/api/get_offers.php").get().build()
        OkHttpClient().newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                try {
                    val res = JSONObject(response.body?.string() ?: "{}")
                    val offers = res.optJSONArray("offers") ?: JSONArray()
                    activity?.runOnUiThread {
                        offersContainer.removeAllViews()
                        for (i in 0 until offers.length()) {
                            val item = offers.getJSONObject(i)
                            val card = inflater.inflate(R.layout.item_offer_card, offersContainer, false)
                            card.findViewById<TextView>(R.id.tv_offer_title).text = item.optString("title")
                            card.findViewById<TextView>(R.id.tv_offer_desc).text = item.optString("tagline")
                            card.findViewById<TextView>(R.id.tv_offer_reward).text = "+ ${item.optString("payout_coins")}"
                            card.setOnClickListener {
                                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.optString("offer_url"))))
                            }
                            offersContainer.addView(card)
                        }
                    }
                } catch (e: Exception) {}
            }
        })
        return view
    }
}

// 3. My Offers Tracker
class MyOffersFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 60, 50, 60)
        }

        val heading = TextView(context).apply {
            text = "Activity & Withdrawals"
            textSize = 22f
            setTextColor(android.graphics.Color.parseColor("#0F172A"))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        layout.addView(heading)

        val session = SessionManager(requireContext())
        val token = session.getAuthToken() ?: return layout
        val body = JSONObject().apply { put("token", token) }.toString().toRequestBody("application/json".toMediaType())
        val req = Request.Builder().url("https://paisaloots.site/api/get_user_history.php").post(body).build()

        OkHttpClient().newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                try {
                    val res = JSONObject(response.body?.string() ?: "{}")
                    val withdraws = res.optJSONArray("withdrawals") ?: JSONArray()
                    activity?.runOnUiThread {
                        if (withdraws.length() == 0) {
                            val emptyTv = TextView(context).apply {
                                text = "No transaction history yet."
                                setPadding(0, 40, 0, 0)
                            }
                            layout.addView(emptyTv)
                        }
                        for (i in 0 until withdraws.length()) {
                            val w = withdraws.getJSONObject(i)
                            val tv = TextView(context).apply {
                                text = "₹${w.optString("amount")} via ${w.optString("payout_details")} | Status: ${w.optString("status").uppercase()}"
                                setPadding(0, 24, 0, 0)
                                textSize = 15f
                            }
                            layout.addView(tv)
                        }
                    }
                } catch (e: Exception) {}
            }
        })

        return layout
    }
}

// 4. Refer & Earn
class ReferFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(50, 100, 50, 60)
        }

        val title = TextView(context).apply {
            text = "Refer & Earn ₹50"
            textSize = 22f
            setTextColor(android.graphics.Color.parseColor("#0F172A"))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        layout.addView(title)

        val code = "LOOT" + (1000..9999).random()
        val codeView = TextView(context).apply {
            text = "CODE: $code"
            textSize = 20f
            setTextColor(android.graphics.Color.parseColor("#6C13F5"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 40, 0, 30)
        }
        layout.addView(codeView)

        val btnShare = Button(context).apply {
            text = "Share Referral Code"
            setBackgroundColor(android.graphics.Color.parseColor("#6C13F5"))
            setTextColor(android.graphics.Color.WHITE)
            setOnClickListener {
                val share = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "Earn cash on Paisa Loots! Use code $code: https://paisaloots.site")
                }
                startActivity(Intent.createChooser(share, "Share Code"))
            }
        }
        layout.addView(btnShare)

        return layout
    }
}

// 5. Profile & Settings
class ProfileFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val session = SessionManager(requireContext())
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 60, 50, 60)
        }

        val nameTv = TextView(context).apply {
            text = session.getUserName()
            textSize = 22f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#0F172A"))
        }
        layout.addView(nameTv)

        val btnWithdraw = Button(context).apply {
            text = "Withdraw via UPI"
            setBackgroundColor(android.graphics.Color.parseColor("#10B981"))
            setTextColor(android.graphics.Color.WHITE)
            setOnClickListener {
                session.getAuthToken()?.let { (activity as? DashboardActivity)?.showWithdrawDialog(it) }
            }
        }
        layout.addView(btnWithdraw)

        val btnLogout = Button(context).apply {
            text = "Logout"
            setBackgroundColor(android.graphics.Color.parseColor("#DC2626"))
            setTextColor(android.graphics.Color.WHITE)
            setOnClickListener {
                session.logout()
                val intent = Intent(activity, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                activity?.finish()
            }
        }
        layout.addView(btnLogout)

        return layout
    }
}
