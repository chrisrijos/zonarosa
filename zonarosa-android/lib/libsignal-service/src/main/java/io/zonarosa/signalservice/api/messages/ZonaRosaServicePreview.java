package io.zonarosa.service.api.messages;


import java.util.Optional;

public class ZonaRosaServicePreview {
  private final String                            url;
  private final String                            title;
  private final String                            description;
  private final long                              date;
  private final Optional<ZonaRosaServiceAttachment> image;

  public ZonaRosaServicePreview(String url, String title, String description, long date, Optional<ZonaRosaServiceAttachment> image) {
    this.url         = url;
    this.title       = title;
    this.description = description;
    this.date        = date;
    this.image       = image;
  }

  public String getUrl() {
    return url;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public long getDate() {
    return date;
  }

  public Optional<ZonaRosaServiceAttachment> getImage() {
    return image;
  }
}
