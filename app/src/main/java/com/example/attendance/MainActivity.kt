package com.example.attendance

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
import com.example.attendance.common.Constants
import com.example.attendance.databinding.ActivityMainBinding
import com.example.attendance.fragment.AttendanceFragment
import com.example.attendance.fragment.PersonalPageFragment
import com.example.attendance.fragment.ReportFragment
import com.example.attendance.util.ToastUtils
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import io.reactivex.Observable
import io.reactivex.ObservableOnSubscribe
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding


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

//        viewpager.offscreenPageLimit = ViewPager2.OFFSCREEN_PAGE_LIMIT_DEFAULT

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
        val bgs = intArrayOf(R.drawable.attendance_icon, R.drawable.report_icon, R.drawable.personal_icon)


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
//                tab?.icon?.setColorFilter(tabIconColor, PorterDuff.Mode.SRC_IN)
                tab?.icon?.colorFilter = null
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }

        })
        mediator.attach()
    }

    override fun onDestroy() {
        super.onDestroy()

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
            val activeCode = FaceEngine.activeOnline(App.myApplication, Constants.APP_ID, Constants.SDK_KEY)
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
                    Log.d(TAG, "onNext: $t")
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

    /**
     * 检查能否找到动态链接库，如果找不到，请修改工程配置
     *
     * @param libraries 需要的动态链接库
     * @return 动态库是否存在
     */
//    private fun checkSoFile(libraries : ArrayList<String>) : Boolean {
//        val dir = File(applicationInfo.nativeLibraryDir)
//        Log.d(TAG, "checkSoFile: $dir")
//        val files = dir.listFiles()
//        if (files == null || files.isEmpty()) {
//            return false
//        }
//        val libraryNameList: MutableList<String> = ArrayList()
//        for (file in files) {
//            libraryNameList.add(file.name)
//        }
//        return libraryNameList.containsAll(libraries)
//    }

//    private fun checkSoFile(libraries: ArrayList<String>): Boolean {
//        val dir = File(applicationInfo.nativeLibraryDir)
//        Log.d(TAG,
//            "checkSoFile: " + dir.absolutePath
//        )
//        val files = dir.listFiles()
//        if (files == null || files.isEmpty()) {
//            return false
//        }
//        val libraryNameList: MutableList<String> = java.util.ArrayList()
//        for (file in files) {
//            Log.d(
//                TAG,
//                "checkSoFile: " + file.name
//            )
//            libraryNameList.add(file.name)
//        }
//        var exists = true
//        for (library in libraries) {
//            exists = exists and libraryNameList.contains(library)
//        }
//        return exists
//    }

    private fun checkPermissions(neededPermissions: Array<String>?): Boolean {
        if (neededPermissions == null || neededPermissions.isEmpty()) {
            return true
        }
        var allGranted = true
        for (neededPermission in neededPermissions) {
            allGranted = allGranted and (ContextCompat.checkSelfPermission(
                this,
                neededPermission!!
            ) === PackageManager.PERMISSION_GRANTED)
        }
        return allGranted
    }

    companion object {
        const val TAG = "MainActivity"
        const val ACTION_REQUEST_PERMISSIONS = 0x001
        val NEEDED_PERMISSIONS = arrayOf(android.Manifest.permission.READ_PHONE_STATE)
        val LIBRARIES = arrayListOf(
            "libarcsoft_face_engine.so",
            "libarcsoft_face.so"
        )
    }
}