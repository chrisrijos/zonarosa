/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.core.util.concurrent

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.subjects.BehaviorSubject

/**
 * An Observer that provides instant access to the latest emitted value.
 * Basically a read-only version of [BehaviorSubject].
 */
class LatestValueObservable<T : Any>(private val subject: BehaviorSubject<T>) : Observable<T>() {
  val value: T?
    get() = subject.value

  override fun subscribeActual(observer: Observer<in T>) {
    subject.subscribe(observer)
  }
}
