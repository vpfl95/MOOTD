package com.example.mootd.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.mootd.R
import com.example.mootd.databinding.FragmentPictureDetailBinding


class PictureDetailFragment : Fragment() {

    private var _binding: FragmentPictureDetailBinding? = null
    private val binding get() = _binding!!
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

        val imagePath = arguments?.getString("imagePath")
        imagePath?.let {
            Glide.with(this)
                .load(it)
                .into(binding.detailImageView)
        }

        val bundle = Bundle().apply {
            putString("imagePath", imagePath) // imagePath는 로드된 이미지의 실제 경로여야 함
        }

        binding.createGuideButton.setOnClickListener{
            findNavController().navigate(R.id.action_pictureDetailFragment_to_mainFragment, bundle)
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}