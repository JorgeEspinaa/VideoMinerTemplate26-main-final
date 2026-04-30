package aiss.peertubeiner.service;

import aiss.peertubeiner.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class PeerTubeService {

    private final RestTemplate restTemplate = new RestTemplate();
    
    @Value("${peertube.base-url}")
    private String peertubeApiBase;

    public PeerTubeVideoResponse getVideosByChannel(String channelHandle, int maxVideos) {
        String url = peertubeApiBase + "/video-channels/" + channelHandle + "/videos?count=" + maxVideos;
        try {
            return restTemplate.getForObject(url, PeerTubeVideoResponse.class);
        } catch (Exception e) {
            System.err.println("Error fetching videos from PeerTube: " + e.getMessage());
            return null;
        }
    }

    public PeerTubeCommentThreadResponse getCommentsByVideo(String videoUuid, int maxComments) {
        String url = peertubeApiBase + "/videos/" + videoUuid + "/comment-threads?count=" + maxComments;
        try {
            return restTemplate.getForObject(url, PeerTubeCommentThreadResponse.class);
        } catch (Exception e) {
            System.err.println("Error fetching comments from PeerTube: " + e.getMessage());
            return null;
        }
    }

    public PeerTubeVideo.CaptionResponse getCaptionsByVideo(String videoUuid) {
        String url = peertubeApiBase + "/videos/" + videoUuid + "/captions";
        try {
            return restTemplate.getForObject(url, PeerTubeVideo.CaptionResponse.class);
        } catch (Exception e) {
            System.err.println("Error fetching captions from PeerTube: " + e.getMessage());
            return null;
        }
    }
}
