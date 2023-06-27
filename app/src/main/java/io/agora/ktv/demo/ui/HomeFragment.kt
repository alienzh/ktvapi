package io.agora.ktv.demo.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import io.agora.ktv.demo.R
import io.agora.ktv.demo.databinding.FragmentHomeBinding
import io.agora.ktvapi.KTVSingRole

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnJoinChannel.setOnClickListener {
            val role = when (binding.radioRole.checkedRadioButtonId) {
                R.id.rbtSoloSinger -> KTVSingRole.SoloSinger
                R.id.rbtCoSinger -> KTVSingRole.CoSinger
                R.id.rbtAudience -> KTVSingRole.Audience
                else -> KTVSingRole.SoloSinger
            }
            findNavController().navigate(R.id.action_homeFragment_to_liveFragment, Bundle().apply {
                putString(LiveFragment.KEY_CHANNEL, binding.etChannelId.text.toString())
                putInt(LiveFragment.KEY_ROLE, role.value)
            })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}