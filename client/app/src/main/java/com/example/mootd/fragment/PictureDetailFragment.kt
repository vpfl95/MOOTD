package com.example.mootd.fragment

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.mootd.R
import com.example.mootd.api.RetrofitInstance
import com.example.mootd.api.GuideResult
import com.example.mootd.databinding.FragmentPictureDetailBinding
import com.example.mootd.viewmodel.GuideOverlayViewModel
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class PictureDetailFragment : Fragment() {

    private var _binding: FragmentPictureDetailBinding? = null
    private val binding get() = _binding!!

    private val guideOverlayViewModel: GuideOverlayViewModel by activityViewModels()

    private val imagePath: String by lazy {
        arguments?.getString("imagePath") ?: ""
    }
    private val folderRootPath = "MyApp/GuideImages"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPictureDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.backButton.setOnClickListener {
            findNavController().popBackStack()
        }

        imagePath?.let {
            Glide.with(this)
                .load(it)
                .into(binding.detailImageView)
        }

        binding.createGuideButton.setOnClickListener{
            showLoadingOverlay()
            lifecycleScope.launch {
                try {
                    val response = uploadImageAndCreateGuide(imagePath)
                    if (response != null) {
                        saveGuideImagesToStorage(response.personGuideLineURL, response.backgroundGuideLineURL)
                        guideOverlayViewModel.setGuideImages(
                            originalUrl = imagePath,
                            personUrl = response.personGuideLineURL,
                            backgroundUrl = response.backgroundGuideLineURL
                        )
                        findNavController().navigate(R.id.action_pictureDetailFragment_to_mainFragment)
                    } else {
                        showToast("가이드라인 생성에 실패했습니다.")
                    }
                } catch (e: Exception) {
                    Log.e("PictureDetailFragment", "Error creating guide: ${e.message}")
                    showToast("네트워크 오류가 발생했습니다.")
                } finally {
                    hideLoadingOverlay()
                }
            }
        }

    }

    private fun showLoadingOverlay() {
        binding.loadingOverlay.visibility = View.VISIBLE
    }

    private fun hideLoadingOverlay() {
        binding.loadingOverlay.visibility = View.GONE
    }

    private suspend fun uploadImageAndCreateGuide(imagePath: String): GuideResult? {
        return withContext(Dispatchers.IO) {
            val imageFile = File(imagePath)
            val requestBody = imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val imagePart = MultipartBody.Part.createFormData("originImageFile", imageFile.name, requestBody)

            val response = RetrofitInstance.guideCreateService.createGuide(imagePart)
            if (response.isSuccessful) {
                response.body()?.data
            } else {
                Log.e("PictureDetailFragment", "API Error: ${response.errorBody()?.string()}")
                null
            }
        }
    }

    private suspend fun saveGuideImagesToStorage(personGuideUrl: String, backgroundGuideUrl: String) {
        withContext(Dispatchers.IO) {
            val folderName = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val folder = File(requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES), "$folderRootPath/$folderName")
            if (!folder.exists()) {
                folder.mkdirs()
            }

            val originalFile = File(folder, "originalImage.png")
            saveImageAsync(loadBitmapFromPath(imagePath), originalFile)

            val personGuideBitmap = loadImageFromUrl(personGuideUrl)
            val personGuideFile = File(folder, "personGuideImage.png")
            saveImageAsync(personGuideBitmap, personGuideFile)

            val backgroundGuideBitmap = loadImageFromUrl(backgroundGuideUrl)
            val backgroundGuideFile = File(folder, "backgroundGuideImage.png")
            saveImageAsync(backgroundGuideBitmap, backgroundGuideFile)
        }
    }


//    private fun loadBitmapFromPath(path: String): Bitmap {
//        val startTime = System.currentTimeMillis()
//        Log.d("Performance", "Bitmap decoding took ${System.currentTimeMillis() - startTime} ms")
//        val options = BitmapFactory.Options().apply {
//            inJustDecodeBounds = true
//        }
//        BitmapFactory.decodeFile(path, options)
//
//        // 이미지 축소 비율 계산
//        options.inSampleSize = calculateInSampleSize(options, 1024, 1024) // 원하는 크기로 축소
//        options.inJustDecodeBounds = false
//
//        val bitmap = BitmapFactory.decodeFile(path, options)
//        val exif = androidx.exifinterface.media.ExifInterface(path)
//
//        // 이미지의 회전 각도 가져오기
//        val orientation = exif.getAttributeInt(
//            androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
//            androidx.exifinterface.media.ExifInterface.ORIENTATION_UNDEFINED
//        )
//
//        val rotatedBitmap = when (orientation) {
//            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(bitmap, 90f)
//            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(bitmap, 180f)
//            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(bitmap, 270f)
//            else -> bitmap // 회전이 필요 없는 경우
//        }
//        Log.d("Performance", "Bitmap rotation took ${System.currentTimeMillis() - startTime} ms")
//
//        return rotatedBitmap
//    }
//
//    // inSampleSize 계산 함수
//    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
//        val (height: Int, width: Int) = options.run { outHeight to outWidth }
//        var inSampleSize = 1
//
//        if (height > reqHeight || width > reqWidth) {
//            val halfHeight: Int = height / 2
//            val halfWidth: Int = width / 2
//
//            // 크기를 줄이면서 메모리 사용량 최소화
//            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
//                inSampleSize *= 2
//            }
//        }
//
//        return inSampleSize
//    }
//
//    private fun rotateImage(source: Bitmap, angle: Float): Bitmap {
//        val matrix = android.graphics.Matrix()
//        matrix.postRotate(angle)
//        val rotatedBitmap = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
//        source.recycle() // 원본 Bitmap 메모리 해제
//        return rotatedBitmap
//    }

//
    private fun loadBitmapFromPath(path: String): Bitmap {
        val startTime = System.currentTimeMillis()
        val bitmap = BitmapFactory.decodeFile(path)
        Log.d("Performance", "Bitmap decoding took ${System.currentTimeMillis() - startTime} ms")
        val exif = androidx.exifinterface.media.ExifInterface(path)

        // 이미지의 회전 각도 가져오기
        val orientation = exif.getAttributeInt(
            androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
            androidx.exifinterface.media.ExifInterface.ORIENTATION_UNDEFINED
        )

        val rotatedBitmap = when (orientation) {
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(bitmap, 90f)
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(bitmap, 180f)
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(bitmap, 270f)
            else -> bitmap
        }

        Log.d("Performance", "Bitmap rotation took ${System.currentTimeMillis() - startTime} ms")
        return rotatedBitmap
    }

    private fun rotateImage(source: Bitmap, angle: Float): Bitmap {
        val matrix = android.graphics.Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }



    private fun loadImageFromUrl(url: String): Bitmap {
        val connection = java.net.URL(url).openConnection()
        connection.connect()
        return BitmapFactory.decodeStream(connection.getInputStream())
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }


    private suspend fun saveImageAsync(bitmap: Bitmap?, file: File) {
        withContext(Dispatchers.IO) {
            bitmap?.let {
                FileOutputStream(file).use { out ->
                    it.compress(Bitmap.CompressFormat.PNG, 80, out)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}