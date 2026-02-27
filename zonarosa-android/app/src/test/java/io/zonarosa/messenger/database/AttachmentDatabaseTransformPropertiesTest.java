package io.zonarosa.messenger.database;

import org.junit.Test;
import io.zonarosa.core.models.media.TransformProperties;
import io.zonarosa.messenger.mms.SentMediaQuality;

import static org.junit.Assert.assertEquals;
import static io.zonarosa.messenger.database.TransformPropertiesUtilKt.parseTransformProperties;
import static io.zonarosa.messenger.database.TransformPropertiesUtilKt.serialize;

public class AttachmentDatabaseTransformPropertiesTest {

  @Test
  public void transformProperties_verifyStructure() {
    TransformProperties properties = TransformProperties.empty();
    assertEquals("Added transform property, need to confirm default behavior for pre-existing payloads in database",
                 "{\"skipTransform\":false,\"videoTrim\":false,\"videoTrimStartTimeUs\":0,\"videoTrimEndTimeUs\":0,\"sentMediaQuality\":0,\"mp4Faststart\":false,\"videoEdited\":false}",
                 serialize(properties));
  }

  @Test
  public void transformProperties_verifyMissingSentMediaQualityDefaultBehavior() {
    String json = "{\"skipTransform\":false,\"videoTrim\":false,\"videoTrimStartTimeUs\":0,\"videoTrimEndTimeUs\":0,\"videoEdited\":false,\"mp4Faststart\":false}";

    TransformProperties properties = parseTransformProperties(json);

    assertEquals(0, properties.sentMediaQuality);
    assertEquals(SentMediaQuality.STANDARD, SentMediaQuality.fromCode(properties.sentMediaQuality));
  }

}
