package io.zonarosa.messenger.components;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.SpannableStringBuilder;
import android.util.AttributeSet;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import io.zonarosa.messenger.R;
import io.zonarosa.messenger.components.emoji.SimpleEmojiTextView;
import io.zonarosa.messenger.recipients.Recipient;
import io.zonarosa.messenger.util.ContextUtil;
import io.zonarosa.messenger.util.DrawableUtil;
import io.zonarosa.messenger.util.RemoteConfig;
import io.zonarosa.messenger.util.SpanUtil;
import io.zonarosa.messenger.util.ViewUtil;

public class FromTextView extends SimpleEmojiTextView {

  public FromTextView(Context context) {
    super(context);
  }

  public FromTextView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public void setText(Recipient recipient) {
    setText(recipient, null);
  }

  public void setText(Recipient recipient, @Nullable CharSequence suffix) {
    setText(recipient, recipient.getDisplayName(getContext()), suffix);
  }

  public void setText(Recipient recipient, @Nullable CharSequence fromString, @Nullable CharSequence suffix) {
    setText(recipient, fromString, suffix, true);
  }

  public void setText(Recipient recipient, @Nullable CharSequence fromString, @Nullable CharSequence suffix, boolean asThread) {
    setText(recipient, fromString, suffix, asThread, false);
  }

  public void setText(Recipient recipient, @Nullable CharSequence fromString, @Nullable CharSequence suffix, boolean asThread, boolean showSelfAsYou) {
    setText(recipient, fromString, suffix, asThread, showSelfAsYou, false);
  }

  public void setText(Recipient recipient, @Nullable CharSequence fromString, @Nullable CharSequence suffix, boolean asThread, boolean showSelfAsYou, boolean isPinned) {
    SpannableStringBuilder builder  = new SpannableStringBuilder();

    if (asThread && recipient.isSelf() && showSelfAsYou) {
      builder.append(getContext().getString(R.string.Recipient_you));
    } else if (asThread && recipient.isSelf()) {
      builder.append(getContext().getString(R.string.note_to_self));
    } else {
      builder.append(fromString);
    }

    if (suffix != null) {
      builder.append(suffix);
    }

    if (asThread && recipient.getShowVerified()) {
      Drawable official = ContextUtil.requireDrawable(getContext(), R.drawable.ic_official_20);
      official.setBounds(0, 0, ViewUtil.dpToPx(20), ViewUtil.dpToPx(20));

      builder.append(" ")
             .append(SpanUtil.buildCenteredImageSpan(official));
    }

    if (recipient.isMuted()) {
      builder.append(" ")
             .append(SpanUtil.buildCenteredImageSpan(getMuted()));
    }

    setText(builder);

    if (RemoteConfig.getInlinePinnedChats() && isPinned) {
      setCompoundDrawablesRelativeWithIntrinsicBounds(getPinned(), null, null, null);
    } else {
      setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0);
    }
  }

  private Drawable getMuted() {
    return getDrawable(R.drawable.ic_bell_disabled_16, R.color.zonarosa_icon_tint_secondary);
  }

  private Drawable getPinned() {
    return getDrawable(R.drawable.symbol_pin_16, io.zonarosa.core.ui.R.color.zonarosa_colorOnSurface);
  }

  private Drawable getDrawable(@DrawableRes int drawable, int colorRes) {
    Drawable mutedDrawable = ContextUtil.requireDrawable(getContext(), drawable);
    mutedDrawable.setBounds(0, 0, ViewUtil.dpToPx(16), ViewUtil.dpToPx(16));
    DrawableUtil.tint(mutedDrawable, ContextCompat.getColor(getContext(), colorRes));
    return mutedDrawable;
  }
}
