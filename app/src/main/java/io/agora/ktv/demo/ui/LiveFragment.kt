package io.agora.ktv.demo.ui

import android.content.Context
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import io.agora.ktv.demo.BuildConfig
import io.agora.ktv.demo.MusicModel
import io.agora.ktv.demo.databinding.FragmentLiveBinding
import io.agora.ktv.demo.rtc.IChannelEventListener
import io.agora.ktv.demo.rtc.RtcEngineInstance
import io.agora.ktv.demo.utils.KeyCenter
import io.agora.ktv.demo.utils.TokenGenerator
import io.agora.ktvapi.IMusicLoadStateListener
import io.agora.ktvapi.KTVApiConfig
import io.agora.ktvapi.KTVApiImpl
import io.agora.ktvapi.KTVLoadMusicConfiguration
import io.agora.ktvapi.KTVLoadMusicMode
import io.agora.ktvapi.KTVLoadSongFailReason
import io.agora.ktvapi.MusicLoadStatus
import io.agora.musiccontentcenter.Music
import io.agora.rtc2.Constants

class LiveFragment : Fragment() {
    private var _binding: FragmentLiveBinding? = null

    private val binding get() = _binding!!

    private lateinit var musicAdapter: ArrayAdapter<MusicModel>

    private val musicList = java.util.ArrayList<MusicModel>()

    private val ktvApi = KTVApiImpl()

    private var currentSongCode: Long = -1

    companion object {
        const val KEY_CHANNEL = "key_channel"
        const val KEY_ROLE = "key_role"
    }

    private val channelId: String by lazy {
        arguments?.getString(KEY_CHANNEL, "") ?: ""
    }
    private val role: Int by lazy {
        arguments?.getInt(KEY_ROLE, Constants.CLIENT_ROLE_AUDIENCE)
            ?: Constants.CLIENT_ROLE_AUDIENCE
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity?.onBackPressedDispatcher?.addCallback {
            handleOnBackPressed()
            RtcEngineInstance.leaveChannel()
            ktvApi.release()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLiveBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvChannelId.text = channelId
        binding.buttonClose.setOnClickListener {
            findNavController().popBackStack()
            RtcEngineInstance.leaveChannel()
            ktvApi.release()
        }
        binding.ivMusicPlay.setOnClickListener {
            if (currentSongCode == -1L) return@setOnClickListener
            ktvApi.resumeSing()
        }
        binding.ivMusicPause.setOnClickListener {
            ktvApi.pauseSing()
        }
        binding.searchMusic.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchMusicByKeyword(query ?: "")
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })

        musicAdapter =
            ArrayAdapter<MusicModel>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                musicList
            )
        binding.musicList.adapter = musicAdapter
        binding.musicList.setOnItemClickListener { parent, view, position, id ->
            val selectMusic = musicList[position]
            if (currentSongCode == selectMusic.songCode) return@setOnItemClickListener
            currentSongCode = selectMusic.songCode
            binding.tvMusicName.text = selectMusic.toString()
            musicAdapter.clear()
            loadMusic()
        }
        initKtvApi()
    }

    private var rtcToken = ""
    private var rtmToken = ""

    private fun initKtvApi() {
        TokenGenerator.generateTokens(
            channelId,
            KeyCenter.rtcUid.toString(),
            TokenGenerator.TokenGeneratorType.token006,
            arrayOf(
                TokenGenerator.AgoraTokenType.rtc,
                TokenGenerator.AgoraTokenType.rtm
            ),
            { ret ->
                rtcToken = ret[TokenGenerator.AgoraTokenType.rtc] ?: ""
                rtmToken = ret[TokenGenerator.AgoraTokenType.rtm] ?: ""
                TokenGenerator.generateToken(channelId + "_ex", KeyCenter.rtcUid.toString(),
                    TokenGenerator.TokenGeneratorType.token006,
                    TokenGenerator.AgoraTokenType.rtc,
                    { chorusToken ->
                        ktvApi.initialize(
                            KTVApiConfig(
                                BuildConfig.AGORA_APP_ID,
                                rtmToken,
                                RtcEngineInstance.rtcEngine,
                                channelId,
                                KeyCenter.rtcUid,
                                channelId + "_ex",
                                chorusToken
                            )
                        )

                        // ------------------ 加入频道 ------------------
                        RtcEngineInstance.rtcEngine.apply {
                            setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING)
                            enableVideo()
                            enableLocalVideo(false)
                            enableAudio()
                            setAudioProfile(
                                Constants.AUDIO_PROFILE_MUSIC_HIGH_QUALITY,
                                Constants.AUDIO_SCENARIO_GAME_STREAMING
                            )
                            enableAudioVolumeIndication(50, 10, true)
                            setClientRole(Constants.CLIENT_ROLE_BROADCASTER)
                            RtcEngineInstance.joinChannel(
                                rtcToken, channelId, KeyCenter.rtcUid, eventListener
                            )
                        }
                    })
            })
    }

    private val eventListener: IChannelEventListener = IChannelEventListener(
        onChannelJoined = {
            showToast("加入频道成功")
        }
    )

    private fun searchMusicByKeyword(condition: String) {
        // 过滤没有歌词的歌曲
        val jsonOption = "{\"pitchType\":1,\"needLyric\":true}"
        ktvApi.searchMusicByKeyword(
            condition, 0, 20, jsonOption,
        ) { id, status, p, size, total, list ->
            val musicList: List<Music> = list?.toList() ?: emptyList()
            this.musicList.clear()
            musicList.forEach {
                this.musicList.add(MusicModel(it.songCode, it.name, it.singer))
            }
            runOnUiThread {
                musicAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun loadMusic() {
        val musicConfiguration = KTVLoadMusicConfiguration(
            currentSongCode.toString(),
            true,
            KeyCenter.rtcUid,
            KTVLoadMusicMode.LOAD_MUSIC_ONLY
        )
        ktvApi.loadMusic(
            currentSongCode, musicConfiguration, object : IMusicLoadStateListener {
                override fun onMusicLoadSuccess(songCode: Long, lyricUrl: String) {
                    showToast("歌曲加载成功")
                }

                override fun onMusicLoadFail(songCode: Long, reason: KTVLoadSongFailReason) {
                    showToast("歌曲加载失败")
                }

                override fun onMusicLoadProgress(
                    songCode: Long,
                    percent: Int,
                    status: MusicLoadStatus,
                    msg: String?,
                    lyricUrl: String?
                ) {
                }
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        RtcEngineInstance.leaveChannel()
        ktvApi.release()
    }

    private fun showToast(message: String) {

        val act = activity ?: return
        runOnUiThread {
            act.let {
                Toast.makeText(it, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun runOnUiThread(runnable: Runnable) {
        if (Thread.currentThread() == Looper.getMainLooper().thread) {
            runnable.run()
        } else {
            activity?.runOnUiThread(runnable)
        }
    }
}