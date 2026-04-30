package aiss.peertubeiner.service;

import aiss.peertubeiner.dto.*;
import aiss.peertubeiner.model.PeerTubeVideo;
import aiss.peertubeiner.model.PeerTubeVideoResponse;
import aiss.peertubeiner.model.PeerTubeCommentThreadResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class VideoMinerIntegrationService {
    @Autowired
    private PeerTubeService peerTubeService;
    private final RestTemplate restTemplate = new RestTemplate();
    
    @Value("${videominer.base-url}")
    private String videoMinerApiBase;

    public Map<String, Object> fetchAndStoreChannelData(String channelHandle, int maxVideos, int maxComments) {
        Map<String, Object> result = new HashMap<>();
        try {
            PeerTubeVideoResponse videoResponse = peerTubeService.getVideosByChannel(channelHandle, maxVideos);
            if (videoResponse == null || videoResponse.getVideos() == null) {
                result.put("status", "error"); result.put("message", "Failed to fetch videos from PeerTube"); return result;
            }

            VMChannel channel = new VMChannel();
            channel.setId(channelHandle); channel.setName(channelHandle);
            channel.setDescription("Channel from PeerTube: " + channelHandle);
            channel.setCreatedTime(new Date().toString());
            
            List<VMVideo> videosList = new ArrayList<>();
            List<String> storedVideosIds = new ArrayList<>();
            
            for (PeerTubeVideo pVideo : videoResponse.getVideos()) {
                VMVideo video = new VMVideo();
                video.setId(pVideo.getUuid()); video.setName(pVideo.getName());
                video.setDescription(pVideo.getDescription()); video.setReleaseTime(pVideo.getPublishedAt());

                if (pVideo.getAccount() != null) {
                    VMUser user = new VMUser();
                    user.setId(pVideo.getAccount().getId().toString()); user.setName(pVideo.getAccount().getName());
                    user.setUser_link(pVideo.getAccount().getUrl());
                    if (pVideo.getAccount().getAvatar() != null) { user.setPicture_link(pVideo.getAccount().getAvatar().getUrl()); }
                    video.setUser(user);
                }

                List<VMComment> commentsList = new ArrayList<>();
                PeerTubeCommentThreadResponse commentResponse = peerTubeService.getCommentsByVideo(pVideo.getUuid(), maxComments);
                if (commentResponse != null && commentResponse.getThreads() != null) {
                    for (PeerTubeCommentThreadResponse.CommentThread thread : commentResponse.getThreads()) {
                        VMComment comment = new VMComment();
                        comment.setId(thread.getId().toString()); comment.setText(thread.getText());
                        comment.setCreatedOn(thread.getCreatedAt()); commentsList.add(comment);
                    }
                }
                video.setComments(commentsList);

                List<VMCaption> captionsList = new ArrayList<>();
                if (pVideo.getCaptions() != null && pVideo.getCaptions().getData() != null) {
                    for (PeerTubeVideo.Caption pCaption : pVideo.getCaptions().getData()) {
                        VMCaption caption = new VMCaption();
                        caption.setId(UUID.randomUUID().toString()); caption.setLink(pCaption.getFileUrl());
                        if (pCaption.getLanguage() != null) { caption.setLanguage(pCaption.getLanguage().getId()); }
                        captionsList.add(caption);
                    }
                }
                video.setCaptions(captionsList);

                videosList.add(video); storedVideosIds.add(video.getId());
            }

            channel.setVideos(videosList);
            restTemplate.postForObject(videoMinerApiBase + "/channels", channel, VMChannel.class);

            result.put("status", "success"); result.put("message", "Data imported successfully from PeerTube");
            result.put("videosImported", storedVideosIds.size()); result.put("channelId", channelHandle);
        } catch (Exception e) {
            result.put("status", "error"); result.put("message", "Error: " + e.getMessage());
        }
        return result;
    }
}