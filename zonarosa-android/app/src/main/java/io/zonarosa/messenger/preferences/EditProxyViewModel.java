package io.zonarosa.messenger.preferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;

import io.zonarosa.core.util.concurrent.ZonaRosaExecutors;
import io.zonarosa.messenger.dependencies.AppDependencies;
import io.zonarosa.messenger.keyvalue.ZonaRosaStore;
import io.zonarosa.messenger.util.ZonaRosaProxyUtil;
import io.zonarosa.core.util.Util;
import io.zonarosa.service.api.websocket.WebSocketConnectionState;
import io.zonarosa.service.internal.configuration.ZonaRosaProxy;

import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import io.reactivex.rxjava3.subjects.PublishSubject;

public class EditProxyViewModel extends ViewModel {

  private final PublishSubject<Event>              events;
  private final BehaviorSubject<UiState>           uiState;
  private final BehaviorSubject<SaveState>         saveState;
  private final Flowable<WebSocketConnectionState> pipeState;

  public EditProxyViewModel() {
    this.events    = PublishSubject.create();
    this.uiState   = BehaviorSubject.create();
    this.saveState = BehaviorSubject.createDefault(SaveState.IDLE);
    this.pipeState = ZonaRosaStore.account().getE164() == null ? Flowable.empty()
                                                             : AppDependencies.getWebSocketObserver()
                                                                              .toFlowable(BackpressureStrategy.LATEST);

    if (ZonaRosaStore.proxy().isProxyEnabled()) {
      uiState.onNext(UiState.ALL_ENABLED);
    } else {
      uiState.onNext(UiState.ALL_DISABLED);
    }
  }

  void onToggleProxy(boolean enabled, String text) {
    if (enabled) {
      ZonaRosaProxy currentProxy = ZonaRosaStore.proxy().getProxy();

      if (currentProxy != null && !Util.isEmpty(currentProxy.getHost())) {
        ZonaRosaProxyUtil.enableProxy(currentProxy);
      }
      uiState.onNext(UiState.ALL_ENABLED);
    } else if (Util.isEmpty(text)) {
        ZonaRosaProxyUtil.disableAndClearProxy();
        uiState.onNext(UiState.ALL_DISABLED);
    } else {
        ZonaRosaProxyUtil.disableProxy();
        uiState.onNext(UiState.ALL_DISABLED);
    }
  }

  public void onSaveClicked(@NonNull String host) {
    String trueHost = ZonaRosaProxyUtil.convertUserEnteredAddressToHost(host);

    saveState.onNext(SaveState.IN_PROGRESS);

    ZonaRosaExecutors.BOUNDED.execute(() -> {
      ZonaRosaProxyUtil.enableProxy(new ZonaRosaProxy(trueHost, 443));

      boolean success = ZonaRosaProxyUtil.testWebsocketConnection(TimeUnit.SECONDS.toMillis(10));

      if (success) {
        events.onNext(Event.PROXY_SUCCESS);
      } else {
        ZonaRosaProxyUtil.disableProxy();
        events.onNext(Event.PROXY_FAILURE);
      }

      saveState.onNext(SaveState.IDLE);
    });
  }

  @NonNull Observable<UiState> getUiState() {
    return uiState.observeOn(AndroidSchedulers.mainThread());
  }

  public @NonNull Observable<Event> getEvents() {
    return events.observeOn(AndroidSchedulers.mainThread());
  }

  @NonNull Flowable<WebSocketConnectionState> getProxyState() {
    return pipeState.observeOn(AndroidSchedulers.mainThread());
  }

  public @NonNull Observable<SaveState> getSaveState() {
    return saveState.observeOn(AndroidSchedulers.mainThread());
  }

  enum UiState {
    ALL_DISABLED, ALL_ENABLED
  }

  public enum Event {
    PROXY_SUCCESS, PROXY_FAILURE
  }

  public enum SaveState {
    IDLE, IN_PROGRESS
  }
}
