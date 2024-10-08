package com.trtc.uikit.livekit.view.liveroom.view.audience;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static com.trtc.uikit.livekit.common.utils.Constants.DATA_REPORT_COMPONENT_LIVE_ROOM;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;

import com.trtc.tuikit.common.livedata.Observer;
import com.trtc.uikit.livekit.R;
import com.trtc.uikit.livekit.common.uicomponent.dashboard.AudienceDashboardView;
import com.trtc.uikit.livekit.common.utils.Constants;
import com.trtc.uikit.livekit.common.utils.LiveKitLog;
import com.trtc.uikit.livekit.common.view.BasicView;
import com.trtc.uikit.livekit.manager.LiveController;
import com.trtc.uikit.livekit.state.LiveDefine;
import com.trtc.uikit.livekit.state.operation.SeatState;
import com.trtc.uikit.livekit.view.liveroom.view.audience.component.livestreaming.AudienceLivingView;
import com.trtc.uikit.livekit.view.liveroom.view.audience.component.video.AudienceVideoView;

@SuppressLint("ViewConstructor")
public class AudienceView extends BasicView {

    private RelativeLayout     mLayoutAudienceVideoContainer;
    private RelativeLayout     mLayoutAudienceDashboardViewContainer;
    private RelativeLayout     mLayoutAudienceLivingContainer;
    private AudienceLivingView mAudienceLivingView;

    private final Observer<LiveDefine.LiveStatus> mAnchorStatusChangeObserver = this::onAnchorStatusChange;

    public AudienceView(@NonNull Context context, LiveController liveController) {
        super(context, liveController);
        mContext = context;
    }

    @Override
    protected void initView() {
        LayoutInflater.from(mContext).inflate(R.layout.livekit_audience_view, this, true);
        bindViewId();

        initAudienceLivingView();
        initAudienceMaskView();
    }


    @Override
    protected void addObserver() {
        mViewState.liveStatus.observe(mAnchorStatusChangeObserver);
    }

    @Override
    protected void removeObserver() {
        mViewState.liveStatus.removeObserver(mAnchorStatusChangeObserver);
    }

    @Override
    protected void onAttachedToWindow() {
        LiveKitLog.info("AudienceView attached to window");
        super.onAttachedToWindow();
        Constants.DATA_REPORT_COMPONENT = DATA_REPORT_COMPONENT_LIVE_ROOM;
        mLiveController.getRoomController().join(mRoomState.roomId);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        LiveKitLog.info("AudienceView detached to window");
        mLiveController.getVideoViewFactory().clearBySeatList(mSeatState.seatList.get());
        mLiveController.getState().reset();
    }

    public void updateStatus(AudienceViewStatus status) {
        switch (status) {
            case CREATE:
                create();
                break;
            case START_DISPLAY:
                startDisPlay();
                break;
            case DISPLAY_COMPLETE:
                displayComplete();
                break;
            case END_DISPLAY:
                endDisplay();
                break;
            case DESTROY:
                destroy();
                break;
            default:
                break;
        }
    }

    private void create() {
    }

    private void startDisPlay() {
        mUserController.muteAllRemoteAudio(true);
    }

    private void displayComplete() {
        mUserController.muteAllRemoteAudio(false);
        mAudienceLivingView.setVisibility(VISIBLE);
    }

    private void endDisplay() {
        mUserController.muteAllRemoteAudio(true);
        mAudienceLivingView.setVisibility(GONE);
    }

    private void destroy() {
        mLiveController.getRoomController().exit();
        removeAllViews();
        mLiveController.getVideoViewFactory().removeVideoViewByUserId(mRoomState.ownerInfo.userId);
        mLiveController.getVideoViewFactory().removeVideoViewByUserId(mUserState.selfInfo.userId);
        if (!mSeatState.seatList.get().isEmpty()) {
            for (SeatState.SeatInfo seatInfo : mSeatState.seatList.get()) {
                mLiveController.getVideoViewFactory().removeVideoViewByUserId(seatInfo.userId.get());
            }
        }
    }

    private void bindViewId() {
        mLayoutAudienceVideoContainer = findViewById(R.id.rl_audience_video_view);
        mLayoutAudienceDashboardViewContainer = findViewById(R.id.rl_audience_mask_container);
        mLayoutAudienceLivingContainer = findViewById(R.id.rl_audience_living);
    }

    private void initAudienceLivingView() {
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT);
        mAudienceLivingView = new AudienceLivingView(mContext, mLiveController);
        mLayoutAudienceLivingContainer.addView(mAudienceLivingView, layoutParams);
        mAudienceLivingView.setVisibility(GONE);
    }

    private void initAudienceMaskView() {
        AudienceDashboardView mAudienceEndView = new AudienceDashboardView(mContext, mLiveController);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT);
        mLayoutAudienceDashboardViewContainer.addView(mAudienceEndView, layoutParams);
        mLayoutAudienceDashboardViewContainer.setVisibility(GONE);
    }

    private void initAudienceVideoView() {
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT);
        AudienceVideoView mAudienceVideoView = new AudienceVideoView(mContext, mLiveController);
        mLayoutAudienceVideoContainer.addView(mAudienceVideoView, layoutParams);
    }

    private void showAudienceMaskView(boolean isShow) {
        if (isShow) {
            mLayoutAudienceDashboardViewContainer.setVisibility(VISIBLE);
        } else {
            mLayoutAudienceDashboardViewContainer.setVisibility(GONE);
        }
    }

    private void onAnchorStatusChange(LiveDefine.LiveStatus liveStatus) {
        showAudienceMaskView(liveStatus == LiveDefine.LiveStatus.DASHBOARD);
        if (liveStatus == LiveDefine.LiveStatus.PLAYING) {
            initAudienceVideoView();
        }
    }

    public enum AudienceViewStatus {
        CREATE,
        START_DISPLAY,
        DISPLAY_COMPLETE,
        END_DISPLAY,
        DESTROY,
    }
}
