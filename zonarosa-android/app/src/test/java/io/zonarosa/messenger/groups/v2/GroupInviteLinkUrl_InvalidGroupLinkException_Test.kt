package io.zonarosa.messenger.groups.v2

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import assertk.assertions.messageContains
import assertk.assertions.rootCause
import okio.ByteString
import org.junit.Test
import io.zonarosa.core.util.Base64.encodeUrlSafeWithoutPadding
import io.zonarosa.core.util.Util
import io.zonarosa.libzonarosa.zkgroup.InvalidInputException
import io.zonarosa.storageservice.storage.protos.groups.GroupInviteLink
import io.zonarosa.messenger.groups.v2.GroupInviteLinkUrl.InvalidGroupLinkException
import io.zonarosa.messenger.groups.v2.GroupInviteLinkUrl.UnknownGroupLinkVersionException
import java.io.IOException

@Suppress("ClassName")
class GroupInviteLinkUrl_InvalidGroupLinkException_Test {
  @Test
  fun empty_string() {
    val uri = ""
    assertThat(GroupInviteLinkUrl.fromUri(uri)).isNull()
  }

  @Test
  fun not_a_url_string() {
    val uri = "abc"
    assertThat(GroupInviteLinkUrl.fromUri(uri)).isNull()
  }

  @Test
  fun wrong_host() {
    val uri = "https://x.zonarosa.io/#CjQKIAD34MKnGrBkzDztTATwjXt-9LhLLCIG9pgzvmz-NN-AEhCbwyTuxDfP2mrluK779H7o"
    assertThat(GroupInviteLinkUrl.fromUri(uri)).isNull()
  }

  @Test
  fun wrong_scheme() {
    val uri = "http://zonarosa.group/#CjQKIAD34MKnGrBkzDztTATwjXt-9LhLLCIG9pgzvmz-NN-AEhCbwyTuxDfP2mrluK779H7o"
    assertThat(GroupInviteLinkUrl.fromUri(uri)).isNull()
  }

  @Test
  fun has_path() {
    val uri = "https://zonarosa.group/not_expected/#CjQKIAD34MKnGrBkzDztTATwjXt-9LhLLCIG9pgzvmz-NN-AEhCbwyTuxDfP2mrluK779H7o"
    assertFailure { GroupInviteLinkUrl.fromUri(uri) }
      .isInstanceOf<InvalidGroupLinkException>()
      .hasMessage("No path was expected in uri")
  }

  @Test
  fun missing_ref() {
    val uri = "https://zonarosa.group/"
    assertFailure { GroupInviteLinkUrl.fromUri(uri) }
      .isInstanceOf<InvalidGroupLinkException>()
      .hasMessage("No reference was in the uri")
  }

  @Test
  fun empty_ref() {
    val uri = "https://zonarosa.group/#"
    assertFailure { GroupInviteLinkUrl.fromUri(uri) }
      .isInstanceOf<InvalidGroupLinkException>()
      .hasMessage("No reference was in the uri")
  }

  @Test
  fun bad_base64() {
    val uri = "https://zonarosa.group/#CAESNAogpQEzURH6BON1bCS264cmTi37Yi6HTOReXZUEHdsBIgSEPCLfiL7k4wCX;mwVi31USVY"
    assertFailure { GroupInviteLinkUrl.fromUri(uri) }
      .isInstanceOf<InvalidGroupLinkException>()
      .rootCause()
      .isInstanceOf<IOException>()
  }

  @Test
  fun bad_protobuf() {
    val uri = "https://zonarosa.group/#CAESNAogpQEzURH6BON1bCS264cmTi37Yi6HTOReXZUEHdsBIgSEPCLfiL7k4wCXmwVi31USVY"
    assertFailure {
      GroupInviteLinkUrl.fromUri(uri)
    }.isInstanceOf<InvalidGroupLinkException>()
      .rootCause()
      .isInstanceOf<IllegalStateException>()
  }

  @Test
  fun version_999_url() {
    val url = "https://zonarosa.group/#uj4zCiDMSxlNUvF4bQ3z3fYzGyZTFbJ1xEqWbPE3uZSD8bjOrxIP8NxV-0GUz3jpxMLR1rN3"
    assertFailure { GroupInviteLinkUrl.fromUri(url) }
      .isInstanceOf<UnknownGroupLinkVersionException>()
      .messageContains("Url contains no known group link content")
  }

  @Test
  fun bad_master_key_length() {
    val masterKeyBytes = Util.getSecretBytes(33)
    val password = GroupLinkPassword.createNew()

    val encoding = createEncodedProtobuf(masterKeyBytes, password.serialize())

    val url = "https://zonarosa.group/#$encoding"

    assertFailure { GroupInviteLinkUrl.fromUri(url) }
      .isInstanceOf<InvalidGroupLinkException>()
      .rootCause()
      .isInstanceOf<InvalidInputException>()
  }

  companion object {
    private fun createEncodedProtobuf(
      groupMasterKey: ByteArray,
      passwordBytes: ByteArray
    ): String {
      return encodeUrlSafeWithoutPadding(
        GroupInviteLink.Builder()
          .contentsV1(
            GroupInviteLink.GroupInviteLinkContentsV1.Builder()
              .groupMasterKey(ByteString.of(*groupMasterKey))
              .inviteLinkPassword(ByteString.of(*passwordBytes))
              .build()
          )
          .build()
          .encode()
      )
    }
  }
}
