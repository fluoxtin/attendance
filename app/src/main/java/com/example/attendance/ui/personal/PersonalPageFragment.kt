package com.example.attendance.ui.personal

import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.arcsoft.imageutil.ArcSoftImageFormat
import com.arcsoft.imageutil.ArcSoftImageUtil
import com.arcsoft.imageutil.ArcSoftImageUtilError
import com.example.attendance.R
import com.example.attendance.databinding.FragmentPersonalPageBinding
import com.example.attendance.faceserver.FaceServer
import com.example.attendance.oss.OSSUploader
import com.example.attendance.ui.MainActivity
import com.example.attendance.ui.attendance.AttendViewModel
import com.example.attendance.ui.login.LoginActivity
import com.example.attendance.util.SharedPreferencesUtils
import com.example.attendance.util.ToastUtils
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class PersonalPageFragment : Fragment() {

    lateinit var binding : FragmentPersonalPageBinding
    private val viewModel by viewModels<AttendViewModel>()
    var executorService : ExecutorService? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =  FragmentPersonalPageBinding.inflate(inflater, container, false).also {
        binding = it
    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        when(SharedPreferencesUtils.getCurrentUserRole()) {
            0 -> initViewForT()
            1 -> initViewForS()
            else -> ToastUtils.showShortToast("undefined role")
        }

        binding.modifyPassword.setOnClickListener {
            requireContext().startActivity(Intent(requireContext(), ModifyInfoActivity::class.java))
        }

        binding.feedback.setOnClickListener {
            requireContext().startActivity(Intent(requireContext(), FeedbackActivity::class.java))
        }

        binding.logout.setOnClickListener {
            SharedPreferencesUtils.removeUser()
            SharedPreferencesUtils.removeToken()
            requireContext().startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }

    }

    private fun initViewForT() {

        binding.uploadFaceInfo.visibility = View.GONE
        binding.leave.visibility = View.GONE

        viewModel.currentTeacher.observe(viewLifecycleOwner) {
            binding.username.text = it.tea_name
            binding.unit.text = it.unit

            when(it.sex) {
                "男" -> binding.userPhoto.setImageResource(R.drawable.male_image)
                "女" -> binding.userPhoto.setImageResource(R.drawable.female_image)
                else -> binding.userPhoto.setImageResource(R.drawable.personal_icon)
            }
        }
    }

    private fun initViewForS() {

        binding.uploadFaceInfo.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/**")
            requireActivity().startActivityFromFragment(this, intent, ACTION_PICK_IMAGE)
        }

        binding.leave.setOnClickListener {
            requireContext().startActivity(Intent(requireContext(), RequestLeaveActivity::class.java))
        }

        viewModel.currentStudent.observe(viewLifecycleOwner) {
            binding.username.text = it.name
            binding.unit.text = it.unit
            binding.studentClass.text = it.stu_class

            binding.uploadFaceInfo.isClickable = it.face_url.isNullOrEmpty()
            if (it.face_url.isNullOrEmpty()) {
                binding.isUploaded.text = "未上传"
                binding.isUploaded.setTextColor(requireContext().getColor(R.color.black))
            }
            else {
                binding.isUploaded.text = "已上传"
                binding.isUploaded.setTextColor(requireContext().getColor(R.color.light_blue_900))
            }

            when(it.sex) {
                "男" -> binding.userPhoto.setImageResource(R.drawable.male_image)
                "女" -> binding.userPhoto.setImageResource(R.drawable.female_image)
                else -> binding.userPhoto.setImageResource(R.drawable.personal_icon)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d(TAG, "onActivityResult: ")

        data?.apply {
            if (requestCode == ACTION_PICK_IMAGE) {
                if (this.data == null) {
                    ToastUtils.showLongToast("failed to pick image")
                    return
                }
                var bitmap = MediaStore.Images.Media.getBitmap(requireContext().contentResolver, this.data)
                bitmap = ArcSoftImageUtil.getAlignedBitmap(bitmap, true)
                executorService = Executors.newSingleThreadExecutor()
                executorService?.execute {
                    val bgr24 = ArcSoftImageUtil
                        .createImageData(bitmap.width, bitmap.height, ArcSoftImageFormat.BGR24)
                    val transformCode = ArcSoftImageUtil
                        .bitmapToImageData(bitmap, bgr24, ArcSoftImageFormat.BGR24)
                    if (transformCode != ArcSoftImageUtilError.CODE_SUCCESS) {
                        requireActivity().runOnUiThread {
                            ToastUtils.showShortToast("transform image failed")
                        }
                        return@execute
                    }
                    val username = SharedPreferencesUtils.getCurrentUser()?.username
                    val success = FaceServer.instance
                        .registerBgr24(
                            requireContext(),
                            bgr24,
                            bitmap.width,
                            bitmap.height,
                            username
                        )
                    if (success) {
                        OSSUploader.instance.uploadFile(username?: "未知",requireContext().filesDir.absolutePath +
                                File.separator + FaceServer.SAVE_IMG_DIR + File.separator + username + FaceServer.IMG_SUFFIX)
                        viewModel.updateFaceUrl(username?:"未知")
                    }
                    requireActivity().runOnUiThread {
                        if (success) {
                            ToastUtils.showShortToast("register success")
                        } else
                            ToastUtils.showShortToast("register failed")
                    }
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)

    }


    companion object {
        const val TAG = "PersonalPageFragment"
        const val ACTION_PICK_IMAGE = 0X202
    }
}