package com.bluebillywig.sapi.types;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Writable playout properties. Unset (null) properties are omitted from the request. */
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE)
public final class PlayoutProps {
    /** Name. */
    @JsonProperty("name")
    private String name;

    /** Label. */
    @JsonProperty("label")
    private String label;

    /** Status. */
    @JsonProperty("status")
    private String status;

    /** Center button type. */
    @JsonProperty("centerButtonType")
    private String centerButtonType;

    /** Corner radius. */
    @JsonProperty("cornerRadius")
    private Integer cornerRadius;

    /** Responsive sizing. */
    @JsonProperty("responsiveSizing")
    private Boolean responsiveSizing;

    /** Width. */
    @JsonProperty("width")
    private Integer width;

    /** Height. */
    @JsonProperty("height")
    private Integer height;

    /** Max width. */
    @JsonProperty("maxWidth")
    private Integer maxWidth;

    /** Auto height. */
    @JsonProperty("autoHeight")
    private Boolean autoHeight;

    /** Aspect ratio. */
    @JsonProperty("aspectRatio")
    private String aspectRatio;

    /** Background color. */
    @JsonProperty("backgroundColor")
    private String backgroundColor;

    /** Foreground color. */
    @JsonProperty("foregroundColor")
    private String foregroundColor;

    /** Widget color. */
    @JsonProperty("widgetColor")
    private String widgetColor;

    /** Bg color. */
    @JsonProperty("bgColor")
    private String bgColor;

    /** Logo ID. */
    @JsonProperty("logoId")
    private Long logoId;

    /** Logo alignment. */
    @JsonProperty("logoAlign")
    private String logoAlign;

    /** Logo click URL. */
    @JsonProperty("logoClickUrl")
    private String logoClickUrl;

    /** Control bar. */
    @JsonProperty("controlBar")
    private Boolean controlBar;

    /** Time display. */
    @JsonProperty("timeDisplay")
    private Boolean timeDisplay;

    /** Time line. */
    @JsonProperty("timeLine")
    private Boolean timeLine;

    /** Mute button. */
    @JsonProperty("muteButton")
    private Boolean muteButton;

    /** Volume. */
    @JsonProperty("volume")
    private Boolean volume;

    /** Full screen. */
    @JsonProperty("fullScreen")
    private Boolean fullScreen;

    /** Auto play. */
    @JsonProperty("autoPlay")
    private Boolean autoPlay;

    /** Auto loop. */
    @JsonProperty("autoLoop")
    private Boolean autoLoop;

    /** Auto mute. */
    @JsonProperty("autoMute")
    private Boolean autoMute;

    /** Title. */
    @JsonProperty("title")
    private Boolean title;

    /** Fit mode. */
    @JsonProperty("fitmode")
    private String fitmode;

    /** Commercials. */
    @JsonProperty("commercials")
    private List<JsonNode> commercials;

    /** Any additional properties to send verbatim. */
    private Map<String, Object> additionalProperties;

    /** @return name */
    public String getName() {
        return name;
    }

    /** @param name name @return this */
    public PlayoutProps setName(String name) {
        this.name = name;
        return this;
    }

    /** @return label */
    public String getLabel() {
        return label;
    }

    /** @param label label @return this */
    public PlayoutProps setLabel(String label) {
        this.label = label;
        return this;
    }

    /** @return status */
    public String getStatus() {
        return status;
    }

    /** @param status status @return this */
    public PlayoutProps setStatus(String status) {
        this.status = status;
        return this;
    }

    /** @return center button type */
    public String getCenterButtonType() {
        return centerButtonType;
    }

    /** @param centerButtonType center button type @return this */
    public PlayoutProps setCenterButtonType(String centerButtonType) {
        this.centerButtonType = centerButtonType;
        return this;
    }

    /** @return corner radius */
    public Integer getCornerRadius() {
        return cornerRadius;
    }

    /** @param cornerRadius corner radius @return this */
    public PlayoutProps setCornerRadius(Integer cornerRadius) {
        this.cornerRadius = cornerRadius;
        return this;
    }

    /** @return responsive sizing */
    public Boolean getResponsiveSizing() {
        return responsiveSizing;
    }

    /** @param responsiveSizing responsive sizing @return this */
    public PlayoutProps setResponsiveSizing(Boolean responsiveSizing) {
        this.responsiveSizing = responsiveSizing;
        return this;
    }

    /** @return width */
    public Integer getWidth() {
        return width;
    }

    /** @param width width @return this */
    public PlayoutProps setWidth(Integer width) {
        this.width = width;
        return this;
    }

    /** @return height */
    public Integer getHeight() {
        return height;
    }

    /** @param height height @return this */
    public PlayoutProps setHeight(Integer height) {
        this.height = height;
        return this;
    }

    /** @return max width */
    public Integer getMaxWidth() {
        return maxWidth;
    }

    /** @param maxWidth max width @return this */
    public PlayoutProps setMaxWidth(Integer maxWidth) {
        this.maxWidth = maxWidth;
        return this;
    }

    /** @return auto height */
    public Boolean getAutoHeight() {
        return autoHeight;
    }

    /** @param autoHeight auto height @return this */
    public PlayoutProps setAutoHeight(Boolean autoHeight) {
        this.autoHeight = autoHeight;
        return this;
    }

    /** @return aspect ratio */
    public String getAspectRatio() {
        return aspectRatio;
    }

    /** @param aspectRatio aspect ratio @return this */
    public PlayoutProps setAspectRatio(String aspectRatio) {
        this.aspectRatio = aspectRatio;
        return this;
    }

    /** @return background color */
    public String getBackgroundColor() {
        return backgroundColor;
    }

    /** @param backgroundColor background color @return this */
    public PlayoutProps setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
        return this;
    }

    /** @return foreground color */
    public String getForegroundColor() {
        return foregroundColor;
    }

    /** @param foregroundColor foreground color @return this */
    public PlayoutProps setForegroundColor(String foregroundColor) {
        this.foregroundColor = foregroundColor;
        return this;
    }

    /** @return widget color */
    public String getWidgetColor() {
        return widgetColor;
    }

    /** @param widgetColor widget color @return this */
    public PlayoutProps setWidgetColor(String widgetColor) {
        this.widgetColor = widgetColor;
        return this;
    }

    /** @return bg color */
    public String getBgColor() {
        return bgColor;
    }

    /** @param bgColor bg color @return this */
    public PlayoutProps setBgColor(String bgColor) {
        this.bgColor = bgColor;
        return this;
    }

    /** @return logo ID */
    public Long getLogoId() {
        return logoId;
    }

    /** @param logoId logo ID @return this */
    public PlayoutProps setLogoId(Long logoId) {
        this.logoId = logoId;
        return this;
    }

    /** @param logoId logo ID @return this */
    public PlayoutProps setLogoId(long logoId) {
        this.logoId = logoId;
        return this;
    }

    /** @return logo alignment */
    public String getLogoAlign() {
        return logoAlign;
    }

    /** @param logoAlign logo alignment @return this */
    public PlayoutProps setLogoAlign(String logoAlign) {
        this.logoAlign = logoAlign;
        return this;
    }

    /** @return logo click URL */
    public String getLogoClickUrl() {
        return logoClickUrl;
    }

    /** @param logoClickUrl logo click URL @return this */
    public PlayoutProps setLogoClickUrl(String logoClickUrl) {
        this.logoClickUrl = logoClickUrl;
        return this;
    }

    /** @return control bar */
    public Boolean getControlBar() {
        return controlBar;
    }

    /** @param controlBar control bar @return this */
    public PlayoutProps setControlBar(Boolean controlBar) {
        this.controlBar = controlBar;
        return this;
    }

    /** @return time display */
    public Boolean getTimeDisplay() {
        return timeDisplay;
    }

    /** @param timeDisplay time display @return this */
    public PlayoutProps setTimeDisplay(Boolean timeDisplay) {
        this.timeDisplay = timeDisplay;
        return this;
    }

    /** @return time line */
    public Boolean getTimeLine() {
        return timeLine;
    }

    /** @param timeLine time line @return this */
    public PlayoutProps setTimeLine(Boolean timeLine) {
        this.timeLine = timeLine;
        return this;
    }

    /** @return mute button */
    public Boolean getMuteButton() {
        return muteButton;
    }

    /** @param muteButton mute button @return this */
    public PlayoutProps setMuteButton(Boolean muteButton) {
        this.muteButton = muteButton;
        return this;
    }

    /** @return volume */
    public Boolean getVolume() {
        return volume;
    }

    /** @param volume volume @return this */
    public PlayoutProps setVolume(Boolean volume) {
        this.volume = volume;
        return this;
    }

    /** @return full screen */
    public Boolean getFullScreen() {
        return fullScreen;
    }

    /** @param fullScreen full screen @return this */
    public PlayoutProps setFullScreen(Boolean fullScreen) {
        this.fullScreen = fullScreen;
        return this;
    }

    /** @return auto play */
    public Boolean getAutoPlay() {
        return autoPlay;
    }

    /** @param autoPlay auto play @return this */
    public PlayoutProps setAutoPlay(Boolean autoPlay) {
        this.autoPlay = autoPlay;
        return this;
    }

    /** @return auto loop */
    public Boolean getAutoLoop() {
        return autoLoop;
    }

    /** @param autoLoop auto loop @return this */
    public PlayoutProps setAutoLoop(Boolean autoLoop) {
        this.autoLoop = autoLoop;
        return this;
    }

    /** @return auto mute */
    public Boolean getAutoMute() {
        return autoMute;
    }

    /** @param autoMute auto mute @return this */
    public PlayoutProps setAutoMute(Boolean autoMute) {
        this.autoMute = autoMute;
        return this;
    }

    /** @return title */
    public Boolean getTitle() {
        return title;
    }

    /** @param title title @return this */
    public PlayoutProps setTitle(Boolean title) {
        this.title = title;
        return this;
    }

    /** @return fit mode */
    public String getFitmode() {
        return fitmode;
    }

    /** @param fitmode fit mode @return this */
    public PlayoutProps setFitmode(String fitmode) {
        this.fitmode = fitmode;
        return this;
    }

    /** @return commercials */
    public List<JsonNode> getCommercials() {
        return commercials;
    }

    /** @param commercials commercials @return this */
    public PlayoutProps setCommercials(List<JsonNode> commercials) {
        this.commercials = commercials;
        return this;
    }

    /**
     * The additional properties, created on first access. Entries are written to and read from
     * the JSON object verbatim, next to the modelled properties.
     *
     * @return the (mutable) additional properties, never {@code null}
     */
    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        if (additionalProperties == null) {
            additionalProperties = new LinkedHashMap<>();
        }
        return additionalProperties;
    }

    /**
     * Replaces the additional properties.
     *
     * @param additionalProperties the properties to send verbatim, or {@code null} for none
     * @return this
     */
    public PlayoutProps setAdditionalProperties(Map<String, Object> additionalProperties) {
        this.additionalProperties = additionalProperties;
        return this;
    }

    /**
     * Adds one additional property (also the Jackson hook for unmodelled properties).
     *
     * @param name the property name
     * @param value the property value
     */
    @JsonAnySetter
    public void addAdditionalProperty(String name, Object value) {
        getAdditionalProperties().put(name, value);
    }
}
