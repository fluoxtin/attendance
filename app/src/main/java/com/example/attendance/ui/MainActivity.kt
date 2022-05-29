package com.example.attendance.ui

import android.content.pm.PackageManager
import android.graphics.PorterDuff
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.arcsoft.face.ErrorInfo
import com.arcsoft.face.FaceEngine
import com.arcsoft.face.VersionInfo
import com.example.attendance.App
import com.example.attendance.R
import com.example.attendance.common.Constants
import com.example.attendance.databinding.ActivityMainBinding
import com.example.attendance.faceserver.FaceServer
import com.example.attendance.ui.attendance.AttendanceFragment
import com.example.attendance.ui.personal.PersonalPageFragment
import com.example.attendance.ui.report.ReportFragment
import com.example.attendance.util.ToastUtils
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import io.reactivex.Observable
import io.reactivex.ObservableOnSubscribe
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.ExecutorService


class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding

    var executorService : ExecutorService ? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.i(App.TAG, "onCreate: ${applicationInfo.nativeLibraryDir}")

        val versionInfo = VersionInfo()
        val code = FaceEngine.getVersion(versionInfo)
        Log.i(
            TAG,
            "onCreate: getVersion, code is $code, versionInfo is $versionInfo"
        )

        // active face engine
        activeEngine()

        val viewpager = binding.viewPager
        val tabLayout = binding.tabLayout

        viewpager.adapter = object : FragmentStateAdapter(supportFragmentManager, lifecycle) {

            override fun getItemCount(): Int {
                return 3
            }

            override fun createFragment(position: Int): Fragment {
                return when(position) {
                    1 -> ReportFragment()
                    2 -> PersonalPageFragment()
                    else -> AttendanceFragment()
                }
            }
        }

        viewpager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
            }
        })

        val titles = resources.getStringArray(R.array.tab_titles)
        val bgs = intArrayOf(
            R.drawable.attendance_icon,
            R.drawable.report_icon,
            R.drawable.personal_icon
        )

        val mediator = TabLayoutMediator(tabLayout, viewpager, true) { tab, position ->
            tab.text = titles[position]
            tab.icon = getDrawable(bgs[position])
        }

        // 隐藏下划线
        tabLayout.setSelectedTabIndicatorHeight(0)

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val tabIconColor = ContextCompat.getColor(this@MainActivity, R.color.tab_color)
                tab?.icon?.setColorFilter(tabIconColor, PorterDuff.Mode.SRC_IN)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                val tabIconColor = ContextCompat.getColor(this@MainActivity, R.color.white)
                tab?.icon?.colorFilter = null
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }

        })
        mediator.attach()

        FaceServer.instance.init(this)
    }

    /**
     *  active arcFace engine
     */
    private fun activeEngine() {

        if (!checkPermissions(NEEDED_PERMISSIONS)) {
            ActivityCompat.requestPermissions(
                this,
                NEEDED_PERMISSIONS,
                ACTION_REQUEST_PERMISSIONS
            )
        }

        Observable.create(ObservableOnSubscribe<Int> {
            val runtimeABI = FaceEngine.getRuntimeABI()
            Log.i(App.TAG, "subscribe: getRuntimeABI() + $runtimeABI")

            val start = System.currentTimeMillis()
            val activeCode = FaceEngine.activeOnline(App.getInstance(), Constants.APP_ID, Constants.SDK_KEY)
            Log.i(App.TAG, "subscribe cost : ${System.currentTimeMillis() - start}")
            it.onNext(activeCode)
        })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<Int> {
                override fun onNext(t: Int) {
                    when(t) {
                        ErrorInfo.MOK ->
                            ToastUtils.showLongToast("active success")

                        ErrorInfo.MERR_ASF_ALREADY_ACTIVATED ->
                            ToastUtils.showShortToast("already activated")

                        else ->
                            ToastUtils.showShortToast("active failed $t")
                    }
                    Log.d(App.TAG, "onNext: $t")
                }

                override fun onError(e: Throwable) {
                    e.message?.let { ToastUtils.showShortToast(it) }
                }

                override fun onSubscribe(d: Disposable) {
                }

                override fun onComplete() {
                }
            })
    }


    private fun checkPermissions(neededPermissions: Array<String>?): Boolean {
        if (neededPermissions == null || neededPermissions.isEmpty()) {
            return true
        }
        var allGranted = true
        for (neededPermission in neededPermissions) {
            allGranted = allGranted && (ContextCompat.checkSelfPermission(
                this,
                neededPermission
            ) == PackageManager.PERMISSION_GRANTED)
        }
        return allGranted
    }

    override fun onDestroy() {
        super.onDestroy()
        executorService?.apply {
            if (!isShutdown)
                shutdown()
            executorService = null
        }
    }

    companion object {
        const val TAG = "MainActivity"
        const val ACTION_REQUEST_PERMISSIONS = 0x001
        val NEEDED_PERMISSIONS = arrayOf(
            android.Manifest.permission.READ_PHONE_STATE,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
        )
    }
}