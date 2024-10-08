package com.trtc.uikit.livekit.view.voiceroom;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static com.trtc.uikit.livekit.common.utils.Constants.DEFAULT_MAX_SEAT_COUNT;
import static com.trtc.uikit.livekit.common.utils.Constants.EVENT_KEY_LIVE_KIT;
import static com.trtc.uikit.livekit.common.utils.Constants.EVENT_PARAMS_KEY_ENABLE_SLIDE;
import static com.trtc.uikit.livekit.common.utils.Constants.EVENT_SUB_KEY_CLOSE_VOICE_ROOM;
import static com.trtc.uikit.livekit.common.utils.Constants.EVENT_SUB_KEY_FINISH_ACTIVITY;
import static com.trtc.uikit.livekit.common.utils.Constants.EVENT_SUB_KEY_LINK_STATUS_CHANGE;
import static com.trtc.uikit.livekit.state.LiveDefine.LinkStatus.NONE;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.tencent.cloud.tuikit.engine.room.TUIRoomDefine;
import com.tencent.qcloud.tuicore.TUICore;
import com.tencent.qcloud.tuicore.interfaces.ITUINotification;
import com.trtc.tuikit.common.livedata.Observer;
import com.trtc.uikit.livekit.R;
import com.trtc.uikit.livekit.common.uicomponent.audioeffect.store.AudioEffectSateFactory;
import com.trtc.uikit.livekit.common.uicomponent.music.store.MusicPanelSateFactory;
import com.trtc.uikit.livekit.manager.LiveController;
import com.trtc.uikit.livekit.state.LiveDefine;
import com.trtc.uikit.livekit.view.voiceroom.view.VoiceRoomRootView;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class TUIVoiceRoomFragment extends Fragment implements ITUINotification {

    private       RelativeLayout                  mLayoutContainer;
    private       VoiceRoomRootView               mVoiceRoomRootView;
    private       LiveController                  mLiveController;
    private       LiveDefine.LinkStatus           mCurrentLinkStatus;
    private final String                          mRoomId;
    private final RoomBehavior                    mRoomBehavior;
    private final RoomParams                      mRoomParams;
    private final Observer<LiveDefine.LinkStatus> mLinkStatusObserver  = this::onLinkStatusChange;
    private final OnBackPressedCallback           mBackPressedCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            LiveDefine.LiveStatus liveStatus = mLiveController.getViewState().liveStatus.get();
            if (LiveDefine.LiveStatus.PUSHING == liveStatus || LiveDefine.LiveStatus.PLAYING == liveStatus) {
                TUICore.notifyEvent(EVENT_KEY_LIVE_KIT, EVENT_SUB_KEY_CLOSE_VOICE_ROOM, null);
            } else {
                requireActivity().finish();
            }
        }
    };

    public TUIVoiceRoomFragment(String roomId, RoomBehavior behavior, RoomParams params) {
        mRoomId = roomId;
        mRoomBehavior = behavior;
        mRoomParams = params;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initLiveController();
        addObserver();
        TUICore.registerEvent(EVENT_KEY_LIVE_KIT, EVENT_SUB_KEY_FINISH_ACTIVITY, this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View contentView = inflater.inflate(R.layout.livekit_voiceroom_fragment_main, container, false);
        mLayoutContainer = contentView.findViewById(R.id.rl_container);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT);
        mVoiceRoomRootView = new VoiceRoomRootView(requireActivity(), mLiveController, mRoomBehavior, mRoomParams);
        mLayoutContainer.addView(mVoiceRoomRootView, layoutParams);
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), mBackPressedCallback);
        return contentView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mVoiceRoomRootView.post(() ->
                mVoiceRoomRootView.updateStatus(VoiceRoomRootView.VoiceRoomViewStatus.START_DISPLAY));
    }

    @Override
    public void onResume() {
        super.onResume();
        mVoiceRoomRootView.post(() ->
                mVoiceRoomRootView.updateStatus(VoiceRoomRootView.VoiceRoomViewStatus.DISPLAY_COMPLETE));
    }

    @Override
    public void onPause() {
        super.onPause();
        mVoiceRoomRootView.post(() ->
                mVoiceRoomRootView.updateStatus(VoiceRoomRootView.VoiceRoomViewStatus.END_DISPLAY));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mVoiceRoomRootView.post(() -> mVoiceRoomRootView.updateStatus(VoiceRoomRootView.VoiceRoomViewStatus.DESTROY));
        mBackPressedCallback.remove();
        mLayoutContainer.removeView(mVoiceRoomRootView);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        removeObserver();
        TUICore.unRegisterEvent(this);
        MusicPanelSateFactory.removeState(mRoomId);
        AudioEffectSateFactory.removeState(mRoomId);
        mLiveController.destroy();
    }

    public void enableBackPressedCallBack(boolean enable) {
        mBackPressedCallback.setEnabled(enable);
    }

    private void initLiveController() {
        mLiveController = new LiveController();
        mLiveController.setRoomId(mRoomId);
    }

    private void addObserver() {
        if (mLiveController != null) {
            mLiveController.getViewState().linkStatus.observe(mLinkStatusObserver);
        }
    }

    private void removeObserver() {
        if (mLiveController != null) {
            mLiveController.getViewState().linkStatus.removeObserver(mLinkStatusObserver);
        }
    }

    private void onLinkStatusChange(LiveDefine.LinkStatus linkStatus) {
        if (mCurrentLinkStatus != linkStatus) {
            mCurrentLinkStatus = linkStatus;
            Map<String, Object> params = new HashMap<>();
            if (NONE == mCurrentLinkStatus) {
                params.put(EVENT_PARAMS_KEY_ENABLE_SLIDE, true);
            } else {
                params.put(EVENT_PARAMS_KEY_ENABLE_SLIDE, false);
            }
            TUICore.notifyEvent(EVENT_KEY_LIVE_KIT, EVENT_SUB_KEY_LINK_STATUS_CHANGE, params);
        }
    }

    @Override
    public void onNotifyEvent(String key, String subKey, Map<String, Object> param) {
        if (EVENT_SUB_KEY_FINISH_ACTIVITY.equals(subKey)) {
            if (param == null) {
                requireActivity().finish();
            } else {
                String roomId = (String) param.get("roomId");
                if (roomId != null && roomId.equals(mRoomId)) {
                    requireActivity().finish();
                }
            }
        }
    }

    public static class RoomParams implements Serializable {
        public String                 roomName     = "";
        public int                    maxSeatCount = DEFAULT_MAX_SEAT_COUNT;
        public TUIRoomDefine.SeatMode seatMode     = TUIRoomDefine.SeatMode.FREE_TO_TAKE;
    }

    public enum RoomBehavior {
        AUTO_CREATE,
        PREPARE_CREATE,
        JOIN
    }
}
